package com.github.sproutparser;

public record Located<T>(int row, int column, T context) { }
