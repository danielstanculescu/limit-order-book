package ch.daniel.orderbook.domain;

import java.time.Instant;

public sealed interface OrderBookEvent
        permits OrderAccepted, TradeExecuted, OrderCancelled {
    Instant occurredAt();
}
