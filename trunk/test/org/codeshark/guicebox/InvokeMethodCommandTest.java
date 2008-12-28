package org.codeshark.guicebox;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import org.junit.*;

/**
 * @author willhains
 */
public class InvokeMethodCommandTest
{
	@Test public void call() throws Exception
	{
		class TestClass
		{
			private boolean _called;
			
			@SuppressWarnings("unused") public void method()
			{
				_called = true;
			}
			
			void assertCalled()
			{
				assertTrue(_called);
			}
		}
		final Method method = TestClass.class.getMethod("method");
		final TestClass instance = new TestClass();
		final InvokeMethodCommand cmd = new InvokeMethodCommand(method, instance);
		cmd.call();
		instance.assertCalled();
		assertEquals("TestClass.method()", cmd.toString());
	}
}
