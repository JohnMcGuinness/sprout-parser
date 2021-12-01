package com.github.sproutparser.common;

import java.util.List;

public record DeadEnd<C, X>(int row, int column, X problem, List<Located<C>> contextStack) { }
