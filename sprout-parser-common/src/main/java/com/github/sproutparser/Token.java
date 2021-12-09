package com.github.sproutparser;

public record Token<X>(String string, X expecting) { }
