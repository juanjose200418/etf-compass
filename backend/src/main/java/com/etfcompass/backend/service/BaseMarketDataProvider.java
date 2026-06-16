package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.DistributionPolicy;
import com.etfcompass.backend.dto.etf.EtfResponse;
import com.etfcompass.backend.dto.etf.PerformanceResponse;
import com.etfcompass.backend.dto.etf.QuoteSnapshotResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.MimeType;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
abstract class BaseMarketDataProvider {

  protected final RestClient.Builder restClientBuilder;
  protected final ObjectMapper objectMapper;

  public abstract String providerName();

  protected JsonNode requestJson(
      RestClient client,
      String path,
      Map<String, String> params,
      String authParam,
      String authValue,
      boolean required,
      List<String> warnings,
      String context
  ) {
    String sanitizedUrl = sanitizedUrl(path, params, authParam);

    try {
      ProviderResponse providerResponse = client.get()
          .uri(uriBuilder -> {
            var builder = uriBuilder.path(path);
            params.forEach(builder::queryParam);
            return builder.queryParam(authParam, authValue).build();
          })
          .exchange((request, clientResponse) -> {
            HttpStatusCode status = clientResponse.getStatusCode();
            String responseBody = StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8);
            String contentType = clientResponse.getHeaders().getContentType() != null ? clientResponse.getHeaders().getContentType().toString() : "";
            log.info("[{}] GET {} -> {}", providerName(), sanitizedUrl, status.value());
            log.info("[{}] {} response: {}", providerName(), context, summarize(responseBody));
            return new ProviderResponse(status, responseBody, contentType);
          });

      if (providerResponse.status().isError()) {
        return handleProviderError(required, warnings, friendlyProviderMessage(context, providerResponse.body()));
      }

      if (!looksLikeJson(providerResponse.body(), providerResponse.contentType())) {
        return handleProviderError(required, warnings, friendlyProviderMessage(context, providerResponse.body()));
      }

      JsonNode json = providerResponse.body() == null || providerResponse.body().isBlank()
          ? objectMapper.nullNode()
          : objectMapper.readTree(providerResponse.body());
      String providerError = firstProviderError(json);
      if (providerError != null) {
        return handleProviderError(required, warnings, friendlyProviderMessage(context, providerError));
      }

      return json;
    } catch (Exception ex) {
      return handleProviderError(required, warnings, providerName() + " " + context + " failed: " + ex.getMessage());
    }
  }

  protected EtfResponse mergeEtfResponses(EtfResponse preferred, EtfResponse fallback) {
    if (preferred == null) return fallback;
    if (fallback == null) return preferred;

    PerformanceResponse preferredPerformance = preferred.performance();
    PerformanceResponse fallbackPerformance = fallback.performance();

    return new EtfResponse(
        preferred.id() != null ? preferred.id() : fallback.id(),
        firstNonBlank(preferred.ticker(), fallback.ticker()),
        firstNonBlank(preferred.name(), fallback.name()),
        firstNonBlank(preferred.isin(), fallback.isin()),
        firstNonBlank(preferred.provider(), fallback.provider()),
        preferred.ter() != null ? preferred.ter() : fallback.ter(),
        firstNonBlank(preferred.assetClass(), fallback.assetClass()),
        firstNonBlank(preferred.region(), fallback.region()),
        firstNonBlank(preferred.indexTracked(), fallback.indexTracked()),
        preferred.distributionPolicy() != null ? preferred.distributionPolicy() : fallback.distributionPolicy(),
        preferred.fundSize() != null ? preferred.fundSize() : fallback.fundSize(),
        firstNonBlank(preferred.currency(), fallback.currency()),
        preferred.riskLevel() != null ? preferred.riskLevel() : fallback.riskLevel(),
        firstNonBlank(preferred.metadataSource(), fallback.metadataSource()),
        preferred.lookThroughCoverage() != null ? preferred.lookThroughCoverage() : fallback.lookThroughCoverage(),
        preferred.metadataUpdatedAt() != null ? preferred.metadataUpdatedAt() : fallback.metadataUpdatedAt(),
        new PerformanceResponse(
            preferredPerformance != null && preferredPerformance.oneYear() != null ? preferredPerformance.oneYear() : fallbackPerformance != null ? fallbackPerformance.oneYear() : null,
            preferredPerformance != null && preferredPerformance.threeYear() != null ? preferredPerformance.threeYear() : fallbackPerformance != null ? fallbackPerformance.threeYear() : null,
            preferredPerformance != null && preferredPerformance.fiveYear() != null ? preferredPerformance.fiveYear() : fallbackPerformance != null ? fallbackPerformance.fiveYear() : null
        ),
        new QuoteSnapshotResponse(null, null, null, null, null, null, null)
    );
  }

  protected String normalizeTicker(String ticker) {
    String normalized = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9.]", "");
    if (normalized.isBlank()) {
      throw new BadRequestException("Ticker is required");
    }
    return normalized;
  }

  protected JsonNode firstObject(JsonNode root) {
    if (root == null || root.isNull()) return objectMapper.nullNode();
    if (root.isArray()) return root.isEmpty() ? objectMapper.nullNode() : root.get(0);
    return root;
  }

  protected JsonNode firstArrayItem(JsonNode root) {
    if (root == null || root.isNull() || !root.isArray() || root.isEmpty()) return objectMapper.nullNode();
    return root.get(0);
  }

  protected JsonNode extractArray(JsonNode root, String... fieldNames) {
    if (root == null || root.isNull()) return objectMapper.nullNode();
    if (root.isArray()) return root;
    for (String fieldName : fieldNames) {
      JsonNode child = root.get(fieldName);
      if (child != null && child.isArray()) return child;
    }
    return objectMapper.nullNode();
  }

  protected String firstText(JsonNode node, String... fields) {
    if (node == null || node.isNull()) return "";
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value != null && !value.isNull()) {
        String text = value.asText("").trim();
        if (!text.isBlank()) return text;
      }
    }
    return "";
  }

  protected String firstTextOrNull(JsonNode node, String... fields) {
    String text = firstText(node, fields);
    return text.isBlank() ? null : text;
  }

  protected BigDecimal firstDecimalOrNull(JsonNode node, String... fields) {
    if (node == null || node.isNull()) return null;
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value == null || value.isNull()) continue;
      try {
        if (value.isNumber()) return value.decimalValue();
        String text = value.asText("").replace("%", "").replace(",", "").trim();
        if (!text.isBlank()) return new BigDecimal(text);
      } catch (NumberFormatException ignored) {
        // Ignore malformed provider values.
      }
    }
    return null;
  }

  protected DistributionPolicy parseDistributionPolicy(String raw) {
    if (raw == null || raw.isBlank()) return null;
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("accum")) return DistributionPolicy.ACCUMULATING;
    if (normalized.contains("distrib") || normalized.contains("income")) return DistributionPolicy.DISTRIBUTING;
    return null;
  }

  protected PriceHistory parseHistory(JsonNode root) {
    JsonNode items = extractArray(root, "historical", "data");
    if (!items.isArray() || items.isEmpty()) return new PriceHistory(List.of());

    List<PricePoint> points = new ArrayList<>();
    for (JsonNode item : items) {
      String dateText = firstText(item, "date");
      BigDecimal close = firstDecimalOrNull(item, "close", "price", "adjClose", "adjustedClose");
      if (dateText.isBlank() || close == null || close.signum() <= 0) continue;
      try {
        points.add(new PricePoint(LocalDate.parse(dateText), close));
      } catch (RuntimeException ignored) {
        // Ignore malformed provider dates.
      }
    }
    points.sort(Comparator.comparing(PricePoint::date));
    return new PriceHistory(points);
  }

  protected BigDecimal calculatePerformance(List<PricePoint> points, int years) {
    if (points.size() < 2) return null;
    PricePoint latest = points.get(points.size() - 1);
    LocalDate targetDate = latest.date().minusYears(years);
    PricePoint closest = points.stream()
        .min(Comparator.comparingLong(point -> Math.abs(ChronoUnit.DAYS.between(point.date(), targetDate))))
        .orElse(null);
    if (closest == null || closest.close().signum() <= 0 || latest.close().signum() <= 0) return null;
    return latest.close()
        .divide(closest.close(), 8, RoundingMode.HALF_UP)
        .subtract(BigDecimal.ONE)
        .multiply(BigDecimal.valueOf(100))
        .setScale(4, RoundingMode.HALF_UP);
  }

  protected String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value.trim();
    }
    return null;
  }

  private JsonNode handleProviderError(boolean required, List<String> warnings, String message) {
    if (required) throw new BadRequestException(message);
    warnings.add(message);
    return objectMapper.nullNode();
  }

  private String sanitizedUrl(String path, Map<String, String> params, String authParam) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
    params.forEach(builder::queryParam);
    builder.queryParam(authParam, "***");
    return builder.toUriString();
  }

  private String summarize(String body) {
    if (body == null || body.isBlank()) return "<empty>";
    String singleLine = body.replaceAll("\\s+", " ").trim();
    return singleLine.length() <= 320 ? singleLine : singleLine.substring(0, 320) + "...";
  }

  private boolean looksLikeJson(String body, String contentType) {
    if (body == null || body.isBlank()) return true;
    String trimmed = body.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[") || "null".equals(trimmed) || "true".equals(trimmed) || "false".equals(trimmed)) return true;
    if (contentType == null || contentType.isBlank()) return false;
    try {
      MimeType mimeType = MimeType.valueOf(contentType);
      return "json".equalsIgnoreCase(mimeType.getSubtype()) || mimeType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private String firstProviderError(JsonNode json) {
    String error = firstText(json, "error", "Error Message");
    return error == null || error.isBlank() ? null : error;
  }

  private String friendlyProviderMessage(String context, String rawMessage) {
    String normalized = rawMessage == null ? "" : rawMessage.trim();
    String lower = normalized.toLowerCase(Locale.ROOT);
    if (lower.contains("restricted endpoint") || lower.contains("don't have access") || lower.contains("not available under your current subscription")) {
      return "Dato no disponible en el plan gratuito del proveedor (" + providerName() + " " + context + ").";
    }
    if (lower.contains("invalid api key")) {
      return providerName() + " " + context + " failed: API key invalida.";
    }
    if (lower.startsWith("<html") || lower.startsWith("<!doctype")) {
      return providerName() + " " + context + " failed: respuesta HTML no valida del proveedor.";
    }
    return providerName() + " " + context + " failed: " + (normalized.isBlank() ? "empty response" : normalized);
  }

  protected record PricePoint(LocalDate date, BigDecimal close) {}
  protected record PriceHistory(List<PricePoint> points) {}
  private record ProviderResponse(HttpStatusCode status, String body, String contentType) {}
}
