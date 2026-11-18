package ch.daniel.orderbook.domain;

import java.time.Instant;
import java.util.Objects;

public record OrderCancelled (
        String symbol,
        String orderId,
        Instant occurredAt
) implements OrderBookEvent {

    public OrderCancelled {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be null or blank");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
