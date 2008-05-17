package org.codeshark.guicebox;

import com.google.inject.*;
import com.google.inject.name.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;

/**
 * Binds both explicit {@link BindingAnnotation}s and {@link Named} annotations to constant values supplied in the
 * command line via command-line switches, and supplied in the environment via properties files.
 * <p>
 * A command-line switch is supplied in the format {@code -key value}. If no value is supplied, the key is bound to
 * {@code true}. See below for a usage example.
 * <p>
 * Any and all properties files in the relative paths {@code properties/} and/or {@code properties/username/} (where
 * <i>username</i> is the login ID of the current user) will be read, and the key-value pairs mapped as Guice
 * constants.
 * <p>
 * Values are given precedence in the following order:
 * <ol>
 * <li>supplied on command line</li>
 * <li>supplied in properties/username/*.properties</li>
 * <li>supplied in properties/*.properties</li>
 * <li>default value</li>
 * </ol>
 * 
 * <pre>
 * 
 * 	Guice.createInjector(
 * 		...
 * 		new CommandLineModule(args) // command-line options
 * 			.bind(HomeDir.class, &quot;home&quot;, &quot;data&quot;) // home directory
 * 			.bind(DebugMode.class, &quot;debug&quot;, &quot;none&quot;) // debug mode
 * 			...
 * 		);
 * 
 * </pre>
 * 
 * @author willhains
 */
public class CommandLineModule extends AbstractModule
{
	// Maps binding annotations to command-line switches (keys)
	final Map<Class<? extends Annotation>, String> _bindings;
	
	// Maps command-line switches to their values
	final Properties _constValues = new Properties();
	
	public CommandLineModule(String... args)
	{
		_bindings = new HashMap<Class<? extends Annotation>, String>();
		
		// Bind constants from properties files
		_loadPropertiesDirectory(new File("properties/"));
		_loadPropertiesDirectory(new File("properties/" + System.getProperty("user.name") + "/"));
		
		// Bind constants from command line
		for(int i = 0; i < args.length; i++)
		{
			if(!args[i].startsWith("-")) throw new IllegalArgumentException("unknown switch: '" + args[i] + "'");
			final String key = args[i].substring(1);
			if(args.length <= i + 1 || args[i + 1].startsWith("-")) _constValues.setProperty(key, "true");
			else _constValues.setProperty(key, args[++i]);
		}
	}
	
	// Load all values from properties files found in the specified directory
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
						_constValues.load(new BufferedReader(new FileReader(propFile)));
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
	 * Binds the specified annotation to the value of the specified command-line option. Requires the value to be
	 * supplied at the command-line.
	 * 
	 * @param binding the binding annotation that will be used to inject the value as a constant.
	 * @param key the command-line switch.
	 * @return {@code this} to allow call chaining.
	 */
	public CommandLineModule bind(Class<? extends Annotation> binding, String key)
	{
		return bind(binding, key, null);
	}
	
	/**
	 * Binds the specified annotation to the value of the specified command-line option. If the value is not supplied at
	 * the command-line, the specified default value will be bound.
	 * 
	 * @param binding the binding annotation that will be used to inject the value as a constant.
	 * @param key the command-line switch.
	 * @param defaultValue the default value for the option if it was not supplied on the command line.
	 * @return {@code this} to allow call chaining.
	 */
	public CommandLineModule bind(Class<? extends Annotation> binding, String key, String defaultValue)
	{
		_bindings.put(binding, key);
		if(defaultValue != null && !_constValues.containsKey(key)) _constValues.setProperty(key, defaultValue);
		return this;
	}
	
	@Override
	protected void configure()
	{
		Names.bindProperties(binder(), _constValues);
		for(Class<? extends Annotation> binding : _bindings.keySet())
		{
			final String key = _bindings.get(binding);
			final String value = _constValues.getProperty(key);
			if(value == null) throw new IllegalArgumentException("'-" + key + "' must be supplied");
			bindConstant().annotatedWith(binding).to(value);
		}
	}
}
