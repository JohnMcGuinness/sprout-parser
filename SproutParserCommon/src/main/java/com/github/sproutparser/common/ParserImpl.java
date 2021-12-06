package com.github.sproutparser.common;

import com.github.sproutparser.common.internal.Bag;
import com.github.sproutparser.common.internal.Empty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ParserImpl {

	private ParserImpl() { }

	/**
	 * Attempt to run a {@code parser}, parsing {@code source}.
	 *
	 * @param parser the parser to run
	 * @param source the source to parse
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a {@link Result} produced by running the parser.
	 */
	public static <C, X, T> Result<List<DeadEnd<C, X>>, T> run(
		final AbstractParser<C, X, T> parser,
		final String source
	) {
		final Function<State<C>, PStep<C, X, T>> parse = parser.parse();
		final PStep<C, X, T> pStep = parse.apply(new State<>(source, 0, 1, List.of(), 1, 1));

		if (pStep instanceof Good<C, X, T> good) {
			return new Ok<>(good.value());
		} else {
			return new Err<>(Bag.bagToList(pStep.asBad().bag(), io.vavr.collection.List.empty()).toJavaList());
		}
	}

	/**
	 * Always produce a successful {@link Result}.
	 *
	 * @param value the value to succeed with.
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a function implementing a {@code parser} that always succeeds.
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> succeedF(final T value) {
		return state -> new Good<>(false, value, state);
	}

	/**
	 * Always produce a failing {@link Result}.
	 *
	 * @param problem the error to fail with.
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a function implementing a {@code parser} that always fails.
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> problemF(final X problem) {
		return state -> new Bad<>(false, Bag.fromState(state, problem));
	}

	/**
	 * Transform the {@link Result} of a parser to a different type of value.
	 *
	 * @param f a function that transforms the {@link Result}
	 * @param parser a parser that provides a {@link Result} to be transformed
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the argument parsers value type
	 * @param <R> the returned parsers value type
	 * @return a function that implements the transformation of a parser's {@link Result} to a new type of value.
	 */
	public static <C, X, T, R> Function<State<C>, PStep<C, X, R>> mapF(
		final Function<T, R> f,
		final AbstractParser<C, X, T> parser
	) {
		return state -> {
			final PStep<C, X, T> result = parser.parse().apply(state);
			if (result instanceof Good<C, X, T> good) {
				return new Good<>(good.progress(), f.apply(good.value()), good.state());
			} else { // Must be an instance of bad
				final Bad<C, X, T> bad = result.asBad();
				return new Bad<>(bad.progress(), bad.bag());
			}
		};
	}

	/** Combines the two parsing {@link Result}s together.
	 *
	 * @param f       the {@link BiFunction} that will combine the results of {@code parserA} and {@code parserB} to produce an {@code R}.
	 * @param parserA the {@code Parser} to invoke first.
	 * @param parserB if {@code parserA} parses successfully then {@code parserB} is invoked.
	 * @param <C>     the context type
	 * @param <X>     the problem type
	 * @param <T1>    value of {@code parserA}
	 * @param <T2>    value of {@code parserB}
	 * @param <R>     the returned parsers value type
	 * @return a {@link Function} that implements the map2 logic.
	 */
	public static <C, X, T1, T2, R> Function<State<C>, PStep<C, X, R>> map2F(
		final BiFunction<T1, T2, R> f,
		final AbstractParser<C, X, T1> parserA,
		final AbstractParser<C, X, T2> parserB
	) {
		return state -> {
			final PStep<C, X, T1> resultA = parserA.parse().apply(state);

			if (resultA instanceof Bad<C, X, T1> badA) {
				return new Bad<>(badA.progress(), badA.bag());

			} else { // Must be an instance of good
				final Good<C, X, T1> goodA = resultA.asGood();
				final PStep<C, X, T2> resultB = parserB.parse().apply(goodA.state());

				if (resultB instanceof Bad<C, X, T2> badB) {
					return new Bad<>(goodA.progress() || badB.progress(), badB.bag());
				} else { // Must be an instance of good
					final Good<C, X, T2> goodB = resultB.asGood();
					return new Good<>(goodA.progress() || goodB.progress(), f.apply(goodA.value(), goodB.value()), goodB.state());
				}
			}
		};
	}

	/**
	 * Allows parsers to be chained together.
	 *
	 * @param callback a function that accepts the result of {@code parser} and returns a parser of another type.
	 * @param parser  a parser that produces a result to be passed to {@code callback}
	 * @param <C>     the context type
	 * @param <X>     the problem type
	 * @param <T>     value of {@code parser}
	 * @param <R>     the returned parsers value type
	 * @return a function that runs a parser and passes that result into a function that produces a parser of another type.
	 */
	public static <C, X, T, R> Function<State<C>, PStep<C, X, R>> andThenF(
		final Function<T, ? extends AbstractParser<C, X, R>> callback,
		final AbstractParser<C, X, T> parser
	) {
		return state -> {

			final PStep<C, X, T> resultA = parser.parse().apply(state);

			if (resultA instanceof Bad<C, X, T> badA) {
				return new Bad<>(badA.progress(), badA.bag());
			} else {
				final Good<C, X, T> goodA = resultA.asGood();
				final AbstractParser<C, X, R> parserB = callback.apply(goodA.value());
				final PStep<C, X, R> resultB = parserB.parse().apply(goodA.state());

				if (resultB instanceof Bad<C, X, R> badB) {
					return new Bad<>(goodA.progress() || badB.progress(), badB.bag());
				} else {
					final Good<C, X, R> goodB = resultB.asGood();
					return new Good<>(goodA.progress() || goodB.progress(), goodB.value(), goodB.state());
				}
			}
		};
	}

	/**
	 * @param thunk a function that wraps the parser to be run
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a function that lazily gets a parser and applies it.
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> lazyF(final Supplier<? extends AbstractParser<C, X, T>> thunk) {
		return s -> thunk.get().parse().apply(s);
	}

	/**
	 * @param token a description of the token to be parsed
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements token parsing logic.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> tokenF(final Token<X> token) {

		final boolean progress = !token.string().isEmpty();

		return state -> {

			final int[] triple
				= isSubString(token.string(), state.offset(), state.row(), state.column(), state.source());

			final int newOffset = triple[0];
			final int newRow = triple[1];
			final int newCol = triple[2];

			if (newOffset == -1) {
				return new Bad<>(false, Bag.fromState(state, token.expecting()));
			} else {
				return new Good<>(progress, null,
					new State<>(state.source(), newOffset, state.indent(), state.context(), newRow, newCol));
			}
		};
	}

	/**
	 * @param expecting the problem to be reported if the parser fails.
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements end of input parsing.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> endF(final X expecting) {
		return state -> {

			if (state.source().length() == state.offset()) {
				return new Good<>(false, null, state);
			} else {
				return new Bad<>(false, Bag.fromState(state, expecting));
			}
		};
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements parsing of zero or more spaces.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> spacesF() {
		return chompWhile(c -> ' ' == c || '\n' == c || '\r' == c);
	}

	/**
	 * @param keepP
	 * @param ignoreP
	 * @param <C>     the context type
	 * @param <X>     the problem type
	 * @param <T1>    value of {@code keepP}
	 * @param <T2>    value of {@code ignoreP}
	 * @return a function that implements running of 2 parsers, discarding the second result.
	 */
	public static <C, X, T1, T2> Function<State<C>, PStep<C, X, T1>> ignoreF(final AbstractParser<C, X, T1> keepP, final AbstractParser<C, X, T2> ignoreP) {
		return map2F((keep, ignore) -> keep, keepP, ignoreP);
	}

	/**
	 * Implements the parsing logic to parse a keyword.
	 *
	 * @param token the token that describes the keyword to parse.
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements parsing of a keyword.
	 */
 	public static <C, X> Function<State<C>, PStep<C, X, Void>> keywordF(final Token<X> token) {

		final String kwd = token.string();
		final X expecting = token.expecting();

		final boolean progress = !kwd.isEmpty();

		return state -> {

			final int[] newPosition = isSubString(kwd, state.offset(), state.row(), state.column(), state.source());

			final int newOffset = newPosition[0];
			final int newRow = newPosition[1];
			final int newCol = newPosition[2];

			if (newOffset == -1 || 0 <= isSubChar(c -> Character.isLetterOrDigit(c) || c == '_', newOffset, state.source())) {
				return new Bad<>(false, Bag.fromState(state, expecting));
			} else {
				return new Good<>(progress, null,
					new State<>(state.source(), newOffset, state.indent(), state.context(), newRow, newCol));
			}
		};
	}

	/**
	 * This is how you mark that you are in a certain context.
	 *
	 * @param context the context which is being parsed
	 * @param parser the parser which can parse the context
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
 	 * @return a function that implements context aware parsing.
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> inContextF(final C context, final AbstractParser<C, X, T> parser) {

		return state -> {

			final List<Located<C>> newContextStack = new ArrayList<>(state.context());
			if (newContextStack.isEmpty()) {
				newContextStack.add(new Located<>(state.row(), state.column(), context));
			} else {
				newContextStack.set(0, new Located<>(state.row(), state.column(), context));
			}

			final PStep<C, X, T> result = parser.parse().apply(changeContext(newContextStack, state));

			if (result instanceof Good<C, X, T> good) {
				return new Good<>(good.progress(), good.value(), changeContext(state.context(), good.state()));
			} else {
				return result;
			}
		};
	}

	private static <C> State<C> changeContext(final List<Located<C>> newContext, final State<C> s) {
		return new State<>(s.source(), s.offset(), s.indent(), newContext, s.row(), s.column());
	}

	/**
	 * @param isGood the predicate that tests if the character can be chomped
	 * @param expecting the problem to report if the character cannot be chomped
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a {@link Function} that implements chompIf parsing
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> chompIfF(final Predicate<Integer> isGood, final X expecting) {

		return s -> {
			final int newOffset = isSubChar(isGood, s.offset(), s.source());

			if (newOffset == -1) {
				return new Bad<>(false, Bag.fromState(s, expecting));
			} else if (newOffset == -2) {
				return new Good<>(true, null, new State<C>(s.source(), s.offset(), s.indent(), s.context(), s.row() + 1, 1));
			} else {
				return new Good<>(true, null, new State<C>(s.source(), newOffset, s.indent(), s.context(), s.row(), s.column()  + 1));
			}
		};
	}

	/**
	 * Consumes characters while a predicate is satisfied.
	 *
	 * @param isGood a predicate that tests if the current character should be consumed
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements parsing while a predicate is satisfied.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> chompWhile(final Predicate<Integer> isGood) {
		return s -> chompWhileHelp(isGood, s.offset(), s.row(), s.column(), s);
	}

	private static <C, X> PStep<C, X, Void> chompWhileHelp(
		final Predicate<Integer> isGood,
		final int initialOffset,
		final int initialRow,
		final int initialColumn,
		final State<C> s0
	) {

		@Mutable int offset = initialOffset;
		@Mutable int row = initialRow;
		@Mutable int column = initialColumn;

		@Mutable int newOffset = isSubChar(isGood, offset, s0.source());

		while (newOffset != -1) {

			if (newOffset == -2) {
				offset = offset + 1;
				row = row + 1;
				column = 1;
			} else {
				offset = newOffset;
				column = column + 1;
			}

			newOffset = isSubChar(isGood, offset, s0.source());
		}

		return new Good<>(s0.offset() < offset, null, new State<>(s0.source(), offset, s0.indent(), s0.context(), row, column));
	}

	/**
	 * Consumes characters to the end of the line or until the specified string is encountered.
	 *
	 * @param str the string to consume to the end of.
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that implements parsing up to the end of the provided string or the end of the line.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> chompUntilEndOrF(final String str) {
		return s -> {
			int[] ints = findSubString(str, s.offset(), s.row(), s.column(), s.source());

			final int newOffset = ints[0];
			final int newRow = ints[1];
			final int newColumn = ints[2];
			final int adjustedOffset = newOffset < 0 ? s.source().length() : newOffset;

			return new Good<>(
				s.offset() < adjustedOffset,
				null,
				new State<>(s.source(), adjustedOffset, s.indent(), s.context(), newRow, newColumn)
			);
		};
	}

	/**
	 * @param token the {@link Token} that defines the {@link String} that causes parsing to stop,
	 *              and the problem to report if the {@link String} is not encountered.
	 *
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a {@link Function} that implements chomping until a specified {@link String} is encountered
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> chompUntilF(final Token<X> token) {

		return s -> {
			final int[] ints = findSubString(token.string(), s.offset(), s.row(), s.column(), s.source());
			final int newOffset = ints[0];
			final int newRow = ints[1];
			final int newColumn = ints[2];

			if (newOffset == -1) {
				return new Bad<>(
					false,
					Bag.fromInfo(newRow, newColumn, token.expecting(), s.context())
				);
			} else {
				return new Good<>(
					s.offset() < newOffset,
					null,
					new State<>(s.source(), newOffset, s.indent(), s.context(), newRow, newColumn)
				);
			}
		};
	}

	static int[] findSubString(
		final String shorterString,
		final int initialOffset,
		final int initialRow,
		final int initialColumn,
		final String longerString
	) {

		@Mutable int offset = initialOffset;
		@Mutable int row = initialRow;
		@Mutable int column = initialColumn;

		final int newOffset = longerString.indexOf(shorterString, offset);
		final int target = newOffset < 0 ? longerString.length() : newOffset + shorterString.length();

		while (offset < target) {
			final int codePoint = longerString.codePointAt(offset);
			if (codePoint == '\n') {
				offset = offset + 1;
				column = 1;
				row = row + 1;
			} else {
				column = column + 1;
				offset = offset + Character.toString(codePoint).length();
			}
		}

		return new int[] {offset, row, column};
	}

	/**
	 * Parses a value, then passes that value and the parsed source that produced that value into a function that
	 * produces a new type of value.
	 *
	 * @param f
	 * @param parser
	 * @param <C>
	 * @param <X>
	 * @param <T1>
	 * @param <T2>
	 * @return a function that implements parsing a value, then passes that value and the parsed source that produced
	 *         that value into a function that produces a new type of value.
	 */
	public static <C, X, T1, T2> Function<State<C>, PStep<C, X, T2>> mapChompedStringF(
		final BiFunction<String, T1, T2> f,
		final AbstractParser<C, X, T1> parser
	) {

		return s -> {
			final PStep<C, X, T1> step = parser.parse().apply(s);
			if (step instanceof Bad<C, X, T1> bad) {
				return new Bad<>(bad.progress(), bad.bag());
			} else {
				final Good<C, X, T1> good = step.asGood();
				return new Good<>(good.progress(), f.apply(s.source().substring(s.offset(), good.state().offset()), good.value()), good.state());
			}
		};
	}

	/**
	 * Parse multi line comments.
	 *
	 * @param open the {@link Token} describing the string that starts a multiline comment
	 * @param close the {@link Token} describing the string that ends a multiline comment
	 * @param nestable indicates if comments can be nested
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a {@link Function} that implements parsing of, possibly nested, multiline comments.
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Void>> multiComment(final Token<X> open, final Token<X> close, final Nestable nestable) {

		return Nestable.NOT_NESTABLE == nestable
				? ignoreF(new AbstractParser<>(tokenF(open)) { }, new AbstractParser<>(chompUntilF(close)) { })
				: nestableComment(open, close);
	}

	private static <C, X> Function<State<C>, PStep<C, X, Void>> nestableComment(final Token<X> open, final Token<X> close) {

		final String oStr = open.string();
		final X oProblem = open.expecting();
		final String cStr = close.string();
		final X cProblem = close.expecting();

		if (oStr.isEmpty()) {
			return problemF(cProblem);
		} else {
			final int openChar = oStr.codePointAt(0);
			if (cStr.isEmpty()) {
				return problemF(cProblem);
			} else {
				final int closeChar = cStr.codePointAt(0);
				final Predicate<Integer> isNotRelevant = character -> character != openChar && character != closeChar;
				final AbstractParser<C, X, Void> chompOpen = new AbstractParser<>(tokenF(open)) { };
				final AbstractParser<C, X, Void> closeToken = new AbstractParser<>(tokenF(close)) { };

				return ignoreF(chompOpen, nestableHelp(isNotRelevant, chompOpen, closeToken, cProblem, 1));
			}
		}
	}

	private static <C, X> AbstractParser<C, X, Void> nestableHelp(
		final Predicate<Integer> isNotRelevent,
		final AbstractParser<C, X, Void> open,
		final AbstractParser<C, X, Void> close,
		final X expectingClose,
		final int nestLevel
	) {
		return null;
	}

	/**
	 * This {@link Function} will keep trying parsers until {@code oneOf} them starts chomping characters. Once a path
	 * is chosen, it does not come back and try the others.
	 *
	 * @param parsers the parsers to try
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a {@link Function} that implements trying parsers until one of them starts chomping
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> oneOfF(final List<? extends AbstractParser<C, X, T>> parsers) {
		return s -> oneOfHelp(s, new Empty<>(), io.vavr.collection.List.ofAll(parsers));
	}

	private static <C, X, T> PStep<C, X, T> oneOfHelp(
		final State<C> s,
		final Bag<C, X> bag,
		final io.vavr.collection.List<? extends AbstractParser<C, X, T>> parsers
	) {
/*      // Original recursive implementation
		if(parsers.isEmpty()) {
			return new Bad<>(false, bag);
		} else {
			final PStep<C, X, T> result = parsers.head().parse().apply(s);

			if(result instanceof Good<C, X, T> good) {
				return good;
			} else {
				final Bad<C, X, T> bad = result.asBad();
				if(bad.progress()) {
					return bad;
				} else {
					return oneOfHelp(s, new Append<>(bag, bad.bag()), parsers.tail());
				}
			}
		}*/
		@Mutable io.vavr.collection.List<? extends AbstractParser<C, X, T>> remaining = parsers;

		while (!remaining.isEmpty()) {

			final PStep<C, X, T> result = remaining.head().parse().apply(s);

			if (result instanceof Good<C, X, T> good) {
				return good;
			} else {
				if (result.asBad().progress()) {
					return result.asBad();
				} else {
					remaining = remaining.tail();
				}
			}
		}

		return new Bad<>(false, bag);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a {@link Function} that implements a backtracking parser
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> backtrackableF(final AbstractParser<C, X, T> parser) {
		return s -> {
			final PStep<C, X, T> result = parser.parse().apply(s);

			if (result instanceof Bad<C, X, T> bad) {
				return new Bad<>(false, bad.bag());
			} else {
				final Good<C, X, T> good = result.asGood();
				return new Good<>(false, good.value(), good.state());
			}
		};
	}

	/**
	 * @param start a predicate that tests if a character is a valid first character of a variable
	 * @param inner a predicate that tests if a character is a valid non-first character of a variable
	 * @param reserved a set of reserved words
	 * @param expecting the error to report if parsing fails
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a function that parses variable names
	 */
	public static <C, X> Function<State<C>, PStep<C, X, String>> variableF(
		final Predicate<Integer> start,
		final Predicate<Integer> inner,
		final Set<String> reserved,
		final X expecting
	) {

		return state -> {

			final int firstOffset = ParserImpl.isSubChar(start, state.offset(), state.source());

			if (firstOffset == -1) {
				return new Bad<>(false, Bag.fromState(state, expecting));
			} else {

				final State<C> s1 = firstOffset == -2
						? varHelp(inner, state.offset() + 1, state.row() + 1, 1, state.source(), state.indent(), state.context())
						: varHelp(inner, firstOffset, state.row(), state.column() + 1, state.source(), state.indent(), state.context());

				final String name = state.source().substring(state.offset(), s1.offset());

				if (reserved.contains(name)) {
					return new Bad<>(false, Bag.fromState(state, expecting));
				} else {
					return new Good<>(true, name, s1);
				}
			}
		};
	}

	private static <C> State<C> varHelp(
		final Predicate<Integer> isGood,
		final int initialOffset,
		final int initialRow,
		final int initialColumn,
		final String source,
		final int indent,
		final List<Located<C>> context
	) {

		@Mutable int offset = initialOffset;
		@Mutable int row = initialRow;
		@Mutable int column = initialColumn;
		@Mutable int newOffset = ParserImpl.isSubChar(isGood, offset, source);

		while (newOffset != -1) {

			if (newOffset == -2) {
				offset = offset + 1;
				row = row + 1;
				column = 1;
			} else {
				offset = newOffset;
				column = column + 1;
			}
			newOffset = isSubChar(isGood, offset, source);
		}

		return new State<>(source, offset, indent, context, row, column);
	}

	/**
	 * @param decimalInteger
	 * @param hexadecimal
	 * @param octal
	 * @param binary
	 * @param floatingPoint
	 * @param invalid
	 * @param expecting
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a function that implements number parsing
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> numberF(
		final Result<X, Function<Integer, T>> decimalInteger,
		final Result<X, Function<Integer, T>> hexadecimal,
		final Result<X, Function<Integer, T>> octal,
		final Result<X, Function<Integer, T>> binary,
		final Result<X, Function<Float, T>> floatingPoint,
		final X invalid,
		final X expecting
	) {

		final int decimalBase = 10;
		final int octalBase = 8;
		final int binaryBase = 2;

		return state -> {

			if (isAsciiCode('0', state.offset(), state.source())) {

				final int zeroOffset = state.offset() + 1;
				final int baseOffset = zeroOffset + 1;

				if (isAsciiCode('x', zeroOffset, state.source())) {
					return finaliseInt(invalid, hexadecimal, baseOffset, consumeBase16(baseOffset, state.source()), state);
				} else if (isAsciiCode('o', zeroOffset, state.source())) {
					return finaliseInt(invalid, octal, baseOffset, consumeBase(octalBase, baseOffset, state.source()), state);
				} else if (isAsciiCode('b', zeroOffset, state.source())) {
					return finaliseInt(invalid, binary, baseOffset, consumeBase(binaryBase, baseOffset, state.source()), state);
				} else {
					return finaliseFloat(invalid, expecting, decimalInteger, floatingPoint, new int[]{zeroOffset, 0}, state);
				}
			} else {
				return finaliseFloat(invalid, expecting, decimalInteger, floatingPoint,
					consumeBase(decimalBase, state.offset(), state.source()), state);
			}
		};
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the current position
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Position>> getPositionF() {
		return s -> new Good<>(false, new Position(s.row(), s.column()), s);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the current row
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Integer>> getRowF() {
		return s -> new Good<>(false, s.row(), s);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the current column
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Integer>> getColumnF() {
		return s -> new Good<>(false, s.column(), s);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the current offset
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Integer>> getOffsetF() {
		return s -> new Good<>(false, s.offset(), s);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the entire source
	 */
	public static <C, X> Function<State<C>, PStep<C, X, String>> getSourceF() {
		return s -> new Good<>(false, s.source(), s);
	}

	/**
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return the current indentation
	 */
	public static <C, X> Function<State<C>, PStep<C, X, Integer>> getIndentF() {
		return s -> new Good<>(false, s.indent(), s);
	}

	/**
	 * @param newIndent
	 * @param parser
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @param <T> the value type
	 * @return a {@link Function} that implements parsing indentation
	 */
	public static <C, X, T> Function<State<C>, PStep<C, X, T>> withIndent(
		final int newIndent,
		final AbstractParser<C, X, T> parser
	) {
		return s -> {
			final PStep<C, X, T> result = parser.parse().apply(changeIndent(newIndent, s));

			if (result instanceof Good<C, X, T> good) {
				return new Good<>(good.progress(), good.value(), changeIndent(newIndent, good.state()));
			} else {
				return result.asBad();
			}
		};
	}

	private static  <C> State<C> changeIndent(final int newIndent, final State<C> state) {
		return new State<>(state.source(), state.offset(), newIndent, state.context(), state.row(), state.column());
	}

	static int[] consumeBase(final int base, final int initialOffset, final String string) {

		@Mutable int offset = initialOffset;
		@Mutable int result = 0;

		for (; offset < string.length(); offset++) {
			int digit = string.charAt(offset) - '0';
			if (digit < 0 || base <= digit) {
				break;
			}
			result = base * result + digit;
		}

		return new int[]{offset, result};
	}

	private static int[] consumeBase16(final int initialOffset, final String string) {

		final int hexadecimalBase = 16;
		@Mutable int offset = initialOffset;
		@Mutable int total = 0;

		for (; offset < string.length(); offset++) {

			final int code = string.charAt(offset);

			if ('0' <= code && code <= '9') {
				total = hexadecimalBase * total + code - '0';
			} else if ('A' <= code && code <= 'F') {
				total = hexadecimalBase * total + code - 55;
			} else if ('a' <= code && code <= 'f') {
				total = hexadecimalBase * total + code - 87;
			} else {
				break;
			}
		}

		return new int[]{offset, total};
	}

	private static <C, X, T> PStep<C, X, T> finaliseInt(
		final X invalid,
		final Result<X, Function<Integer, T>> handler,
		final int startOffset,
		final int[] tuple,
		final State<C> state
	) {

		if (handler.isErr()) {
			return new Bad<>(true, Bag.fromState(state, handler.error()));
		} else {
			final Function<Integer, T> toValue = handler.value();

			if (startOffset == tuple[0]) {
				return new Bad<>((state.offset() < startOffset), Bag.fromState(state, invalid));
			} else {
				return new Good<>(true, toValue.apply(tuple[1]), bumpOffset(tuple[0], state));
			}
		}
	}


	private static <C, X, T> PStep<C, X, T> finaliseFloat(
		final X invalid,
		final X expecting,
		final Result<X, Function<Integer, T>> intSettings,
		final Result<X, Function<Float, T>> floatSettings,
		final int[] intPair,
		final State<C> state) {

		final int intOffset = intPair[0];
		final int floatOffset = consumeDotAndExp(intOffset, state.source());

		if (floatOffset < 0) {
			return new Bad<>(true, Bag.fromInfo(state.row(),
				(state.column() - (floatOffset + state.offset())), invalid, state.context()));
		} else if (state.offset() == floatOffset) {
			return new Bad<>(false, Bag.fromState(state, expecting));
		} else if (intOffset == floatOffset) {
			return finaliseInt(invalid, intSettings, state.offset(), intPair, state);
		} else {
			if (floatSettings.isErr()) {
				return new Bad<>(true, Bag.fromState(state, invalid));
			} else {
				final Function<Float, T> toValue = floatSettings.value();

				try {
					final Float value = Float.valueOf(state.source().substring(state.offset(), floatOffset));

					return new Good<>(true, toValue.apply(value), bumpOffset(floatOffset, state));
				} catch (NumberFormatException e) {
					// TODO - Don't throw return a Bad<>
					return null;
				}
			}
		}
	}

	private static int consumeDotAndExp(final int offset, final String source) {
		if (isAsciiCode('.', offset, source)) {
			return consumeExp(chompBase10((offset + 1), source), source);
		} else {
			return consumeExp(offset, source);
		}
	}

	private static int consumeExp(final int offset, final String source) {
		if (isAsciiCode('.', offset, source) || isAsciiCode('E', offset, source)) {

			final int eOffset = offset + 1;
			final int expOffset = (isAsciiCode('+', eOffset, source) || isAsciiCode('-', eOffset, source)) ? eOffset + 1 : eOffset;
			final int newOffset = chompBase10(expOffset, source);

			if (expOffset == newOffset) {
				return -1 * newOffset;
			} else {
				return newOffset;
			}
		} else {
			return offset;
		}
	}

	private static int chompBase10(final int initialOffset, final String string) {

		@Mutable int offset = initialOffset;

		for (; offset < string.length(); offset++) {
			int code = string.codePointAt(offset);

			if (code < '0' || '9' < code) {
				return offset;
			}
		}

		return offset;
	}

	/**
	 *
	 * @param shorterString the {@link String} to look for in {@code longerString}
	 * @param offset where to start looking in {@code longerString}
	 * @param row the row
	 * @param col the column
	 * @param longerString the {@link String} to look for {@code shorterString} in
	 * @return a new offset, row and column:
	 *         <ul>
	 *             <li>if {@code shorterString} is not present in {@code longerString} then {@code {-1, row, col}}</li>
	 *             <li>if {@code shorterString} is present in {@code longerString} then a new offset, row and column</li>
	 *         </ul>
	 */
	public static int[] isSubString(
		final String shorterString,
		final int offset,
		final int row,
		final int col,
		final String longerString
	) {

		final String substring = longerString.substring(offset, offset + shorterString.length());

		if (shorterString.equals(substring)) {

			final int newOffset = offset + substring.length();
			final int newRow = row + ((int) substring.lines().count() - 1);
			final int newColumn =
				((substring.lastIndexOf('\n') == -1)
					? col + substring.length()
					: substring.length() - substring.lastIndexOf('\n'));

			return new int[]{newOffset, newRow, newColumn};
		} else {
			return new int[]{-1, row, col};
		}
	}

	/**
	 *
	 * @param predicate the test to apply to the character at {@code offset} in {@code string}.
	 * @param offset the position in the input string
	 * @param string the input string
	 * @return a value indicating the result of applying the predicate:
	 *          <ul>
	 *              <li>if the predicate is false then -1</li>
	 *              <li>if the predicate is true and the matched character is a '\n' then -2</li>
	 *              <li>otherwise the offset of the next character in the input</li>
	 *          </ul>
	 */
	public static int isSubChar(
		final Predicate<Integer> predicate,
		final int offset,
		final String string
	) {

		if (string.length() <= offset) {
			return -1;
		}

		final int codePoint = string.codePointAt(offset);

		if (predicate.test(codePoint)) {
			final String subChar = Character.toString(codePoint);
			return codePoint == '\n' ? -2 : offset + subChar.length();
		} else {
			return -1;
		}
	}

	private static boolean isAsciiCode(final int code, final int offset, final String string) {
		return offset < string.length() && string.codePointAt(offset) == code;
	}

	private static <C> State<C> bumpOffset(final int newOffset, final State<C> s) {
		return new State<>(s.source(), newOffset, s.indent(), s.context(), s.row(), s.column() + (newOffset - s.offset()));
	}
}
