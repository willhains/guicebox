package org.codeshark.guicebox.failover.udp;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.net.InetAddress;
import java.util.concurrent.*;
import net.jcip.annotations.*;
import org.codeshark.guicebox.failover.*;

/**
 * Implements a UDP-based transport of heartbeats.
 * 
 * @author willhains
 */
@ThreadSafe public class UdpTransport implements Transport
{
	// The multicast address for heartbeats
	private final InetAddress _groupAddress;
	
	// The multicast port for receiving heartbeats
	private volatile int _sourcePort = 7979;
	
	// The multicast port for sending heartbeats
	private volatile int _destPort = 9797;
	
	// Time to Live
	private volatile int _ttl = 16;
	
	// Sockets used for sending/receiving
	@GuardedBy("this") private MulticastSocket _sendSocket;
	@GuardedBy("this") private MulticastSocket _receiveSocket;
	
	@Inject UdpTransport(@GroupAddress String groupAddress) throws UnknownHostException
	{
		_groupAddress = InetAddress.getByName(groupAddress);
	}
	
	@Inject(optional = true) final void setSourcePort(@SourcePort int port)
	{
		_sourcePort = port;
	}
	
	@Inject(optional = true) final void setDestinationPort(@DestinationPort int port)
	{
		_destPort = port;
	}
	
	@Inject(optional = true) final void setTimeToLive(@TimeToLive int ttl)
	{
		_ttl = ttl;
	}
	
	@Override public synchronized Heartbeat receive(Heartbeat ownHeartbeat, int timeout) throws TransportException,
		TimeoutException
	{
		// Loop until timeout expires
		final long start = System.currentTimeMillis();
		for(int remaining = timeout; remaining > 0; remaining -= System.currentTimeMillis() - start)
		{
			try
			{
				// Join the multicast group
				if(_receiveSocket != null && _receiveSocket.getPort() != _destPort) _receiveSocket.close();
				if(_receiveSocket == null || _receiveSocket.isClosed())
				{
					_receiveSocket = createSocket(_destPort);
					_receiveSocket.joinGroup(_groupAddress);
					_receiveSocket.setSoTimeout(remaining);
				}
				
				// Timeout on thread interrupt
				if(Thread.currentThread().isInterrupted()) throw new TimeoutException("Thread interrupted");
				
				// Receive the next message
				final byte[] buf = new byte[1024];
				final DatagramPacket msg = new DatagramPacket(buf, buf.length);
				_receiveSocket.receive(msg);
				
				// Deserialise the heartbeat 
				final Heartbeat heartbeat = decodePacket(msg);
				
				// Ignore own heartbeats and heartbeats from other clusters
				if(ownHeartbeat.equals(heartbeat)) continue;
				if(!ownHeartbeat.isSameCluster(heartbeat)) continue;
				
				return heartbeat;
			}
			catch(ClassNotFoundException e)
			{
				// Not a heartbeat
			}
			catch(ClassCastException e)
			{
				// Not a heartbeat
			}
			catch(SocketTimeoutException e)
			{
				// Avoid being caught as IOException
			}
			catch(IOException e)
			{
				// Wrap and re-throw
				throw new TransportException(e);
			}
		}
		
		// Time up!
		throw new TimeoutException();
	}
	
	@Override public synchronized void send(Heartbeat hb) throws TransportException
	{
		try
		{
			// Connect to the multicast group
			if(_sendSocket != null && _sendSocket.getPort() != _sourcePort) _sendSocket.close();
			if(_sendSocket == null || _sendSocket.isClosed())
			{
				_sendSocket = createSocket(_sourcePort);
				_sendSocket.setTimeToLive(_ttl);
			}
			
			// Create & send heartbeat packet
			_sendSocket.send(createPacket(hb, _groupAddress, _destPort));
		}
		catch(IOException e)
		{
			throw new TransportException(e);
		}
	}
	
	static DatagramPacket createPacket(Heartbeat hb, InetAddress groupAddress, int destPort) throws IOException
	{
		final ByteArrayOutputStream msg = new ByteArrayOutputStream();
		new ObjectOutputStream(msg).writeObject(hb);
		return new DatagramPacket(msg.toByteArray(), msg.size(), groupAddress, destPort);
	}
	
	static Heartbeat decodePacket(DatagramPacket msg) throws IOException, ClassNotFoundException, ClassCastException
	{
		final InputStream in = new ByteArrayInputStream(msg.getData(), 0, msg.getLength());
		final Object obj = new ObjectInputStream(in).readObject();
		return (Heartbeat)obj;
	}
	
	@Override public synchronized void disconnect()
	{
		if(_receiveSocket != null) _receiveSocket.close();
		if(_sendSocket != null) _sendSocket.close();
	}
	
	@Override public String toString()
	{
		return "udp://" + _groupAddress + ":" + _sourcePort + "-->" + _destPort;
	}
	
	/**
	 * Factory method to avoid using real {@link java.net.MulticastSocket}s in unit tests.
	 * 
	 * @param port the port number.
	 * @return a {@link MulticastSocket} on the specified port.
	 * @throws IOException if the port cannot be opened.
	 */
	protected MulticastSocket createSocket(int port) throws IOException
	{
		return new MulticastSocketAdapter(port);
	}
}

// Used to abstract java.net for testing
interface MulticastSocket
{
	int getPort();
	
	void send(DatagramPacket datagramPacket) throws IOException;
	
	void setTimeToLive(int _ttl) throws IOException;
	
	void receive(DatagramPacket msg) throws SocketTimeoutException, IOException;
	
	void setSoTimeout(int remaining) throws SocketException;
	
	void joinGroup(InetAddress address) throws IOException;
	
	boolean isClosed();
	
	void close();
}

// Adapts MulticastSocket to the real java.net.MulticastSocket
final class MulticastSocketAdapter implements MulticastSocket
{
	private final java.net.MulticastSocket _inner;
	
	public MulticastSocketAdapter(int port) throws IOException
	{
		_inner = new java.net.MulticastSocket(port);
	}
	
	@Override public void close()
	{
		_inner.close();
	}
	
	@Override public int getPort()
	{
		return _inner.getPort();
	}
	
	@Override public boolean isClosed()
	{
		return _inner.isClosed();
	}
	
	@Override public void receive(DatagramPacket p) throws IOException
	{
		_inner.receive(p);
	}
	
	@Override public void send(DatagramPacket p) throws IOException
	{
		_inner.send(p);
	}
	
	@Override public void setSoTimeout(int timeout) throws SocketException
	{
		_inner.setSoTimeout(timeout);
	}
	
	@Override public void setTimeToLive(int ttl) throws IOException
	{
		_inner.setTimeToLive(ttl);
	}
	
	@Override public void joinGroup(InetAddress address) throws IOException
	{
		_inner.joinGroup(address);
	}
}
