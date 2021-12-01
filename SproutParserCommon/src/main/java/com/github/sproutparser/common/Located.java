package com.github.sproutparser.common;

public record Located<T>(int row, int column, T context) { }
