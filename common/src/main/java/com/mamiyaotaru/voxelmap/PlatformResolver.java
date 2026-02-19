package com.mamiyaotaru.voxelmap;

import java.util.EnumMap;
import java.util.function.Function;

public final class PlatformResolver {
    public enum ResolverType {
        GPU_DEVICE_TO_GL_DEVICE,
        GPU_TEXTURE_TO_GL_TEXTURE
    }

    private static final EnumMap<ResolverType, Function<Object, Object>> RESOLVERS = new EnumMap<>(ResolverType.class);
    private static boolean initialized = false;

    public static void validate() {
        for (ResolverType resolverType : ResolverType.values()) {
            if (!RESOLVERS.containsKey(resolverType)) {
                throw new RuntimeException("Resolver for " + resolverType + " was not registered!");
            }
        }

        initialized = true;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> void registerResolver(ResolverType type, Function<T, R> resolver) {
        if (initialized) {
            throw new RuntimeException("Cannot register resolver after validation!");
        }

        RESOLVERS.put(type, (Function<Object, Object>) resolver);
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R resolve(ResolverType type, T target) {
        if (!initialized) {
            throw new RuntimeException("Cannot access resolver before validation! Call validate() first.");
        }

        Function<Object, Object> resolver = RESOLVERS.get(type);
        if (resolver != null) {
            return (R) resolver.apply(target);
        }

        return null;
    }
}
