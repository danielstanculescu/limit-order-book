package ch.daniel.orderbook.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Order (
        String orderId,
        String symbol,
        Side side,
        BigDecimal price,
        long quantity,
        Instant timestamp
) {
    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null or blank");
        }
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be strictly positive");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }
    }

    public Order withRemainingQuantity(long newQuantity) {
        return new Order(orderId, symbol, side, price, newQuantity, timestamp);
    }
}

