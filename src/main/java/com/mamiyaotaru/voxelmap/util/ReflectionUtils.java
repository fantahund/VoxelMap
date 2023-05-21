package com.mamiyaotaru.voxelmap.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;

public final class ReflectionUtils {
    private ReflectionUtils() {}

    public static Object getPrivateFieldValueByType(Object o, Class<?> objectClasstype, Class<?> fieldClasstype) {
        return getPrivateFieldValueByType(o, objectClasstype, fieldClasstype, 0);
    }

    @Nullable
    public static Object getPrivateFieldValueByType(Object o, Class<?> objectClasstype, Class<?> fieldClasstype, int index) {
        Class<?> objectClass;
        if (o != null) {
            objectClass = o.getClass();
        } else {
            objectClass = objectClasstype;
        }

        while (!objectClass.equals(objectClasstype) && objectClass.getSuperclass() != null) {
            objectClass = objectClass.getSuperclass();
        }

        int counter = 0;
        Field[] fields = objectClass.getDeclaredFields();

        for (Field field : fields) {
            if (fieldClasstype.equals(field.getType())) {
                if (counter == index) {
                    try {
                        field.setAccessible(true);
                        return field.get(o);
                    } catch (IllegalAccessException ignored) {}
                }

                ++counter;
            }
        }

        return null;
    }

    public static ArrayList<Field> getFieldsByType(Object o, Class<?> objectClassBaseType, Class<?> fieldClasstype) {
        ArrayList<Field> matches = new ArrayList<>();

        for (Class<?> objectClass = o.getClass(); !objectClass.equals(objectClassBaseType) && objectClass.getSuperclass() != null; objectClass = objectClass.getSuperclass()) {
            Field[] fields = objectClass.getDeclaredFields();

            for (Field field : fields) {
                if (fieldClasstype.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    matches.add(field);
                }
            }
        }

        return matches;
    }

    @Nullable
    public static Field getFieldByType(Object o, Class<?> objectClasstype, Class<?> fieldClasstype, int index) {
        Class<?> objectClass = o.getClass();

        while (!objectClass.equals(objectClasstype) && objectClass.getSuperclass() != null) {
            objectClass = objectClass.getSuperclass();
        }

        int counter = 0;
        Field[] fields = objectClass.getDeclaredFields();

        for (Field field : fields) {
            if (fieldClasstype.equals(field.getType())) {
                if (counter == index) {
                    field.setAccessible(true);
                    return field;
                }

                ++counter;
            }
        }

        return null;
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException var2) {
            return false;
        }
    }
}
