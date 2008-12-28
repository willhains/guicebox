package org.guicebox.failover.udp;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import com.google.inject.*;
import java.io.*;
import java.net.*;
import java.net.InetAddress;
import java.util.concurrent.*;
import org.guicebox.failover.*;
import org.easymock.*;
import org.junit.*;

/**
 * @author willhains
 */
@SuppressWarnings("unchecked") public class UdpTransportTest
{
	// Mocks
	private Object[] _mocks;
	private MulticastSocket _socket;
	private Provider<Heartbeat> _pulse, _peerPulse;
	
	// Captures
	private final Capture<DatagramPacket> _msg = new Capture<DatagramPacket>();
	
	// Values
	private final Node _localhost = new Node("127.0.0.1", "HeartTestProcess");
	private final Node _peer = new Node("1.1.1.1", "HeartTestPeer");
	private final String _groupAddress = "1.1.1.1";
	private final int _timeout = 10;
	private final int _ttl = 8, _sourcePort = 1111, _destPort = 2222;
	
	@Before public void createMocks() throws Exception
	{
		_socket = createMock(MulticastSocket.class);
		_mocks = new Object[] {
			_socket = createMock(MulticastSocket.class),
			_pulse = createMock(Provider.class),
			_peerPulse = createMock(Provider.class),
		// Add all mocks here!
		};
		expect(_pulse.get()).andReturn(new Heartbeat("NonBlockingHeartTest", "TEST", _localhost)).anyTimes();
		expect(_peerPulse.get()).andReturn(new Heartbeat("NonBlockingHeartTest", "TEST", _peer)).anyTimes();
	}
	
