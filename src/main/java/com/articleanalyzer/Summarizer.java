package com.articleanalyzer;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements extractive summarization using TextRank or frequency-based sentence scoring
 */
public class Summarizer {
    private final DocumentProcessor documentProcessor;
    private final StanfordCoreNLP pipeline;
    private final double similarityThreshold = 0.3;
    private final int maxSummaryLength = 3; // Maximum number of sentences in summary

    /**
     * Constructor initializes the document processor and NLP pipeline
     */
    public Summarizer() {
        documentProcessor = new DocumentProcessor();
        
        // Initialize Stanford CoreNLP pipeline
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        pipeline = new StanfordCoreNLP(props);
    }

    /**
     * Generates a summary using TextRank algorithm
     *
     * @param text Input text to summarize
     * @return Summary text (2-3 sentences)
     */
    public String summarizeWithTextRank(String text) {
        // Extract sentences
        List<String> sentences = extractSentences(text);
        if (sentences.size() <= maxSummaryLength) {
            return String.join(" ", sentences);
        }
        
        // Build similarity matrix
        double[][] similarityMatrix = buildSimilarityMatrix(sentences);
        
        // Apply TextRank algorithm
        double[] scores = textRankScores(similarityMatrix, 50);
        
        // Get top sentences
        List<ScoredSentence> scoredSentences = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            scoredSentences.add(new ScoredSentence(sentences.get(i), scores[i], i));
        }
        
        // Sort by score (descending)
        scoredSentences.sort(Comparator.comparing(ScoredSentence::getScore).reversed());
        
        // Take top sentences and sort by original position
        List<ScoredSentence> topSentences = scoredSentences.subList(0, Math.min(maxSummaryLength, scoredSentences.size()));
        topSentences.sort(Comparator.comparing(ScoredSentence::getPosition));
        
        // Build summary
        return topSentences.stream()
                .map(ScoredSentence::getSentence)
                .collect(Collectors.joining(" "));
    }

    /**
     * Generates a summary using frequency-based scoring
     *
     * @param text Input text to summarize
     * @return Summary text (2-3 sentences)
     */
    public String summarizeWithFrequencyScoring(String text) {
        // Extract sentences
        List<String> sentences = extractSentences(text);
        if (sentences.size() <= maxSummaryLength) {
            return String.join(" ", sentences);
        }
        
        // Clean text and get word frequencies
        String cleanedText = documentProcessor.cleanText(text);
        List<String> tokens = documentProcessor.tokenizeIntoWords(cleanedText);
        Map<String, Integer> wordFrequencies = documentProcessor.getTokenFrequencies(tokens);
        
        // Score sentences based on word frequencies
        List<ScoredSentence> scoredSentences = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            String cleanedSentence = documentProcessor.cleanText(sentence);
            List<String> sentenceTokens = documentProcessor.tokenizeIntoWords(cleanedSentence);
            
            // Calculate score as sum of word frequencies
            double score = sentenceTokens.stream()
                    .mapToDouble(token -> wordFrequencies.getOrDefault(token, 0))
                    .sum();
            
            // Normalize by sentence length to avoid bias towards longer sentences
            if (!sentenceTokens.isEmpty()) {
                score /= sentenceTokens.size();
            }
            
            scoredSentences.add(new ScoredSentence(sentence, score, i));
        }
        
        // Sort by score (descending)
        scoredSentences.sort(Comparator.comparing(ScoredSentence::getScore).reversed());
        
        // Take top sentences and sort by original position
        List<ScoredSentence> topSentences = scoredSentences.subList(0, Math.min(maxSummaryLength, scoredSentences.size()));
        topSentences.sort(Comparator.comparing(ScoredSentence::getPosition));
        
        // Build summary
        return topSentences.stream()
                .map(ScoredSentence::getSentence)
                .collect(Collectors.joining(" "));
    }

    /**
     * Extracts sentences from text using Stanford CoreNLP
     *
     * @param text Input text
     * @return List of sentences
     */
    private List<String> extractSentences(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        List<CoreMap> sentenceAnnotations = document.get(CoreAnnotations.SentencesAnnotation.class);
        List<String> sentences = new ArrayList<>();
        
        for (CoreMap sentenceAnnotation : sentenceAnnotations) {
            String sentence = sentenceAnnotation.toString().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        
        return sentences;
    }

    /**
     * Builds a similarity matrix for sentences
     *
     * @param sentences List of sentences
     * @return Similarity matrix
     */
    private double[][] buildSimilarityMatrix(List<String> sentences) {
        int size = sentences.size();
        double[][] similarityMatrix = new double[size][size];
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    similarityMatrix[i][j] = 1.0;
                } else {
                    similarityMatrix[i][j] = calculateSimilarity(sentences.get(i), sentences.get(j));
                }
            }
        }
        
        return similarityMatrix;
    }

    /**
     * Calculates cosine similarity between two sentences
     *
     * @param sentence1 First sentence
     * @param sentence2 Second sentence
     * @return Similarity score
     */
    private double calculateSimilarity(String sentence1, String sentence2) {
        // Clean and tokenize sentences
        String cleanedSentence1 = documentProcessor.cleanText(sentence1);
        String cleanedSentence2 = documentProcessor.cleanText(sentence2);
        
        List<String> tokens1 = documentProcessor.tokenizeIntoWords(cleanedSentence1);
        List<String> tokens2 = documentProcessor.tokenizeIntoWords(cleanedSentence2);
        
        // Create sets of tokens
        Set<String> set1 = new HashSet<>(tokens1);
        Set<String> set2 = new HashSet<>(tokens2);
        
        // Calculate intersection and union
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        // Calculate Jaccard similarity
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }

    /**
     * Implements TextRank algorithm to score sentences
     *
     * @param similarityMatrix Similarity matrix
     * @param iterations Number of iterations
     * @return Array of scores
     */
    private double[] textRankScores(double[][] similarityMatrix, int iterations) {
        int size = similarityMatrix.length;
        double[] scores = new double[size];
        Arrays.fill(scores, 1.0 / size);
        
        double dampingFactor = 0.85;
        
        for (int iter = 0; iter < iterations; iter++) {
            double[] newScores = new double[size];
            
            for (int i = 0; i < size; i++) {
                double sum = 0.0;
                
                for (int j = 0; j < size; j++) {
                    if (i != j && similarityMatrix[j][i] > similarityThreshold) {
                        // Calculate outbound link sum for j
                        double outboundSum = 0.0;
                        for (int k = 0; k < size; k++) {
                            if (similarityMatrix[j][k] > similarityThreshold) {
                                outboundSum += similarityMatrix[j][k];
                            }
                        }
                        
                        if (outboundSum > 0) {
                            sum += similarityMatrix[j][i] * scores[j] / outboundSum;
                        }
                    }
                }
                
                newScores[i] = (1 - dampingFactor) + dampingFactor * sum;
            }
            
            scores = newScores;
        }
        
        return scores;
    }

    /**
     * Helper class to store a sentence with its score and original position
     */
    private static class ScoredSentence {
        private final String sentence;
        private final double score;
        private final int position;

        public ScoredSentence(String sentence, double score, int position) {
            this.sentence = sentence;
            this.score = score;
            this.position = position;
        }

        public String getSentence() {
            return sentence;
        }

        public double getScore() {
            return score;
        }

        public int getPosition() {
            return position;
        }
    }
} 