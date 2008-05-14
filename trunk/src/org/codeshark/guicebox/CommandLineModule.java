package org.codeshark.guicebox;

import com.google.inject.*;
import com.google.inject.name.*;
import java.lang.annotation.*;
import java.util.*;

/**
 * Binds both explicit {@link BindingAnnotation}s and {@link Named} annotations to constant values supplied in the
 * command line via command-line switches. A command-line switch is supplied in the format {@code -key value}. See
 * below for a usage example.
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
		for(int i = 0; i < args.length; i++)
		{
			if(!args[i].startsWith("-")) throw new IllegalArgumentException("unknown switch: '" + args[i] + "'");
			final String key = args[i++].substring(1);
			if(args.length <= i) throw new IllegalArgumentException("no value given for '-" + key + "'");
			final String value = args[i];
			_constValues.setProperty(key, value);
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
			if(value == null) throw new IllegalArgumentException("'-" + key + "' must be supplied!");
			bindConstant().annotatedWith(binding).to(value);
		}
	}
}
