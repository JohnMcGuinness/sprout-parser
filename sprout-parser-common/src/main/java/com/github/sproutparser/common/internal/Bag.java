package com.github.sproutparser.common.internal;

import com.github.sproutparser.common.DeadEnd;
import com.github.sproutparser.common.Located;
import com.github.sproutparser.common.State;

import java.util.List;

public sealed interface Bag<C, X> permits Empty, AddRight, Append {

	/**
	 * Create a {@link Bag} from a {@link State} and a problem.
	 *
	 * @param state
	 * @param x the problem to use to create a {@link Bag}.
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a new {@link Bag} instance.
	 */
	static <C, X> Bag<C, X> fromState(final State<C> state, X x) {
		return new AddRight<>(new Empty<>(), new DeadEnd<>(state.row(), state.column(), x, state.context()));
	}

	/**
	 * Create a {@link Bag} from a {@code row}, {@code column}, a problem and a {@code context}.
	 * @param row the row
	 * @param col the column
	 * @param x the problem
	 * @param context the context
	 * @param <C> the context type
	 * @param <X> the problem type
	 * @return a new {@link Bag} instance.
	 */
	static <C, X> Bag<C, X> fromInfo(final int row, final int col, final X x, final List<Located<C>> context) {
		return new AddRight<>(new Empty<>(), new DeadEnd<>(row, col, x, context));
	}

	/**
	 * Flattens the Bag tree into a list of problems.
	 *
	 * @param bag the bag to flatten.
	 * @param list the list to prepend
	 * @param <C> context type
	 * @param <X> problem type
	 * @return a list of problems
	 */
	static <C, X> io.vavr.collection.List<DeadEnd<C, X>> bagToList(final Bag<C, X> bag, final io.vavr.collection.List<DeadEnd<C, X>> list) {

		if (bag instanceof Empty) {
			return list;
		} else if (bag instanceof AddRight<C, X> addRight) {
			return bagToList(addRight.bag(), list.prepend(addRight.deadend()));
		} else {
			final Append<C, X> append = (Append<C, X>) bag;
			return bagToList(append.left(), bagToList(append.right(), list));
		}
	}
}
