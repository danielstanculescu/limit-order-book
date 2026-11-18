package ch.daniel.orderbook.domain;

import java.time.Instant;
import java.util.Objects;

public record OrderAccepted (
        Order order,
        Instant occurredAt
) implements OrderBookEvent {

    public OrderAccepted {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
