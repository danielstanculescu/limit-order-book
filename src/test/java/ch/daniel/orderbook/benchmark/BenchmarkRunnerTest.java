package ch.daniel.orderbook.benchmark;

import ch.daniel.orderbook.domain.Order;
import ch.daniel.orderbook.domain.Side;
import ch.daniel.orderbook.engine.OrderBook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class BenchmarkRunnerTest {

    private static final String SYMBOL = "NESN";
    private static final int WARMUP_ORDERS = 20_000;
    private static final int BENCHMARK_ORDERS = 100_000;

    @Test
    @DisplayName("Benchmark throughput and average latency across 100,000 randomized orders")
    void benchmarkThroughput() {
        OrderBook warmupBook = new OrderBook(SYMBOL);
        List<Order> warmupOrders = generateOrders(WARMUP_ORDERS, 42L);
        for (Order order : warmupOrders) {
            warmupBook.submit(order);
        }

        OrderBook benchmarkBook = new OrderBook(SYMBOL);
        List<Order> orders = generateOrders(BENCHMARK_ORDERS, 99L);

        long totalTrades = 0;
        long startNanos = System.nanoTime();

        for (Order order : orders) {
            totalTrades += benchmarkBook.submit(order).size();
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        double ordersPerSecond = (BENCHMARK_ORDERS / (elapsedNanos / 1_000_000_000.0));
        double avgLatencyNanos = (double) elapsedNanos / BENCHMARK_ORDERS;

        System.out.printf("""
                =========================================================
                Limit Order Book Benchmark (%d orders)
                =========================================================
                Total Trades Executed : %,d
                Elapsed Time          : %.2f ms
                Throughput            : %,.0f orders/sec
                Average Latency       : %.0f ns/order (%.2f µs)
                =========================================================
                """,
                BENCHMARK_ORDERS,
                totalTrades,
                elapsedMillis,
                ordersPerSecond,
                avgLatencyNanos,
                avgLatencyNanos / 1_000.0
        );

        assertThat(totalTrades).isGreaterThan(0);
    }

    private List<Order> generateOrders(int count, long seed) {
        Random random = new Random(seed);
        List<Order> orders = new ArrayList<>(count);
        Instant baseTime = Instant.now();

        for (int i=0; i<count; i++) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;

            int centsOffset = random.nextInt(101);
            BigDecimal price = BigDecimal.valueOf(9950 + centsOffset, 2);
            long quantity = 1 + random.nextInt(50);

            orders.add(new Order(
                    "ORD-" + i,
                    SYMBOL,
                    side,
                    price,
                    quantity,
                    baseTime.plusNanos(i)
            ));
        }
        return orders;
    }
}
