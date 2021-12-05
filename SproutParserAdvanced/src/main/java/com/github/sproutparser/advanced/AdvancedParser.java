package com.github.sproutparser.advanced;

import com.github.sproutparser.common.AbstractParser;
import com.github.sproutparser.common.DeadEnd;
import com.github.sproutparser.common.PStep;
import com.github.sproutparser.common.ParserImpl;
import com.github.sproutparser.common.Position;
import com.github.sproutparser.common.Result;
import com.github.sproutparser.common.State;
import com.github.sproutparser.common.Token;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An {@link AdvancedParser} helps turn a {@link String} into nicely structured data. For example, we can {@code run} the {@link AdvancedParser#integer} parser
 * to turn a {@link String} into {@link Integer}:
 *
 * <pre>{@code
 *     Parser.run(integer(), "123456");
 *     // returns Ok(123456)
 *
 *     Parser.run(integer(), "3.1415");
 *     // returns Err(...)
 * }</pre>
 * <p>
 * The cool thing is that you can combine {@link AdvancedParser} values to handle much more complex scenarios.
 * <p>
 * <strong>Attribution</strong>: This library is a port of the Elm/Parser library from the Elm Language to Java&trade;. I'd like to attribute
 * Evan Cziplicki with the API design. I merely rewrote the logic in Java&trade;, although there are some parts of the API that
 * had to be modified, or omitted, due to the constraints of the Java&trade; language.
 *
 * @param <C> context
 * @param <X> problem
 * @param <T> value
 * @see <a href="https://github.com/elm/parser">elm/parser</a>
 */
public final class AdvancedParser<C, X, T> extends AbstractParser<C, X, T> {

	private AdvancedParser(
		final Function<State<C>, PStep<C, X, T>> parse
	) {
		super(parse);
	}

	/**
	 * This works just like {@code com.github.simpleparser.Parser.run()}. The only difference is that when it fails, it has much more
	 * precise information for each dead end.
	 *
	 * @param parser the parser to run
	 * @param source the {@link String} for the parser to parse
	 * @param <C>    context
	 * @param <X>    problem
	 * @param <T>    value
	 * @return the result of running the parser
	 */
	public static <C, X, T> Result<List<DeadEnd<C, X>>, T> run(
		final AdvancedParser<C, X, T> parser,
		final String source
	) {
		return ParserImpl.run(parser, source);
	}

	/**
	 * Just like {@code com.github.simpleparser.Parser.succeed()}.
	 *
	 * @param value the value that the parser should return.
	 * @param <C>   context
	 * @param <X>   problem
	 * @param <T>   value
	 * @return a parser that always succeeds with the argument passed in.
	 */
	public static <C, X, T> AdvancedParser<C, X, T> succeed(final T value) {
		return new AdvancedParser<>(ParserImpl.succeedF(value));
	}

	/**
	 * Just like {@code com.github.simpleparser.Parser.problem()} except you provide a type for the problem.
	 *
	 * @param problem
	 * @param <C>     context
	 * @param <X>     problem
	 * @param <T>     value
	 * @return a {@link AdvancedParser} that always fails with the provided problem.
	 */
	public static <C, X, T> AdvancedParser<C, X, T> problem(final X problem) {
		return new AdvancedParser<>(ParserImpl.problemF(problem));
	}

	/**
	 * Just like the {@code Parser.ignore()}.
	 *
	 * @param keep
	 * @param ignore
	 * @param <C>
	 * @param <X>
	 * @param <T1>
	 * @param <T2>
	 * @return a Parser that ignores the result of it's second argument.
	 */
	public static <C, X, T1, T2> AdvancedParser<C, X, T1> ignore(final AdvancedParser<C, X, T1> keep, final AdvancedParser<C, X, T2> ignore) {
		return new AdvancedParser<>(ParserImpl.ignoreF(keep, ignore));
	}

	/**
	 * Just like {@code com.github.simpleparser.Parser.map()}.
	 *
	 * @param f      the {@link Function} to be applied to the result of invoking {@code parser}.
	 * @param parser the {@link AdvancedParser} to invoke first.
	 * @param <C>    context
	 * @param <X>    problem
	 * @param <T>    value
	 * @param <R>    result
	 * @return a {@link AdvancedParser} that transforms the result of another {@link AdvancedParser}.
	 */
	public static <C, X, T, R> AdvancedParser<C, X, R> map(
		final Function<T, R> f,
		final AdvancedParser<C, X, T> parser
	) {
		return new AdvancedParser<>(ParserImpl.mapF(f, parser));
	}

	/**
	 * Combines the results of two {@link AdvancedParser}s.
	 *
	 * @param f       the {@link BiFunction} that will combine the results of {@code parserA} and {@code parserB} to produce an {@code R}.
	 * @param parserA the {@link AdvancedParser} to invoke first.
	 * @param parserB if {@code parserA} parses successfully then {@code parserB} is invoked.
	 * @param <C>     context
	 * @param <X>     problem
	 * @param <T1>    value of {@code parserA}
	 * @param <T2>    value of {@code parserB}
	 * @param <R>     value of the returned {@link AdvancedParser}
	 * @return a {@link AdvancedParser} that combines the result of two other {@link AdvancedParser}s.
	 */
	public static <C, X, T1, T2, R> AdvancedParser<C, X, R> map2(
		final BiFunction<T1, T2, R> f,
		final AdvancedParser<C, X, T1> parserA,
		final AdvancedParser<C, X, T2> parserB
	) {
		return new AdvancedParser<>(ParserImpl.map2F(f, parserA, parserB));
	}

//	public static <C, X, T, R> Parser<C, X, R> keep(final Parser<C, X, Function<T, R>> parseFunc, final Parser<C, X, T> parseArg) {
//		return map2(Function::apply, parseFunc, parseArg);
//	}

	/**
	 * Just like {@link }Parser#andThen}.
	 *
	 * @param callback applied to the result of {@code parser}
	 * @param parser
	 * @param <C>      context
	 * @param <X>      problem
	 * @param <T>      value of {@code parser}
	 * @param <R>      value of the returned {@link AdvancedParser}
	 * @return a {@link AdvancedParser} that transforms the result of another {@link AdvancedParser}.
	 */
	public static <C, X, T, R> AdvancedParser<C, X, R> andThen(
		final Function<T, AdvancedParser<C, X, R>> callback,
		final AdvancedParser<C, X, T> parser
	) {
		return new AdvancedParser<>(ParserImpl.andThenF(callback, parser));
	}

	/**
	 * This parser will keep trying parsers until {@code oneOf} them starts chomping characters. Once a path is chosen,
	 * it does not come back and try the others.
	 *
	 * @param parsers the {@link AdvancedParser parsers} to try
	 * @param <C> context type
	 * @param <X> problem type
	 * @param <T> value type
	 * @return an {@link AdvancedParser} that keeps trying parsers until one of them starts chomping
	 */
	@SafeVarargs
	public static <C, X, T> AdvancedParser<C, X, T> oneOf(final AdvancedParser<C, X, T>... parsers) {
		return new AdvancedParser<>(ParserImpl.oneOfF(List.of(parsers)));
	}

	public static <C, X, T> AdvancedParser<C, X, T> bracktrackable(final AdvancedParser<C, X, T> parser) {
		return new AdvancedParser<>(ParserImpl.backtrackableF(parser));
	}

	/**
	 *
	 * @param thunk the thunk that provides the {@link AdvancedParser}.
	 *
	 * @param <C> context type
	 * @param <X> problem type
	 * @param <T> value type
	 * @return the parser
	 */
	public static <C, X, T> AdvancedParser<C, X, T> lazy(
		final Supplier<AdvancedParser<C, X, T>> thunk
	) {
		return new AdvancedParser<>(ParserImpl.lazyF(thunk));
	}

	/**
	 * With the simpler {@code Parser}, you could just say {@code Parser.symbol(",")} and
	 * parse all the commas you wanted. But now that we have a generic type {@code X} for our
	 * problems, we actually have to specify that as well. So anywhere you just used
	 * a {@link String} in the simpler module, you now use a {@link Token &lt;X&gt;} in {@link AdvancedParser}:
	 *
	 * <pre>{@code
	 *     enum Problem {
	 *       ExpectingComma,
	 *       ExpectingListEnd
	 *     }
	 *
	 *     Token<Problem> comma = new Token(",", ExpectingComma);
	 *
	 *     Token<Problem> listEnd = Token("]", ExpectingListEnd);
	 * }</pre>
	 * You can be creative with your custom type. Maybe you want a lot of detail.
	 * Maybe you want looser categories. It is a custom type. Do what makes sense for
	 * you!
	 * @param token the {@code Token} to parse.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return the token parser
	 */
	public static <C, X> AdvancedParser<C, X, Void> token(final Token<X> token) {
		return new AdvancedParser<>(ParserImpl.tokenF(token));
	}

	/**
	 * Just like {@code Parser.symbol()} except you provide a {@link Token} to clearly indicate your type of problems:
	 *
	 * <pre>{@code
	 *     enum Problem {
	 * 	       ExpectingComma,
	 * 	       ...
	 * 	   };
	 *
	 *     Parser<Context, Problem, Void> comma
	 *         = symbol(new Token(",", ExpectingComma));
	 * }</pre>
	 *
	 * @param token the {@code Token} to parse
	 * @param <C> context type
	 * @param <X> problem type
	 * @return the symbol parser.
	 */
	public static <C, X> AdvancedParser<C, X, Void> symbol(final Token<X> token) {
		return token(token);
	}

	/**
	 *  Just like {@code Parser.end()} except you provide the problem that arises when the parser is not
	 *  at the end of the input.
	 *
	 * @param expecting the problem to return if you're not at the end of input.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a parser the checks that you're at the end of the input.
	 */
	public static <C, X> AdvancedParser<C, X, Void> end(final X expecting) {
		return new AdvancedParser<>(ParserImpl.endF(expecting));
	}

	/**
	 * Just like {@code Parser.integer()} where you have to handle negation yourself. The only difference is that
	 * you provide two potential problems:
	 *
	 * <blockquote><pre>
	 *     enum Problem {
	 *         ExpectingInt,
	 * 	       InvalidNumber,
	 * 	       ...
	 *     };
	 *
	 *     Parser&lt;Context, Problem, Integer&gt; myInt
	 *         = integer(ExpectingInt, InvalidNumber);
	 * </pre></blockquote>
	 *
	 * You can use problems like `ExpectingInt` and `InvalidNumber`.
	 * @param expecting the problem to report if the parse failed.
	 * @param invalid the problem to report if the integer is invalid.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return an integer parser with custom problems
	 */
	public static <C, X> AdvancedParser<C, X, Integer> integer(
		final X expecting,
		final X invalid
	) {
		return number(
			Result.ok(Function.identity()),
			Result.err(invalid),
			Result.err(invalid),
			Result.err(invalid),
			Result.err(invalid),
			invalid,
			expecting);
	}
//
//	/**
//	 * Just like {@link EasyParser#floatingPoint} where you have to handle negation yourself. The only difference is
//	 * that you provide two potential problems:
//	 *
//	 * <blockquote><pre>
//	 * enum Problem {
//	 *     ExpectingFloat,
//	 * 	   InvalidNumber,
//	 * 	   ...
//	 * };
//	 *
//	 * Parser&lt;Context, Problem, Integer&gt; myFloat
//	 *     = floatingPoint(ExpectingFloat, InvalidNumber);
//	 * </pre></blockquote>
//	 *
//	 * You can use problems like `ExpectingFloat` and `InvalidNumber`.
//	 *
//	 * @param expecting the problem to report if the parse failed.
//	 * @param invalid the problem to report if the integer is invalid.
//	 * @param <C> context type
//	 * @param <X> problem type
//	 * @return an floating point number parser with custom problems
//	 */
//	public static <C, X> AdvancedParser<C, X, Float> float_(final X expecting, final X invalid) {
//		return number(
//			Either.right(Float::valueOf),
//			Either.left(invalid),
//			Either.left(invalid),
//			Either.left(invalid),
//			Either.right(Function.identity()),
//			invalid,
//			expecting);
//	}

	/**
	 * Just like {@code Parser.number()} where you have to handle negation yourself. The only difference is that you
	 * provide all the potential problems.
	 *
	 * @param decimalInteger
	 * @param hexadecimal
	 * @param octal
	 * @param binary
	 * @param floatingPoint
	 * @param invalid
	 * @param expecting
	 * @param <C> context type
	 * @param <X> problem type
	 * @param <T> value type
	 * @return a parser that parses numeric content and produces a type that you specify.
	 */
	public static <C, X, T> AdvancedParser<C, X, T> number(
		final Result<X, Function<Integer, T>> decimalInteger,
		final Result<X, Function<Integer, T>> hexadecimal,
		final Result<X, Function<Integer, T>> octal,
		final Result<X, Function<Integer, T>> binary,
		final Result<X, Function<Float, T>> floatingPoint,
		final X invalid,
		final X expecting
	) {
		return new AdvancedParser<>(ParserImpl.numberF(decimalInteger, hexadecimal, octal, binary, floatingPoint, invalid, expecting));
	}

	/**
	 * This is how you mark that you are in a certain context.
	 *
	 * @param context the context which is being parsed
	 * @param parser the parser which can parse the context
	 * @param <C> context type
	 * @param <X> problem type
	 * @param <T> value type
 	 * @return a parser that is aware of the context it is parsing.
	 */
	public static <C, X, T> AdvancedParser<C, X, T> inContext(final C context, final AdvancedParser<C, X, T> parser) {
		return new AdvancedParser<>(ParserImpl.inContextF(context, parser));
	}

	/**
	 * Just like {@code Parser.keyword()} except you provide a {@code Token} to indicate your type of problems:
	 *
	 * <blockquote><pre>
	 * enum Problem {
	 *     ExpectingLet,
	 *     ...
	 * };
	 *
	 * Parser&lt;Context, Problem, Void&gt; let =
	 *     keyword (new Token&lt;&gt;("let",ExpectingLet));
	 * </pre></blockquote>
	 *
	 * Note that this would fail to chomp {@code "letter"} because of the subsequent
	 * characters. Use {@link AdvancedParser#token} if you do not want that last letter check.
	 *
	 * @param token the token that describes the keyword to parse.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a keyword {@link AdvancedParser}
	 */
 	public static <C, X> AdvancedParser<C, X, Void> keyword(final Token<X> token) {
		return new AdvancedParser<>(ParserImpl.keywordF(token));
	}

	/**
	 * Just like {@code Parser.variable()} except you specify the problem yourself.
	 *
	 * @param start a {@link Predicate} that defines the characters a variable can start with.
	 * @param inner a {@link Predicate} that defines the characters a variable can contain, excluding the starting character.
	 * @param reserved a {@link Set} of reserved words
	 * @param expecting the problem to report if the parser fails.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a parser that parses a variable/identifier name.
	 */
	public static <C, X> AdvancedParser<C, X, String> variable(
		final Predicate<Integer> start,
		final Predicate<Integer> inner,
		final Set<String> reserved,
		final X expecting
	) {
		return new AdvancedParser<>(ParserImpl.variableF(start, inner, reserved, expecting));
	}

	/**
	 * Parse zero or more characters that match {@code Character::isWhitespace}.
	 * The implementation is pretty simple:
	 *
	 * <blockquote><pre>
	 *     Parser&lt;Void&gt; spaces = chompWhile(Character::isWhitespace);
	 * </pre></blockquote>
	 * <p>
	 * So if you need something different (like tabs) just define an alternative with the necessary tweaks! Check out
	 * {@link AdvancedParser#lineComment} and {@link multiComment} for more complex situations.
	 *
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a Parser that parses spaces.
	 */
	public static <C, X> AdvancedParser<C, X, Void> spaces() {
		return new AdvancedParser<>(ParserImpl.spacesF());
	}

	/**
	 * This works just like {@link AdvancedParser#getChompedString} but gives a bit more flexibility.
	 *
	 * @param f the function that transforms the source and the {@link Result} of {@code parser}
	 * @param parser the {@link AdvancedParser} that provides an input to {@code f}
	 * @param <C> context type
	 * @param <X> problem type
	 * @param <T1> value type of the {@code parser} argument
	 * @param <T2> value type of the returned {@link AdvancedParser}
	 * @return an {@link AdvancedParser} that transforms the {@link Result} of another parser and the parsed source to
	 *         produce another type of value.
	 */
	public static <C, X, T1, T2> AdvancedParser<C, X, T2> mapChompedString(final BiFunction<String, T1, T2> f, final AdvancedParser<C, X, T1> parser) {
		return new AdvancedParser<>(ParserImpl.mapChompedStringF(f, parser));
	}

	public static <C, X, T> AdvancedParser<C, X, String> getChompedString(final AdvancedParser<C, X, T> parser) {
		return mapChompedString((a, b) -> a, parser);
	}

	public static <C, X> AdvancedParser<C, X, Void> chompUntilEndOr(final String str) {
		return new AdvancedParser<>(ParserImpl.chompUntilEndOrF(str));
	}

	/**
	 * Just like {@code Parser.chompIf} except you provide a problem in case a character cannot be chomped.
	 *
	 * @param isGood the predicate that tests if the character can be chomped
	 * @param problem the problem to report if the character cannot be chomped
	 * @param <C> context type
	 * @param <X> problem type
	 * @return an {@link AdvancedParser} that parses single character
	 */
	public static <C, X> AdvancedParser<C, X, Void> chompIf(final Predicate<Integer> isGood, final X problem) {
		return new AdvancedParser<>(ParserImpl.chompIfF(isGood, problem));
	}

	/**
	 * Chomp zero or more characters if they pass the test.
	 *
	 * @param isGood the predicate that tests if characters should be chomped
	 * @param <C> context type
	 * @param <X> problem type
	 * @return an {@link AdvancedParser} that chomps characters while the {@link Predicate} is satisfied
	 */
	public static <C, X> AdvancedParser<C, X, Void> chompWhile(final Predicate<Integer> isGood) {
		return new AdvancedParser<>(ParserImpl.chompWhile(isGood));
	}

	public static <C, X> AdvancedParser<C, X, Void> chompUntil(final Token<X> token) {
		return new AdvancedParser<>(ParserImpl.chompUntilF(token));
	}

	/**
	 * Parse single-line comments.
	 *
	 * @param start
	 * @param <C>
	 * @param <X>
	 * @return an {@link AdvancedParser} that parses single line comments
	 */
	public static <C, X> AdvancedParser<C, X, Void> lineComment(final Token<X> start) {
		return ignore(token(start), chompUntilEndOr("\n"));
	}

	public static <C, X> AdvancedParser<C, X, Position> getPosition() {
		return new AdvancedParser<>(ParserImpl.getPositionF());
	}

	public static <C, X> AdvancedParser<C, X, Integer> getRow() {
		return new AdvancedParser<>(ParserImpl.getRowF());
	}

	public static <C, X> AdvancedParser<C, X, Integer> getColumn() {
		return new AdvancedParser<>(ParserImpl.getColumnF());
	}

	public static <C, X> AdvancedParser<C, X, Integer> getOffset() {
		return new AdvancedParser<>(ParserImpl.getOffsetF());
	}

	public static <C, X> AdvancedParser<C, X, String> getSource() {
		return new AdvancedParser<>(ParserImpl.getSourceF());
	}
}
