package org.guicebox.failover;

import static java.util.concurrent.TimeUnit.*;
import static org.guicebox.NamedExecutors.*;

import com.google.inject.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jcip.annotations.*;

/**
 * Utility for sending and receiving heartbeats. The actual wire protocol is implemented by {@link Transport}. This
 * class is responsible for managing the background threads and firing events to the {@link HeartbeatListener}.
 * 
 * @author willhains
 */
@ThreadSafe public final class NonBlockingHeart implements Heart
{
	private final Logger _log;
	
	// Heartbeats from this node
	private final Provider<Heartbeat> _pulse;
	
	// Transport for heartbeats
	private final Transport _transport;
	
	// Interval between heartbeats
	private volatile int _hbInterval = 1000;
	
	// Number of heartbeats to miss
	private volatile int _hbTolerance = 5;
	
	// Send/receive threads & tasks and their locks
	private final ScheduledExecutorService _listen, _beat;
	private final Object _listenLock = new Object();
	private final Object _beatLock = new Object();
	@GuardedBy("_listenLock") private Future<?> _listenTask;
	@GuardedBy("_beatLock") private Future<?> _beatTask;
	
	@Inject NonBlockingHeart(Provider<Heartbeat> pulse, Transport transport, Logger log)
	{
		this(
			pulse,
			transport,
			newSingleThreadScheduledExecutor("Heartbeat listener"),
			newSingleThreadScheduledExecutor("Hearbeat sender"),
			log);
	}
	
	// Should only be called by unit tests
	NonBlockingHeart(
		Provider<Heartbeat> pulse,
		Transport transport,
		ScheduledExecutorService listenThread,
		ScheduledExecutorService beatThread,
		Logger log)
	{
		_pulse = pulse;
		_transport = transport;
		_listen = listenThread;
		_beat = beatThread;
		_log = log;
	}
	
	@Inject(optional = true) final void setHeartbeatInterval(@HeartbeatInterval int interval)
	{
		assert interval > 0 : "Heartbeat interval must be > 0";
		_hbInterval = interval;
	}
	
	@Inject(optional = true) final void setHeartbeatTolerance(@HeartbeatTolerance int tolerance)
	{
		assert tolerance > 0 : "Heartbeat tolerance must be > 0";
		_hbTolerance = tolerance;
	}
	
	public void listen(final HeartbeatListener heartbeatListener)
	{
		synchronized(_listenLock)
		{
			// Make sure we don't have multiple listeners running
			stopListening();
			
			// Start listening
			final Heartbeat ownHeartbeat = _pulse.get();
			final Runnable listen = new Runnable()
			{
				public void run()
				{
					for(int failures = 1; failures <= _hbTolerance; failures++)
					{
						// Abort if interrupted
						if(Thread.currentThread().isInterrupted())
						{
							_log.finest("Heartbeat listener stopped");
							return;
						}
						
						try
						{
							// Receive heartbeat
							final Heartbeat heartbeat = _transport.receive(ownHeartbeat, _hbInterval);
							
							// Received successfully
							_log.finest("Received heartbeat: " + heartbeat);
							heartbeatListener.onHeartbeat(heartbeat);
							return;
						}
						catch(TimeoutException e)
						{
							_log.fine("Time out (" + failures + "/" + _hbTolerance + ")");
						}
						catch(TransportException e)
						{
							try
							{
								_log.severe("Could not read heartbeat (" + failures + "/" + _hbTolerance + "): " + e);
								Thread.sleep(_hbInterval);
							}
							catch(InterruptedException ee)
							{
								Thread.currentThread().interrupt();
							}
						}
					}
					
					// Beyond tolerance - notify listener
					heartbeatListener.onHeartbeatTimeout();
				}
			};
			_listenTask = _listen.scheduleWithFixedDelay(listen, 0, 1, MILLISECONDS);
		}
	}
	
	public void beat()
	{
		synchronized(_beatLock)
		{
			// Make sure we don't have multiple hearbeaters running
			stopBeating();
			
			// Start beating
			final Runnable beat = new Runnable()
			{
				public void run()
				{
					for(int failures = 0; failures < _hbTolerance; failures++)
					{
						// Abort if interrupted
						if(Thread.currentThread().isInterrupted())
						{
							_log.finest("Heartbeat sender stopped");
							return;
						}
						
						try
						{
							// Send heartbeat
							final Heartbeat heartbeat = _pulse.get();
							_transport.send(heartbeat);
							
							// Sent successfully
							_log.finest("Sent heartbeat: " + heartbeat);
							return;
						}
						catch(TransportException e)
						{
							try
							{
								_log.severe("Couldn't send heartbeat (" + failures + "/" + _hbTolerance + "): " + e);
								Thread.sleep(_hbInterval);
							}
							catch(InterruptedException ee)
							{
								Thread.currentThread().interrupt();
							}
						}
					}
					
					// Beyond tolerance - stop beating
					_log.severe(NonBlockingHeart.class.getSimpleName() + " failure");
					stopBeating();
				}
			};
			_beatTask = _beat.scheduleAtFixedRate(beat, 0, _hbInterval, MILLISECONDS);
		}
	}
	
	public void stopListening()
	{
		synchronized(_listenLock)
		{
			if(_listenTask != null) _listenTask.cancel(true);
		}
	}
	
	public void stopBeating()
	{
		synchronized(_beatLock)
		{
			if(_beatTask != null) _beatTask.cancel(true);
		}
	}
	
	public void stop()
	{
		// Shut down the executors
		_beat.shutdownNow();
		_listen.shutdownNow();
		
		try
		{
			// Wait for the executors to terminate
			while(!_beat.awaitTermination(_hbInterval, MILLISECONDS))
			{
				_log.finest("Waiting for heartbeating to terminate...");
			}
			_log.finest("Heartbeating terminated");
			while(!_listen.awaitTermination(_hbInterval, MILLISECONDS))
			{
				_log.finest("Waiting for heartbeat listening to terminate...");
			}
			_log.finest("Hearbeat listening terminated");
		}
		catch(InterruptedException e)
		{
			// Restore interrupted status and return
			Thread.currentThread().interrupt();
		}
	}
}
