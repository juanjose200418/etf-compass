package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.Broker;
import com.etfcompass.backend.domain.ImportJob;
import com.etfcompass.backend.domain.ImportStatus;
import com.etfcompass.backend.domain.Position;
import com.etfcompass.backend.dto.imports.ImportJobResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.etfcompass.backend.exception.NotFoundException;
import com.etfcompass.backend.repository.AppUserRepository;
import com.etfcompass.backend.repository.ImportJobRepository;
import com.etfcompass.backend.repository.PositionRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportService {

  private final AppUserRepository userRepository;
  private final ImportJobRepository importJobRepository;
  private final PortfolioService portfolioService;
  private final EtfService etfService;
  private final PositionRepository positionRepository;

  @Transactional(readOnly = true)
  public List<ImportJobResponse> list(String email) {
    return importJobRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(email).stream().map(this::toResponse).toList();
  }

  @Transactional
  public ImportJobResponse importCsv(String email, UUID portfolioId, Broker broker, MultipartFile file) {
    if (file.isEmpty()) {
      throw new BadRequestException("Import file is empty");
    }
    var user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new NotFoundException("User not found"));
    var portfolio = portfolioService.findOwned(email, portfolioId);
    var job = new ImportJob();
    job.setUser(user);
    job.setPortfolio(portfolio);
    job.setBroker(broker == null ? Broker.MANUAL : broker);
    job.setFileName(file.getOriginalFilename() == null ? "portfolio.csv" : file.getOriginalFilename());
    importJobRepository.save(job);

    try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        throw new BadRequestException("CSV file has no header row");
      }
      Map<String, Integer> headers = headers(headerLine);
      int imported = 0;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String[] values = splitCsv(line);
        String ticker = value(headers, values, "ticker", "symbol", "isin");
        if (ticker.isBlank()) {
          continue;
        }
        String currency = optional(headers, values, "currency");
        var etf = etfService.getOrCreateMinimal(ticker, optional(headers, values, "name", "security", "instrument"), currency);
        var position = new Position();
        position.setPortfolio(portfolio);
        position.setEtf(etf);
        position.setBroker(job.getBroker());
        position.setExternalSymbol(ticker);
        position.setQuantity(decimal(value(headers, values, "quantity", "shares", "units")));
        position.setAverageCost(decimal(value(headers, values, "averagecost", "avgprice", "averageprice", "buyprice")));
        position.setCurrentPrice(decimal(optional(headers, values, "currentprice", "marketprice", "price"), position.getAverageCost()));
        position.setCurrency(currency.isBlank() ? portfolio.getBaseCurrency() : currency.toUpperCase());
        positionRepository.save(position);
        imported++;
      }
      job.setImportedPositions(imported);
      job.setStatus(ImportStatus.COMPLETED);
    } catch (Exception ex) {
      job.setStatus(ImportStatus.FAILED);
      job.setErrorMessage(ex.getMessage());
    }
    return toResponse(job);
  }

  private Map<String, Integer> headers(String headerLine) {
    String[] headers = splitCsv(headerLine);
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      index.put(normalize(headers[i]), i);
    }
    return index;
  }

  private String value(Map<String, Integer> headers, String[] values, String... names) {
    String value = optional(headers, values, names);
    if (value.isBlank()) {
      throw new BadRequestException("Missing required CSV column: " + names[0]);
    }
    return value;
  }

  private String optional(Map<String, Integer> headers, String[] values, String... names) {
    for (String name : names) {
      Integer index = headers.get(normalize(name));
      if (index != null && index < values.length) {
        return values[index].replace("\"", "").trim();
      }
    }
    return "";
  }

  private BigDecimal decimal(String value) {
    return decimal(value, null);
  }

  private BigDecimal decimal(String value, BigDecimal fallback) {
    if (value == null || value.isBlank()) {
      if (fallback != null) {
        return fallback;
      }
      throw new BadRequestException("Missing numeric CSV value");
    }
    return new BigDecimal(value.replace("€", "").replace("$", "").replace(",", ".").trim());
  }

  private String[] splitCsv(String line) {
    return Arrays.stream(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)).map(String::trim).toArray(String[]::new);
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private ImportJobResponse toResponse(ImportJob job) {
    return new ImportJobResponse(job.getId(), job.getPortfolio().getId(), job.getBroker(), job.getStatus(), job.getFileName(), job.getImportedPositions(), job.getErrorMessage(), job.getCreatedAt());
  }
}
