package org.codeshark.guicebox.ha;

import static org.codeshark.guicebox.Log.*;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.concurrent.*;

/**
 * Utility for pinging a specified network address.
 * <p>
 * <b>Note:</b> When this class is statically initialised, {@link Security} properties {@code networkaddress.cache.ttl}
 * and {@code networkaddress.cache.negative.ttl} are set to {@code 0}.
 * 
 * @author willhains
 */
public final class Ping
{
	static
	{
		// Turn off address caching
		Security.setProperty("networkaddress.cache.ttl", "0");
		Security.setProperty("networkaddress.cache.negative.ttl", "0");
	}
	
	// The well-known address to ping
	private final InetAddress _wka;
	
	// How long to wait between each ping
	@Inject(optional = true)
	@PingInterval
	private int _pingInterval = 10000;
	
	// How many multiples of the ping interval to wait before timing out
	@Inject(optional = true)
	@PingTolerance
	private int _pingTolerance = 5;
	
	/**
	 * Prepares a ping to the specified well-known address (WKA). Call {@link #start(PingListener)} to start pinging.
	 * 
	 * @param wka the host name or IP address of the ping target.
	 * @throws UnknownHostException if the specified host name could not be found in DNS.
	 */
	@Inject
	public Ping(@WellKnownAddress String wka) throws UnknownHostException
	{
		// Look up the IP address of the WKA
		_wka = InetAddress.getByName(wka);
	}
	
	// Timer
	private ScheduledExecutorService _timer;
	
	/**
	 * Starts pinging the {@link WellKnownAddress} every {@link PingInterval}. Each time a response is received, the
	 * specified listener will get a {@link PingListener#onPing()} callback. If no response is received within {@link
	 * PingInterval} x {@link PingTolerance}, the specified listener will get a {@link PingListener#onPingTimeout()}
	 * callback.
	 */
	public synchronized void start(final PingListener pingListener)
	{
		// Make sure we don't have multiple pings running
		stop();
		
		// Start a new timer
		_timer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
		{
			@Override
			public Thread newThread(Runnable r)
			{
				final Thread thread = new Thread(r, "Ping:" + _wka.getHostAddress());
				thread.setDaemon(false);
				return thread;
			}
		});
		
		// Create and schedule the ping task
		final Runnable command = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if(_wka.isReachable(_pingInterval * _pingTolerance))
					{
						pingListener.onPing();
						return;
					}
				}
				catch(IOException e)
				{
					log.error("Network error:", e);
				}
				pingListener.onPingTimeout();
			}
		};
		final TimeUnit unit = _pingInterval < 1000 ? TimeUnit.SECONDS : TimeUnit.MILLISECONDS;
		_timer.scheduleWithFixedDelay(command, 0, _pingInterval, unit);
	}
	
	/**
	 * Stops pinging the {@link WellKnownAddress}.
	 */
	public synchronized void stop()
	{
		if(_timer != null) _timer.shutdownNow();
	}
}
