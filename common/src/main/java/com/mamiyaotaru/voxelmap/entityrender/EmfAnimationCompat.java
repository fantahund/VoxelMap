package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;

final class EmfAnimationCompat {
    static final EmfAnimationCompat INSTANCE = new EmfAnimationCompat(discoverBackend());
    private static final PauseScope NO_PAUSE = () -> { };
    private static final VertexConsumer EMPTY_VERTEX_CONSUMER = new EmptyVertexConsumer();

    private final Backend backend;
    private final AtomicBoolean warned = new AtomicBoolean();

    EmfAnimationCompat(Backend backend) {
        this.backend = backend;
    }

    CustomizationState customizationState(EntityModel<?> model) {
        if (backend == null) {
            return CustomizationState.UNAVAILABLE;
        }

        try {
            return backend.isModelCustomized(model) ? CustomizationState.CUSTOMIZED : CustomizationState.NOT_CUSTOMIZED;
        } catch (Throwable error) {
            warnOnce("querying the model customization state", error);
            return CustomizationState.UNKNOWN;
        }
    }

    AnimationState animationState(EntityModel<?> model) {
        if (backend == null) {
            return AnimationState.UNAVAILABLE;
        }

        try {
            return backend.isModelAnimated(model) ? AnimationState.ANIMATED : AnimationState.NOT_ANIMATED;
        } catch (Throwable error) {
            warnOnce("querying the model animation state", error);
            return AnimationState.UNKNOWN;
        }
    }

    boolean primeAnimations(EntityModel<?> model) {
        try {
            model.root().render(new PoseStack(), EMPTY_VERTEX_CONSUMER, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            return true;
        } catch (Throwable error) {
            warnOnce("preparing the animated radar pose", error);
            return false;
        }
    }

    PauseScope pause(Entity entity, boolean enabled) {
        if (!enabled || backend == null) {
            return NO_PAUSE;
        }

        try {
            PauseScope delegate = backend.pause(entity);
            return () -> {
                try {
                    delegate.close();
                } catch (Throwable error) {
                    warnOnce("resuming custom entity animations", error);
                }
            };
        } catch (Throwable error) {
            warnOnce("pausing custom entity animations", error);
            return NO_PAUSE;
        }
    }

    private void warnOnce(String operation, Throwable error) {
        if (warned.compareAndSet(false, true)) {
            VoxelConstants.getLogger().warn("Could not use Entity Model Features while {}; radar icons remain animated", operation, error);
        }
    }

    private static Backend discoverBackend() {
        try {
            Class<?> apiClass = Class.forName("traben.entity_model_features.EMFAnimationApi", false, EmfAnimationCompat.class.getClassLoader());
            Method emfEntityOf = apiClass.getMethod("emfEntityOf", Entity.class);
            Method isModelCustomized = apiClass.getMethod("isModelCustomizedByEMF", EntityModel.class);
            Method isModelAnimated = apiClass.getMethod("isModelAnimatedByEMF", EntityModel.class);
            Class<?> emfEntityClass = emfEntityOf.getReturnType();
            Method pause = apiClass.getMethod("pauseAllCustomAnimationsForEntity", emfEntityClass);
            Method resume = apiClass.getMethod("resumeAllCustomAnimationsForEntity", emfEntityClass);
            return new ReflectionBackend(emfEntityOf, isModelCustomized, isModelAnimated, pause, resume);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Throwable error) {
            return new BrokenBackend(error);
        }
    }

    enum AnimationState {
        ANIMATED,
        NOT_ANIMATED,
        UNAVAILABLE,
        UNKNOWN
    }

    enum CustomizationState {
        CUSTOMIZED,
        NOT_CUSTOMIZED,
        UNAVAILABLE,
        UNKNOWN
    }

    @FunctionalInterface
    interface PauseScope extends AutoCloseable {
        @Override
        void close();
    }

    interface Backend {
        boolean isModelCustomized(EntityModel<?> model) throws Throwable;

        boolean isModelAnimated(EntityModel<?> model) throws Throwable;

        PauseScope pause(Entity entity) throws Throwable;
    }

    private record ReflectionBackend(Method emfEntityOf, Method isModelCustomized, Method isModelAnimated, Method pause, Method resume) implements Backend {
        @Override
        public boolean isModelCustomized(EntityModel<?> model) throws Throwable {
            return Boolean.TRUE.equals(isModelCustomized.invoke(null, model));
        }

        @Override
        public boolean isModelAnimated(EntityModel<?> model) throws Throwable {
            return Boolean.TRUE.equals(isModelAnimated.invoke(null, model));
        }

        @Override
        public PauseScope pause(Entity entity) throws Throwable {
            Object emfEntity = emfEntityOf.invoke(null, entity);
            pause.invoke(null, emfEntity);
            return () -> {
                try {
                    resume.invoke(null, emfEntity);
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
            };
        }
    }

    private record BrokenBackend(Throwable cause) implements Backend {
        @Override
        public boolean isModelCustomized(EntityModel<?> model) {
            throw new IllegalStateException(cause);
        }

        @Override
        public boolean isModelAnimated(EntityModel<?> model) {
            throw new IllegalStateException(cause);
        }

        @Override
        public PauseScope pause(Entity entity) {
            throw new IllegalStateException(cause);
        }
    }

    private static final class EmptyVertexConsumer implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
