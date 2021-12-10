package com.github.sproutparser.common;

public enum Nestable {
	/** Indicates that multiline comments should be parsed in a way that allows them to be nested. */
	NESTABLE,
	/** Indicates that multiline comments should be parsed in a way that allows them to not be nested. */
	NOT_NESTABLE
}
