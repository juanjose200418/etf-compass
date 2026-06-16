package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Broker;
import com.etfcompass.backend.domain.Portfolio;
import com.etfcompass.backend.domain.Position;
import com.etfcompass.backend.dto.portfolio.CreatePortfolioRequest;
import com.etfcompass.backend.dto.portfolio.PortfolioResponse;
import com.etfcompass.backend.dto.portfolio.PositionRequest;
import com.etfcompass.backend.dto.portfolio.PositionResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.etfcompass.backend.exception.NotFoundException;
import com.etfcompass.backend.repository.AppUserRepository;
import com.etfcompass.backend.repository.PortfolioRepository;
import com.etfcompass.backend.repository.PositionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final AppUserRepository userRepository;
  private final PortfolioRepository portfolioRepository;
  private final PositionRepository positionRepository;
  private final EtfService etfService;
  private final PositionMapper positionMapper;

  @Transactional
  public PortfolioResponse create(String email, CreatePortfolioRequest request) {
    var user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("User not found"));
    var portfolio = new Portfolio();
    portfolio.setUser(user);
    portfolio.setName(request.name().trim());
    portfolio.setBaseCurrency(request.baseCurrency() == null ? "EUR" : request.baseCurrency());
    return toResponse(portfolioRepository.save(portfolio));
  }

  @Transactional(readOnly = true)
  public List<PortfolioResponse> list(String email) {
    return portfolioRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(email).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public PortfolioResponse get(String email, UUID id) {
    return toResponse(findOwned(email, id));
  }

  @Transactional
  public PortfolioResponse update(String email, UUID id, CreatePortfolioRequest request) {
    var portfolio = findOwned(email, id);
    portfolio.setName(request.name().trim());
    portfolio.setBaseCurrency(request.baseCurrency() == null ? portfolio.getBaseCurrency() : request.baseCurrency());
    return toResponse(portfolio);
  }

  @Transactional
  public void delete(String email, UUID id) {
    portfolioRepository.delete(findOwned(email, id));
  }

  @Transactional
  public PositionResponse addPosition(String email, UUID portfolioId, PositionRequest request) {
    var portfolio = findOwned(email, portfolioId);
    return createPosition(portfolio, request);
  }

  @Transactional
  public List<PositionResponse> addPositions(String email, UUID portfolioId, List<PositionRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new BadRequestException("At least one position is required");
    }

    var portfolio = findOwned(email, portfolioId);
    return requests.stream().map(request -> createPosition(portfolio, request)).toList();
  }

  private PositionResponse createPosition(Portfolio portfolio, PositionRequest request) {
    var etf = etfService.getOrCreateMinimal(request.ticker(), request.ticker(), request.currency());
    var position = new Position();
    position.setPortfolio(portfolio);
    position.setEtf(etf);
    position.setBroker(request.broker() == null ? Broker.MANUAL : request.broker());
    position.setExternalSymbol(request.externalSymbol());
    position.setQuantity(request.quantity());
    position.setAverageCost(request.averageCost());
    position.setCurrentPrice(request.currentPrice());
    position.setCurrency(request.currency() == null ? portfolio.getBaseCurrency() : request.currency());
    return positionMapper.toResponse(positionRepository.save(position));
  }

  @Transactional
  public PositionResponse updatePosition(String email, UUID positionId, PositionRequest request) {
    var position = positionRepository.findByIdAndPortfolio_User_EmailIgnoreCase(positionId, email)
        .orElseThrow(() -> new NotFoundException("Position not found"));
    position.setEtf(etfService.getOrCreateMinimal(request.ticker(), request.ticker(), request.currency()));
    position.setBroker(request.broker() == null ? position.getBroker() : request.broker());
    position.setExternalSymbol(request.externalSymbol());
    position.setQuantity(request.quantity());
    position.setAverageCost(request.averageCost());
    position.setCurrentPrice(request.currentPrice());
    position.setCurrency(request.currency() == null ? position.getCurrency() : request.currency());
    return positionMapper.toResponse(position);
  }

  @Transactional
  public void deletePosition(String email, UUID positionId) {
    var position = positionRepository.findByIdAndPortfolio_User_EmailIgnoreCase(positionId, email)
        .orElseThrow(() -> new NotFoundException("Position not found"));
    positionRepository.delete(position);
  }

  public Portfolio findOwned(String email, UUID portfolioId) {
    return portfolioRepository.findByIdAndUser_EmailIgnoreCase(portfolioId, email)
        .orElseThrow(() -> new NotFoundException("Portfolio not found"));
  }

  private PortfolioResponse toResponse(Portfolio portfolio) {
    List<PositionResponse> positions = positionRepository.findByPortfolio_IdOrderByCreatedAtAsc(portfolio.getId()).stream()
        .map(positionMapper::toResponse).toList();
    BigDecimal totalValue = positions.stream().map(PositionResponse::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalInvested = positions.stream().map(PositionResponse::investedCapital).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PortfolioResponse(portfolio.getId(), portfolio.getName(), portfolio.getBaseCurrency(), totalValue, totalInvested, positions);
  }
}
