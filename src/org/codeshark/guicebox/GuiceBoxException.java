package org.codeshark.guicebox;

/**
 * Generic exception to indicate an error encountered by GuiceBox.
 * 
 * @author willhains
 */
@SuppressWarnings("serial")
public final class GuiceBoxException extends RuntimeException
{
	GuiceBoxException(String message)
	{
		super(message);
	}
}