	@Test public void receiveSuccessfully() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				final DatagramPacket packet = UdpTransport.createPacket(_peerPulse.get(), groupAddress, _destPort);
				final DatagramPacket msg = _msg.getValue();
				msg.setLength(packet.getLength());
				msg.setData(packet.getData());
				return null;
			}
		});
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		assertEquals(_peerPulse.get(), udp.receive(_pulse.get(), _timeout));
		
		verify(_mocks);
	}
	
	@Test public void receiveSocketTimeout() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				Thread.sleep(_timeout + 1);
				throw new SocketTimeoutException();
			}
		});
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TimeoutException");
		}
		catch(TimeoutException e)
		{
			// Correct behaviour
		}
		
		verify(_mocks);
	}
	
	@Test public void receiveIOException() throws Exception
	{
		final String errorStr = "Aaarrggh!! Network error!!";
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		_socket.receive(capture(_msg));
		expectLastCall().andThrow(new IOException(errorStr));
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TransportException");
		}
		catch(TransportException e)
		{
			// Correct behaviour
			assertEquals("java.io.IOException: " + errorStr, e.getMessage());
		}
		
		verify(_mocks);
	}
	
	@Test public void receiveOwnHeartbeatAndTimeout() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		expect(_socket.getPort()).andReturn(_sourcePort).anyTimes();
		_socket.close();
		expectLastCall().anyTimes();
		expect(_socket.isClosed()).andReturn(false).anyTimes();
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				Thread.sleep(1);
				final DatagramPacket packet = UdpTransport.createPacket(_pulse.get(), groupAddress, _destPort);
				final DatagramPacket msg = _msg.getValue();
				msg.setLength(packet.getLength());
				msg.setData(packet.getData());
				return null;
			}
		}).anyTimes();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TimeoutException");
		}
		catch(TimeoutException e)
		{
			// Correct behaviour
		}
		
		verify(_mocks);
	}
	
	@Test public void receiveOtherClusterHeartbeatAndTimeout() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		expect(_socket.getPort()).andReturn(_destPort).anyTimes();
		expect(_socket.isClosed()).andReturn(false).anyTimes();
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				Thread.sleep(1);
				final Heartbeat hb = new Heartbeat("AnotherCluster", "TEST", _peer);
				final DatagramPacket packet = UdpTransport.createPacket(hb, groupAddress, _destPort);
				final DatagramPacket msg = _msg.getValue();
				msg.setLength(packet.getLength());
				msg.setData(packet.getData());
				return null;
			}
		}).anyTimes();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TimeoutException");
		}
		catch(TimeoutException e)
		{
			// Correct behaviour
		}
		
		verify(_mocks);
	}
	
	@Test public void receiveNonHeartbeatAndTimeout() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		expect(_socket.getPort()).andReturn(_destPort).anyTimes();
		expect(_socket.isClosed()).andReturn(false).anyTimes();
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				Thread.sleep(1);
				final Object foreignObject = "I am not a heartbeat";
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				new ObjectOutputStream(baos).writeObject(foreignObject);
				final byte[] data = baos.toByteArray();
				final DatagramPacket packet = new DatagramPacket(data, baos.size(), groupAddress, _destPort);
				final DatagramPacket msg = _msg.getValue();
				msg.setLength(packet.getLength());
				msg.setData(packet.getData());
				return null;
			}
		}).anyTimes();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TimeoutException");
		}
		catch(TimeoutException e)
		{
			// Correct behaviour
		}
		
		verify(_mocks);
	}
	
	@Test public void receiveInterrupted() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setSoTimeout(_timeout);
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		try
		{
			Thread.currentThread().interrupt();
			udp.receive(_pulse.get(), _timeout);
			fail("Expected TimeoutException");
		}
		catch(TimeoutException e)
		{
			// Correct behaviour
			assertTrue(Thread.interrupted());
		}
		
		verify(_mocks);
	}
	
	@Test public void sendSuccessfully() throws Exception
	{
		_socket.setTimeToLive(_ttl);
		_socket.send(capture(_msg));
		_socket.send(capture(_msg));
		expect(_socket.getPort()).andReturn(_destPort);
		expect(_socket.isClosed()).andReturn(false);
		_socket.close();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		udp.send(_pulse.get());
		assertEquals(_pulse.get(), UdpTransport.decodePacket(_msg.getValue()));
		udp.send(_pulse.get());
		assertEquals(_pulse.get(), UdpTransport.decodePacket(_msg.getValue()));
		
		verify(_mocks);
	}
	
	@Test public void sendIOException() throws Exception
	{
		final String errorStr = "Aaarrggh!! Network error!!";
		_socket.setTimeToLive(_ttl);
		_socket.send(capture(_msg));
		_socket.send(capture(_msg));
		expectLastCall().andThrow(new IOException(errorStr));
		expect(_socket.getPort()).andReturn(_destPort);
		expect(_socket.isClosed()).andReturn(false);
		_socket.close();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		udp.send(_pulse.get());
		assertEquals(_pulse.get(), UdpTransport.decodePacket(_msg.getValue()));
		try
		{
			udp.send(_pulse.get());
			fail("Expected TransportException");
		}
		catch(TransportException e)
		{
			// Correct behaviour
			assertEquals("java.io.IOException: " + errorStr, e.getMessage());
		}
		
		verify(_mocks);
	}
	
	@Test public void disconnect() throws Exception
	{
		final InetAddress groupAddress = InetAddress.getByName(_groupAddress);
		_socket.joinGroup(groupAddress);
		_socket.setTimeToLive(_ttl);
		_socket.setSoTimeout(_timeout);
		_socket.send(capture(_msg));
		_socket.receive(capture(_msg));
		expectLastCall().andAnswer(new IAnswer()
		{
			@Override public Object answer() throws Throwable
			{
				final DatagramPacket packet = UdpTransport.createPacket(_peerPulse.get(), groupAddress, _destPort);
				final DatagramPacket msg = _msg.getValue();
				msg.setLength(packet.getLength());
				msg.setData(packet.getData());
				return null;
			}
		});
		_socket.close();
		_socket.close();
		
		replay(_mocks);
		
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		udp.send(_pulse.get());
		udp.receive(_pulse.get(), _timeout);
		udp.disconnect();
		
		verify(_mocks);
	}
	
	@Test public void testToString() throws Exception
	{
		final MockSocketUdpTransport udp = new MockSocketUdpTransport(_groupAddress);
		udp.setTimeToLive(_ttl);
		udp.setSourcePort(_sourcePort);
		udp.setDestinationPort(_destPort);
		final String toString = udp.toString();
		assertTrue(toString.startsWith("udp://"));
		assertTrue(toString.contains(_groupAddress));
		assertTrue(toString.contains(Integer.toString(_sourcePort)));
		assertTrue(toString.contains(Integer.toString(_destPort)));
		assertTrue(toString.indexOf(Integer.toString(_sourcePort)) < toString.indexOf(Integer.toString(_destPort)));
	}
	
	private class MockSocketUdpTransport extends UdpTransport
	{
		MockSocketUdpTransport(String groupAddress) throws UnknownHostException
		{
			super(groupAddress);
		}
		
		@Override protected MulticastSocket createSocket(int port) throws IOException
		{
			return _socket;
		}
	}
}
