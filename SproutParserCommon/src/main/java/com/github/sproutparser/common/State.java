package com.github.sproutparser.common;

import java.util.List;

public record State<T>(String source, int offset, int indent, List<Located<T>> context, int row, int column) { }
