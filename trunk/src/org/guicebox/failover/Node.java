package org.guicebox.failover;

import com.google.inject.*;
import java.io.*;
import net.jcip.annotations.*;

/**
 * The unique fingerprint of a node process in the cluster.
 * 
 * @author willhains
 */
@Immutable public final class Node implements Comparable<Node>, Serializable
{
	private static final long serialVersionUID = -8054003957782729302L;
	
	private final String _address;
	private final String _processId;
	private transient boolean _preferLowerOlder = true;
	
	@Inject public Node(@Localhost String address, @ProcessId String processId)
	{
		assert processId != null;
		assert address != null;
		
		_processId = processId;
		_address = address;
	}
	
	@Inject(optional = true) void setFailDirection(@FailDirection boolean preferLowerOlder)
	{
		_preferLowerOlder = preferLowerOlder;
	}
	
	// Should only be called by JUnit tests
	String getAddress()
	{
		return _address;
	}
	
	// Should only be called by JUnit tests
	String getProcessID()
	{
		return _processId;
	}
	
	/**
	 * @return {@code true} if this node is superior to the specified node.
	 */
	public boolean isSuperiorTo(Node that)
	{
		final boolean lowerOlder = this.compareTo(that) < 0;
		return _preferLowerOlder ? lowerOlder : !lowerOlder;
	}
	
	/**
	 * Compares nodes first by start time, then by IP address, then by process ID. Consistent with
	 * {@link #equals(Object)}.
	 */
	public int compareTo(Node that)
	{
		// Compare IP addresses
		int compare = this._address.toString().compareTo(that._address.toString());
		if(compare != 0) return compare;
		
		// Compare process IDs
		compare = this._processId.compareTo(that._processId);
		return compare;
	}
	
	@Override public int hashCode()
	{
		int hash = 17;
		hash = 37 * hash + _address.hashCode();
		hash = 37 * hash + _processId.hashCode();
		return hash;
	}
	
	@Override public boolean equals(Object o)
	{
		if(o == this) return true;
		if(o instanceof Node)
		{
			final Node that = (Node)o;
			if(!this._address.equals(that._address)) return false;
			if(!this._processId.equals(that._processId)) return false;
			return true;
		}
		return false;
	}
	
	@Override public String toString()
	{
		return _processId + " on " + _address;
	}
}
