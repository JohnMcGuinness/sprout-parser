package com.github.sproutparser;

import com.github.sproutparser.common.AbstractParser;
import com.github.sproutparser.common.Err;
import com.github.sproutparser.common.Ok;
import com.github.sproutparser.common.PStep;
import com.github.sproutparser.common.Result;
import com.github.sproutparser.common.State;
import com.github.sproutparser.common.ParserImpl;
import com.github.sproutparser.common.Token;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Parser<T> extends AbstractParser<Void, Problem, T> {

	private Parser(final Function<State<Void>, PStep<Void, Problem, T>> parse) {
		super(parse);
	}

	/**
	 *  Try a parser. Here are some examples using the keyword parser:
	 *
	 * <pre>{@code
	 *     run(keyword("true"), "true");
	 *     // returns Ok(...)
	 *
	 *     run(keyword("true"), "True");
	 *     // returns Err(...)
	 *
	 *     run(keyword("true"), "false");
	 *     // returns Err(...)
	 *
	 *     run(keyword("true"), "true!");
	 *     // returns Ok(...)
	 * }</pre>
	 * Notice the last case! A Parser will chomp as much as possible and not worry about the rest. Use the
	 * end parser to ensure you made it to the end of the string!
	 *
	 * @param parser the parser to run
	 * @param source the source for the parser to parse
	 * @param <T> the type of the result of successful parsing
	 * @return the {@link Result} of parsing
	 */
	public static <T> Result<List<DeadEnd>, T> run(final Parser<T> parser, final String source) {

		final Result<List<com.github.sproutparser.common.DeadEnd<Void, Problem>>, T> result = ParserImpl.run(parser, source);

		if (result instanceof Ok<?, T> right) {
			return new Ok<>(right.value());
		} else {
			final List<com.github.sproutparser.common.DeadEnd<Void, Problem>> problems = result.error();

			final List<DeadEnd> deadEnds = problems
				.stream()
				.map(Parser::problemToDeadEnd)
				.collect(Collectors.toList());

			return new Err<>(deadEnds);
		}
	}

	private static DeadEnd problemToDeadEnd(final com.github.sproutparser.common.DeadEnd<Void, Problem> problem) {
		return new DeadEnd(problem.row(), problem.column(), problem.problem());
	}

	/**
	 *  A parser that succeeds without chomping any characters.
	 * <pre>{@code
	 *     run(succeed(90210), "mississippi");
	 *     // returns Ok(90210)
	 *     run(succeed(3.141), "mississippi");
	 *     // returns Ok(3.141)
	 *     run(succeed(null)), "mississippi");
	 *     // returns Ok(null)
	 * }</pre>
	 * Seems weird on its own, but it is very useful in combination with other functions.
	 *
	 * @param value the value to return as the parsing result
	 * @param <T> the type of the result of successful parsing
	 * @return a {@link Parser} that returns an {@link Ok} result with a {@code value} of value.
	 */
	public static <T> Parser<T> succeed(final T value) {
		return  new Parser<>(ParserImpl.succeedF(value));
	}

	/**
	 * Indicate that a parser has reached a dead end. "Everything was going fine until I ran into this problem." Check
	 * out the {@link Parser#andThen} docs to see an example usage.
	 * @param problem the problem to return as the {@link Error}
	 * @param <T> the type of the result of successful parsing
	 * @return a {@link Parser} that returns an {@link Error} result with a {@code problem} of problem.
	 */
	public static <T> Parser<T> problem(final Problem problem) {
		return new Parser<>(ParserImpl.problemF(problem));
	}

//	public static <T1, T2> Parser<T1> ignore(final Parser<T1> keep, final Parser<T2> ignore) {
//		return new Parser<>(ParserImpl.ignoreF(keep, ignore));
//	}

//	public static Parser<Void> lineComment(final Token start) {
//		return ignore(token(start), chompUntilEndOr("\n"));
//	}

	/**
	 * Transform the result of a parser. Maybe you have a value that is an {@code int} or {@code null}:
	 *
	 * <p>TODO - check this code is correct</p>
	 * <pre>{@code
	 *     Parser<Optional<Integer>> nullOrInt =
	 *         oneOf(
	 *             map(Optional::of, integer()),
	 *             map(() -> null, keyword("null"))
	 *         );
	 *
	 *     run(nullOrInt, "0");
	 *     // returns Ok(Optional.of(0))
	 *
	 *     run(nullOrInt, "13");
	 *     // returns Ok(Optional.of(13))
	 *
	 *     run(nullOrInt, "null");
	 *     // returns Ok(null)
	 *
	 *     run(nullOrInt, "zero");
	 *     // returns Error(...)
	 * }</pre>
	 * @param f a {@link Function} that transform the result of {@code parser}
	 * @param parser the {@link Parser} that when run successfully provides a value to {@code f};
	 * @param <T> the type of the result from {@code parser} when parsing successfully
	 * @param <R> the type of the result from {@code f}
	 * @return a {@link Parser} that transforms the result of another {@link Parser}.
	 */
	public static <T, R> Parser<R> map(final Function<T, R> f, final Parser<T> parser) {
		return new Parser<>(ParserImpl.mapF(f, parser));
	}

//	public static <T1, T2, R> Parser<R> map2(final BiFunction<T1, T2, R> f, final Parser<T1> parserA, final Parser<T2> parserB) {
//		return new Parser<>(ParserImpl.map2F(f, parserA, parserB));
//	}

	/**
	 *  Parse one thing {@code andThen} parse another thing. This is useful when you want to check on what you just
	 *  parsed. For example, maybe you want U.S. zip codes and {@link Parser#integer} is not suitable because it does not
	 *  allow leading zeros. You could say:
	 * <p>TODO - check this code is correct</p>
	 * <pre>{@code
	 * Parser<String> zipCode
	 *     = andThen(
	 *           checkZipCode(getChompedString()),
	 *           chompWhile(Character::isDigit)
	 *       );
	 *
	 * Function<String, Parser<String>> checkZipCode = code -> {
	 *     if(code.length() == 5) {
	 *         return succeed(code);
	 *     } else {
	 *         return problem("a U.S. zip code has exactly 5 digits");
	 *     }
	 * };
	 * }</pre>
	 * First we chomp digits andThen we check if it is a valid U.S. zip code. We succeed if it has exactly five digits
	 * and report a problem if not.
	 *
	 * @param callback the {@link Function} that accepts the result of the first {@link Parser} and then parses a second
	 *                 thing.
	 * @param parser the parser that parses the first thing
	 * @param <T> the type of the result of successfully parsing the first thing
	 * @param <R> the type of the result of successfully parsing the second thing
	 * @return a {@link Parser} that parses a thing and then another thing.
	 */
	public static <T, R> Parser<R> andThen(final Function<T, Parser<R>> callback, final Parser<T> parser) {
		return new Parser<>(ParserImpl.andThenF(callback, parser));
	}

	/**
	 * Helper to define recursive parsers.
	 *
	 * @param thunk
	 * @param <T> the type of the result from the {@code parser} in {@code thunk} when parsing successfully
	 * @return a parser
	 */
	public static <T> Parser<T> lazy(final Supplier<Parser<T>> thunk) {
		return new Parser<>(ParserImpl.lazyF(thunk));
	}

	/**
	 * Parse exactly the given string, without any regard to what comes next.
	 *
	 * @param token the string to parse
	 * @return a {@link Parser} that parses the given {@link String}
	 */
	public static Parser<Void> token(final String token) {
		return new Parser<>(ParserImpl.tokenF(new Token<>(token, new Expecting(token))));
	}

	/**
	 * Parse zero or more ' ', '\n' or '\r' characters.
	 * <p>
	 * If you need something different (like tabs) just define an alternative with the necessary tweaks! Check out
	 * {@link lineComment} and {@link multiComment} for more complex situations.
	 *
	 * @return a Parser that parses spaces.
	 */
	public static Parser<Void> spaces() {
		return new Parser<>(ParserImpl.spacesF());
	}

	/**
	 * Check if you have reached the end of the string you are parsing.
	 * <p>
	 * Ending your parser with {@link Parser#end} guarantees that you have successfully parsed the whole string.
	 * @return a {@link Parser} that is successful if it parses the end of the input
	 */
	public static Parser<Void> end() {
		return new Parser<>(ParserImpl.endF(Problem.EXPECTING_END));
	}

	/**
	 * Parse a bunch of different kinds of numbers without backtracking.
	 *
	 * @param integerFn
	 * @param hexadecimalFn
	 * @param octalFn
	 * @param binaryFn
	 * @param floatFn
	 * @param <T>
	 * @return a {@link Parser} that can parse different kinds of numbers
	 */
	public static <T> Parser<T> number(
		final Optional<Function<Integer, T>> integerFn,
		final Optional<Function<Integer, T>> hexadecimalFn,
		final Optional<Function<Integer, T>> octalFn,
		final Optional<Function<Integer, T>> binaryFn,
		final Optional<Function<Float, T>> floatFn) {

		return new Parser<>(ParserImpl.numberF(
			Result.fromOptional(Problem.EXPECTING_INT, integerFn),
			Result.fromOptional(Problem.EXPECTING_HEX, hexadecimalFn),
			Result.fromOptional(Problem.EXPECTING_OCTAL, octalFn),
			Result.fromOptional(Problem.EXPECTING_BINARY, binaryFn),
			Result.fromOptional(Problem.EXPECTING_FLOAT, floatFn),
			Problem.EXPECTING_NUMBER,
			Problem.EXPECTING_NUMBER)
		);
	}

	/**
	 * Parse integers.
	 * @return a {@link Parser} that parses integers
	 */
	public static Parser<Integer> integer() {
		return number(
			Optional.of(Function.identity()),
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Optional.empty()
		);
	}

	/**
	 * Create a parser for variables.
	 * @param start
	 * @param inner
	 * @param reserved
	 * @return a {@link Parser} of variables
	 */
	public static Parser<String> variable(
		final Predicate<Integer> start,
	    final Predicate<Integer> inner,
	    final Set<String> reserved
	) {
		return new Parser<>(ParserImpl.variableF(start, inner, reserved, Problem.EXPECTING_VARIABLE));
	}

	private static Token<Problem> toToken(final String str) {
		return new Token<>(str, Problem.expecting(str));
	}
}
