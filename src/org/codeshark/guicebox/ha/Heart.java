package org.codeshark.guicebox.ha;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import org.codeshark.guicebox.*;

/**
 * Utility for sending and receiving UDP heartbeats.
 * 
 * @author willhains
 */
final class Heart
{
	private static final Log log = Log.forClass();
	
	// Heartbeats from this node
	private final Provider<Heartbeat> _pulse;
	
	// The multicast address for heartbeats
	private final InetAddress _groupAddress;
	
	// The multicast port for receiving heartbeats
	@Inject(optional = true)
	@SourcePort
	private int _sourcePort = 7979;
	
	// The multicast port for sending heartbeats
	@Inject(optional = true)
	@DestinationPort
	private int _destPort = 9797;
	
	// Time to Live
	@Inject(optional = true)
	@TimeToLive
	private int _ttl = 16;
	
	// Interval between heartbeats
	@Inject(optional = true)
	@HeartbeatInterval
	private int _hbInterval = 1000;
	
	// Number of heartbeats to miss
	@Inject(optional = true)
	@HeartbeatTolerance
	private int _hbTolerance = 5;
	
	// Send/receive threads
	private Thread _listen, _beat;
	
	@Inject
	private Heart(Provider<Heartbeat> pulse, @GroupAddress String groupAddress) throws UnknownHostException
	{
		_pulse = pulse;
		_groupAddress = InetAddress.getByName(groupAddress);
	}
	
	/**
	 * Starts listening for heartbeats. Each time a heartbeat is received, the specified listener will get a {@link
	 * HeartbeatListener#onHeartbeat(Heartbeat)} callback. If no response is received within {@link HeartbeatInterval} x
	 * {@link HeartbeatTolerance}, the specified listener will get a {@link HeartbeatListener#onHeartbeatTimeout()}
	 * callback.
	 */
	public synchronized void listen(final HeartbeatListener heartbeatListener)
	{
		// Make sure we don't have multiple listeners running
		if(_listen != null) _listen.interrupt();
		
		_listen = new Thread("Heartbeat listener (" + _destPort + ")")
		{
			private MulticastSocket _socket;
			private int _hbMissed = 0; // number of missed heartbeats
			private long _hbStart = System.currentTimeMillis(); // measure start of interval
			
			@Override
			public void run()
			{
				try
				{
					final TimeUnit unit = _hbInterval < 1000 ? TimeUnit.SECONDS : TimeUnit.MILLISECONDS;
					final long hbIntervalMillis = unit.toMillis(_hbInterval);
					while(true)
					{
						try
						{
							// Check time since start of interval
							if(System.currentTimeMillis() - _hbStart > hbIntervalMillis + 100) // leeway for TX time
							{
								throw new SocketTimeoutException(); // fake a socket timeout
							}
							
							// Join the multicast group
							if(_socket == null || _socket.isClosed())
							{
								_socket = new MulticastSocket(_destPort);
								_socket.joinGroup(_groupAddress);
								_socket.setSoTimeout(100); // leeway for transmission time etc.
							}
							
							// Receive the next message
							final byte[] buf = new byte[1024];
							final DatagramPacket msg = new DatagramPacket(buf, buf.length);
							_socket.receive(msg);
							final InputStream in = new ByteArrayInputStream(msg.getData(), 0, msg.getLength());
							final Object obj = new ObjectInputStream(in).readObject();
							
							// Check that it is a heartbeat
							final Heartbeat heartbeat = (Heartbeat)obj;
							
							// Ignore own heartbeats
							final Heartbeat ownHeartbeat = _pulse.get();
							if(ownHeartbeat.equals(heartbeat)) continue;
							
							// Ignore heartbeats from other clusters
							if(!ownHeartbeat.isSameCluster(heartbeat)) continue;
							
							// Process heartbeat
							_hbMissed = 0;
							_hbStart = System.currentTimeMillis(); // reset interval start
							heartbeatListener.onHeartbeat(heartbeat);
						}
						catch(final SocketTimeoutException e)
						{
							// Count the miss (prevent overflow)
							if(_hbMissed < _hbTolerance)
							{
								_hbMissed++;
								log.warn("Time out", _hbMissed, "/", _hbTolerance);
								_hbStart = System.currentTimeMillis(); // reset interval start
							}
							
							// Beyond tolerance - notify listener
							if(_hbMissed >= _hbTolerance) heartbeatListener.onHeartbeatTimeout();
						}
						catch(Exception e)
						{
							log.error("Could not read heartbeat:", e);
						}
						
						// Sleep until next iteration
						Thread.sleep(hbIntervalMillis);
					}
				}
				catch(InterruptedException e)
				{
					// Die gracefully
				}
			}
		};
		_listen.setDaemon(false);
		_listen.start();
	}
	
	/**
	 * Stops listening for heartbeats.
	 */
	public synchronized void stopListening()
	{
		if(_listen != null) _listen.interrupt();
	}
	
	/**
	 * Starts sending heartbeats to the {@link GroupAddress}, starting immediately and followed once every {@link
	 * HeartbeatInterval}.
	 */
	public synchronized void beat()
	{
		// Make sure we don't have multiple hearbeaters running
		if(_beat != null) _beat.interrupt();
		
		_beat = new Thread("Heartbeat sender (" + _groupAddress.getHostAddress() + ":" + _sourcePort + ")")
		{
			private MulticastSocket _socket;
			private int _hbFailures = 0;
			
			@Override
			public void run()
			{
				try
				{
					final TimeUnit unit = _hbInterval < 1000 ? TimeUnit.SECONDS : TimeUnit.MILLISECONDS;
					final long hbIntervalMillis = unit.toMillis(_hbInterval);
					while(true)
					{
						try
						{
							// Connect to the multicast group
							if(_socket == null || _socket.isClosed())
							{
								_socket = new MulticastSocket(_sourcePort);
								_socket.setTimeToLive(_ttl);
							}
							
							// Send heartbeat
							final Heartbeat heartbeat = _pulse.get();
							final ByteArrayOutputStream msg = new ByteArrayOutputStream();
							final ObjectOutputStream out = new ObjectOutputStream(msg);
							out.writeObject(heartbeat);
							_socket.send(new DatagramPacket(msg.toByteArray(), msg.size(), _groupAddress, _destPort));
							
							// Sent successfully
							_hbFailures = 0;
						}
						catch(IOException e)
						{
							// Count the miss (prevent overflow)
							if(_hbFailures <= _hbTolerance)
							{
								_hbFailures++;
								log.error("Couldn't send heartbeat:", e);
							}
							
							// Beyond tolerance - stop beating
							if(_hbFailures >= _hbTolerance)
							{
								log.fatal("Heart failure");
								stopBeating();
							}
						}
						
						// Sleep until next iteration
						Thread.sleep(hbIntervalMillis);
					}
				}
				catch(InterruptedException e)
				{
					// Die gracefully
				}
			}
		};
		_beat.setDaemon(false);
		_beat.start();
	}
	
	/**
	 * Stops sending heartbeats.
	 */
	public synchronized void stopBeating()
	{
		if(_beat != null) _beat.interrupt();
	}
}
