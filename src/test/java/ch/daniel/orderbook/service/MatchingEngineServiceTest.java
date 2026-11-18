package ch.daniel.orderbook.service;

import ch.daniel.orderbook.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MatchingEngineServiceTest {
    private static final String SYMBOL1 = "NESN";
    private static final String SYMBOL2 = "NOVN";

    @Test
    @DisplayName("Multi symbol isolation")
    void multiSymbolIsolation() {
        MatchingEngineService service = new MatchingEngineService();
        Order order = new Order(
                "B1",
                SYMBOL1,
                Side.BUY,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        service.submitOrder(order);

        Order order2 = new Order(
                "S1",
                SYMBOL2,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        List<Trade> trades = service.submitOrder(order2);
        assertThat(trades).isEmpty();
    }

    @Test
    @DisplayName("Event sequence and exhaustive pattern matching")
    void eventSequenceAndExhaustivePatternMatching() {
        List<OrderBookEvent> events = new ArrayList<>();
        Order sellOrder = new Order (
                "S1",
                SYMBOL1,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        MatchingEngineService service = new MatchingEngineService(events::add);

        service.submitOrder(sellOrder);
        service.cancelOrder(SYMBOL1, "S1");

        List<String> summaries = events.stream()
                .map(event -> switch (event) {
                    case OrderAccepted(var order, var time) -> "ACCEPTED:" + order.orderId();
                    case TradeExecuted(var trade, var time) -> "TRADED:" + trade.quantity();
                    case OrderCancelled(var sym, var id, var time) -> "CANCELLED:" + sym + ":" + id;
                })
                .toList();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.getFirst()).isEqualTo("ACCEPTED:" + sellOrder.orderId());
        assertThat(summaries.get(1)).isEqualTo("CANCELLED:" + SYMBOL1 + ":" + sellOrder.orderId());
    }


}
