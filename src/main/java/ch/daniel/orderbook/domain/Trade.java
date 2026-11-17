package ch.daniel.orderbook.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Trade (
    String tradeId,
    String buyOrderId,
    String sellOrderId,
    String symbol,
    BigDecimal price,
    long quantity,
    Instant executedAt
) {
    public Trade {
        if (tradeId == null || tradeId.isBlank()) {
            throw new IllegalArgumentException("tradeId must not be null or blank");
        }
        if (buyOrderId == null || buyOrderId.isBlank()) {
            throw new IllegalArgumentException("buyOrderId must not be null or blank");
        }
        if(sellOrderId == null || sellOrderId.isBlank()) {
            throw new IllegalArgumentException("sellOrderId must not be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null or blank");
        }
        if(price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be strictly positive");
        }
        Objects.requireNonNull(executedAt, "executedAt must not be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }

        if(buyOrderId.equals(sellOrderId)) {
            throw new IllegalArgumentException("buyOrderId and sellOrderId must be distinct");
        }
    }
}
