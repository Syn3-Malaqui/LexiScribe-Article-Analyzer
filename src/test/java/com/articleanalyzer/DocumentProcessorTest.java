package com.articleanalyzer;

import org.junit.Test;
import static org.junit.Assert.*;

public class DocumentProcessorTest {

    @Test
    public void testCleanText() {
        DocumentProcessor processor = new DocumentProcessor();
        String input = "Hello, world! This is a test.";
        String expected = "hello world this is a test";
        assertEquals(expected, processor.cleanText(input));
    }
} 