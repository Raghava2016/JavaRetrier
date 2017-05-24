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
	
	private Retrier(Predicate<Integer> stopStratgey) {
		this.stopStratgey = stopStratgey;
	}

	public static class Builder {
		Predicate<Integer> stopStratgey = e -> false;

		public Builder withStopStrategy(final Predicate<Integer> stopStratgey) {
			this.stopStratgey = stopStratgey;
			return this;
		}
		
		public Retrier build() {
			return new Retrier(stopStratgey);
		}
	}

	public static class Strategy {
		
		public static Predicate<Integer> stopAfter(int maxAttempts)
		{
			return attempts -> attempts >= maxAttempts;
		}
	}

	public <T> T execute(Callable<T> callable) throws Exception {
		int attempts = 0;
		boolean shouldRetry;
		//boolean attemptFailed;
		do {
			attempts++;
			callable.call();
			shouldRetry = !stopStratgey.test(attempts);
		} while (shouldRetry);
		return null;
	}
}
