package org.guicebox;

import static org.easymock.EasyMock.*;

import org.junit.*;

/**
 * @author willhains
 */
public class StandaloneTest
{
	// Mocks
	private Object[] _mocks;
	private Application _app;
	
	@Before public void createMocks()
	{
		_mocks = new Object[] { _app = createMock(Application.class) };
	}
	
	@Test public void joinAndLeave() throws Throwable
	{
		_app.start();
		
		replay(_mocks);
		
		final Cluster cluster = new Standalone();
		cluster.join(_app);
		cluster.leave();
		
		verify(_mocks);
	}
}
