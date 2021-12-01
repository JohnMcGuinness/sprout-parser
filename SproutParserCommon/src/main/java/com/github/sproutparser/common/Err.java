package com.github.sproutparser.common;

import java.io.Serializable;
import java.util.NoSuchElementException;

public record Err<X, T>(X error) implements Serializable, Result<X, T> {

	@Override
	public T value() {
		throw new NoSuchElementException("value() on Err");
	}

	@Override
	public boolean isOk() {
		return !isErr();
	}

	@Override
	public boolean isErr() {
		return true;
	}
}
