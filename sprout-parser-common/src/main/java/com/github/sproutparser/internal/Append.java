package com.github.sproutparser.internal;

public record Append<C, X>(Bag<C, X> left, Bag<C, X> right) implements Bag<C, X> { }
