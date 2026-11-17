package ch.daniel.orderbook.engine;

import ch.daniel.orderbook.domain.Order;
import ch.daniel.orderbook.domain.Side;
import ch.daniel.orderbook.domain.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class OrderBook {

    private final String symbol;

    private final TreeMap<BigDecimal, Deque<Order>> bids;

    private final TreeMap<BigDecimal, Deque<Order>> asks;

    private final Map<String, Order> activeOrders;

    public OrderBook(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        this.symbol = symbol;
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
        this.activeOrders = new HashMap<>();
    }

    public List<Trade> submit(Order incomingOrder) {
        Objects.requireNonNull(incomingOrder, "incomingOrder must not be null");
        if (!incomingOrder.symbol().equals(this.symbol)) {
            throw new IllegalArgumentException("mismatched symbols");
        }

        if (activeOrders.containsKey(incomingOrder.orderId()))
            throw new IllegalArgumentException("incomingOrder exists already");

        return switch (incomingOrder.side()) {
            case BUY -> match(incomingOrder, Side.BUY, asks, bids);
            case SELL -> match(incomingOrder, Side.SELL, bids, asks);
        };
    }

    public boolean cancel(String orderId) {
        if (!activeOrders.containsKey(orderId)) {
            return false;
        }

        Order o = activeOrders.remove(orderId);
        removeFromDeque(o, o.side() == Side.BUY ? bids : asks);

        return true;
    }

    private void removeFromDeque (Order o, TreeMap<BigDecimal, Deque<Order>> customSet) {

        Deque<Order> q = customSet.get(o.price());
        if(q != null) {
            q.remove(o);
            if(q.isEmpty()) {
                customSet.remove(o.price());
            }
        }
    }

    private List<Trade> match (Order incomingOrder, Side side,  TreeMap<BigDecimal, Deque<Order>> operations, TreeMap<BigDecimal, Deque<Order>> operationsOpposite) {
        List<Trade> trades = new ArrayList<>();

        BigDecimal bestPrice;
        long orderQuantity = incomingOrder.quantity();

        while (orderQuantity > 0 &&
                !operations.isEmpty() &&
                (side == Side.BUY ?
                        (incomingOrder.price().compareTo(bestPrice = operations.firstEntry().getKey()) >= 0) :
                        (side == Side.SELL && incomingOrder.price().compareTo(bestPrice = operations.firstEntry().getKey()) <= 0)) ) {

            Deque<Order> deque = operations.firstEntry().getValue();

            while (!deque.isEmpty()) {
                Order o = deque.poll();
                String buyOrderId = (side == Side.BUY) ? incomingOrder.orderId() : o.orderId();
                String sellOrderId = (side == Side.BUY) ? o.orderId() : incomingOrder.orderId();

                activeOrders.remove(o.orderId());

                long minQuantity = Math.min(o.quantity(), orderQuantity);
                trades.add(new Trade(UUID.randomUUID().toString(), buyOrderId, sellOrderId, symbol, bestPrice, minQuantity, Instant.now()));

                if (orderQuantity < o.quantity()) {
                    Order newOrder = o.withRemainingQuantity(o.quantity() - orderQuantity);
                    deque.addFirst(newOrder);
                    activeOrders.put(newOrder.orderId(), newOrder);
                    orderQuantity = 0;
                    break;

                } else if (orderQuantity == o.quantity()) {
                    if (deque.isEmpty()) {
                        operations.remove(bestPrice);
                    }
                    orderQuantity = 0;
                    break;

                } else {
                    if (deque.isEmpty()) {
                        operations.remove(bestPrice);
                    }
                    orderQuantity = orderQuantity - o.quantity();
                }
            }
        }

        if (orderQuantity > 0) {
            Order newOrder = incomingOrder.withRemainingQuantity(orderQuantity);
            operationsOpposite.computeIfAbsent(incomingOrder.price(), k -> new ArrayDeque<>()).addLast(newOrder);
            activeOrders.put(newOrder.orderId(), newOrder);
        }

        return Collections.unmodifiableList(trades);
    }
}
