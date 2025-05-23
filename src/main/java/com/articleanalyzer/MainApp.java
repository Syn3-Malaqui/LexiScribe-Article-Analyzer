package com.articleanalyzer;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Main application that accepts user input via CLI, processes it through the classifier and summarizer,
 * and displays the final topic + summary output
 */
public class MainApp {
    private final DocumentProcessor documentProcessor;
    private final ClassifierModel classifierModel;
    private final Summarizer summarizer;
    private final Tika tika;

    /**
     * Constructor initializes all components
     */
    public MainApp() {
        documentProcessor = new DocumentProcessor();
        classifierModel = new ClassifierModel();
        summarizer = new Summarizer();
        tika = new Tika();
    }

    /**
     * Main method to run the application
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        MainApp app = new MainApp();
        app.run();
    }

    /**
     * Runs the application
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===== Article Analyzer =====");
        System.out.println("This application classifies articles into topics and generates summaries.");
        
        try {
            // Initialize the classifier with sample data
            System.out.println("Initializing classifier with sample data...");
            classifierModel.loadSampleDataset();
            classifierModel.trainModel();
            System.out.println("Classifier initialized successfully.");
            
            while (true) {
                System.out.println("\nPlease select an option:");
                System.out.println("1. Analyze text input");
                System.out.println("2. Analyze text from file");
                System.out.println("3. Exit");
                System.out.print("Enter your choice (1-3): ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        analyzeTextInput(scanner);
                        break;
                    case "2":
                        analyzeTextFromFile(scanner);
                        break;
                    case "3":
                        System.out.println("Exiting application. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * Analyzes text input provided by the user
     *
     * @param scanner Scanner for user input
     */
    private void analyzeTextInput(Scanner scanner) {
        System.out.println("\n=== Text Analysis ===");
        System.out.println("Please enter or paste the article text (type 'END' on a new line when finished):");
        
        StringBuilder textBuilder = new StringBuilder();
        String line;
        
        while (!(line = scanner.nextLine()).equals("END")) {
            textBuilder.append(line).append("\n");
        }
        
        String text = textBuilder.toString().trim();
        
        if (text.isEmpty()) {
            System.out.println("No text provided. Returning to main menu.");
            return;
        }
        
        analyzeText(text);
    }

    /**
     * Analyzes text from a file
     *
     * @param scanner Scanner for user input
     */
    private void analyzeTextFromFile(Scanner scanner) {
        System.out.println("\n=== File Analysis ===");
        System.out.print("Enter the path to the file: ");
        String filePath = scanner.nextLine().trim();
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File does not exist or is not a valid file.");
            return;
        }
        
        try {
            String text = extractTextFromFile(file);
            analyzeText(text);
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Extracts text from a file using Apache Tika
     *
     * @param file File to extract text from
     * @return Extracted text
     * @throws IOException If file cannot be read
     * @throws TikaException If text extraction fails
     */
    private String extractTextFromFile(File file) throws IOException, TikaException {
        return tika.parseToString(file);
    }

    /**
     * Analyzes text by classifying it and generating a summary
     *
     * @param text Text to analyze
     */
    private void analyzeText(String text) {
        try {
            System.out.println("\nAnalyzing text...");
            
            // Classify the text
            String topic = classifierModel.classify(text);
            
            // Generate summaries using both methods
            String textRankSummary = summarizer.summarizeWithTextRank(text);
            String frequencySummary = summarizer.summarizeWithFrequencyScoring(text);
            
            // Display results
            System.out.println("\n=== Analysis Results ===");
            System.out.println("Topic Classification: " + topic);
            
            System.out.println("\nTextRank Summary:");
            System.out.println(textRankSummary);
            
            System.out.println("\nFrequency-based Summary:");
            System.out.println(frequencySummary);
            
            // Display additional metrics
            displayTextMetrics(text);
            
        } catch (Exception e) {
            System.err.println("Error analyzing text: " + e.getMessage());
        }
    }

    /**
     * Displays additional text metrics
     *
     * @param text Text to analyze
     */
    private void displayTextMetrics(String text) {
        try {
            // Get sentence count
            List<String> sentences = documentProcessor.tokenizeIntoSentences(text);
            int sentenceCount = sentences.size();
            
            // Get word count (excluding stopwords)
            List<String> tokens = documentProcessor.tokenizeIntoWords(text);
            int wordCount = tokens.size();
            
            // Get key phrases
            List<String> keyPhrases = documentProcessor.extractKeyPhrases(text);
            
            System.out.println("\n=== Text Metrics ===");
            System.out.println("Sentence Count: " + sentenceCount);
            System.out.println("Word Count (excluding stopwords): " + wordCount);
            
            System.out.println("\nTop Key Phrases:");
            int phrasesToShow = Math.min(5, keyPhrases.size());
            for (int i = 0; i < phrasesToShow; i++) {
                System.out.println("- " + keyPhrases.get(i));
            }
        } catch (Exception e) {
            System.err.println("Error calculating text metrics: " + e.getMessage());
        }
    }
} 