package ch.daniel.orderbook.domain;

import java.time.Instant;
import java.util.Objects;

public record TradeExecuted(
        Trade trade,
        Instant occurredAt
) implements OrderBookEvent {

    public TradeExecuted {
        Objects.requireNonNull(trade, "trade must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
