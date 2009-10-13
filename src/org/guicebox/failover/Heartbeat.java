package org.guicebox.failover;

import com.google.inject.*;
import java.io.*;
import java.util.concurrent.atomic.*;
import net.jcip.annotations.*;

/**
 * A heartbeat message, containing the application (cluster) name, node status and process fingerprint.
 * 
 * @author willhains
 */
@Immutable public final class Heartbeat implements Serializable
{
	private static final long serialVersionUID = -721811874522058238L;
	
	// Cluster ID
	private final String _appName, _env;
	
	// Node ID
	private final Node _info;
	
	@Inject public Heartbeat(@ApplicationName String appName, @Environment String env, Node info)
	{
		_appName = appName;
		_env = env;
		_info = info;
	}
	
	String getAppName()
	{
		return _appName;
	}
	
	String getEnv()
	{
		return _env;
	}
	
	/**
	 * @return {@code true} if the specified heartbeat has the same {@link ApplicationName} and {@link Environment}.
	 */
	public boolean isSameCluster(final Heartbeat that)
	{
		if(!this._appName.equals(that._appName)) return false;
		if(!this._env.equals(that._env)) return false;
		return true;
	}
	
	/**
	 * @return the fingerprint of the node that sent this heartbeat.
	 */
	public Node getNode()
	{
		return _info;
	}
	
	@Override public boolean equals(Object o)
	{
		if(o == this) return true;
		if(o instanceof Heartbeat)
		{
			final Heartbeat that = (Heartbeat)o;
			if(!isSameCluster(that)) return false;
			if(!this._info.equals(that._info)) return false;
			return true;
		}
		return false;
	}
	
	@Override public int hashCode()
	{
		int hash = 17;
		hash = 37 * hash + _appName.hashCode();
		hash = 37 * hash + _env.hashCode();
		hash = 37 * hash + _info.hashCode();
		return hash;
	}
	
	// Unique sequence number - for logging only
	private static final AtomicLong _SEQ = new AtomicLong();
	private final String _seqNo = Long.toString(_SEQ.incrementAndGet());
	
	@Override public String toString()
	{
		return _seqNo + ": " + _appName + " " + _env + " " + _info;
	}
}
