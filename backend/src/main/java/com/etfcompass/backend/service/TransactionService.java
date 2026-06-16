package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.InvestmentTransaction;
import com.etfcompass.backend.dto.portfolio.TransactionRequest;
import com.etfcompass.backend.dto.portfolio.TransactionResponse;
import com.etfcompass.backend.exception.NotFoundException;
import com.etfcompass.backend.repository.InvestmentTransactionRepository;
import com.etfcompass.backend.repository.PositionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final PositionRepository positionRepository;
  private final InvestmentTransactionRepository transactionRepository;

  @Transactional(readOnly = true)
  public List<TransactionResponse> listForPortfolio(String email, UUID portfolioId) {
    return transactionRepository.findByPosition_Portfolio_IdOrderByTransactionDateAsc(portfolioId).stream()
        .filter(transaction -> transaction.getPosition().getPortfolio().getUser().getEmail().equalsIgnoreCase(email))
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public TransactionResponse add(String email, UUID positionId, TransactionRequest request) {
    var position = positionRepository.findByIdAndPortfolio_User_EmailIgnoreCase(positionId, email)
        .orElseThrow(() -> new NotFoundException("Position not found"));
    var transaction = new InvestmentTransaction();
    transaction.setPosition(position);
    transaction.setType(request.type());
    transaction.setTransactionDate(request.transactionDate());
    transaction.setQuantity(request.quantity() == null ? BigDecimal.ZERO : request.quantity());
    transaction.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
    transaction.setFees(request.fees() == null ? BigDecimal.ZERO : request.fees());
    transaction.setCurrency(request.currency() == null ? position.getCurrency() : request.currency());
    return toResponse(transactionRepository.save(transaction));
  }

  private TransactionResponse toResponse(InvestmentTransaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getPosition().getId(),
        transaction.getPosition().getEtf().getTicker(),
        transaction.getType(),
        transaction.getTransactionDate(),
        transaction.getQuantity(),
        transaction.getPrice(),
        transaction.getFees(),
        transaction.getCurrency()
    );
  }
}
