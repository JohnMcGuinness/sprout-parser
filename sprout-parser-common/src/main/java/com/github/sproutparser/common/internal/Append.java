package com.github.sproutparser.common.internal;

public record Append<C, X>(Bag<C, X> left, Bag<C, X> right) implements Bag<C, X> { }
