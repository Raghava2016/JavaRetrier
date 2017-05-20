package com.sideprojects.Retrier;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Retrier
 *
 */
public class Retrier {
	
	@FunctionalInterface
	public interface GiveUpStrategy {
		<T> T whenNoMoreAttempts(T lastResult, Exception lastException) throws Exception;
	}

	// what to do when all attempts have been exhausted and retier wasn't able
	// to perform the operation.
	private final GiveUpStrategy giveUpStrategy;
	// Strategies used to check if the retry is required given a failed
	// execution.
	private final Predicate<Exception> exceptionRetryStrategy;
	// Strategies used to check if the retry is required given a successful
	// execution with result.
	private final Predicate<Object> resultRetryStrategy;
	// Strategies used to check if the retry should be stopped given the
	// provided number of attempts already performed.
	private final Predicate<Integer> stopStrategy;
	// How much time to wait between retry attempts.
	private final Function<Integer, Long> waitStrategy;

	private Retrier(final Predicate<Integer> stopStrategy, final Predicate<Object> resultRetryStrategy,
			final Predicate<Exception> exceptionRetryStrategy, final GiveUpStrategy giveUpStrategy,
			final Function<Integer, Long> waitStrategy) {
		this.giveUpStrategy = giveUpStrategy;
		this.waitStrategy = waitStrategy;
		this.stopStrategy = stopStrategy;
		this.resultRetryStrategy = resultRetryStrategy;
		this.exceptionRetryStrategy = exceptionRetryStrategy;
	}

}
