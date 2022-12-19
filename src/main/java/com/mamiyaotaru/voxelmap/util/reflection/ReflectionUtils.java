package com.mamiyaotaru.voxelmap.util.reflection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;

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

    // TODO Continue from here
}