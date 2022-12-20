package com.mamiyaotaru.voxelmap.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;

public class ReflectionUtils {
    public static <T> Optional<T> getPrivateFieldValueByType(T object, Class<T> objectClassType, Class<T> fieldClassType) { return getPrivateFieldValueByType(object, objectClassType, fieldClassType, 0); }

    public static <T> Optional<T> getPrivateFieldValueByType(T object, Class<T> objectClassType, Class<T> fieldClassType, int index) {
        Class<?> objectClass = object != null ? object.getClass() : objectClassType;

        while (!(objectClass.equals(objectClassType)) && objectClass.getSuperclass() != null) objectClass = objectClass.getSuperclass();

        int counter = 0;

        for (Field field : objectClassType.getDeclaredFields()) {
            if (fieldClassType.equals(field.getType())) {
                if (counter == index) {
                    try {
                        field.setAccessible(true);

                        //noinspection unchecked
                        return Optional.of((T) field.get(object));
                    } catch (IllegalAccessException ignored) {}
                }

                counter++;
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> getFieldValueByName(T object, String fieldName) {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                try {
                    field.setAccessible(true);

                    //noinspection unchecked
                    return Optional.of((T) field.get(object));
                } catch (IllegalAccessException ignored) {}
            }
        }

        return Optional.empty();
    }

    public static <T> ArrayList<Field> getFieldsByType(T object, Class<T> objectClassBaseType, Class<T> fieldClassType) {
        ArrayList<Field> matches = new ArrayList<>();

        for (Class<?> objectClass = object.getClass(); (!(objectClass.equals(objectClassBaseType))) && objectClass.getSuperclass() != null; objectClass = objectClass.getSuperclass()) {
            for (Field field : objectClass.getDeclaredFields()) {
                if (fieldClassType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    matches.add(field);
                }
            }
        }

        return matches;
    }

    public static <T> Optional<Field> getFieldByType(T object, Class<T> objectClassType, Class<T> fieldClassType) { return getFieldByType(object, objectClassType, fieldClassType, 0); }

    public static <T> Optional<Field> getFieldByType(T object, Class<T> objectClassType, Class<T> fieldClassType, int index) {
        Class<?> objectClass = object.getClass();

        while (!(objectClass.equals(objectClassType)) && objectClass.getSuperclass() != null) objectClass = objectClass.getSuperclass();

        int counter = 0;

        for (Field field : objectClass.getDeclaredFields()) {
            if (fieldClassType.equals(field.getType())) {
                if (counter == index) {
                    field.setAccessible(true);

                    return Optional.of(field);
                }

                counter++;
            }
        }

        return Optional.empty();
    }

    @SafeVarargs
    public static <T> Optional<Method> getMethodByType(Class<T> objectType, Class<T> returnType, Class<T>... parameterTypes) { return getMethodByType(0, objectType, returnType, parameterTypes); }

    @SafeVarargs
    public static <T> Optional<Method> getMethodByType(int index, Class<T> objectType, Class<T> returnType, Class<T>... parameterTypes) {
        int counter = 0;

        for (Method method : objectType.getDeclaredMethods()) {
            if (returnType.equals(method.getReturnType())) {
                Class<?>[] methodParameterTypes = method.getParameterTypes();

                if (parameterTypes.length == methodParameterTypes.length) {
                    boolean match = IntStream.range(0, parameterTypes.length).noneMatch(type -> parameterTypes[type] != methodParameterTypes[type]);

                    if (counter == index && match) {
                        method.setAccessible(true);

                        return Optional.of(method);
                    }
                }

                counter++;
            }
        }

        return Optional.empty();
    }

    public static boolean classExists(String name) {
        try {
            Class.forName(name);

            return true;
        } catch (ClassNotFoundException ignored) {}

        return false;
    }
}