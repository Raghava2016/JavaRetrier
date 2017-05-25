package com.sideprojects.Retrier;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.anyInt;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.fail;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sideprojects.Retrier.Retrier.Strategy;

/**
 * Unit test for simple App.
 */
public class RetrierTest {

	private static final int MAX_ATTEMPTS = 10;

	@Mock
	private Callable<Object> callable;
	private Retrier victim;

	@BeforeMethod
	public void setUp() {
		Thread.interrupted();
		initMocks(this);
		victim = createDefaultBuilder().build();
	}

	private Retrier.Builder createDefaultBuilder() {
		return new Retrier.Builder().withStopStrategy(Strategy.stopAfter(MAX_ATTEMPTS));
	}

	private static class CustomException extends Exception {

	}

	private Retrier withCustomExceptionFailedStrategy() {
		return createDefaultBuilder().withFailedRetryStrategy(CustomException.class::isInstance).build();
	}

	@Test
	public void shouldStopFailingAfterMaxAttemptsReached() throws Exception {
		doThrow(Exception.class).when(callable).call();
		try {
			victim.execute(callable);
			fail("Did not throw an exception after last attempt failed");
		} catch (Exception e) {

		}
		verify(callable, times(MAX_ATTEMPTS)).call();
	}

	@Test(expectedExceptions = RuntimeException.class)
	public void shouldPropogateWithNoRetryWhenNotCustomExceptionThrown() throws Exception {
		doThrow(RuntimeException.class).when(callable).call();
		try {
			victim = withCustomExceptionFailedStrategy();
			victim.execute(callable);
		} catch (Exception e) {
			verify(callable, times(1)).call();
			throw e;
		}
	}

	@Test
	public void shouldRetryOnRecoverableException() throws Exception {
		willThrow(CustomException.class).willAnswer(i -> 2).given(callable).call();
		victim = withCustomExceptionFailedStrategy();
		Assert.assertEquals(victim.execute(callable), new Integer(2));
		verify(callable, times(2)).call();
	}

	@Test(expectedExceptions = InterruptedException.class)
	public void shouldRetryOnRecoverableExceptionAndStopOnInterruptedException() throws Exception {
		willThrow(CustomException.class).willThrow(CustomException.class).willThrow(InterruptedException.class).given(callable).call();
		victim = withCustomExceptionFailedStrategy();
		try {
			victim.execute(callable);
		} finally {
			verify(callable, times(3)).call();
			Assert.assertTrue(Thread.currentThread().isInterrupted());
		}
	}
	
	@Test
	public void shouldInvokeOnlyOnceWhenFirstCallDoesNotFail() throws Exception {
		doReturn(2).when(callable).call();
		victim = createDefaultBuilder().build();
		victim.execute(callable);
		verify(callable, times(1)).call();
	}
	
	@Test
	public void shouldRetryWhenUsingCustomRetry() throws Exception {
		victim = createDefaultBuilder().withResultRetryStrategy(e -> ((Integer)e >= new Integer(2))).build();
		doReturn(new Integer(2)).when(callable).call();
		willAnswer(e -> 3).willAnswer( e -> 2).willAnswer(e -> 1).given(callable).call();
		victim.execute(callable);
		verify(callable, times(3)).call();
	}
	
	@Test
	public void shouldInvokeWaitStrategyForEveryRetry() throws Exception {
		doThrow(CustomException.class).when(callable).call();
		Function<Integer,Long> waitStrategy = mock(Function.class);
		doReturn(1l).when(waitStrategy).apply(anyInt());
		try {
			victim = createDefaultBuilder().withFailedRetryStrategy(CustomException.class::isInstance).withWaitStrategy(waitStrategy).build();
			victim.execute(callable);
			fail("Did not throw an exception after last attempt failed");
		} catch (Exception e) {
			verify(callable, times(MAX_ATTEMPTS)).call();
		}
		
		verify(callable, times(MAX_ATTEMPTS)).call();
		verify(waitStrategy, times(MAX_ATTEMPTS-1)).apply(anyInt());
	}
}
