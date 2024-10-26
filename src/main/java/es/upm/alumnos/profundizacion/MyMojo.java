package es.upm.alumnos.profundizacion;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Goal execute tsdetect without any Test Smell detected.
 */
@Mojo( name = "tsdetect", defaultPhase = LifecyclePhase.TEST )
public class MyMojo extends AbstractMojo
{
	private static final String JAVA_EXT = ".java";
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
	
	public class ProductionFile extends SuperObject
	{
		private String fullname;  // es.upm.Master
		private String name;      // Master
		private String filepath;  // /path/to/project/es/upm/Master.java
		
		public ProductionFile ( final String filepath )
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
    
    //configuration has higher preference over pom project property.
    @Parameter(defaultValue = "<blank>", property = "tsdetect.inputCSV", required = false)
    private String inputCSV;
    
    @Parameter
    private String nothing;

    public void execute() throws MojoExecutionException
    {
    	info("pwd: %s", pwd());
    	Map context = getPluginContext();
    	info("context: %s", context);
    	info("Auto variables:");
    	printAttribute("sourceCodeDir", "- %s: %s");
    	printAttribute("testCodeDir", "- %s: %s");
    	info("Other variables:");
    	printAttribute("nothing", "- %s: %s");
    	printAttribute("inputCSV", "- %s: %s");
    	info("Gathering production files:");
    	final List<ProductionFile> prodFiles = getProductionFiles();
    	prodFiles.forEach( file -> info("- %s", file) );
    	//Report errors through MojoExecutionException, wrapping actual Exception
    	//throw reportException(e, "Error creating file %s", filename);
    	if (context != null)
			for(Object entry: context.entrySet())
			{
				info("[%s]: %s", entry.getClass(), entry);
			}
    }
    
    public List<ProductionFile> getProductionFiles ( )
    {
    	//final List<ProductionFile> result = new ArrayList<>();
    	//getFullpathFiles(sourceCodeDir).forEach();
    	return getFullpathFiles(sourceCodeDir).stream().filter(file -> file.endsWith(JAVA_EXT)).map(ProductionFile::new).collect(Collectors.toList());
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
    
    private void printAttribute ( String name, String... fmt )
    {
    	Class<?> clazz = this.getClass();
    	try {
			Field attribute = clazz.getDeclaredField(name);//find(clazz.getFi);
			if (fmt.length != 0)
				info(fmt[0], name, attribute.get(this));
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