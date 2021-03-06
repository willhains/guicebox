package org.guicebox;

import com.google.inject.*;
import com.google.inject.name.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;

/**
 * Binds both explicit {@link BindingAnnotation}s and {@link Named} annotations to constant values supplied in the
 * environment via properties files.
 * <p>
 * Any and all properties files in the relative paths {@code properties/} and/or {@code properties/username/} (where
 * <i>username</i> is the login ID of the current user) will be read, and the key-value pairs mapped as Guice constants.
 * <p>
 * Values are given precedence in the following order:
 * <ol>
 * <li>supplied in properties/username/*.properties</li>
 * <li>supplied in properties/*.properties</li>
 * <li>default value, when using {@code Inject(optional=true)}</li>
 * </ol>
 * 
 * @author willhains
 */
public class PropertiesModule extends AbstractModule
{
	/*
	 * Read the Java Logging configuration from properties/logging.properties rather than requiring a parameter to be
	 * passed to the JVM. However, if a JVM parameter has been supplied, this will not override it.
	 */
	static
	{
		if(System.getProperty("java.util.logging.config.file") == null) try
		{
			System.setProperty("java.util.logging.config.file", "./properties/logging.properties");
			LogManager.getLogManager().readConfiguration();
		}
		catch(Exception e)
		{
			Logger.getLogger(PropertiesModule.class.getName()).log(Level.WARNING, "Could not load logger config", e);
		}
	}
	
	private final Logger _log;
	
	// Current user ID - used to define the environment
	private static final String _USERNAME = System.getProperty("user.name");
	
	public PropertiesModule()
	{
		this(_loadLogHeader(), _loadPropertiesFiles());
	}
	
	private static String _loadLogHeader()
	{
		// Load the log header
		final StringBuffer header = new StringBuffer();
		final File headerFile = new File("loghead.txt");
		if(headerFile.exists()) try
		{
			final BufferedReader reader = _read(headerFile);
			for(String line; (line = reader.readLine()) != null; header.append('\n').append(line));
		}
		catch(IOException e)
		{
			// ignore
		}
		return header.toString();
	}
	
	private static List<InputStream> _loadPropertiesFiles()
	{
		final List<InputStream> propFiles = new ArrayList<InputStream>();
		for(String path : Arrays.asList("properties/", "properties/" + _USERNAME + "/"))
		{
			final File propDir = new File(path);
			try
			{
				if(propDir.exists() && propDir.isDirectory()) for(File file : propDir.listFiles())
				{
					if(file.getName().endsWith(".properties"))
					{
						propFiles.add(new BufferedInputStream(new FileInputStream(file)));
					}
				}
			}
			catch(FileNotFoundException e)
			{
				// impossible
			}
		}
		return propFiles;
	}
	
	private static BufferedReader _read(File file) throws FileNotFoundException
	{
		return new BufferedReader(new FileReader(file));
	}
	
	// Maps constant names to their values
	private final Properties _constValues = new Properties();
	
	/**
	 * Should be called only from unit tests.
	 */
	PropertiesModule(String header, List<InputStream> propertiesFiles)
	{
		// Print the log header
		_log = Logger.getLogger(PropertiesModule.class.getName());
		if(header != null && header.trim().length() > 0) _log.info(header);
		
		// Load properties files from the environment
		for(InputStream propFile : propertiesFiles)
		{
			try
			{
				_constValues.load(propFile);
			}
			catch(IOException e)
			{
				_log.warning("unable to load properties: " + e);
			}
		}
	}
	
	/**
	 * Sets the value of the specified constant. Constant values may be set multiple times, and the last value set when
	 * Guice {@link #configure() configures} the module will be used by the application.
	 * 
	 * @param key the name of the constant - may be either the FQN of a {@link BindingAnnotation} or a simple name.
	 * @param value the value of the constant.
	 */
	protected final void setConstant(String key, String value)
	{
		_constValues.put(key, value);
	}
	
	/**
	 * @return the value of the specified key.
	 */
	protected final String getConstant(String key)
	{
		return _constValues.getProperty(key);
	}
	
	/**
	 * Loads the collected constant values into Guice.
	 */
	@Override protected final void configure()
	{
		// Bind the current user ID
		bindConstant().annotatedWith(UserName.class).to(_USERNAME);
		
		// Find BindingAnnotations to bind
		final StringBuilder properties = new StringBuilder(System.getProperty("line.separator"));
		for(Object oKey : _constValues.keySet())
		{
			final String key = String.valueOf(oKey);
			final String value = String.valueOf(_constValues.get(key));
			
			// Mask passwords
			final String displayValue = key.toLowerCase().endsWith("password") ? "********" : value;
			
			try
			{
				// Find binding annotation
				final Class<?> annotation = Class.forName(key);
				if(annotation.getAnnotation(BindingAnnotation.class) == null) throw new ClassNotFoundException();
				
				// Bind constant with binding annotation if available...
				final Class<? extends Annotation> bindingAnnotation = annotation.asSubclass(Annotation.class);
				bindConstant().annotatedWith(bindingAnnotation).to(value);
				properties.append(String.format("   @" + _FORMAT, key, displayValue));
			}
			catch(ClassNotFoundException e)
			{
				// ...if not, bind named constant
				bindConstant().annotatedWith(Names.named(key)).to(value);
				properties.append(String.format("    " + _FORMAT, key, displayValue));
			}
		}
		_log.info(properties.toString());
	}
	
	private static final String _FORMAT = "%-60s= %s%n";
}
