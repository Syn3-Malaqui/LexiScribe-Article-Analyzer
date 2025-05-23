package com.articleanalyzer;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles text cleaning, tokenization, and basic NLP tasks
 */
public class DocumentProcessor {
    private final StanfordCoreNLP pipeline;
    private final Set<String> stopwords;
    private final Pattern punctuationPattern;

    /**
     * Constructor that initializes the NLP pipeline and stopwords
     */
    public DocumentProcessor() {
        // Set up Stanford CoreNLP pipeline with required annotators
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
        
        // Initialize punctuation pattern
        punctuationPattern = Pattern.compile("[^a-zA-Z0-9\\s]");
        
        // Load common English stopwords
        stopwords = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "but", "or", "for", "nor", "on", "at", "to", "by", "about",
            "in", "of", "with", "this", "that", "these", "those", "is", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "do", "does", "did", "can", "could", "will",
            "would", "shall", "should", "may", "might", "must", "i", "you", "he", "she", "it", "we",
            "they", "me", "him", "her", "us", "them", "who", "whom", "which", "what", "whose"
        ));
    }

    /**
     * Reads text from a file
     *
     * @param filePath Path to the file
     * @return Text content of the file
     * @throws IOException If file cannot be read
     */
    public String readTextFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Cleans text by removing punctuation, converting to lowercase, and removing extra whitespace
     *
     * @param text Input text
     * @return Cleaned text
     */
    public String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Convert to lowercase
        String cleanedText = text.toLowerCase();
        
        // Remove punctuation
        cleanedText = punctuationPattern.matcher(cleanedText).replaceAll(" ");
        
        // Remove extra whitespace
        cleanedText = cleanedText.trim().replaceAll("\\s+", " ");
        
        return cleanedText;
    }

    /**
     * Tokenizes text into sentences
     *
     * @param text Input text
     * @return List of sentences
     */
    public List<String> tokenizeIntoSentences(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        return sentences.stream()
                .map(CoreMap::toString)
                .collect(Collectors.toList());
    }

    /**
     * Tokenizes text into words and removes stopwords
     *
     * @param text Input text
     * @return List of tokens (words)
     */
    public List<String> tokenizeIntoWords(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        List<String> tokens = new ArrayList<>();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class).toLowerCase();
                if (!stopwords.contains(word) && word.length() > 1) {
                    tokens.add(word);
                }
            }
        }
        
        return tokens;
    }

    /**
     * Lemmatizes tokens (reduces words to their base form)
     *
     * @param text Input text
     * @return Map of original tokens to their lemmas
     */
    public Map<String, String> lemmatizeTokens(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        Map<String, String> lemmaMap = new HashMap<>();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                lemmaMap.put(word, lemma);
            }
        }
        
        return lemmaMap;
    }

    /**
     * Extracts key phrases from text based on part-of-speech patterns
     *
     * @param text Input text
     * @return List of key phrases
     */
    public List<String> extractKeyPhrases(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        
        List<String> keyPhrases = new ArrayList<>();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            StringBuilder currentPhrase = new StringBuilder();
            
            for (int i = 0; i < tokens.size(); i++) {
                CoreLabel token = tokens.get(i);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                
                // Simple pattern: adjective + noun
                if (pos.startsWith("JJ") && i < tokens.size() - 1) {
                    CoreLabel nextToken = tokens.get(i + 1);
                    String nextPos = nextToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    String nextWord = nextToken.get(CoreAnnotations.TextAnnotation.class);
                    
                    if (nextPos.startsWith("NN")) {
                        keyPhrases.add(word + " " + nextWord);
                    }
                }
                
                // Collect noun phrases
                if (pos.startsWith("NN") && !stopwords.contains(word.toLowerCase())) {
                    if (currentPhrase.length() > 0) {
                        currentPhrase.append(" ");
                    }
                    currentPhrase.append(word);
                } else if (currentPhrase.length() > 0) {
                    String phrase = currentPhrase.toString();
                    if (StringUtils.countMatches(phrase, " ") > 0) {
                        keyPhrases.add(phrase);
                    }
                    currentPhrase = new StringBuilder();
                }
            }
            
            // Add the last phrase if it exists
            if (currentPhrase.length() > 0 && StringUtils.countMatches(currentPhrase.toString(), " ") > 0) {
                keyPhrases.add(currentPhrase.toString());
            }
        }
        
        return keyPhrases;
    }

    /**
     * Gets the frequency distribution of tokens in the text
     *
     * @param tokens List of tokens
     * @return Map of tokens to their frequencies
     */
    public Map<String, Integer> getTokenFrequencies(List<String> tokens) {
        Map<String, Integer> frequencies = new HashMap<>();
        
        for (String token : tokens) {
            frequencies.put(token, frequencies.getOrDefault(token, 0) + 1);
        }
        
        return frequencies;
    }
} 