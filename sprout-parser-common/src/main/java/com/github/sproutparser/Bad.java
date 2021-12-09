package com.github.sproutparser;

import com.github.sproutparser.internal.Bag;

public record Bad<C, X, T>(boolean progress, Bag<C, X> bag) implements PStep<C, X, T> { }
