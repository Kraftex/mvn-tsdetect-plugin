package es.upm.alumnos.profundizacion;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Goal execute tsdetect without any Test Smell detected.
 */
@Mojo( name = "tsdetect", defaultPhase = LifecyclePhase.TEST )
public class MyMojo extends AbstractMojo
{
	private static final Object VERBOSE = new Object();
	private static final String JAVA_EXT = ".java";
	private static final Predicate<String> isJavaFile = file -> file.endsWith(JAVA_EXT);
	
	public static class SuperObject
	{
		@Override
		public String toString()
		{
			final StringBuilder result = new StringBuilder(this.getClass().toString()).append('\n');
			for( Field attribute: this.getClass().getDeclaredFields() )
			{
				attribute.setAccessible(true);
				try {
					result.append(attribute.getName()).append(": ").append(attribute.get(this)).append('\n');
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return result.toString();
		}
	}
	
	public class InfoFile extends SuperObject
	{
		private String fullname;  // es.upm.Master
		private String name;      // Master
		private String filepath;  // /path/to/project/es/upm/Master.java
		
		public InfoFile ( final String filepath )
		{
			this.filepath = filepath;
			final String source = sourceCodeDir.getAbsolutePath();
			String relative = filepath.substring(source.length());
			fullname = relative.replaceAll(Pattern.quote(File.separator), ".").substring(1, relative.length() - JAVA_EXT.length());
			name = fullname.substring(fullname.lastIndexOf('.') + 1);
		}
		public String fullname() { return fullname; }
		public String name() { return name; }
		public String filepath() { return filepath; }
	}
	
	private MojoExecutionException reportException ( Throwable ex, String fmt, Object... args )
	{
		return new MojoExecutionException(String.format(fmt, args), ex);
	}

	private void info (String fmt, Object... args)
	{
		if (args.length > 0 && args[args.length - 1] == VERBOSE && !verbose)
		{
			return;
		}
		getLog().info(String.format(fmt, args));
	}
	
	private void warn (String fmt, Object... args)
	{
		getLog().warn(String.format(fmt, args));
	}
	
	private void error (String fmt, Object... args)
	{
		getLog().error(String.format(fmt, args));
	}
	
    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceCodeDir; 

    @Parameter(defaultValue = "${project.build.testSourceDirectory}")
    private File testCodeDir;
    
    @Parameter(defaultValue = "${project.reporting.outputDirectory}")
    private File projReportDir;
    
    @Parameter(defaultValue = "${project.artifactId}")
    private String projArtifactId;
    
    //configuration has higher preference over pom project property.
    @Parameter(property = "tsdetect.verbose")
    private boolean verbose = false;
    
    @Parameter(property = "tsdetect.threshold")
    private long threshold = 0;
    
    @Parameter(defaultValue = "java", property = "tsdetect.java")
    private String java;
    
    @Parameter(property = "tsdetect.jar", required = true)
    private File jar;
    
    public void execute() throws MojoExecutionException
    {
    	info("pwd: %s", pwd(), VERBOSE);
    	info("java.home: ",System.getProperty("java.home"), VERBOSE);
    	info("Auto variables:", VERBOSE);
    	printAttribute("sourceCodeDir", "- %s: %s");
    	printAttribute("testCodeDir", "- %s: %s");
    	printAttribute("projReportDir", "- %s: %s");
    	printAttribute("projArtifactId", "- %s: %s");
    	info("Other variables:", VERBOSE);
    	printAttribute("verbose", "- %s: %s");
    	printAttribute("threshold", "- %s: %s");
    	printAttribute("jar", "- %s: %s");
    	printAttribute("java", "- %s: %s");
    	if (jar == null)
    	{
    		throw reportException(null, "Property 'tsdetect.jar' or configuration for 'jar' no configure.");
    	}
    	if (!jar.exists())
    	{
    		throw reportException(null, "File '%s' doesn't exist.\nCheck property 'tsdetect.jar' or configuration for 'jar'.", jar.getAbsolutePath());
    	}
    	if (!projReportDir.exists())
    	{
    		info("Creating reporting directory: %s", projReportDir.getAbsolutePath());
    		projReportDir.mkdirs();
    	}
    	info("Gathering production files:", VERBOSE);
    	final List<InfoFile> prodFiles = getProductionFiles();
    	prodFiles.forEach( file -> info("- %s", file, VERBOSE) );
    	info("Gathering test files:", VERBOSE);
    	final List<InfoFile> testFiles = getTestFiles();
    	testFiles.forEach( file -> info("- %s", file, VERBOSE) );
    	info("Matching prod-test files:", VERBOSE);
    	final Map<InfoFile, InfoFile> matchedFiles = matchProductionToTestFile(prodFiles, testFiles);
    	matchedFiles.entrySet().forEach( entry -> info("- %s > %s", entry.getKey().name(), entry.getValue().name(), VERBOSE) );
    	File inputCSV = null;
    	try {
			inputCSV = File.createTempFile("inputCSV", null);
			inputCSV.deleteOnExit();
			writeInputCSV(inputCSV, matchedFiles);
		}
    	catch (IOException e) {
    		throw reportException(e, "Can't create the input file to run the JAR.\nDirectory: %s", pwd());
		}
    	try {
    		Process jarRunning = runJAR(inputCSV);
    		jarRunning.waitFor();
    		printOutputFor(jarRunning);
		}
    	catch (InterruptedException e) {
    		throw reportException(e, "There was an error running the JAR.");
		}
    	final File lastOutputReport = getLastOutputReport();
        
        if (lastOutputReport == null) throw new MojoExecutionException("No output test smells files were found");

    	long totalTSDetected = reportCSVGenerated(lastOutputReport);
    	if (totalTSDetected != 0)
    	{
    		if (totalTSDetected > threshold)
    		{
    			error("Test Smells threshold exceeded!");
    			error("Threshold: %d", threshold);
    			error("Total Test Smells: %d", totalTSDetected);
    			error("Aborting execution");
    			throw new MojoExecutionException("Test Smells threshold exceeded!");
    		}
			warn("Total Test Smells: %d", totalTSDetected);
    	}
    }
    
    private File getLastOutputReport() throws MojoExecutionException {
        final String PREFIX = "Output_TestSmellDetection_";
        final String EXTENSION = ".csv";
        try {
            return Stream.of(projReportDir.listFiles())
                .filter(file -> !file.isDirectory())
                .filter(file -> file.getName().startsWith(PREFIX) && file.getName().endsWith(EXTENSION))
                .filter(file -> {
                    String timestamp = file.getName().substring(PREFIX.length(), file.getName().length() - EXTENSION.length());
                    return timestamp.matches("\\d+");
                })
                .max(Comparator.comparingLong(file ->
                    Long.parseLong(file.getName().substring(PREFIX.length(), file.getName().length() - EXTENSION.length()))
                ))
                .orElse(null);
        } catch (NumberFormatException e) {
            throw reportException(e, "Couldn't parse a filename's milisecond timestamp as long");
        }
    }
    
    private long reportCSVGenerated ( File lastOutputReport ) throws MojoExecutionException
    {
    	long result = 0;
    	try {
            final int NON_TEST_SMELL_COLUMNS = 7;

            List<String> smells = Files.lines(lastOutputReport.toPath())
                           .findFirst()
                           .map(line -> Arrays.stream(line.split(","))
                                              .skip(NON_TEST_SMELL_COLUMNS) 
                                              .collect(Collectors.toList()))
                           .orElse(Collections.emptyList());

            boolean printedBefore = false;
            for(String line: Files.lines(lastOutputReport.toPath()).skip(1).collect(Collectors.toList()))
            {
				final String[] splitLine = line.split(",");
				final String app = splitLine[0];
				final String testFile = splitLine[2];
				for (int i = NON_TEST_SMELL_COLUMNS; i < splitLine.length; i++) {
					String str = splitLine[i];
					if (str.matches("\\d+") && Long.parseLong(str) > 0) {
						long count = Long.parseLong(str);
						result += count;
						if (!printedBefore)
						{
							warn("Detected test smells:");
							warn(" -       App: %s", app);
							warn(" - Test File: %s", testFile);
						}
						warn("  + %s count: %d", smells.get(i - NON_TEST_SMELL_COLUMNS), count);                            
						printedBefore = true;
					}
				}
				printedBefore = false;
            }
        } catch (IOException e) {
            throw reportException(e, "Couldn't open specified output report file");
        }
    	return result;
    }
    
    public void writeInputCSV ( final File inputCSV, final Map<InfoFile, InfoFile> matchedFiles ) throws MojoExecutionException
    {
        try {
            FileWriter outputfile = new FileWriter(inputCSV);
            CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
            for (Map.Entry<InfoFile, InfoFile> entry : matchedFiles.entrySet()) {
                String[] header = { projArtifactId, entry.getValue().filepath, entry.getKey().filepath}; 
                writer.writeNext(header); 
            }
            writer.close();
        } catch (IOException e) {
            throw reportException(e, "failed to write temporary csv file for .jar input");
        }
    }
    
    private Process runJAR ( File inputCSV ) throws MojoExecutionException
    {
    	String[] cmdline = {"java", "-jar", jar.getAbsolutePath(), inputCSV.getAbsolutePath()};
    	ProcessBuilder runner = new ProcessBuilder(cmdline);
    	runner.redirectErrorStream(true);
		runner.directory(projReportDir);
		try {
			info("Starting command: %s", String.join(" ", cmdline), VERBOSE);
			return runner.start();
		}
		catch (IOException e) {
    		throw reportException(e, "Can't run the JAR.\nCheck your PATH variable or set on configuration <java> the full path to java executable.");
		}
    }
    
    private List<InfoFile> getProductionFiles ( )
    {
    	return getFullpathFiles(sourceCodeDir).stream().filter(isJavaFile).map(InfoFile::new).collect(Collectors.toList());
    }
    
    private List<InfoFile> getTestFiles ( )
    {
    	return getFullpathFiles(testCodeDir).stream().filter(isJavaFile).map(InfoFile::new).collect(Collectors.toList());
    }
    
    private boolean checkProductionMatchTestFile ( InfoFile prodFile, InfoFile testFile )
    {
    	return testFile.name().equals(prodFile.name()+"Test")
    		   || testFile.name().equals(prodFile.name()+"TestSuite")
    		   || testFile.name().equals("Test"+prodFile.name());
    }
    
    private Map<InfoFile, InfoFile> matchProductionToTestFile ( final List<InfoFile> prodFiles, final List<InfoFile> testFiles )
    {
    	Map<InfoFile, InfoFile> result = new HashMap<>();
    	boolean[] testFilesMask = new boolean[testFiles.size()];
    	for ( InfoFile file: prodFiles )
    	{
    		for ( int i = 0; i < testFilesMask.length; i++ )
    		{
    			if (testFilesMask[i])
    				continue;
    			InfoFile testFile = testFiles.get(i);
    			if (checkProductionMatchTestFile(file, testFile))
    			{
    				testFilesMask[i] = true;
    				result.put(file, testFile);
    			}
    		}
    	}
    	return result;
    }
    
    private List<String> getFullpathFiles ( File dir )
    {
    	final List<String> result = new ArrayList<>();
    	getFullpathFiles(dir, result);
    	return result;
    }
    
    private void getFullpathFiles ( File dir, List<String> result )
    {
    	if (dir.isFile())
    		result.add(dir.getAbsolutePath());
    	if (!dir.isDirectory())
    		return;
    	for (File files: dir.listFiles())
    	{
    		getFullpathFiles(files, result);
    	}
    }

    private void printOutputFor ( Process proc ) throws MojoExecutionException
	{
		InputStream out = proc.getInputStream();
		byte bytes[];
		try {
			bytes = new byte[out.available()];
			out.read(bytes,0,bytes.length);
			info(new String(bytes), VERBOSE);
		}
		catch (IOException e) {
			throw reportException(e, "There was an error reading the output for the process");
		}
	}
    
    private void printAttribute ( String name, String fmt )
    {
    	Class<?> clazz = this.getClass();
    	try {
			Field attribute = clazz.getDeclaredField(name);
			if (fmt != null)
				info(fmt, name, attribute.get(this), VERBOSE);
			else
				info("%s: %s", name, attribute.get(this), VERBOSE);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private String pwd ( )
    {
    	return new File(".").getAbsolutePath();
    }
}