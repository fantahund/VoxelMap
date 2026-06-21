package com.mamiyaotaru.voxelmap.util;

import java.util.Arrays;

public class FastArrayList<E> {
    private final E[] values;
    private int size;

    public FastArrayList(int capacity) {
        values = (E[]) new Object[capacity];
    }

    public void add(E value) {
        values[size] = value;
        size++;
    }

    public E get(int index) {
        return values[index];
    }

    public int size() {
        return size;
    }

    public void clear() {
        Arrays.fill(values, null);
        size = 0;
    }
}
