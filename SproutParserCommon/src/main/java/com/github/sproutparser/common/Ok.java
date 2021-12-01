package com.github.sproutparser.common;

import java.io.Serializable;
import java.util.NoSuchElementException;

public record Ok<X, T>(T value) implements Serializable, Result<X, T> {

	@Override
	public X error() {
		throw new NoSuchElementException("error on Ok");
	}

	@Override
	public boolean isOk() {
		return true;
	}

	@Override
	public boolean isErr() {
		return !isOk();
	}
}

