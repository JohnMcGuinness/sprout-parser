package com.github.sproutparser.common.internal;

import com.github.sproutparser.common.DeadEnd;

public record AddRight<C, X>(Bag<C, X> bag, DeadEnd<C, X> deadend) implements Bag<C, X> { }
