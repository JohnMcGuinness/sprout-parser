package com.github.sproutparser;

public sealed interface PStep<C, X, T> permits Good, Bad {

	/**
	 * @return {@code this} as an instance of {@link Good}.
	 */
	default Good<C, X, T> asGood() {
		return (Good<C, X, T>) this;
	}

	/**
	 * @return {@code this} as an instance of {@link Bad}.
	 */
	default Bad<C, X, T> asBad() {
		return (Bad<C, X, T>) this;
	}
}
