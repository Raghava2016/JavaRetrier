package com.sideprojects.Retrier;

import java.util.function.Function;
import java.util.function.Predicate;

import com.sideprojects.Retrier.Retrier.Builder;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Retrier
 *
 */
public class Retrier {

	final Predicate<Integer> stopStratgey;
	final Predicate<Exception> failedStratgey;
	final Predicate<Object> retryStratgey;
	final Function<Integer, Long> waitStratgey;

	private Retrier(final Predicate<Integer> stopStratgey, final Predicate<Exception> failedStratgey,
			final Predicate<Object> retryStratgey, final Function<Integer, Long> waitStratgey) {
		this.stopStratgey = stopStratgey;
		this.failedStratgey = failedStratgey;
		this.waitStratgey = waitStratgey;
		this.retryStratgey = retryStratgey;
	}

	public static class Builder {
		Predicate<Integer> stopStratgey = e -> false;
		Predicate<Exception> failedStratgey = e -> true;
		Predicate<Object> retryStratgey = obj -> false;
		Function<Integer, Long> waitStratgey = i -> 1l;

		public Builder withStopStrategy(final Predicate<Integer> stopStratgey) {
			this.stopStratgey = stopStratgey;
			return this;
		}

		public Retrier build() {
			return new Retrier(stopStratgey, failedStratgey, retryStratgey, waitStratgey);
		}

		public Builder withFailedRetryStrategy(final Predicate<Exception> failedRetryStrategy) {
			this.failedStratgey = failedRetryStrategy;
			return this;
		}

		public Builder withResultRetryStrategy(final Predicate<Object> retryStratgey) {
			this.retryStratgey = retryStratgey;
			return this;
		}
		
		public Builder withWaitStrategy(final Function<Integer, Long> waitStratgey) {
			this.waitStratgey = waitStratgey;
			return this;
		}
	}

	public static class Strategy {

		public static Predicate<Integer> stopAfter(int maxAttempts) {
			return attempts -> attempts >= maxAttempts;
		}
	}

	public <T> T execute(Callable<T> callable) throws Exception {
		int attempts = 0;
		boolean shouldRetry;
		Exception lastException = null;
		T lastResult = null;
		boolean attemptFailed = false;
		boolean isInterrupted = false;
		do {
			attempts++;
			lastResult = null;
			lastException = null;
			attemptFailed = false;
			isInterrupted = false;
			try {
				lastResult = callable.call();
				attemptFailed = retryStratgey.test(lastResult);
			} catch (Exception e) {
				lastException = e;
				attemptFailed = failedStratgey.test(lastException);
			} finally {
				isInterrupted = Thread.interrupted() || isInterruptedException(lastException);
				shouldRetry = attemptFailed && !isInterrupted && !stopStratgey.test(attempts);
			}
			if (shouldRetry) {
				final long waitMillis = waitStratgey.apply(attempts);
				if (waitMillis > 0) {
					try {
						Thread.currentThread().sleep(waitMillis);
					} catch (InterruptedException e) {
						isInterrupted = true;
						shouldRetry = false;
					}
				}
			}
		} while (shouldRetry);
		if (isInterrupted) {
			Thread.currentThread().interrupt();
		}
		if (lastException != null) {
			throw lastException;
		}
		return lastResult;
	}

	private boolean isInterruptedException(Exception lastException) {
		Throwable lastError = lastException;
		while (lastError != null && !InterruptedException.class.isInstance(lastError)) {
			lastError = lastError.getCause();
		}

		return lastError != null;
	}
}
