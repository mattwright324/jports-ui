package io.mattw.jports;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Configure and start a multithreaded cycle through a block of addresses.
 * <p>
 * A single thread will produce to a queue and the consuming threads will
 * use the provided consuming method to process them to.
 */
public class BlockScanImpl extends BlockScan<BlockScanImpl> {

    private Consumer<Address> consumingMethod;
    private final Queue<Address> objectQueue = new LinkedBlockingQueue<>();

    /**
     * Scan multiple blocks of addresses
     */
    public BlockScanImpl(final AddressBlock... addressBlock) {
        super(addressBlock);
    }

    /**
     * Scan a block of addresses
     */
    public BlockScanImpl(final AddressBlock addressBlock) {
        super(addressBlock);
    }

    /**
     * Endlessly scan in either direction (or a single address though this doesn't make much sense to do so).
     */
    public BlockScanImpl(final String address, final ScanMethod scanMethod) {
        super(address, scanMethod);
    }

    /**
     * Endlessly scan in either direction (or a single address though this doesn't make much sense to do so).
     */
    public BlockScanImpl(final Address address, final ScanMethod scanMethod) {
        super(address, scanMethod);
    }

    /**
     * Scan a specific list of addresses.
     */
    public BlockScanImpl(final List<String> addresses) {
        super(addresses);
    }

    /**
     * Scan a specific list of addresses.
     */
    public BlockScanImpl(final Collection<Address> addresses) {
        super(addresses);
    }

    public BlockScanImpl setConsumingMethod(final Consumer<Address> consumingMethod) {
        this.consumingMethod = consumingMethod;
        return this;
    }

    @Override
    public BlockScanImpl execute() {
        Objects.requireNonNull(consumingMethod);

        producer.submitAndShutdown(this::producer);

        consumers = new ExecutorGroup(threadCount);
        consumers.submitAndShutdown(this::consumer);

        return this;
    }

    @Override
    void producer() {
        switch (scanMethod) {
            case SINGLE_ADDRESS:
                waitAndOfferToQueue(objectQueue, startAddress);
                break;

            case RANGE_ADDRESS:
                addressBlocks.forEach(addressBlock -> {
                    var address1 = addressBlock.getFirstAddress();
                    do {
                        waitAndOfferToQueue(objectQueue, address1);

                        address1 = address1.nextAddress();
                    } while (address1.getDecimal() < addressBlock.getLastAddress().getDecimal() && !shutdown);
                });
                break;

            case MULTI_ADDRESS:
                for (var address2 : addresses) {
                    waitAndOfferToQueue(objectQueue, address2);

                    if (shutdown) {
                        break;
                    }
                }
                break;

            case ENDLESS_DECREASE:
            case ENDLESS_INCREASE:
                var address3 = startAddress;
                do {
                    waitAndOfferToQueue(objectQueue, address3);

                    if (scanMethod == ScanMethod.ENDLESS_INCREASE) {
                        address3 = address3.nextAddress();
                    } else {
                        address3 = address3.prevAddress();
                    }
                } while (!shutdown);

            default:
                throw new IllegalStateException("Could not determine which scan method to perform.");
        }
    }

    @Override
    void consumer() {
        final var threadId = UUID.randomUUID().toString();
        while (producer.isStillWorking() || !objectQueue.isEmpty()) {
            final var address = objectQueue.poll();

            if (address != null) {
                updateThreadTime(threadId);
                consumingMethod.accept(address);
            }

            if (shutdown) {
                break;
            }

            // Without delay, would freeze JavaFX UI despite being on a separate thread.
            sleep(2);
        }
    }

    @Override
    public long getQueueSize() {
        return objectQueue.size();
    }



}
