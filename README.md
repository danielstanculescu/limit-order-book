# In-Memory Limit Order Book & Matching Engine (Java 21)

![Java 21 CI](https://github.com/danielstanculescu/limit-order-book/workflows/ci.yml/badge.svg)

A deterministic, low-latency **Limit Order Book and Price-Time Priority (FIFO) Matching Engine** built in pure **Java 21** with zero external runtime frameworks. Designed to model core electronic exchange mechanics—including price improvement, partial fills, multi-level sweeps, targeted cancellations, and event-driven audit publishing.

---

## Key Features & Exchange Semantics

* **Price-Time Priority (FIFO) Matching:**
    * **Bids (`BUY`):** Matched highest price first (`Comparator.reverseOrder()`), breaking ties by earliest arrival order.
    * **Asks (`SELL`):** Matched lowest price first (natural ascending order), breaking ties by earliest arrival order.
* **Resting Price Execution Rule:** Incoming aggressive orders always execute at the resting order's limit price, preserving price improvement for the aggressor.
* **Partial Fills & Queue Priority Preservation:** Partially filled resting orders retain their exact position at the front (`addFirst`) of their price-level queue.
* **Financial Precision:** Uses `java.math.BigDecimal` with `compareTo` semantics across all pricing operations to eliminate IEEE-754 floating-point rounding defects.
* **Multi-Symbol Routing & Sealed Domain Events:** `MatchingEngineService` isolates order books per instrument (`NESN`, `NOVN`, `ROG`) and emits immutable Java 21 `sealed` events (`OrderAccepted`, `TradeExecuted`, `OrderCancelled`).

---

## Architecture & Data Structure Trade-Offs

```text
Incoming Order ──▶ MatchingEngineService (ConcurrentHashMap<Symbol, OrderBook>)
                          │
                          ├──▶ Publishes Sealed Events (OrderAccepted / TradeExecuted / OrderCancelled)
                          │
                          └──▶ OrderBook (Single-Symbol Engine)
                                 ├── Bids: TreeMap<BigDecimal, Deque<Order>> (Descending)
                                 ├── Asks: TreeMap<BigDecimal, Deque<Order>> (Ascending)
                                 └── Active Index: HashMap<OrderId, Order>
```

| Operation | Data Structure | Time Complexity | Engineering Rationale                                                                                                         |
| :--- | :--- | :--- |:------------------------------------------------------------------------------------------------------------------------------|
| **Best Bid / Ask Lookup** | `TreeMap` (`firstEntry()`) | \(O(\log P)\) | Red-black tree maintains sorted price levels dynamically as levels appear and clear (P = active price levels).                |
| **FIFO Order Queue** | `ArrayDeque` | O(1) | Contiguous array-backed deque avoids `LinkedList` node heap allocations and improves CPU cache locality.                      |
| **Order Tracking & Cancel** | `HashMap` + `TreeMap` | \(O(\log P + K)\) | Direct lookup by `orderId` locates the exact price level without scanning the book (K = orders at that single price).         |
| **Concurrency Model** | Single-Writer per Symbol | Lock-Free Core | Inspired by the LMAX Disruptor pattern: each symbol's book runs single-threaded to avoid lock contention on hot price levels. |

---

## Modern Java 21 Idioms Used

* **Records & Compact Constructors (`Order`, `Trade`):** Fail-fast invariant validation at construction time with zero boilerplate.
* **Sealed Interfaces (`OrderBookEvent`):** Exhaustive compile-time event modeling (`permits OrderAccepted, TradeExecuted, OrderCancelled`).
* **Record Patterns in `switch`:** Type-safe destructuring of domain events without `instanceof` casts.

---

## Benchmark Results

Measured locally on **Java 21 LTS** (HotSpot C2 JIT warmed up with 20,000 orders prior to measuring 100,000 randomized orders across a 100-tick spread):

| Metric | Result |
| :--- | :--- |
| **Workload** | 100,000 randomized `BUY` / `SELL` limit orders |
| **Total Trades Executed** | 76,973 |
| **Elapsed Time** | 172.71 ms |
| **Throughput** | **579,016 orders/sec** |
| **Average Latency** | **1,727 ns/order (1.73 µs)** |

---

## Build & Run Tests

```bash
# Run the full unit test suite and 100k-order throughput benchmark
mvn test
```

### Project Structure

```text
src/
├── main/java/ch/daniel/orderbook/
│   ├── domain/     # Immutable records (Order, Trade), Side enum, sealed OrderBookEvent
│   ├── engine/     # Core single-symbol OrderBook matching algorithm
│   └── service/    # Multi-symbol MatchingEngineService & event publisher
└── test/java/ch/daniel/orderbook/
    ├── domain/     # Domain invariant validation tests
    ├── engine/     # Price-time priority, partial fill, sweep, and cancellation tests
    ├── service/    # Multi-symbol isolation & Java 21 pattern-matching event tests
    └── benchmark/  # 100k-order throughput & latency benchmark
```