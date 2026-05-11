import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ParserPanel extends JPanel {
    private final MontoyaApi api;
    private final Consumer<ParsingConfig> onRun;

    // Flag checkboxes
    private final JCheckBox proxyHistoryCheckbox = new JCheckBox("Proxy History");
    private final JCheckBox proxyHistoryResponseCheckbox = new JCheckBox("include responses");
    private final JCheckBox siteMapCheckbox = new JCheckBox("Site Map");
    private final JCheckBox siteMapResponseCheckbox = new JCheckBox("include responses");

    // Search section
    private final JCheckBox responseHeaderCheckbox = new JCheckBox("Response Header regex:");
    private final JTextField responseHeaderField = new JTextField(30);
    private final JCheckBox responseBodyCheckbox = new JCheckBox("Response Body regex:");
    private final JTextField responseBodyField = new JTextField(30);

    // Ignore extensions section
    private final JTextField ignoreExtField = new JTextField(ParsingConfig.DEFAULT_IGNORED_EXTENSIONS, 50);

    // Content-type filtering
    private final JCheckBox ignoreContentTypeCheckbox = new JCheckBox("Ignore response with these Content-Type:");
    private final JTextField ignoreContentTypeField = new JTextField(ParsingConfig.DEFAULT_IGNORED_CONTENT_TYPES, 50);

    // Output file section
    private final JTextField outputFileField = new JTextField(40);
    private final JButton browseButton = new JButton("Browse...");

    // Actions
    private final JButton runButton = new JButton("Run");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar();

    public ParserPanel(MontoyaApi api, Consumer<ParsingConfig> onRun) {
        this.api = api;
        this.onRun = onRun;
        initUI();
        api.userInterface().applyThemeToComponent(this);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Flags section
        JPanel flagsPanel = createSection("Extract Data From");

        JPanel proxyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        proxyRow.add(proxyHistoryCheckbox);
        proxyRow.add(Box.createHorizontalStrut(12));
        proxyRow.add(proxyHistoryResponseCheckbox);
        proxyHistoryResponseCheckbox.setEnabled(false);
        proxyHistoryCheckbox.addActionListener(e ->
                proxyHistoryResponseCheckbox.setEnabled(proxyHistoryCheckbox.isSelected()));
        flagsPanel.add(proxyRow);

        JPanel siteMapRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        siteMapRow.add(siteMapCheckbox);
        siteMapRow.add(Box.createHorizontalStrut(12));
        siteMapRow.add(siteMapResponseCheckbox);
        siteMapResponseCheckbox.setEnabled(false);
        siteMapCheckbox.addActionListener(e ->
                siteMapResponseCheckbox.setEnabled(siteMapCheckbox.isSelected()));
        flagsPanel.add(siteMapRow);

        centerPanel.add(flagsPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        // Search section
        JPanel searchPanel = createSection("Search Responses");
        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.add(responseHeaderCheckbox);
        headerRow.add(responseHeaderField);
        responseHeaderField.setEnabled(false);
        responseHeaderCheckbox.addActionListener(e ->
                responseHeaderField.setEnabled(responseHeaderCheckbox.isSelected()));
        searchPanel.add(headerRow);

        JPanel bodyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bodyRow.add(responseBodyCheckbox);
        bodyRow.add(responseBodyField);
        responseBodyField.setEnabled(false);
        responseBodyCheckbox.addActionListener(e ->
                responseBodyField.setEnabled(responseBodyCheckbox.isSelected()));
        searchPanel.add(bodyRow);

        centerPanel.add(searchPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        // Ignore extensions section
        JPanel ignorePanel = createSection("Ignore Static Files");
        JPanel ignoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ignoreRow.add(new JLabel("Ignore URLs ending with extensions:"));
        ignoreRow.add(Box.createHorizontalStrut(5));
        ignoreRow.add(ignoreExtField);
        ignorePanel.add(ignoreRow);

        JPanel contentTypeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contentTypeRow.add(ignoreContentTypeCheckbox);
        contentTypeRow.add(Box.createHorizontalStrut(5));
        contentTypeRow.add(ignoreContentTypeField);
        ignoreContentTypeField.setEnabled(false);
        ignoreContentTypeCheckbox.addActionListener(e ->
                ignoreContentTypeField.setEnabled(ignoreContentTypeCheckbox.isSelected()));
        ignorePanel.add(contentTypeRow);

        centerPanel.add(ignorePanel);
        centerPanel.add(Box.createVerticalStrut(10));

        // Output file section
        JPanel outputPanel = createSection("Output");
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fileRow.add(new JLabel("Save to:"));
        fileRow.add(Box.createHorizontalStrut(5));
        fileRow.add(outputFileField);
        fileRow.add(Box.createHorizontalStrut(5));
        fileRow.add(browseButton);
        outputPanel.add(fileRow);

        browseButton.addActionListener(e -> chooseOutputFile());

        centerPanel.add(outputPanel);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom: Run button + status
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        runButton.addActionListener(e -> runParsing());
        bottomPanel.add(runButton);
        bottomPanel.add(progressBar);
        progressBar.setVisible(false);
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Output As");
        chooser.setSelectedFile(new java.io.File("output.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runParsing() {
        ParsingConfig config = new ParsingConfig(
                proxyHistoryCheckbox.isSelected(),
                proxyHistoryCheckbox.isSelected() && proxyHistoryResponseCheckbox.isSelected(),
                siteMapCheckbox.isSelected(),
                siteMapCheckbox.isSelected() && siteMapResponseCheckbox.isSelected(),
                responseHeaderCheckbox.isSelected(),
                responseHeaderField.getText(),
                responseBodyCheckbox.isSelected(),
                responseBodyField.getText(),
                outputFileField.getText().isEmpty() ? null : outputFileField.getText(),
                ParsingConfig.parseExtensions(ignoreExtField.getText()),
                ignoreContentTypeCheckbox.isSelected(),
                ParsingConfig.parseContentTypes(ignoreContentTypeField.getText())
        );

        if (!config.proxyHistory() && !config.siteMap()
                && !config.responseHeader() && !config.responseBody()) {
            statusLabel.setText("Please select at least one option.");
            return;
        }

        runButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Running...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                onRun.accept(config);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Complete");
                } catch (Exception e) {
                    statusLabel.setText("Error: " + e.getMessage());
                }
                runButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
            }
        }.execute();
    }
}
