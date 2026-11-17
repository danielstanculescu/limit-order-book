package ch.daniel.orderbook.engine;

import ch.daniel.orderbook.domain.Order;
import ch.daniel.orderbook.domain.Side;
import ch.daniel.orderbook.domain.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// JUnit 5 and AssertJ
public class OrderBookTest {

    private static final String SYMBOL = "NESN";
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(SYMBOL);
    }

    @Test
    @DisplayName("Should add resting order when opposite book is empty and allow cancelling once")
    void shouldAddRestingOrderWhenOppositeBookIsEmpty() {
        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );

        List<Trade> trades = orderBook.submit(buyOrder);

        assertThat(trades).isEmpty();
        assertThat(orderBook.cancel("B1")).isTrue();
        assertThat(orderBook.cancel("B1")).isFalse();
    }

    @Test
    @DisplayName("Should execute trade at resting order price")
    void shouldExecuteTradeAtRestingOrderPrice() {
        List<Trade> trades;
        Order sellOrder = new Order(
                "S1",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("105.00"),
                10L,
                Instant.now()
        );
        trades = orderBook.submit(buyOrder);

        assertThat(trades).hasSize(1);

        Trade trade = trades.getFirst();
        assertThat(trade.price()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(trade.buyOrderId()).isEqualTo(buyOrder.orderId());
        assertThat(trade.sellOrderId()).isEqualTo(sellOrder.orderId());
    }

    @Test
    @DisplayName("Should enforce price time priority and partial fills")
    void shouldEnforcePriceTimePriorityAndPartialFills() {
        List<Trade> trades;
        Order sellOrder = new Order(
                "S1",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        sellOrder = new Order(
                "S2",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("100.00"),
                15L,
                Instant.now()
        );
        trades = orderBook.submit(buyOrder);

        assertThat(trades).hasSize(2);

        Trade trade = trades.getFirst();
        assertThat(trade.sellOrderId()).isEqualTo("S1");
        assertThat(trade.quantity()).isEqualTo(10);

        Trade trade2 = trades.get(1);
        assertThat(trade2.sellOrderId()).isEqualTo("S2");
        assertThat(trade2.quantity()).isEqualTo(5);

        buyOrder = new Order(
                "B2",
                SYMBOL,
                Side.BUY,
                new BigDecimal("100.00"),
                5L,
                Instant.now()
        );
        trades = orderBook.submit(buyOrder);

        Trade trade3 = trades.getFirst();
        assertThat(trade3.sellOrderId()).isEqualTo("S2");
        assertThat(trade3.quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should sweep multiple price levels")
    void shouldSweepMultiplePriceLevels() {
        List<Trade> trades;
        Order sellOrder = new Order(
                "S1",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        sellOrder = new Order(
                "S2",
                SYMBOL,
                Side.SELL,
                new BigDecimal("101.00"),
                15L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("102.00"),
                25L,
                Instant.now()
        );
        trades = orderBook.submit(buyOrder);

        assertThat(trades).hasSize(2);

        Trade trade = trades.getFirst();
        assertThat(trade.price()).isEqualByComparingTo(new BigDecimal("100"));

        Trade trade2 = trades.get(1);
        assertThat(trade2.price()).isEqualByComparingTo(new BigDecimal("101"));
    }

    @Test
    @DisplayName("Should cancel only target order at price level")
    void shouldCancelOnlyTargetOrderAtPriceLevel() {
        List<Trade> trades;
        Order sellOrder = new Order(
                "S1",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(sellOrder);

        sellOrder = new Order(
                "S2",
                SYMBOL,
                Side.SELL,
                new BigDecimal("100.00"),
                15L,
                Instant.now()
        );
        orderBook.submit(sellOrder);
        orderBook.cancel("S1");

        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("102.00"),
                25L,
                Instant.now()
        );
        trades = orderBook.submit(buyOrder);

        assertThat(trades).hasSize(1);
        assertThat(trades.getFirst().sellOrderId()).isEqualTo("S2");
    }

    @Test
    @DisplayName("Should reject invalid orders")
    void shouldRejectInvalidOrders() {
        Order wrongSymbolOrder = new Order(
          "B1",
          "APPL",
          Side.BUY,
          new BigDecimal("100.00"),
          10L,
          Instant.now()
        );

        assertThatThrownBy(() ->  orderBook.submit(wrongSymbolOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("mismatched symbols");

        Order validOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(validOrder);

        assertThatThrownBy(() -> orderBook.submit(validOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incomingOrder exists already");
    }

    @Test
    @DisplayName("Should match incoming sell against highest bids")
    void shouldMatchIncomingSellAgainstHighestBids() {
        Order buyOrder = new Order(
                "B1",
                SYMBOL,
                Side.BUY,
                new BigDecimal("99.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(buyOrder);

        buyOrder = new Order(
                "B2",
                SYMBOL,
                Side.BUY,
                new BigDecimal("101.00"),
                10L,
                Instant.now()
        );
        orderBook.submit(buyOrder);

        Order sellOrder = new Order(
                "S1",
                SYMBOL,
                Side.SELL,
                new BigDecimal("98.00"),
                50L,
                Instant.now()
        );
        List<Trade> trades = orderBook.submit(sellOrder);

        assertThat(trades).hasSize(2);
        assertThat(trades.getFirst().price()).isEqualByComparingTo(new BigDecimal("101.00"));
        assertThat(trades.get(1).price()).isEqualByComparingTo(new BigDecimal("99.00"));

        assertThat(orderBook.cancel("S1")).isTrue();
    }
}
