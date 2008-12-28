package org.guicebox;

import java.util.*;

/**
 * Utility class for discovering information about Types.
 * 
 * @author willhains
 */
public final class Types
{
	private Types()
	{
		// Utility class
	}
	
	/**
	 * Utility method to reflectively determine all of the supertypes, including interfaces, of the given type.
	 */
	public static List<Class<?>> inheritedBy(Class<?> type)
	{
		final List<Class<?>> allTypes = new ArrayList<Class<?>>();
		if(type != null)
		{
			allTypes.add(type);
			for(Class<?> intfc : type.getInterfaces())
			{
				allTypes.addAll(inheritedBy(intfc));
			}
			allTypes.addAll(inheritedBy(type.getSuperclass()));
		}
		return allTypes;
	}
}
