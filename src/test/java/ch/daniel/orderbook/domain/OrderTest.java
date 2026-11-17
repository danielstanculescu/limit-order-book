package ch.daniel.orderbook.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// JUnit 5 and AssertJ
public class OrderTest {

    @Test
    @DisplayName("Reject negative prices, quantity and blank IDs")
    void rejectNegativePricesQuantityAndBlankIDs() {

        assertThatThrownBy(() -> new Order(
                "B1",
                "NESN",
                Side.BUY,
                new BigDecimal("-100.00"),
                10L,
                Instant.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price must be strictly positive");

        assertThatThrownBy(() -> new Order(
                "B2",
                "NESN",
                Side.BUY,
                new BigDecimal("100.00"),
                0L,
                Instant.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be strictly positive");

        assertThatThrownBy(() -> new Order(
                "",
                "NESN",
                Side.BUY,
                new BigDecimal("100.00"),
                10L,
                Instant.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId must not be null or blank");
    }

}
