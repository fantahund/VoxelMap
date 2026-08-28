package com.mamiyaotaru.voxelmap.entityrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

class EmfAnimationCompatTest {
    @Test
    void reportsUnavailableWhenEmfIsAbsent() {
        EmfAnimationCompat compat = new EmfAnimationCompat(null);

        assertEquals(EmfAnimationCompat.AnimationState.UNAVAILABLE, compat.animationState(testModel()));
        assertEquals(EmfAnimationCompat.CustomizationState.UNAVAILABLE, compat.customizationState(testModel()));
    }

    @Test
    void reflectionFailureFallsBackToUnknown() {
        EmfAnimationCompat compat = new EmfAnimationCompat(new EmfAnimationCompat.Backend() {
            @Override
            public boolean isModelCustomized(EntityModel<?> model) {
                throw new IllegalStateException("incompatible API");
            }

            @Override
            public boolean isModelAnimated(EntityModel<?> model) {
                throw new IllegalStateException("incompatible API");
            }

            @Override
            public EmfAnimationCompat.PauseScope pause(Entity entity) {
                throw new IllegalStateException("incompatible API");
            }
        });

        assertEquals(EmfAnimationCompat.AnimationState.UNKNOWN, compat.animationState(testModel()));
        assertEquals(EmfAnimationCompat.CustomizationState.UNKNOWN, compat.customizationState(testModel()));
    }

    @Test
    void distinguishesLazyCustomizedModelFromNotAnimatedModel() {
        EmfAnimationCompat compat = new EmfAnimationCompat(new EmfAnimationCompat.Backend() {
            @Override
            public boolean isModelCustomized(EntityModel<?> model) {
                return true;
            }

            @Override
            public boolean isModelAnimated(EntityModel<?> model) {
                return false;
            }

            @Override
            public EmfAnimationCompat.PauseScope pause(Entity entity) {
                return () -> { };
            }
        });

        assertEquals(EmfAnimationCompat.CustomizationState.CUSTOMIZED, compat.customizationState(testModel()));
        assertEquals(EmfAnimationCompat.AnimationState.NOT_ANIMATED, compat.animationState(testModel()));
    }

    @Test
    void resumesAnimationsInFinally() {
        AtomicBoolean paused = new AtomicBoolean();
        AtomicBoolean resumed = new AtomicBoolean();
        EmfAnimationCompat compat = new EmfAnimationCompat(new EmfAnimationCompat.Backend() {
            @Override
            public boolean isModelCustomized(EntityModel<?> model) {
                return true;
            }

            @Override
            public boolean isModelAnimated(EntityModel<?> model) {
                return true;
            }

            @Override
            public EmfAnimationCompat.PauseScope pause(Entity entity) {
                paused.set(true);
                return () -> resumed.set(true);
            }
        });

        assertThrows(IllegalStateException.class, () -> {
            try (EmfAnimationCompat.PauseScope ignored = compat.pause(null, true)) {
                throw new IllegalStateException("render failed");
            }
        });

        assertTrue(paused.get());
        assertTrue(resumed.get());
    }

    @Test
    void canPrimeAPlainModelWithoutEmf() {
        EmfAnimationCompat compat = new EmfAnimationCompat(null);

        assertTrue(compat.primeAnimations(testModel()));
    }

    private static EntityModel<EntityRenderState> testModel() {
        return new TestModel(new ModelPart(List.of(), Map.of()));
    }

    private static final class TestModel extends EntityModel<EntityRenderState> {
        private TestModel(ModelPart root) {
            super(root);
        }
    }
}
