package es.upm.alumnos.profundizacion.wc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import es.upm.grise.profundizacion.wc.Counter;

public class CounterTest {

    private static final String TEST_TEXT = " \thello\n\n\n this is a test text\n\t",
            BLANK = " ",
            ONE_WORD = "word",
            TWO_WORDS = " one two",
            GERMAN_WORDS = "Fußball, Eselsbrücke und Fremdschämen ",
            LINES = "Hello     Richard Feynman!\n",
            LINES1 = "\nHej     Søren Kierkegaard!";


    static BufferedReader latestBufferedReader;

    private static Counter createCounter(String testedString) throws IOException {
        latestBufferedReader = new BufferedReader(new StringReader(testedString));
        return new Counter(latestBufferedReader);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {TEST_TEXT, BLANK, ONE_WORD, TWO_WORDS, GERMAN_WORDS, LINES, LINES1})
    public void getNumberCharactersTest(String testedString) throws IOException {
        Counter counter = createCounter(testedString);
        assertEquals(testedString.length(), counter.getNumberCharacters(), "Not correct count of characters for input <"+testedString+">");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {TEST_TEXT, BLANK, ONE_WORD, TWO_WORDS, GERMAN_WORDS, LINES, LINES1})
    public void getNumberWordsTest(String testedString) throws IOException {
        Counter counter = createCounter(testedString);
        if ( ! testedString.isBlank())
            assertEquals(testedString.trim().split("\\s+").length, counter.getNumberWords(), "Not correct count of words for input <"+testedString+">");
        else
            assertEquals(0, counter.getNumberWords());
    }

    @ParameterizedTest 
    @EmptySource
    @ValueSource(strings = {TEST_TEXT, BLANK, ONE_WORD, TWO_WORDS, GERMAN_WORDS, LINES, LINES1})
    public void getNumberLines(String testedString) throws IOException {
        Counter counter = createCounter(testedString);
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(testedString);
        int lines = 0;
        while (m.find()){lines ++;}
        assertEquals(lines, counter.getNumberLines(), "Not correct count of words for lines <"+testedString+">");
    }

    @AfterEach
    public void closeBufferedReader() throws IOException {
        latestBufferedReader.close();
    }
}