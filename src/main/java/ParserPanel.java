import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ParserPanel extends JPanel {
    private final MontoyaApi api;
    private final Consumer<ParsingConfig> onRun;

        // Flag checkboxes
    private final JCheckBox proxyHistoryCheckbox = new JCheckBox("Proxy History");
    private final JCheckBox proxyHistoryResponseCheckbox = new JCheckBox("+ Response");
    private final JCheckBox siteMapCheckbox = new JCheckBox("Site Map");
    private final JCheckBox siteMapResponseCheckbox = new JCheckBox("+ Response");

    // Search section
    private final JCheckBox responseHeaderCheckbox = new JCheckBox("Header:");
    private final JTextField responseHeaderField = new JTextField(30);
    private final JCheckBox responseBodyCheckbox = new JCheckBox("Body:");
    private final JTextField responseBodyField = new JTextField(30);

    // Ignore extensions section
    private final JTextField ignoreExtField = new JTextField(ParsingConfig.DEFAULT_IGNORED_EXTENSIONS, 50);

    // Content-type filtering
    private final JCheckBox ignoreContentTypeCheckbox = new JCheckBox("Response Content-Type:");
    private final JTextField ignoreContentTypeField = new JTextField(ParsingConfig.DEFAULT_IGNORED_CONTENT_TYPES, 50);

    // Output file section
    private final JTextField outputFileField = new JTextField(40);
    private final JButton browseButton = new JButton("Browse...");
    private final JComboBox<String> formatCombo = new JComboBox<>(new String[]{"CSV (.csv)", "SQLite (.db)"});

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
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Flags section
        JPanel flagsPanel = createSection("Data Sources");

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
        centerPanel.add(Box.createVerticalStrut(4));

        // Search section
        JPanel searchPanel = createSection("Regex Search");
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
        centerPanel.add(Box.createVerticalStrut(4));

        // Ignore extensions section
        JPanel ignorePanel = createSection("Filter Static Files");
        JPanel ignoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ignoreRow.add(new JLabel("Extensions:"));
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
        centerPanel.add(Box.createVerticalStrut(4));

        // Output file section
        JPanel outputPanel = createSection("Output");
        JPanel formatRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formatRow.add(new JLabel("Format:"));
        formatRow.add(Box.createHorizontalStrut(5));
        formatRow.add(formatCombo);
        outputPanel.add(formatRow);

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fileRow.add(new JLabel("File:"));
        fileRow.add(Box.createHorizontalStrut(5));
        fileRow.add(outputFileField);
        fileRow.add(Box.createHorizontalStrut(5));
        fileRow.add(browseButton);
        outputPanel.add(fileRow);

        browseButton.addActionListener(e -> chooseOutputFile());
        formatCombo.addActionListener(e -> onFormatChanged());

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
        boolean isCsv = formatCombo.getSelectedIndex() == 0;
        var csvFilter = new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv");
        var dbFilter = new javax.swing.filechooser.FileNameExtensionFilter("SQLite database (*.db)", "db");
        chooser.addChoosableFileFilter(csvFilter);
        chooser.addChoosableFileFilter(dbFilter);
        chooser.setFileFilter(isCsv ? csvFilter : dbFilter);
        String path = outputFileField.getText().trim();
        if (path.isEmpty()) {
            chooser.setSelectedFile(new java.io.File(isCsv ? "output.csv" : "output.db"));
        } else {
            chooser.setSelectedFile(new java.io.File(path));
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String chosen = chooser.getSelectedFile().getAbsolutePath();
            var filter = chooser.getFileFilter();
            if (filter instanceof javax.swing.filechooser.FileNameExtensionFilter fef) {
                String ext = fef.getExtensions()[0];
                if (!chosen.toLowerCase().endsWith("." + ext)) {
                    chosen += "." + ext;
                }
            }
            outputFileField.setText(chosen);
        }
    }

    private void onFormatChanged() {
        String current = outputFileField.getText().trim();
        boolean isCsv = formatCombo.getSelectedIndex() == 0;
        if (current.isEmpty()) {
            outputFileField.setText(isCsv ? "output.csv" : "output.db");
        } else {
            String lower = current.toLowerCase();
            if ((isCsv && lower.endsWith(".db")) || (!isCsv && lower.endsWith(".csv"))) {
                String base = lower.endsWith(".csv") ? current.substring(0, current.length() - 4)
                                                     : current.substring(0, current.length() - 3);
                outputFileField.setText(base + (isCsv ? ".csv" : ".db"));
            }
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
