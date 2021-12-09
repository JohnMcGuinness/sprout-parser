package com.github.sproutparser.basic;

public sealed interface Problem
		permits
		Expecting,
		ExpectingInt,
		ExpectingNumber,
		ExpectingHex,
		ExpectingOctal,
		ExpectingBinary,
		ExpectingFloat,
		ExpectingEnd,
		ExpectingVariable,
		ExpectingSymbol,
		ExpectingKeyword,
		UnexpectedCharacter {

	/**
	 * The {@link ExpectingInt} problem.
	 */
	Problem EXPECTING_INT = new ExpectingInt();

	/**
	 * The {@link UnexpectedCharacter} problem.
	 */
	Problem UNEXPECTED_CHARACTER = new UnexpectedCharacter();

	/**
	 * The {@link ExpectingHex} problem.
	 */
	Problem EXPECTING_HEX = new ExpectingHex();

	/**
	 * The {@link ExpectingOctal} problem.
	 */
	Problem EXPECTING_OCTAL = new ExpectingOctal();

	/**
	 * The {@link ExpectingBinary} problem.
	 */
	Problem EXPECTING_BINARY = new ExpectingBinary();

	/**
	 * The {@link ExpectingFloat} problem.
	 */
	Problem EXPECTING_FLOAT = new ExpectingFloat();

	/**
	 * The {@link ExpectingNumber} problem.
	 */
	Problem EXPECTING_NUMBER = new ExpectingNumber();

	/**
	 * The {@link ExpectingEnd} problem.
	 */
	Problem EXPECTING_END = new ExpectingEnd();

	/**
	 * The {@link ExpectingVariable} problem.
	 */
	Problem EXPECTING_VARIABLE = new ExpectingVariable();

	/**
	 * Creates a problem with a custom message.
	 *
	 * @param expecting a description of the thing that is expected.
	 * @return an {@link Expecting} with a custom description of what's expected
	 */
	static Problem expecting(final String expecting) {
		return new Expecting(expecting);
	}

	/**
	 * Creates a problem indicating a symbol that is expected.
	 *
	 * @param expecting a description of the symbol that is expected.
	 * @return an {@link ExpectingSymbol} with a description of the expected symbol
	 */
	static Problem expectingSymbol(final String expecting) {
		return new ExpectingSymbol(expecting);
	}

	/**
	 * Creates a problem indicating a keyword that is expected.
	 *
	 * @param keyword a description of the keyword that is expected.
	 * @return an {@link ExpectingSymbol} with a description of the expected keyword
	 */
	static Problem expectingKeyword(final String keyword) {
		return new ExpectingKeyword(keyword);
	}
}
