package io.mattw.jports.ui;

import com.formdev.flatlaf.FlatLaf;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;

public class MainWindow extends JFrame {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final BrowserUtil browserUtil = new BrowserUtil();
    private final ClipboardUtil clipboardUtil = new ClipboardUtil();

    private final JTextArea addressesList, portsList;
    private final JComboBox<String> modeCombo;
    private final JSpinner timeoutMs, threadCount;
    private final JButton controlBtn;
    private final JProgressBar progress;
    private final JTable resultsTable;

    public MainWindow() throws Exception {
        logger.info("MainWindow()");

        var icon = ImageIO.read(Objects.requireNonNull(getClass().getResource("/icon.png")));

        setIconImage(icon);
        setTitle("JPorts Scanner");
        setSize(450, 600);
        setMinimumSize(new Dimension(450, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("fillx"));

        var centerX = (Toolkit.getDefaultToolkit().getScreenSize().width - getSize().width) / 2;
        var centerY = (Toolkit.getDefaultToolkit().getScreenSize().height - getSize().height) / 2;
        setLocation(centerX, centerY);

        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0094FF"));

        var configPane = new JPanel(new MigLayout("", "[right]rel[grow]", "[]10[]"));
        //configPane.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        add(configPane, "grow, wrap");

        var configLabel = new JLabel("Configuration");
        configLabel.putClientProperty("FlatLaf.style", "font: 150% $light.font");
        configPane.add(configLabel, "left, span 2, wrap");

        var leftWidth = "width 80:80:80";

        var modeChoices = new String[]{"Address(es)", "Endless Increase", "Endless Decrease"};
        modeCombo = new JComboBox<>(modeChoices);
        configPane.add(new JLabel("Scan Mode"), leftWidth);
        configPane.add(modeCombo, "wrap");

        addressesList = new JTextArea("192.168.0.1/24, 192.168.1.120, 192.168.2.1-192.168.2.255");
        addressesList.setLineWrap(true);
        addressesList.setWrapStyleWord(true);
        addressesList.setRows(2);
        configPane.add(new JLabel("Address(es)"), leftWidth);
        var addressesPanel = new JScrollPane(addressesList);
        addressesPanel.setMinimumSize(new Dimension(0, 45));
        configPane.add(addressesPanel, "grow, wrap");

        portsList = new JTextArea("21, 22, 80, 443, 8000-8888");
        portsList.setLineWrap(true);
        portsList.setWrapStyleWord(true);
        portsList.setRows(2);
        configPane.add(new JLabel("Port(s)"), leftWidth);
        var portsPanel = new JScrollPane(portsList);
        portsPanel.setMinimumSize(new Dimension(0, 45));
        configPane.add(portsPanel, "grow, wrap");

        timeoutMs = new JSpinner(new SpinnerNumberModel(301, 1, 60000, 50));
        configPane.add(new JLabel("Timeout (ms)"), leftWidth);
        configPane.add(timeoutMs, "wrap");

        threadCount = new JSpinner(new SpinnerNumberModel(64, 1, 1000, 8));
        configPane.add(new JLabel("Thread Count"), leftWidth);
        configPane.add(threadCount, "wrap");

        var scannerPane = new JPanel(new MigLayout("fillx"));
        //scannerPane.setBorder(BorderFactory.createLineBorder(Color.RED));
        add(scannerPane, "grow, wrap");

        var scannerLabel = new JLabel("Scanner");
        scannerLabel.putClientProperty("FlatLaf.style", "font: 150% $light.font");
        scannerPane.add(scannerLabel, "left, wrap");

        controlBtn = new JButton("Start");
        scannerPane.add(controlBtn, "wrap, width 80:80:9999");
        controlBtn.addActionListener((e) -> startScan());

        progress = new JProgressBar();
        progress.setStringPainted(true);
        progress.setString("Idle");
        scannerPane.add(progress, "grow, wrap, height 25:25:25");

        resultsTable = new JTable();
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setModel(new DefaultTableModel(
                new Object[][]{
                        {"C", "192.168.0.1:80", "L"},
                        {"C", "192.168.0.1:443", "L"}
                }, new String[]{"", "Open Port", "Web Link"}
        ) {
            final Class<?>[] columnTypes = {Object.class, String.class, Object.class};
            final boolean[] columnEditable = {false, false, false};

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnEditable[columnIndex];
            }
        });

        var scrollPane = new JScrollPane(resultsTable);
        scannerPane.add(scrollPane, "grow, height 0:999999:999999");

        var window = this;
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                // debugMessage("App started");
                // split.setDividerLocation(0.65);
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
        this.addWindowStateListener(e -> {
            // if ((e.getNewState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
            //     logger.debug("Minified");
            // }
            if ((e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                SwingUtilities.invokeLater(window::doLayout);
            }
        });
    }

    private void toggleScan() {

    }

    private void startScan() {
        SwingUtilities.invokeLater(() -> {
            controlBtn.setText("Stop");
            progress.setIndeterminate(true);
            progress.setString("Starting");
        });
    }

}
