package com.articleanalyzer;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVLoader;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads datasets, vectorizes input, and trains a topic classifier
 */
public class ClassifierModel {
    private Classifier classifier;
    private StringToWordVector vectorizer;
    private Instances trainingData;
    private ArrayList<String> classValues;
    private Attribute classAttribute;
    
    /**
     * Constructor initializes the classifier and vectorizer
     */
    public ClassifierModel() {
        // Use Naive Bayes - better for text classification with small datasets
        classifier = new NaiveBayes();
        
        // Initialize vectorizer with proper text classification settings
        vectorizer = new StringToWordVector();
        
        // Configure for proper text feature extraction
        vectorizer.setIDFTransform(true);
        vectorizer.setTFTransform(true);
        vectorizer.setLowerCaseTokens(true);
        vectorizer.setOutputWordCounts(true);
        vectorizer.setWordsToKeep(500);  // Keep top 500 words
        vectorizer.setMinTermFreq(1);    // Include words that appear at least once
        vectorizer.setDoNotOperateOnPerClassBasis(false);
        
        // Use word tokenizer
        WordTokenizer tokenizer = new WordTokenizer();
        vectorizer.setTokenizer(tokenizer);
    }
    
    /**
     * Loads a CSV dataset for training
     *
     * @param filePath Path to the CSV file
     * @param textColumnIndex Index of the text column
     * @param classColumnIndex Index of the class column
     * @throws Exception If loading fails
     */
    public void loadCSVDataset(String filePath, int textColumnIndex, int classColumnIndex) throws Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(filePath));
        
        // Configure CSV loader to treat first column as string
        loader.setStringAttributes("first");  // Treat first column as string
        
        Instances data = loader.getDataSet();
        
        System.out.println("DEBUG: Loaded CSV with " + data.numInstances() + " instances and " + data.numAttributes() + " attributes");
        System.out.println("DEBUG: Attribute types:");
        for (int i = 0; i < data.numAttributes(); i++) {
            System.out.println("  " + i + ": " + data.attribute(i).name() + " (" + 
                             (data.attribute(i).isString() ? "String" : 
                              data.attribute(i).isNominal() ? "Nominal" : "Other") + ")");
        }
        
        // Set class attribute
        data.setClassIndex(classColumnIndex);
        
        // Process the dataset
        processDataset(data, textColumnIndex);
    }
    
    /**
     * Loads an ARFF dataset for training
     *
     * @param filePath Path to the ARFF file
     * @param textAttributeName Name of the text attribute
     * @param classAttributeName Name of the class attribute
     * @throws Exception If loading fails
     */
    public void loadARFFDataset(String filePath, String textAttributeName, String classAttributeName) throws Exception {
        ArffLoader loader = new ArffLoader();
        loader.setSource(new File(filePath));
        Instances data = loader.getDataSet();
        
        int textIndex = data.attribute(textAttributeName).index();
        int classIndex = data.attribute(classAttributeName).index();
        data.setClassIndex(classIndex);
        
        // Process the dataset
        processDataset(data, textIndex);
    }
    
    /**
     * Loads a sample dataset for demo purposes
     * 
     * @throws Exception If loading fails
     */
    public void loadSampleDataset() throws Exception {
        // Load the sample dataset from CSV file in resources
        try {
            // Try to load from resources first
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("sample_articles.csv");
            if (is != null) {
                // Create a temporary file to work with Weka's CSV loader
                java.io.File tempFile = java.io.File.createTempFile("sample_articles", ".csv");
                tempFile.deleteOnExit();
                
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                is.close();
                
                loadCSVDataset(tempFile.getAbsolutePath(), 0, 1);
            } else {
                // Fallback to file path method
                String resourcePath = getClass().getClassLoader().getResource("sample_articles.csv").getPath();
                loadCSVDataset(resourcePath, 0, 1);
            }
        } catch (Exception e) {
            System.err.println("Error loading sample dataset: " + e.getMessage());
            throw e;
        }
        
        // Debug: Print class distribution
        System.out.println("Loaded training data with " + trainingData.numInstances() + " instances");
        System.out.println("Classes found: " + classValues);
        
        // Print class distribution
        int[] classCounts = new int[classValues.size()];
        for (int i = 0; i < trainingData.numInstances(); i++) {
            classCounts[(int) trainingData.instance(i).classValue()]++;
        }
        
        for (int i = 0; i < classValues.size(); i++) {
            System.out.println(classValues.get(i) + ": " + classCounts[i] + " instances");
        }
    }
    
    /**
     * Process dataset by applying vectorization
     *
     * @param data The dataset
     * @param textColumnIndex Index of the text column
     * @throws Exception If processing fails
     */
    private void processDataset(Instances data, int textColumnIndex) throws Exception {
        // Extract class values
        classAttribute = data.classAttribute();
        classValues = new ArrayList<>();
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classValues.add(classAttribute.value(i));
        }
        
        System.out.println("DEBUG: Processing dataset with " + data.numInstances() + " instances");
        System.out.println("DEBUG: Text column index: " + textColumnIndex);
        System.out.println("DEBUG: Class column index: " + data.classIndex());
        
        // Ensure the text attribute is string type for vectorization
        if (!data.attribute(textColumnIndex).isString()) {
            throw new Exception("Text attribute must be of String type for vectorization");
        }
        
        // Reset and configure vectorizer
        vectorizer = new StringToWordVector();
        vectorizer.setIDFTransform(true);
        vectorizer.setTFTransform(true);
        vectorizer.setLowerCaseTokens(true);
        vectorizer.setOutputWordCounts(true);
        vectorizer.setWordsToKeep(500);
        vectorizer.setMinTermFreq(1);
        vectorizer.setDoNotOperateOnPerClassBasis(false);
        
        // Set tokenizer
        WordTokenizer tokenizer = new WordTokenizer();
        vectorizer.setTokenizer(tokenizer);
        
        // Configure to process only the text attribute (1-based indexing)
        vectorizer.setAttributeIndices(String.valueOf(textColumnIndex + 1));
        
        System.out.println("DEBUG: Setting up vectorizer input format...");
        vectorizer.setInputFormat(data);
        
        System.out.println("DEBUG: Applying vectorization...");
        trainingData = Filter.useFilter(data, vectorizer);
        
        System.out.println("DEBUG: Vectorization complete:");
        System.out.println("  - Original attributes: " + data.numAttributes());
        System.out.println("  - Vectorized attributes: " + trainingData.numAttributes());
        
        if (trainingData.numAttributes() > 10) {
            System.out.println("  - Sample word features:");
            for (int i = 0; i < Math.min(10, trainingData.numAttributes() - 1); i++) {
                System.out.println("    " + i + ": " + trainingData.attribute(i).name());
            }
        } else {
            System.out.println("  - WARNING: Very few attributes created - vectorization may have failed!");
        }
    }
    
    /**
     * Trains the classifier on the loaded dataset
     *
     * @throws Exception If training fails
     */
    public void trainModel() throws Exception {
        if (trainingData == null) {
            throw new IllegalStateException("No training data loaded. Load a dataset first.");
        }
        
        System.out.println("DEBUG: Training classifier...");
        System.out.println("DEBUG: Training data attributes: " + trainingData.numAttributes());
        System.out.println("DEBUG: Training data instances: " + trainingData.numInstances());
        
        // Print some feature information
        System.out.println("DEBUG: First few attribute names:");
        for (int i = 0; i < Math.min(10, trainingData.numAttributes()); i++) {
            System.out.println("  Attribute " + i + ": " + trainingData.attribute(i).name());
        }
        
        // Print class distribution in training data
        System.out.println("DEBUG: Class distribution in training data:");
        int[] classDistribution = new int[classValues.size()];
        for (int i = 0; i < trainingData.numInstances(); i++) {
            int classIndex = (int) trainingData.instance(i).classValue();
            classDistribution[classIndex]++;
        }
        for (int i = 0; i < classValues.size(); i++) {
            System.out.println("  " + classValues.get(i) + " (index " + i + "): " + classDistribution[i] + " instances");
        }
        
        // Build the classifier
        classifier.buildClassifier(trainingData);
        System.out.println("DEBUG: Classifier training completed");
        
        // Test the classifier on a few training examples to see if it learned
        System.out.println("DEBUG: Testing classifier on training examples:");
        for (int i = 0; i < Math.min(3, trainingData.numInstances()); i++) {
            double prediction = classifier.classifyInstance(trainingData.instance(i));
            double actualClass = trainingData.instance(i).classValue();
            System.out.println("  Instance " + i + ": actual=" + classValues.get((int)actualClass) + 
                             ", predicted=" + classValues.get((int)prediction));
        }
    }
    
    /**
     * Sets the classifier type
     *
     * @param classifierType Type of classifier to use ("naivebayes" or "decisiontree")
     */
    public void setClassifierType(String classifierType) {
        if ("decisiontree".equalsIgnoreCase(classifierType)) {
            classifier = new J48();
        } else {
            // Default to Naive Bayes
            classifier = new NaiveBayes();
        }
    }
    
    /**
     * Classifies a given text into one of the predefined topics
     *
     * @param text Text to classify
     * @return Predicted class label
     * @throws Exception If classification fails
     */
    public String classify(String text) throws Exception {
        if (classifier == null || trainingData == null) {
            throw new IllegalStateException("Model not trained. Train the model first.");
        }
        
        System.out.println("DEBUG: Classifying text: " + text.substring(0, Math.min(100, text.length())) + "...");
        
        // Create a simple test dataset with same structure as training
        ArrayList<Attribute> testAttributes = new ArrayList<>();
        
        // Create string attribute for text
        testAttributes.add(new Attribute("text", (ArrayList<String>) null));
        
        // Create class attribute with same values as training
        ArrayList<String> classLabels = new ArrayList<>();
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classLabels.add(classAttribute.value(i));
        }
        Attribute testClassAttribute = new Attribute("topic", classLabels);
        testAttributes.add(testClassAttribute);
        
        // Create instances
        Instances testData = new Instances("TestData", testAttributes, 0);
        testData.setClassIndex(1);
        
        // Add the text instance
        double[] values = new double[2];
        values[0] = testData.attribute(0).addStringValue(text);
        values[1] = Utils.missingValue(); // Missing class value
        
        Instance testInstance = new DenseInstance(1.0, values);
        testData.add(testInstance);
        
        // Apply the same vectorizer transformation
        Instances transformedTestData = Filter.useFilter(testData, vectorizer);
        Instance transformedInstance = transformedTestData.instance(0);
        
        // Set the dataset reference to the training data for compatibility
        transformedInstance.setDataset(trainingData);
        
        // Get prediction probabilities for debugging
        double[] probabilities = classifier.distributionForInstance(transformedInstance);
        
        System.out.println("DEBUG: Class probabilities:");
        for (int i = 0; i < probabilities.length && i < classValues.size(); i++) {
            System.out.println("  " + classValues.get(i) + ": " + String.format("%.3f", probabilities[i]));
        }
        
        // Get the predicted class
        double prediction = classifier.classifyInstance(transformedInstance);
        String predictedClass = classAttribute.value((int) prediction);
        
        System.out.println("DEBUG: Predicted class: " + predictedClass);
        
        return predictedClass;
    }
    
    /**
     * Evaluates the model using cross-validation
     *
     * @param folds Number of folds for cross-validation
     * @return Evaluation metrics as a string
     * @throws Exception If evaluation fails
     */
    public String evaluateModel(int folds) throws Exception {
        if (trainingData == null) {
            throw new IllegalStateException("No training data loaded. Load a dataset first.");
        }
        
        Evaluation evaluation = new Evaluation(trainingData);
        evaluation.crossValidateModel(classifier, trainingData, folds, new Random(1));
        
        return evaluation.toSummaryString() + "\n" + evaluation.toClassDetailsString();
    }
    
    /**
     * Gets the list of possible class values (topics)
     *
     * @return List of class values
     */
    public List<String> getClassValues() {
        return classValues;
    }
} 