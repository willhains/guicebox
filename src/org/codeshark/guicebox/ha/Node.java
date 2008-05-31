package org.codeshark.guicebox.ha;

import com.google.inject.*;
import java.io.*;

/**
 * The unique fingerprint of a node process in the cluster.
 * 
 * @author willhains
 */
final class Node implements Comparable<Node>, Serializable
{
	private static final long serialVersionUID = -8054003957782729302L;
	
	private final String _address;
	private final String _processId;
	
	@Inject
	private Node(@Localhost String address, @ProcessId String processId)
	{
		assert processId != null;
		assert address != null;
		
		_processId = processId;
		_address = address;
	}
	
	/**
	 * @return {@code true} if this node is superior to the specified node.
	 */
	public boolean isSuperiorTo(Node that)
	{
		final boolean superior = this.compareTo(that) < 0;
		System.out.println("Received heartbeat from " + (superior ? "INFERIOR" : "SUPERIOR") + " node: " + that);
		return superior;
	}
	
	/**
	 * Compares nodes first by start time, then by IP address, then by process ID. Consistent with {@link
	 * #equals(Object)}.
	 */
	@Override
	public int compareTo(Node that)
	{
		// Compare IP addresses
		int compare = this._address.toString().compareTo(that._address.toString());
		if(compare != 0) return compare;
		
		// Compare process IDs
		compare = this._processId.compareTo(that._processId);
		return compare;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 17;
		hash = 37 * hash + _address.hashCode();
		hash = 37 * hash + _processId.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object o)
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
	
	@Override
	public String toString()
	{
		return _processId + " on " + _address;
	}
}
