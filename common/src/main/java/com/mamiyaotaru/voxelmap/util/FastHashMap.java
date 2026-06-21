package com.mamiyaotaru.voxelmap.util;

import java.util.Arrays;

public class FastHashMap<K, V> {
    private final K[] keys;
    private final V[] values;
    private int size;

    public FastHashMap(int capacity) {
        keys = (K[]) new Object[capacity];
        values = (V[]) new Object[capacity];
    }

    public void put(K key, V value) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                values[i] = value;
                return;
            }
        }
        keys[size] = key;
        values[size] = value;
        size++;
    }

    public K getKey(int index) {
        return keys[index];
    }

    public V getValue(int index) {
        return values[index];
    }

    public V get(K key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return values[i];
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        size = 0;
    }
}
