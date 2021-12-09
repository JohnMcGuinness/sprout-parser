package com.github.sproutparser;

import java.io.Serializable;
import java.util.Optional;

public sealed interface Result<X, T> extends Serializable permits Ok, Err {

	/**
	 * Gets the error.
	 * @return the error wrapped by this {@link Error}
	 * @throws java.util.NoSuchElementException if {@code this} is an {@link Ok}.
	 */
	X error();

	/**
	 * Gets the value.
	 * @return the value wrapped by this {@link Ok}
	 * @throws java.util.NoSuchElementException if {@code this} is an {@link Error}.
	 */
	T value();

	/**
	 * Indicates if this is an {@link Ok}.
	 *
	 * @return {@code true} if this is an {@link Ok}, false otherwise.
	 */
	boolean isOk();

	/**
	 * Indicates if this is an {@link Error}.
	 *
	 * @return {@code true} if this is an {@link Error}, false otherwise.
	 */
	boolean isErr();

	/**
	 * Creates an {@link Ok} from the provided {@code value}.
	 *
	 * @param value the value to wrap in the {@link Ok}
	 * @param <X> the type of the error
	 * @param <T> the type of the value
	 * @return an {@link Ok}
	 */
	static <X, T> Result<X, T> ok(T value) {
		return new Ok<>(value);
	}

	/**
	 * Creates an {@link Error} from the provided {@code error}.
	 *
	 * @param error the error to wrap in the {@link Error}
	 * @param <X> the type of the error
	 * @param <T> the type of the value
	 * @return an {@link Error}.
	 */
	static <X, T> Result<X, T> err(X error) {
		return new Err<>(error);
	}

	/**
	 * Creates a {@link Result} from an {@code error} and an {@link Optional}.
	 *
	 * @param error the error for if the {@link Optional} is not present
	 * @param value the {@link Optional} to transform
	 * @param <X> the type of the error
	 * @param <T> the type of the value
	 * @return a {@link Result} generated from the provided {@link Optional}.
	 */
	static <X, T> Result<X, T> fromOptional(X error, Optional<T> value) {
		return value.isPresent() ? new Ok<>(value.get()) : new Err<>(error);
	}
}
