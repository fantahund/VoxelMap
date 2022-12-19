package com.mamiyaotaru.voxelmap.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.stream.IntStream;

@Deprecated
public class ReflectionUtils {
    @Deprecated
    public static Object getPrivateFieldValueByType(Object o, Class<?> objectClasstype, Class<?> fieldClasstype) {
        return getPrivateFieldValueByType(o, objectClasstype, fieldClasstype, 0);
    }

    @Deprecated
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

    @Deprecated
    public static Object getFieldValueByName(Object o, String fieldName) {
        Field[] fields = o.getClass().getFields();

        for (Field field : fields) {
            if (fieldName.equals(field.getName())) {
                try {
                    field.setAccessible(true);
                    return field.get(o);
                } catch (IllegalAccessException ignored) {}
            }
        }

        return null;
    }

    @Deprecated
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

    // TODO Continue from here

    @Deprecated
    public static Field getFieldByType(Object o, Class<?> objectClasstype, Class<?> fieldClasstype) {
        return getFieldByType(o, objectClasstype, fieldClasstype, 0);
    }

    @Deprecated
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

    @Deprecated
    public static Method getMethodByType(Class<?> objectType, Class<?> returnType, Class<?>... parameterTypes) {
        return getMethodByType(0, objectType, returnType, parameterTypes);
    }

    @Deprecated
    public static Method getMethodByType(int index, Class<?> objectType, Class<?> returnType, Class<?>... parameterTypes) {
        Method[] methods = objectType.getDeclaredMethods();
        int counter = 0;

        for (Method method : methods) {
            if (returnType.equals(method.getReturnType())) {
                Class<?>[] methodParameterTypes = method.getParameterTypes();
                if (parameterTypes.length == methodParameterTypes.length) {
                    boolean match = IntStream.range(0, parameterTypes.length).noneMatch(t -> parameterTypes[t] != methodParameterTypes[t]);

                    if (counter == index && match) {
                        method.setAccessible(true);
                        return method;
                    }
                }

                ++counter;
            }
        }

        return null;
    }

    @Deprecated
    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException var2) {
            return false;
        }
    }
}
