package es.upm.alumnos.profundizacion;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Goal execute tsdetect without any Test Smell detected.
 */
@Mojo( name = "tsdetect", defaultPhase = LifecyclePhase.TEST )
public class MyMojo extends AbstractMojo
{
	private static final String JAVA_EXT = ".java";
	private static final Predicate<String> isJavaFile = file -> file.endsWith(JAVA_EXT);
	public class ProcessHolder
	{
		/*public static ProcessHolder from ( ProcessBuilder builder )
		{
			return new ProcessHolder(builder.start());
		}*/
		
		private Process proc;
		
		public ProcessHolder ( Process proc )
		{
			this.proc = proc;
			info("PID[%d]", proc.pid());
		}
		
		public ProcessHolder waitFor ( ) throws InterruptedException
		{
			proc.waitFor();
			return this;
		}
		
		public void printOutput ( )
		{
			InputStream out = proc.getInputStream();
			byte b[];
			try {
				b = new byte[out.available()];
				out.read(b,0,b.length);
				info(new String(b));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
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
		if (args.length > 0 && args[args.length - 1] instanceof Throwable)
		{
			getLog().info(String.format(fmt, args), (Throwable)args[args.length - 1]);
			return;
		}
		getLog().info(String.format(fmt, args));
	}
	
	private void warn (String fmt, Object... args)
	{
		if (args.length > 0 && args[args.length - 1] instanceof Throwable)
		{
			getLog().warn(String.format(fmt, args), (Throwable)args[args.length - 1]);
			return;
		}
		getLog().warn(String.format(fmt, args));
	}
	
	private void error (String fmt, Object... args)
	{
		if (args.length > 0 && args[args.length - 1] instanceof Throwable)
		{
			getLog().error(String.format(fmt, args), (Throwable)args[args.length - 1]);
			return;
		}
		getLog().error(String.format(fmt, args));
	}
	
    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceCodeDir; 

    @Parameter(defaultValue = "${project.build.testSourceDirectory}")
    private File testCodeDir;
    
    @Parameter(defaultValue = "${project.reporting.outputDirectory}")
    private File projReportDir;
    
    @Parameter(defaultValue = "${project.groupId}")
    private String projGroupId;
    
    @Parameter(defaultValue = "${project.artifactId}")
    private String projArtifactId;
    
    //configuration has higher preference over pom project property.
    @Parameter(property = "tsdetect.jar", required = true)
    private File jar;
    
    //TODO: Make debugs to be output as infos
    @Parameter(property = "tsdetect.verbose")
    private boolean verbose = false;
    
    @Parameter(defaultValue = "java", property = "tsdetect.java", required = false)
    private String java;
    
    @Parameter
    private String nothing;

    public void execute() throws MojoExecutionException
    {
    	info("pwd: %s", pwd());
    	info("java.home: ",System.getProperty("java.home"));
    	Map context = getPluginContext();
    	if (context != null)
    	{
			for(Object entry: context.entrySet())
			{
				info("[%s]: %s", entry.getClass(), entry);
			}
    	}
    	info("context: %s", context);
    	info("Auto variables:");
    	printAttribute("sourceCodeDir", "- %s: %s");
    	printAttribute("testCodeDir", "- %s: %s");
    	printAttribute("projReportDir", "- %s: %s");
    	printAttribute("projGroupId", "- %s: %s");
    	printAttribute("projArtifactId", "- %s: %s");
    	info("Other variables:");
    	printAttribute("nothing", "- %s: %s");
    	printAttribute("verbose", "- %s: %s");
    	printAttribute("jar", "- %s: %s");
    	if (!jar.exists())
    	{
    		throw reportException(null, "File '%s' doesn't exist.\nCheck property 'tsdetect.jar' or configuration for 'jar'.", jar.getAbsolutePath());
    	}
    	if (!projReportDir.exists())
    	{
    		info("Creating reporting directory: %s", projReportDir.getAbsolutePath());
    		projReportDir.mkdirs();
    	}
    	info("Gathering production files:");
    	final List<InfoFile> prodFiles = getProductionFiles();
    	prodFiles.forEach( file -> info("- %s", file) );
    	info("Gathering test files:");
    	final List<InfoFile> testFiles = getTestFiles();
    	testFiles.forEach( file -> info("- %s", file) );
    	info("Matching prod-test files:");
    	final Map<InfoFile, InfoFile> matchedFiles = matchProductionToTestFile(prodFiles, testFiles);
    	matchedFiles.entrySet().forEach( entry -> info("- %s > %s", entry.getKey().name(), entry.getValue().name()) );
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
    		//new ProcessHolder(runCMD("pwd")).waitFor().printOutput();
			new ProcessHolder(runJAR(inputCSV)).waitFor().printOutput();
		}
    	catch (InterruptedException e) {
    		throw reportException(e, "There was an error running the JAR.");
		}
    	
    	//Report errors through MojoExecutionException, wrapping actual Exception
    	//throw reportException(e, "Error creating file %s", filename);
    }
    
    private File getLastOutputReport ( )
    {
    	return null;
    }
    
    private Process runCMD ( String... cmd ) throws MojoExecutionException
    {
    	ProcessBuilder runner = new ProcessBuilder(cmd);
    	runner.redirectErrorStream(true);
		//runner.directory(projReportDir);
		try {
			return runner.start();
		}
		catch (IOException e) {
    		throw reportException(e, "Can't run the JAR.");
		}
    }
    
    private Process runJAR ( File inputCSV ) throws MojoExecutionException
    {
    	ProcessBuilder runner = new ProcessBuilder("java", "-jar", jar.getAbsolutePath(), inputCSV.getAbsolutePath());
    	runner.redirectErrorStream(true);
		runner.directory(projReportDir);
		try {
			return runner.start();
		}
		catch (IOException e) {
    		throw reportException(e, "Can't run the JAR.");
		}
    }
    
    public void writeInputCSV ( final File inputCSV, final Map<InfoFile, InfoFile> mathedFiles )
    {
    	//TODO
    	//Format to write: appName, testFile, sourceFile
    }
    
    public List<InfoFile> getProductionFiles ( )
    {
    	return getFullpathFiles(sourceCodeDir).stream().filter(isJavaFile).map(InfoFile::new).collect(Collectors.toList());
    }
    
    public List<InfoFile> getTestFiles ( )
    {
    	return getFullpathFiles(testCodeDir).stream().filter(isJavaFile).map(InfoFile::new).collect(Collectors.toList());
    }
    
    public boolean checkProductionMatchTestFile ( InfoFile prodFile, InfoFile testFile )
    {
    	return testFile.name().equals(prodFile.name()+"Test")
    		   || testFile.name().equals(prodFile.name()+"TestSuite")
    		   || testFile.name().equals("Test"+prodFile.name());
    }
    
    public Map<InfoFile, InfoFile> matchProductionToTestFile ( final List<InfoFile> prodFiles, final List<InfoFile> testFiles )
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
    
    public List<String> getFullpathFiles ( File dir )
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
    
    private void printAttribute ( String name, String fmt )
    {
    	Class<?> clazz = this.getClass();
    	try {
			Field attribute = clazz.getDeclaredField(name);
			if (fmt != null)
				info(fmt, name, attribute.get(this));
			else
				info("%s: %s", name, attribute.get(this));
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