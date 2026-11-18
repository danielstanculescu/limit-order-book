package ch.daniel.orderbook.service;

import ch.daniel.orderbook.domain.*;
import ch.daniel.orderbook.engine.OrderBook;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MatchingEngineService {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Consumer<OrderBookEvent> eventPublisher;

    public MatchingEngineService(Consumer<OrderBookEvent> eventPublisher) {
        Objects.requireNonNull(eventPublisher);
        this.eventPublisher = eventPublisher;
    }

    public MatchingEngineService() {
        this(event -> {});
    }

    public List<Trade> submitOrder(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        OrderBook book = orderBooks.computeIfAbsent(order.symbol(), OrderBook::new);

        List<Trade> trades = book.submit(order);
        eventPublisher.accept(new OrderAccepted(order, Instant.now()));
        for (Trade trade : trades) {
            eventPublisher.accept(new TradeExecuted(trade, trade.executedAt()));
        }

        return trades;
    }

    public boolean cancelOrder(String symbol, String orderId) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw  new IllegalArgumentException("orderId must not be null or blank");
        }

        OrderBook book = orderBooks.get(symbol);
        if (book == null) {
            return false;
        }
        boolean cancelled = book.cancel(orderId);
        if (cancelled) {
            eventPublisher.accept(new OrderCancelled(symbol, orderId, Instant.now()));
        }

        return cancelled;
    }
}
