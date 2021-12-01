package com.github.sproutparser.common;

import com.github.sproutparser.common.internal.Bag;

public record Bad<C, X, T>(boolean progress, Bag<C, X> bag) implements PStep<C, X, T> { }
