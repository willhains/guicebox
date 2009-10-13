package org.guicebox.failover;

import static java.util.concurrent.TimeUnit.*;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jcip.annotations.*;
import org.guicebox.*;

/**
 * Utility for pinging a list of specified network addresses. The wire protocol is implemented by
 * {@link java.net.InetAddress#isReachable(int)}.
 * 
 * @author willhains
 */
@ThreadSafe public final class JavaPing implements Ping
{
	private final Logger _log;
	
	// The well-known addresses to ping
	private final List<InetAddress> _wka;
	
	// How long to wait between each ping
	private volatile int _pingInterval = 1000;
	
	// How many multiples of the ping interval to wait before timing out
	private volatile int _pingTolerance = 3;
	
	// Timer thread & task and its lock
	private final ScheduledExecutorService _ping;
	private final Object _pingLock = new Object();
	@GuardedBy("_pingLock") private Future<?> _pingTask;
	
	/**
	 * Prepares a ping to the specified well-known address (WKA). Call {@link #start(PingListener)} to start pinging.
	 * 
	 * @param wka the comma-or-whitespace-separated host names or IP addresses of the ping targets.
	 * @throws UnknownHostException if the specified host name could not be found in DNS.
	 */
	@Inject public JavaPing(@WellKnownAddress String wka, Logger log) throws UnknownHostException
	{
		// Look up the IP address of the WKA
		this( //
			_parseSet(wka),
			NamedExecutors.newSingleThreadScheduledExecutor("JavaPing: " + wka),
			log);
	}
	
	// Should only be called from unit tests
	JavaPing(Set<InetAddress> wka, ScheduledExecutorService pingThread, Logger log)
	{
		_wka = new LinkedList<InetAddress>(wka);
		_ping = pingThread;
		_log = log;
	}
	
	private static Set<InetAddress> _parseSet(String addresses) throws UnknownHostException
	{
		assert addresses != null : "WKAs must be set";
		final Set<InetAddress> wkaSet = new LinkedHashSet<InetAddress>();
		for(String wka : addresses.split("[,;\\s]"))
		{
			if(wka.length() > 0) wkaSet.add(new InetAddressAdapter(wka));
		}
		return wkaSet;
	}
	
	@Inject(optional = true) void setPingInterval(@PingInterval int interval)
	{
		assert interval > 0 : "JavaPing interval must be > 0";
		_pingInterval = interval;
	}
	
	@Inject(optional = true) void setPingTolerance(@PingTolerance int tolerance)
	{
		assert tolerance > 0 : "JavaPing tolerance must be > 0";
		_pingTolerance = tolerance;
	}
	
	public void start(final PingListener pingListener)
	{
		synchronized(_pingLock)
		{
			// Make sure we don't have multiple pings running
			stopPinging();
			
			// Create and schedule the ping task
			final Runnable command = new Runnable()
			{
				public void run()
				{
					for(int failures = 1; failures <= _pingTolerance; failures++)
					{
						// Abort if interrupted
						if(Thread.currentThread().isInterrupted())
						{
							_log.finest("JavaPing interrupted. Shutting down.");
							return;
						}
						
						try
						{
							for(final InetAddress wka : _wka)
							{
								// Java ping
								_log.finest("Pinging " + wka.getHostAddress() + "...");
								if(wka.isReachable(_pingInterval * _pingTolerance))
								{
									// Pinged successfully
									_log.finest("JavaPing response");
									pingListener.onPing();
									
									// Move this host to the front of the queue to avoid pinging a dead host
									Collections.sort(_wka, new Comparator<InetAddress>()
									{
										public int compare(InetAddress o1, InetAddress o2)
										{
											if(o1 == wka) return o2 == wka ? 0 : -1;
											return o2 == wka ? 1 : 0;
										}
									});
									return;
								}
							}
							
							// JavaPing timeout
							_log.warning("Time out (" + failures + "/" + _pingTolerance + ")");
						}
						catch(IOException e)
						{
							_log.severe("Could not verify WKA: " + e);
							break;
						}
					}
					
					// Beyond tolerance - notify listener
					pingListener.onPingTimeout();
				}
			};
			_pingTask = _ping.scheduleWithFixedDelay(command, 0, _pingInterval, MILLISECONDS);
		}
	}
	
	public void stopPinging()
	{
		synchronized(_pingLock)
		{
			if(_pingTask != null) _pingTask.cancel(true);
		}
	}
	
	public void stop()
	{
		// Shut down the executor
		_ping.shutdownNow();
	}
}

// Used to abstract java.net for testing
interface InetAddress
{
	String getHostAddress();
	
	boolean isReachable(int timeout) throws IOException;
}

// Adapts InetAddress to the real java.net.InetAddress
final class InetAddressAdapter implements InetAddress
{
	private final java.net.InetAddress _inner;
	
	InetAddressAdapter(String host) throws UnknownHostException
	{
		_inner = java.net.InetAddress.getByName(host);
	}
	
	public String getHostAddress()
	{
		return _inner.getHostAddress();
	}
	
	public boolean isReachable(int timeout) throws IOException
	{
		return _inner.isReachable(timeout);
	}
}
