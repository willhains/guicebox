package org.guicebox.failover;

import static java.util.concurrent.TimeUnit.*;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jcip.annotations.*;
import org.guicebox.*;

/**
 * Utility for pinging a specified network address. The wire protocol is implemented by
 * {@link java.net.InetAddress#isReachable(int)}.
 * 
 * @author willhains
 */
@ThreadSafe public final class JavaPing implements Ping
{
	private final Logger _log;
	
	// The well-known address to ping
	private final InetAddress _wka;
	
	// How long to wait between each ping
	private volatile int _pingInterval = 1500;
	
	// How many multiples of the ping interval to wait before timing out
	private volatile int _pingTolerance = 3;
	
	// Timer
	private final ScheduledExecutorService _ping;
	@GuardedBy("this") private Future<?> _pingTask;
	
	/**
	 * Prepares a ping to the specified well-known address (WKA). Call {@link #start(PingListener)} to start pinging.
	 * 
	 * @param wka the host name or IP address of the ping target.
	 * @throws UnknownHostException if the specified host name could not be found in DNS.
	 */
	@Inject public JavaPing(@WellKnownAddress String wka, Logger log) throws UnknownHostException
	{
		// Look up the IP address of the WKA
		this( //
			new InetAddressAdapter(wka),
			NamedExecutors.newSingleThreadScheduledExecutor("JavaPing: " + wka),
			log);
	}
	
	// Should only be called from unit tests
	JavaPing(InetAddress wka, ScheduledExecutorService pingThread, Logger log)
	{
		_wka = wka;
		_ping = pingThread;
		_log = log;
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
	
	public synchronized void start(final PingListener pingListener)
	{
		// Make sure we don't have multiple pings running
		stopPinging();
		
		// Create and schedule the ping task
		final Runnable command = new Runnable()
		{
			@Override public void run()
			{
				for(int failures = 0; failures < _pingTolerance; failures++)
				{
					// Abort if interrupted
					if(Thread.currentThread().isInterrupted())
					{
						_log.finest("JavaPing stopped");
						return;
					}
					
					try
					{
						// JavaPing
						_log.finest("Pinging...");
						if(_wka.isReachable(_pingInterval * _pingTolerance))
						{
							// Pinged successfully
							_log.finest("JavaPing response");
							pingListener.onPing();
							return;
						}
						
						// JavaPing timeout
						_log.warning("Time out (" + failures + "/" + _pingTolerance + ")");
					}
					catch(IOException e)
					{
						try
						{
							_log.severe("Could not verify WKA (" + failures + "/" + _pingTolerance + "): " + e);
							Thread.sleep(_pingInterval);
						}
						catch(InterruptedException ee)
						{
							Thread.currentThread().interrupt();
						}
					}
				}
				
				// Beyond tolerance - notify listener
				pingListener.onPingTimeout();
			}
		};
		_pingTask = _ping.scheduleWithFixedDelay(command, 0, _pingInterval, TimeUnit.MILLISECONDS);
	}
	
	public synchronized void stopPinging()
	{
		if(_pingTask != null) _pingTask.cancel(true);
	}
	
	public void stop()
	{
		// Shut down the executor
		_ping.shutdownNow();
		
		try
		{
			// Wait for the executor to terminate
			while(!_ping.awaitTermination(_pingInterval, MILLISECONDS))
			{
				_log.finest("Waiting for pinging to terminate...");
			}
			_log.fine("Pinging terminated");
		}
		catch(InterruptedException e)
		{
			// Restore interrupted status and return
			Thread.currentThread().interrupt();
		}
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
	
	@Override public String getHostAddress()
	{
		return _inner.getHostAddress();
	}
	
	@Override public boolean isReachable(int timeout) throws IOException
	{
		return _inner.isReachable(timeout);
	}
}
