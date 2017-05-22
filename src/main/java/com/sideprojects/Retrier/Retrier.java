package com.sideprojects.Retrier;

import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

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

	private static final Retrier retrier = new Retrier.Builder().withStopStrategy(attempts -> attempts >= 1).build();

	public static Retrier singleAttenpt() {
		return retrier;
	}

	private Retrier(final Predicate<Integer> stopStrategy, final Predicate<Object> resultRetryStrategy,
			final Predicate<Exception> exceptionRetryStrategy, final GiveUpStrategy giveUpStrategy,
			final Function<Integer, Long> waitStrategy) {
		this.giveUpStrategy = giveUpStrategy;
		this.waitStrategy = waitStrategy;
		this.stopStrategy = stopStrategy;
		this.resultRetryStrategy = resultRetryStrategy;
		this.exceptionRetryStrategy = exceptionRetryStrategy;
	}

	public static class Builder {
		private Predicate<Exception> failedRetryStrategy = e -> true;
		private GiveUpStrategy giveUpStrategy = new GiveUpStrategy() {
			@Override
			public <T> T whenNoMoreAttempts(T lastResult, Exception lastException) throws Exception {
				if (lastException != null) {
					throw lastException;
				} else {
					return lastResult;
				}
			}
		};

		private Predicate<Integer> stopStrategy = attempt -> false;
		private Function<Integer, Long> waitStrategy = attempt -> 0l;
		private Predicate<Object> resultRetryStrategy = e -> true;

		public Retrier build() {
			return new Retrier(stopStrategy, resultRetryStrategy, failedRetryStrategy, giveUpStrategy, waitStrategy);
		}

		private Builder withFailedRetryStrategy(final Predicate<Exception> failedStrategy) {
			this.failedRetryStrategy = requireNonNull(failedStrategy);
			this.failedRetryStrategy = failedStrategy;
			return this;
		}

		private Builder withStopStrategy(final Predicate<Integer> stopStrategy) {
			this.stopStrategy = stopStrategy;
			return this;
		}

		private Builder withWaitStrategy(final Function<Integer, Long> waitStrategy) {
			this.waitStrategy = waitStrategy;
			return this;
		}

		private Builder withResultRetryStrategy(final Predicate<Object> resultRetryStrategy) {
			this.resultRetryStrategy = resultRetryStrategy;
			return this;
		}
	}

	public static class Strategies {

		public static Function<Integer, Long> waitConstantly(final long delay) {
			return whateveryoupass -> delay;
		}

		public static Function<Integer, Long> waitExponential() {
			return waitExponential(2);
		}

		public static Function<Integer, Long> waitExponential(final double backOffBase) {
			return attempts -> {
				if (attempts > 0) {
					final double backOffMillis = Math.pow(backOffBase, attempts);
					return Math.min(Math.round(backOffMillis), 1000L);
				}
				return 0l;
			};
		}

		// Retry only if provided exception was thrown.
		public static Predicate<Exception> retryOn(Class<? extends Throwable>... exceptions) {
			return exception -> Arrays.stream(exceptions).anyMatch(clazz -> clazz.getClass().isInstance(exception));
		}

		// Limit the number of attempts to a fixed value.
		public static Predicate<Integer> stopAfter(final Integer maxAttempts) {
			return attempts -> attempts >= maxAttempts;
		}
	}
}
