package com.articleanalyzer;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * GUI version of the MainApp that provides a graphical interface for article analysis
 */
public class MainAppGUI extends JFrame {
    private final DocumentProcessor documentProcessor;
    private final ClassifierModel classifierModel;
    private final Summarizer summarizer;
    private final Tika tika;

    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JButton analyzeButton;
    private JButton loadFileButton;
    private JButton clearButton;
    private JComboBox<String> summaryMethodComboBox;
    private JLabel statusLabel;
    private JLabel topicLabel;
    private JProgressBar progressBar;

    /**
     * Constructor initializes all components
     */
    public MainAppGUI() {
        super("Article Analyzer");
        
        // Initialize NLP components
        documentProcessor = new DocumentProcessor();
        classifierModel = new ClassifierModel();
        summarizer = new Summarizer();
        tika = new Tika();
        
        // Set up the GUI
        setupUI();
        
        // Initialize the classifier with sample data
        initializeClassifier();
    }

    /**
     * Set up the user interface
     */
    private void setupUI() {
        // Set window properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 600));
        
        // Create main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create input panel
        JPanel inputPanel = createInputPanel();
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        
        // Create output panel
        JPanel outputPanel = createOutputPanel();
        
        // Create status panel
        JPanel statusPanel = createStatusPanel();
        
        // Add panels to main panel
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(controlPanel, BorderLayout.CENTER);
        mainPanel.add(outputPanel, BorderLayout.SOUTH);
        
        // Add main panel and status panel to frame
        add(mainPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
        // Center the window on screen
        setLocationRelativeTo(null);
    }

    /**
     * Creates the input panel with text area
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Input Article Text"));
        
        inputTextArea = new JTextArea(10, 50);
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(inputTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Creates the control panel with buttons
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        analyzeButton = new JButton("Analyze Text");
        loadFileButton = new JButton("Load File");
        clearButton = new JButton("Clear");
        
        String[] summaryMethods = {"TextRank", "Frequency-based"};
        summaryMethodComboBox = new JComboBox<>(summaryMethods);
        
        // Add action listeners
        analyzeButton.addActionListener(e -> analyzeText());
        loadFileButton.addActionListener(e -> loadFile());
        clearButton.addActionListener(e -> {
            inputTextArea.setText("");
            outputTextArea.setText("");
            statusLabel.setText("Ready");
            topicLabel.setText("Topic: None");
        });
        
        panel.add(new JLabel("Summary Method:"));
        panel.add(summaryMethodComboBox);
        panel.add(analyzeButton);
        panel.add(loadFileButton);
        panel.add(clearButton);
        
        return panel;
    }

    /**
     * Creates the output panel with results
     */
    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Analysis Results"));
        
        // Create topic panel
        JPanel topicPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topicLabel = new JLabel("Topic: None");
        topicLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topicPanel.add(topicLabel);
        
        // Create output text area
        outputTextArea = new JTextArea(15, 50);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        
        panel.add(topicPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Creates the status panel
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Initializing...");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        
        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(progressBar, BorderLayout.EAST);
        
        return panel;
    }

    /**
     * Initialize the classifier with sample data
     */
    private void initializeClassifier() {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Loading classifier model...");
                progressBar.setIndeterminate(true);
                
                try {
                    classifierModel.loadSampleDataset();
                    classifierModel.trainModel();
                    publish("Classifier initialized successfully.");
                } catch (Exception e) {
                    publish("Error initializing classifier: " + e.getMessage());
                }
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                // Update status with the latest message
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                analyzeButton.setEnabled(true);
                loadFileButton.setEnabled(true);
                statusLabel.setText("Ready");
            }
        };
        
        // Disable buttons until initialization is complete
        analyzeButton.setEnabled(false);
        loadFileButton.setEnabled(false);
        
