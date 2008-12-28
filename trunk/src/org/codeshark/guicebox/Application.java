package org.codeshark.guicebox;

/**
 * Interface to allow a {@link Cluster} to start & stop an application.
 * 
 * @author willhains
 */
public interface Application
{
	void start();
	
	void stop();
}