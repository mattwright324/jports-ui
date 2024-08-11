package io.mattw.jports;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BlockScan<T extends BlockScan> {

    /**
     * Restriction on queue size so it doesn't get infinitely larger in the event an ENDLESS search has been running
     * for hours.
     * <p>
     * Calculation should be this value multiplied by the {@link #threadCount}, this way it can scale if the provided thread
     * count changes.
     * - 64 threads * 128 = 8192 max items
     */
    public static final long MAX_QUEUE_SIZE_MULTIPLIER = 16;

    ScanMethod scanMethod;
    Address startAddress;
    Collection<Address> addresses;
    Collection<AddressBlock> addressBlocks;
    int threadCount = 1;

    ExecutorGroup producer = new ExecutorGroup(1);
    ExecutorGroup consumers;
    boolean shutdown = false;

    Map<String, Instant> threadTimes = new HashMap<>();
    Duration quickest;
    Duration longest;

    /**
     * Scan multiple blocks of addresses
     */
    public BlockScan(ScanMethod method, final AddressBlock... addressBlocks) {
        this.addressBlocks = Arrays.asList(addressBlocks);
        this.startAddress = this.addressBlocks.stream().findFirst().get().getFirstAddress();

        this.scanMethod = method;
    }
    /**
     * Scan multiple blocks of addresses
     */
    public BlockScan(final AddressBlock... addressBlocks) {
        this(ScanMethod.RANGE_ADDRESS, addressBlocks);
    }

    /**
     * Scan a block of addresses
     */
    public BlockScan(final AddressBlock addressBlock) {
        this.addressBlocks = Collections.singletonList(addressBlock);

        this.scanMethod = ScanMethod.RANGE_ADDRESS;
    }

    /**
     * Endlessly scan in either direction (or single addresses, though this doesn't make much sense to do so).
     */
    public BlockScan(final String address, final ScanMethod scanMethod) {
        this(new Address(address), scanMethod);
    }

    /**
     * Endlessly scan in either direction (or single addresses, though this doesn't make much sense to do so).
     */
    public BlockScan(final Address address, final ScanMethod scanMethod) {
        if (scanMethod != ScanMethod.ENDLESS_DECREASE && scanMethod != ScanMethod.ENDLESS_INCREASE && scanMethod != ScanMethod.SINGLE_ADDRESS) {
            throw new IllegalArgumentException("Invalid scan method. Must be ENDLESS_DECREASE, ENDLESS_INCREASE, or SINGLE_ADDRESS");
        }

        this.startAddress = address;
        this.scanMethod = scanMethod;
    }

    /**
     * Scan a specific list of addresses.
     */
    public BlockScan(final List<String> addresses) {
        this(addresses.stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .filter(Address::matchesIPv4Pattern)
                .map(Address::new)
                .collect(Collectors.toList()));
    }

    /**
     * Scan a specific list of addresses.
     */
    public BlockScan(final Collection<Address> addresses) {
        this.addresses = addresses;

        this.scanMethod = ScanMethod.MULTI_ADDRESS;
    }

    /**
     * Signals a shutdown of the producing and consuming threads to end the process early.
     */
    public void shutdown() {
        this.shutdown = true;
    }

    private T getThis() {
        return (T) this;
    }

    public T setAddressBlocks(final AddressBlock... addressBlocks) {
        this.addressBlocks = Arrays.asList(addressBlocks);
        return getThis();
    }

    public T setThreadCount(final int threadCount) {
        this.threadCount = Math.abs(threadCount);
        return getThis();
    }

    /**
     * Offer that waits for the queue to open up before adding items back to it.
     */
    <K> void waitAndOfferToQueue(final Queue<K> queue, final K object) {
        while (queue.size() >= getMaxQueueSize()) {
            sleep(100);
        }

        queue.offer(object);
    }

    /**
     * Executes and shuts down the ExecutorServices.
     */
    public abstract T execute();

    abstract void producer();

    abstract void consumer();

    abstract long getQueueSize();

    public long getMaxQueueSize() {
        return MAX_QUEUE_SIZE_MULTIPLIER * threadCount;
    }

    void updateThreadTime(final String threadId) {
        final var previous = threadTimes.get(threadId);
        final var now = Instant.now();
        threadTimes.put(threadId, now);

        if(previous != null) {
            var threadTime = Duration.between(previous, now);

            if (quickest == null || threadTime.compareTo(quickest) < 0) {
                quickest = threadTime;
            }
            if (longest == null || threadTime.compareTo(longest) > 0) {
                longest = threadTime;
            }
        }
    }

    public Collection<Duration> getHangingThreadTimes(final Duration threshold) {
        var times = new ArrayList<Duration>();

        for (var key : threadTimes.keySet()) {
            var threadTime = Duration.between(threadTimes.get(key), Instant.now());

            var difference = threshold.minus(threadTime);
            var exceedsOrMeets = difference.isNegative() || difference.isZero();

            if(exceedsOrMeets) {
                times.add(threadTime);
            }
        }

        return times;
    }

    public Duration getAverageThreadTime() {
        final var values = threadTimes.values();
        final var now = Instant.now();

        var sum = values.stream()
                .map(instant -> Duration.between(instant, now))
                .mapToLong(Duration::toNanos)
                .sum();

        return Duration.ofNanos(sum / values.size());
    }

    public Duration getQuickest() {
        return quickest;
    }

    public Duration getLongest() {
        return longest;
    }

    /**
     * Await the ExecutorServices to finish working
     */
    public T await() throws InterruptedException {
        producer.await();
        consumers.await();
        return getThis();
    }

    /**
     * Execute, shutdown, and await the ExecutorServices.
     */
    public T executeAndAwait() throws InterruptedException {
        return (T) execute().await();
    }

    void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

}
