package io.mattw.jports;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Configure and start a multithreaded cycle through one or more blocks of addresses and ports.
 * <p>
 * Each address:port can checked being open with a {@link java.net.Socket} and sent to
 * the {@link #consumingMethod} for processing.
 */
public class BlockPortScan extends BlockScan<BlockPortScan> {

    private boolean checkPortOpen = true;
    private int checkTimeout = 300;
    private Collection<Integer> ports;
    private Consumer<AddressPort> progressMethod;
    private Consumer<AddressPort> consumingMethod;

    private final Queue<AddressPort> objectQueue = new LinkedBlockingQueue<>();

    /**
     * Scan a block of addresses
     */
    public BlockPortScan(final AddressBlock addressBlock) {
        super(addressBlock);
    }

    /**
     * Endlessly scan in either direction or single addresses. Single addresses are more relevant here
     * than in {@link BlockScanImpl} as you may have multiple ports to cycle through for a single address.
     */
    public BlockPortScan(final String address, final ScanMethod scanMethod) {
        super(address, scanMethod);
    }

    /**
     * Endlessly scan in either direction or single addresses. Single addresses are more relevant here
     * than in {@link BlockScanImpl} as you may have multiple ports to cycle through for a single address.
     */
    public BlockPortScan(final Address address, final ScanMethod scanMethod) {
        super(address, scanMethod);
    }

    /**
     * Scan a specific list of addresses.
     * @param addresses
     */
    public BlockPortScan(final List<String> addresses) {
        super(addresses);
    }

    /**
     * Scan a specific list of addresses.
     */
    public BlockPortScan(final Collection<Address> addresses) {
        super(addresses);
    }

    public BlockPortScan setAddressBlocks(final AddressBlock... addressBlocks) {
        this.addressBlocks = Arrays.asList(addressBlocks);
        this.startAddress = this.addressBlocks.stream().findFirst().get().getFirstAddress();
        return this;
    }

    public BlockPortScan setThreadCount(final int threadCount) {
        this.threadCount = Math.abs(threadCount);
        return this;
    }

    public BlockPortScan setConsumingMethod(final Consumer<AddressPort> consumingMethod) {
        this.consumingMethod = consumingMethod;
        return this;
    }

    public BlockPortScan setPorts(final Collection<Integer> ports) {
        this.ports = ports;
        return this;
    }

    /**
     * Additional method consumer that pushes for every address:port grabbed as used by threads
     * and can be used to increment a counter in the external application
     * when {@link #checkPortOpen} is true (because you won't know where it is in the scan otherwise).
     */
    public BlockPortScan setProgressMethod(final Consumer<AddressPort> progressMethod) {
        this.progressMethod = progressMethod;
        return this;
    }

    /**
     * Flag to check if the port is open on an address.
     *
     * @param checkPortOpen when true, if port is open, will push to consumer, otherwise ignored
     *                      when false, will push all generated values to consumer regardless if open
     */
    public BlockPortScan setCheckPortOpen(final boolean checkPortOpen) {
        this.checkPortOpen = checkPortOpen;
        return this;
    }

    /**
     * limit in milliseconds to timeout when checking a port
     *
     * @param checkTimeout millis
     */
    public BlockPortScan setCheckTimeout(final int checkTimeout) {
        this.checkTimeout = checkTimeout;
        return this;
    }

    @Override
    public BlockPortScan execute() {
        Objects.requireNonNull(consumingMethod);
        Objects.requireNonNull(ports);

        if (ports.isEmpty()) {
            throw new IllegalStateException("Ports list should not be empty.");
        }

        producer.submitAndShutdown(this::producer);

        consumers = new ExecutorGroup(threadCount);
        consumers.submitAndShutdown(this::consumer);

        return this;
    }

    @Override
    void producer() {
        switch (scanMethod) {
            case SINGLE_ADDRESS:
                if (offerPorts(startAddress)) {
                    break;
                }
                break;

            case RANGE_ADDRESS:
                addressBlocks.forEach(addressBlock -> {
                    Address address1 = addressBlock.getFirstAddress();
                    do {
                        if (offerPorts(address1)) {
                            break;
                        }

                        address1 = address1.nextAddress();
                    } while (address1.getDecimal() < addressBlock.getLastAddress().getDecimal() && !shutdown);
                });
                break;

            case MULTI_ADDRESS:
                for (Address address2 : addresses) {
                    if (offerPorts(address2)) {
                        break;
                    }

                    if (shutdown) {
                        break;
                    }
                }
                break;

            case ENDLESS_DECREASE:
            case ENDLESS_INCREASE:
                Address address3 = startAddress;
                do {
                    if (offerPorts(address3)) {
                        break;
                    }

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

    /**
     * Cycles through the list of ports for this address to offer to consumers
     *
     * @param address any address
     * @return ended from shutdown
     */
    private boolean offerPorts(final Address address) {
        for (Integer port : ports) {
            waitAndOfferToQueue(objectQueue, new AddressPort(address, port));

            if (shutdown) {
                return true;
            }

            // Without delay, would freeze JavaFX UI despite being on a separate thread.
            sleep(2);
        }

        return false;
    }

    @Override
    void consumer() {
        final String threadId = UUID.randomUUID().toString();
        while (producer.isStillWorking() || !objectQueue.isEmpty()) {
            final AddressPort addressPort = objectQueue.poll();

            if (addressPort != null) {
                updateThreadTime(threadId);
                if(progressMethod != null) {
                    progressMethod.accept(addressPort);
                }

                if (!checkPortOpen || checkPortOpen && isPortOpen(addressPort)) {
                    consumingMethod.accept(addressPort);
                }
            }

            if (shutdown) {
                break;
            }

            // Without delay, would freeze JavaFX UI despite being on a separate thread.
            sleep(2);
        }
    }

    private boolean isPortOpen(final AddressPort addressPort) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(addressPort.getiPv4Address().getAddress(), addressPort.getPort()), checkTimeout);
            socket.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long getQueueSize() {
        return objectQueue.size();
    }

    public boolean isCheckPortOpen() {
        return checkPortOpen;
    }

    public int getCheckTimeout() {
        return checkTimeout;
    }

    public Collection<Integer> getPorts() {
        return ports;
    }
}
