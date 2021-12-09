package com.github.sproutparser;

import java.util.function.Function;

public abstract class AbstractParser<C, X, T> {

	private final Function<State<C>, PStep<C, X, T>> parse;

	protected AbstractParser(final Function<State<C>, PStep<C, X, T>> parse) {
		this.parse = parse;
	}

	protected Function<State<C>, PStep<C, X, T>> parse() {
		return parse;
	}
}
