package com.github.sproutparser;

public record Good<C, X, T>(boolean progress, T value, State<C> state) implements PStep<C, X, T> { }
