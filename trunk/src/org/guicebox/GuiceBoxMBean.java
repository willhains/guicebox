package org.guicebox;

/**
 * JMX interface for GuiceBox.
 * 
 * @author willhains
 */
public interface GuiceBoxMBean
{
	void start();
	
	void stop();
	
	void kill();
}
