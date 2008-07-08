package org.codeshark.guicebox;

import com.google.inject.*;
import com.google.inject.name.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;

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
	public PropertiesModule()
	{
		this(Log.forClass().getLogger(), _loadLogHeader(), _loadPropertiesFiles());
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
	
	private static List<Reader> _loadPropertiesFiles()
	{
		final List<Reader> propFiles = new ArrayList<Reader>();
		for(String path : Arrays.asList("properties/", "properties/" + System.getProperty("user.name") + "/"))
		{
			final File propDir = new File(path);
			try
			{
				if(propDir.exists() && propDir.isDirectory()) for(File file : propDir.listFiles())
				{
					if(file.getName().endsWith(".properties")) propFiles.add(_read(file));
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
	
	private final Logger log;
	
	// Maps constant names to their values
	private final Properties _constValues = new Properties();
	
	/**
	 * Should be called only from unit tests.
	 */
	PropertiesModule(Logger logger, String header, List<Reader> propertiesFiles)
	{
		log = logger;
		
		// Print the log header
		if(header != null && header.trim().length() > 0) log.log(LogLevel.INFO, header);
		
		// Load properties files from the environment
		for(Reader reader : propertiesFiles)
		{
			try
			{
				_constValues.load(reader);
			}
			catch(IOException e)
			{
				log.log(LogLevel.ERROR, "unable to load properties: " + e);
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
	@Override
	protected final void configure()
	{
		// Find BindingAnnotations to bind
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
				log.log(LogLevel.INFO, String.format("   @" + _FORMAT, key, displayValue));
			}
			catch(ClassNotFoundException e)
			{
				// ...if not, bind named constant
				bindConstant().annotatedWith(Names.named(key)).to(value);
				log.log(LogLevel.INFO, String.format("    " + _FORMAT, key, displayValue));
			}
		}
	}
	
	private static final String _FORMAT = "%-50s= %s";
}
