package es.upm.alumnos.profundizacion.wc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.OS;

import es.upm.grise.profundizacion.wc.App;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.stream.SystemOut;

@ExtendWith(SystemStubsExtension.class)
public class AppTest {

	@TempDir
    private static Path dir;
    private static Path emptyFile;
    private static Path quotes;
    
    @BeforeAll
    private static void initFiles() throws IOException {
        emptyFile = dir.resolve("emptyFile.txt");
        Files.createFile(emptyFile);
        quotes = dir.resolve("quotes.txt");
        Files.write(quotes,
                List.of("Without music, life would be a mistake. - Nietzsche",
                        "There's no point in being grown up if you can't act childish sometimes. - 4th Doctor",
                        "Forget the past. No one becomes successful in the past. - Anonymous")
                );
    }
    
    /*
     * Issues on Windows working with @TempDir
     * https://github.com/junit-team/junit5/issues/2811
     */
    @AfterEach
	@EnabledOnOs(OS.WINDOWS)
	private void cleanUp() {
		System.gc();
	}
    
    private static void callApp(String... args) {
        App.main(args);
    }

    @SystemStub
    private SystemOut sysOut;

    private List<String> getOutLines() {
        return sysOut.getLines().collect(Collectors.toList());
    }

    @Test
    public void testNoArgs() {
        callApp();
        assertLinesMatch(List.of("Usage: wc [-clw file]"), getOutLines(), "The program should print how to use it!");
    }

    @Test
    public void testOneArg() {
        callApp("-w");
        assertLinesMatch(List.of("Wrong arguments!"), getOutLines(), "The program only work with 2 arguments!");
    }

    @Test
    public void testMoreThanTwoArgs() {
        callApp("-w", "file.txt", "other.txt");
        assertLinesMatch(List.of("Wrong arguments!"), getOutLines(), "The program only work with 2 arguments!");
    }

    @Test
    public void testAnNonExistingFile() {
        final String filename = "no_exist_file.txt";
        callApp("-w", filename);
        final List<String> output = getOutLines();
        assertLinesMatch(List.of("Cannot find file.*"), output,
                "The program should report if the file cannot be found!");
        assertLinesMatch(List.of(".*" + Pattern.quote(filename)), output, "The program should report the name of the file!");
    }

    @Test
    public void testAnEmptyFile() {
        final String filename = emptyFile.toString();
        callApp("-cwl", filename);
        final List<String> output = getOutLines();
        assertLinesMatch(List.of("\\s\\d+\\s\\d+\\s\\d+.*"), output,
                "The program should report as many counts requested!");
        assertLinesMatch(List.of(".*" + Pattern.quote(filename)), output,
                "The program should report the name of the file that had been processed!");
    }

    @Test
    public void testAWrittenFileTwoOptions() {
        final String filename = quotes.toString();
        callApp("-cl", filename);
        final List<String> output = getOutLines();
        assertLinesMatch(List.of("\\s\\d+\\s\\d+.*"), output,
                "The program should report as many counts requested!");
        assertLinesMatch(List.of(".*" + Pattern.quote(filename)), output,
                "The program should report the name of the file that had been processed!");
    }
}
