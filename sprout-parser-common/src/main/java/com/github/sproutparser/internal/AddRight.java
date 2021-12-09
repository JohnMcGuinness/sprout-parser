package com.github.sproutparser.internal;

import com.github.sproutparser.DeadEnd;

public record AddRight<C, X>(Bag<C, X> bag, DeadEnd<C, X> deadend) implements Bag<C, X> { }
