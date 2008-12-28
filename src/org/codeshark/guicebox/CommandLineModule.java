package org.guicebox;

/**
 * An extension of {@link PropertiesModule} that allows binding of constants from the command line via command-line
 * switches, overriding values bound via properties files. (See {@link PropertiesModule} for more information.)
 * <p>
 * A command-line switch is supplied in the format {@code -key value}. If no value is supplied, the key is bound to
 * {@code true}.
 * 
 * @author willhains
 */
public class CommandLineModule extends PropertiesModule
{
	/**
	 * @param args the command line arguments passed to a {@code main} method.
	 */
	public CommandLineModule(String... args)
	{
		// Bind constants frop properties files
		super();
		
		// Bind constants from command line (overriding properties files)
		for(int i = 0; i < args.length; i++)
		{
			if(!args[i].startsWith("-")) throw new IllegalArgumentException("unknown switch: '" + args[i] + "'");
			final String key = args[i].substring(1);
			setConstant(key, args.length <= i + 1 || args[i + 1].startsWith("-") ? "true" : args[++i]);
		}
	}
}
