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
 * <li>default value, when using {@code @Inject(optional=true)}</li>
 * </ol>
 * 
 * @author willhains
 */
public class PropertiesModule extends AbstractModule
{
	// Maps constant names to their values
	protected final Map<String, String> _constValues = new HashMap<String, String>();
	
	public PropertiesModule()
	{
		// Load constant values from properties files
		_loadPropertiesDirectory(new File("properties/"));
		_loadPropertiesDirectory(new File("properties/" + System.getProperty("user.name") + "/"));
	}
	
	// Searches specified path for .properties files and loads their contents into _constValues
	private void _loadPropertiesDirectory(final File basePropertiesDir)
	{
		try
		{
			if(basePropertiesDir.exists() && basePropertiesDir.isDirectory())
			{
				for(File propFile : basePropertiesDir.listFiles())
				{
					if(propFile.getName().endsWith(".properties"))
					{
						final Properties props = new Properties();
						props.load(new BufferedReader(new FileReader(propFile)));
						for(Object oKey : props.keySet())
						{
							final String key = String.valueOf(oKey);
							final String value = props.getProperty(key);
							setConstant(key, value);
						}
					}
				}
			}
		}
		catch(IOException e)
		{
			System.err.println("unable to load properties: " + e);
		}
	}
	
	/**
	 * Sets the value of the specified constant. Constant values may be set multiple times, and the last value set when
	 * Guice {@link #configure() configures} the module will be used by the application.
	 * 
	 * @param key the name of the constant - may be either the FQN of a {@link BindingAnnotation} or a simple name.
	 * @param value the value of the constant.
	 */
	protected final void setConstant(final String key, final String value)
	{
		_constValues.put(key, value);
	}
	
	/**
	 * Loads the collected constant values into Guice.
	 */
	@Override
	protected final void configure()
	{
		// Bind all properties by name as a baseline way to access them
		Names.bindProperties(binder(), _constValues);
		
		// Find BindingAnnotations to bind
		for(String key : _constValues.keySet())
		{
			try
			{
				final Class<?> annotation = Class.forName(key);
				if(annotation.getAnnotation(BindingAnnotation.class) != null)
				{
					final Class<? extends Annotation> bindingAnnotation = annotation.asSubclass(Annotation.class);
					bindConstant().annotatedWith(bindingAnnotation).to(_constValues.get(key));
				}
			}
			catch(ClassNotFoundException e)
			{
				// Ignore - these properties will be available via @Named
				e.printStackTrace();
			}
		}
	}
}