        // Start the worker
        worker.execute();
    }

    /**
     * Analyze the text from the input text area
     */
    private void analyzeText() {
        String text = inputTextArea.getText().trim();
        
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter some text to analyze.", 
                    "No Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SwingWorker<AnalysisResult, String> worker = new SwingWorker<AnalysisResult, String>() {
            @Override
            protected AnalysisResult doInBackground() throws Exception {
                publish("Analyzing text...");
                progressBar.setIndeterminate(true);
                
                AnalysisResult result = new AnalysisResult();
                
                try {
                    // Classify the text
                    result.topic = classifierModel.classify(text);
                    publish("Classified as: " + result.topic);
                    
                    // Generate summary
                    String summaryMethod = (String) summaryMethodComboBox.getSelectedItem();
                    if ("TextRank".equals(summaryMethod)) {
                        result.summary = summarizer.summarizeWithTextRank(text);
                    } else {
                        result.summary = summarizer.summarizeWithFrequencyScoring(text);
                    }
                    
                    // Get additional metrics
                    List<String> sentences = documentProcessor.tokenizeIntoSentences(text);
                    result.sentenceCount = sentences.size();
                    
                    List<String> tokens = documentProcessor.tokenizeIntoWords(text);
                    result.wordCount = tokens.size();
                    
                    List<String> keyPhrases = documentProcessor.extractKeyPhrases(text);
                    result.keyPhrases = keyPhrases.subList(0, Math.min(5, keyPhrases.size()));
                    
                } catch (Exception e) {
                    publish("Error analyzing text: " + e.getMessage());
                    result.error = e.getMessage();
                }
                
                return result;
            }
            
            @Override
            protected void process(List<String> chunks) {
                // Update status with the latest message
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }
            
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                
                try {
                    AnalysisResult result = get();
                    
                    if (result.error != null) {
                        outputTextArea.setText("Error: " + result.error);
                        return;
                    }
                    
                    // Update topic label
                    topicLabel.setText("Topic: " + result.topic);
                    
                    // Build output text
                    StringBuilder output = new StringBuilder();
                    output.append("Summary:\n");
                    output.append(result.summary);
                    output.append("\n\n");
                    
                    output.append("Text Metrics:\n");
                    output.append("• Sentence Count: ").append(result.sentenceCount).append("\n");
                    output.append("• Word Count (excluding stopwords): ").append(result.wordCount).append("\n\n");
                    
                    output.append("Key Phrases:\n");
                    for (String phrase : result.keyPhrases) {
                        output.append("• ").append(phrase).append("\n");
                    }
                    
                    outputTextArea.setText(output.toString());
                    statusLabel.setText("Analysis complete");
                    
                } catch (Exception e) {
                    outputTextArea.setText("Error retrieving analysis results: " + e.getMessage());
                    statusLabel.setText("Analysis failed");
                }
            }
        };
        
        worker.execute();
    }

    /**
     * Load text from a file
     */
    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Analyze");
        
        // Add file filters
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Word documents", "doc", "docx"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("All supported files", "txt", "pdf", "doc", "docx"));
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            SwingWorker<String, String> worker = new SwingWorker<String, String>() {
                @Override
                protected String doInBackground() throws Exception {
                    publish("Loading file: " + selectedFile.getName());
                    progressBar.setIndeterminate(true);
                    
                    try {
                        return tika.parseToString(selectedFile);
                    } catch (IOException | TikaException e) {
                        publish("Error loading file: " + e.getMessage());
                        throw e;
                    }
                }
                
                @Override
                protected void process(List<String> chunks) {
                    // Update status with the latest message
                    if (!chunks.isEmpty()) {
                        statusLabel.setText(chunks.get(chunks.size() - 1));
                    }
                }
                
                @Override
                protected void done() {
                    progressBar.setIndeterminate(false);
                    
                    try {
                        String fileContent = get();
                        inputTextArea.setText(fileContent);
                        statusLabel.setText("File loaded successfully");
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(MainAppGUI.this,
                                "Error loading file: " + e.getMessage(),
                                "File Load Error", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("File loading failed");
                    }
                }
            };
            
            worker.execute();
        }
    }

    /**
     * Main method to run the application
     */
    public static void main(String[] args) {
        // Set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show the GUI
        SwingUtilities.invokeLater(() -> {
            MainAppGUI app = new MainAppGUI();
            app.setVisible(true);
        });
    }

    /**
     * Class to hold analysis results
     */
    private static class AnalysisResult {
        String topic;
        String summary;
        int sentenceCount;
        int wordCount;
        List<String> keyPhrases;
        String error;
    }
} 