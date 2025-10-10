package com.mamiyaotaru.voxelmap.util;

import net.minecraft.util.Mth;

public class EasingUtils {
    private static final double num1 = 1.70158;
    private static final double num2 = num1 * 1.525;
    private static final double num3 = num1 + 1.0;

    public static float easeInSine(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - Math.cos((timeFactor * Math.PI) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutSine(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = Math.sin((timeFactor * Math.PI) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutSine(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = -(Math.cos(Math.PI * timeFactor) - 1.0) / 2.0;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInQuad(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor * timeFactor;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutQuad(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - (1.0 - timeFactor) * (1.0 - timeFactor);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutQuad(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? (2.0 * timeFactor * timeFactor) : (1.0 - Math.pow(-2.0 * timeFactor + 2.0, 2.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInCubic(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor * timeFactor * timeFactor;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutCubic(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - Math.pow(1.0 - timeFactor, 3.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutCubic(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? (4.0 * timeFactor * timeFactor * timeFactor) : (1.0 - Math.pow(-2.0 * timeFactor + 2.0, 3.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInQuart(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor * timeFactor * timeFactor * timeFactor;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutQuart(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - Math.pow(1.0 - timeFactor, 4.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutQuart(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? (8.0 * timeFactor * timeFactor * timeFactor * timeFactor) : (1.0 - Math.pow(-2.0 * timeFactor + 2.0, 4.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInQuint(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor * timeFactor * timeFactor * timeFactor * timeFactor;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutQuint(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - Math.pow(1.0 - timeFactor, 5.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutQuint(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? (16.0 * timeFactor * timeFactor * timeFactor * timeFactor * timeFactor) : (1.0 - Math.pow(-2.0 * timeFactor + 2.0, 5.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInExpo(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * timeFactor - 10.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutExpo(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor == 1.0 ? 1.0 : (1.0 - Math.pow(2.0, -10.0 * timeFactor));

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutExpo(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor == 0.0 ? 0.0 : timeFactor == 1.0 ? 1.0 : timeFactor < 0.5 ? (Math.pow(2.0, 20.0 * timeFactor - 10.0) / 2.0) : ((2.0 - Math.pow(2.0, -20.0 * timeFactor + 10.0)) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInCirc(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 - Math.sqrt(1.0 - Math.pow(timeFactor, 2.0));

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutCirc(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = Math.sqrt(1.0 - Math.pow(timeFactor - 1.0, 2.0));

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutCirc(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? ((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * timeFactor, 2.0))) / 2.0) : ((Math.sqrt(1 - Math.pow(-2.0 * timeFactor + 2.0, 2.0)) + 1.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInBack(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = num3 * timeFactor * timeFactor * timeFactor - num1 * timeFactor * timeFactor;

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeOutBack(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = 1.0 + num3 * Math.pow(timeFactor - 1.0, 3.0) + num1 * Math.pow(timeFactor - 1.0, 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

    public static float easeInOutBack(float startValue, float finalValue, float elapsedTime, float totalTime) {
        float timeFactor = elapsedTime / totalTime;
        double easeFactor = timeFactor < 0.5 ? ((Math.pow(2.0 * timeFactor, 2.0) * ((num2 + 1.0) * 2.0 * timeFactor - num2)) / 2) : ((Math.pow(2.0 * timeFactor - 2.0, 2.0) * ((num2 + 1.0) * (timeFactor * 2.0 - 2.0) + num2) + 2.0) / 2.0);

        return Mth.lerp((float) easeFactor, startValue, finalValue);
    }

}