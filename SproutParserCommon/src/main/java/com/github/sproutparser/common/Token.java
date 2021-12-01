package com.github.sproutparser.common;

public record Token<X>(String string, X expecting) { }
