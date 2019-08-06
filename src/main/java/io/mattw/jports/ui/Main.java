package io.mattw.jports.ui;

import io.mattw.jports.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main implements Initializable {

    private static final Logger logger = LogManager.getLogger();

    private final ClipboardUtil clipboard = new ClipboardUtil();

    @FXML private TitledPane configPane;
    @FXML private ComboBox<String> addressMethod;
    @FXML private Pane singleBox;
    @FXML private TextField singleAddress;
    @FXML private Pane multiBox;
    @FXML private CheckBox cidrFormat;
    @FXML private TextField firstAddress;
    @FXML private TextField secondAddress;

    @FXML private ComboBox<String> portMethod;
    @FXML private Pane portRangeBox;
    @FXML private TextField firstPort;
    @FXML private TextField secondPort;
    @FXML private Pane customPortBox;
    @FXML private TextArea customPort;
    @FXML private TextField timeout;
    @FXML private TextField threads;
    @FXML private CheckBox checkWebPage;

    @FXML private TitledPane scanPane;
    @FXML private Button btnStart;
    @FXML private Label status;
    @FXML private ProgressBar scanProgress;
    @FXML private ListView<IPv4AddressPortView> resultsList;
    @FXML private MenuItem menuCopy;

    private IPv4BlockPortScan portScan;

    private ExecutorGroup websiteCheckers;
    private Queue<IPv4AddressPort> webQueue = new LinkedBlockingQueue<>();

    private boolean running = false;

    public void initialize(final URL location, final ResourceBundle resources) {
        addressMethod.getSelectionModel().select(2);
        portMethod.getSelectionModel().select(1);

        addressMethod.setOnAction(ae -> Platform.runLater(this::uiAddressMethodChange));
        cidrFormat.setOnAction(ae -> Platform.runLater(this::uiCidrSelectChange));
        portMethod.setOnAction(ae -> Platform.runLater(this::uiPortMethodChange));

        menuCopy.setOnAction(ae -> copySelectedToClipboard());
        resultsList.setOnKeyPressed(ke -> {
            if(ke.isControlDown() && ke.getCode() == KeyCode.C) {
                copySelectedToClipboard();
            }
        });

        btnStart.setOnAction(ae -> new Thread(this::actionStartStop).start());
    }

    private void copySelectedToClipboard() {
        final List<String> selected = resultsList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .map(IPv4AddressPortView::getAddressPort)
                .map(IPv4AddressPort::getFullAddress)
                .collect(Collectors.toList());

        clipboard.setClipboard(selected);
    }

    private void actionStartStop() {
        if(running) {
            logger.debug("Called Stop on scan");

            portScan.shutdown();

            Platform.runLater(this::uiStop);
        } else {
            logger.debug("Called Start on scan");

            running = true;

            Platform.runLater(this::uiStart);

            try {
                final int addressType = this.addressMethod.getSelectionModel().getSelectedIndex();
                final PortType portType = PortType.values()[portMethod.getSelectionModel().getSelectedIndex()];

                List<Integer> ports = new ArrayList<>();
                if(portType == PortType.CUSTOM_PORT) {
                    final String[] split = customPort.getText()
                            .replaceAll("[^\\d,]", "")
                            .split(",");

                    ports.addAll(Stream.of(split)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
                } else {
                    final int first = Integer.parseInt(firstPort.getText());
                    final int second = Integer.parseInt(secondPort.getText());

                    ports.addAll(IntStream.range(Math.min(first, second), Math.max(first, second))
                            .boxed()
                            .collect(Collectors.toList()));
                }

                final int timeout = Integer.parseInt(this.timeout.getText());
                final int threads = Integer.parseInt(this.threads.getText());

                if(addressType == 0) { // Single Address(es)
                    final String[] split = singleAddress.getText()
                            .replaceAll("[^\\d.,]+", "")
                            .split(",");

                    switch (split.length) {
                        case 0:
                            break;

                        case 1:
                            portScan = new IPv4BlockPortScan(split[0], ScanMethod.SINGLE_ADDRESS);

                        default: // > 1
                            portScan = new IPv4BlockPortScan(Arrays.asList(split));
                    }

                } else if(addressType == 1) { // address range
                    final IPv4AddressBlock addressBlock = cidrFormat.isSelected() ?
                            new IPv4AddressBlock(firstAddress.getText()) :
                            new IPv4AddressBlock(firstAddress.getText(), secondAddress.getText());

                    portScan = new IPv4BlockPortScan(addressBlock);
                } else {
                    final ScanMethod scanMethod = addressType == 2 ?
                            ScanMethod.ENDLESS_INCREASE :
                            ScanMethod.ENDLESS_DECREASE;

                    portScan = new IPv4BlockPortScan(singleAddress.getText(), scanMethod);
                }

                if(checkWebPage.isSelected()) {
                    websiteCheckers = new ExecutorGroup(10);
                    websiteCheckers.submitAndShutdown(() -> {
                        while(running || !webQueue.isEmpty()) {
                            IPv4AddressPort addressPort = webQueue.poll();

                            if(addressPort != null) {
                                final int port = addressPort.getPort();
                                final String expectedUrl = String.format("http%s://%s",
                                        port == 443 ? "s" : "",
                                        port == 80 || port == 443 ?
                                                addressPort.getiPv4Address().getAddress() :
                                                addressPort.getFullAddress());

                                logger.info("Checking... {}", expectedUrl);

                                try {
                                    final Document document = Jsoup.connect(expectedUrl).get();

                                    logger.info("{}    size={}    title={}", expectedUrl, document.html().length(), document.title());

                                    Platform.runLater(() -> uiAddToList(new IPv4AddressPortView(addressPort, expectedUrl)));
                                } catch (IOException e) {
                                    logger.info("{}    {}: {}", expectedUrl, e.getClass().getSimpleName(), e.getMessage());

                                    Platform.runLater(() -> uiAddToList(new IPv4AddressPortView(addressPort, null)));
                                }
                            }

                            try { Thread.sleep(25); } catch (InterruptedException ignored) {}
                        }

                        logger.info("Website checking stopped");
                    });
                }

                portScan.setThreadCount(threads)
                        .setCheckTimeout(timeout)
                        .setPorts(ports)
                        .setCheckPortOpen(true)
                        .setProgressMethod(this::uiProgressMethod)
                        .setConsumingMethod(this::checkForWebsite)
                        .executeAndAwait();

            } catch (NumberFormatException | InterruptedException e) {
                e.printStackTrace();
                logger.error("Failed to start scan.", e);
            }

            Platform.runLater(this::uiScanEnd);

            running = false;
        }
    }

    private void checkForWebsite(final IPv4AddressPort addressPort) {
        logger.info(addressPort.getFullAddress());
        final boolean checkForWebsite = checkWebPage.isSelected();

        if(checkForWebsite) {
           webQueue.offer(addressPort);
        } else {
            Platform.runLater(() -> uiAddToList(new IPv4AddressPortView(addressPort, null)));
        }
    }

    private void uiProgressMethod(final IPv4AddressPort addressPort) {
        Platform.runLater(() -> status.setText(addressPort.getiPv4Address().getAddress()));
    }

    private void uiAddToList(final IPv4AddressPortView addressPortView) {
        resultsList.getItems().add(addressPortView);
    }

    private void uiAddressMethodChange() {
        final int method = addressMethod.getSelectionModel().getSelectedIndex();

        singleBox.setManaged(method == 0 || method == 2 || method == 3);
        singleBox.setVisible(method == 0 || method == 2 || method == 3);
        multiBox.setManaged(method == 1);
        multiBox.setVisible(method == 1);
    }

    private void uiCidrSelectChange() {
        final boolean selected = cidrFormat.isSelected();

        firstAddress.setPromptText(selected ? "192.168.0.0/16" : "192.168.0.1");
        secondAddress.setDisable(selected);
    }

    private void uiPortMethodChange() {
        final int method = portMethod.getSelectionModel().getSelectedIndex();

        portRangeBox.setManaged(method == 0);
        portRangeBox.setVisible(method == 0);
        customPortBox.setManaged(method == 1);
        customPortBox.setVisible(method == 1);
    }

    private void uiStart() {
        resultsList.getItems().clear();
        configPane.setDisable(true);
        btnStart.getStyleClass().remove("btnPrimary");
        btnStart.getStyleClass().add("btnWarning");
        btnStart.setText("Stop");

        scanProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
    }

    private void uiScanEnd() {
        btnStart.setDisable(false);
        configPane.setDisable(false);
        btnStart.getStyleClass().remove("btnWarning");
        btnStart.getStyleClass().add("btnPrimary");
        btnStart.setText("Start");

        scanProgress.setProgress(1.0);
    }

    private void uiStop() {
        btnStart.setDisable(true);
    }

}
