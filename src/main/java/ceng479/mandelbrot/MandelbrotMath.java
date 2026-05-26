package ceng479.mandelbrot;

public final class MandelbrotMath {
    private MandelbrotMath() {
    }

    public static int computePixel(MandelbrotConfig config, int x, int y) {
        double real = mapCoordinate(x, config.getWidth(), config.getMinReal(), config.getMaxReal());
        double imaginary = mapCoordinate(y, config.getHeight(), config.getMaxImaginary(), config.getMinImaginary());

        double zReal = 0.0;
        double zImaginary = 0.0;
        int iteration = 0;

        while (zReal * zReal + zImaginary * zImaginary <= 4.0
                && iteration < config.getMaxIterations()) {
            double nextReal = zReal * zReal - zImaginary * zImaginary + real;
            double nextImaginary = 2.0 * zReal * zImaginary + imaginary;
            zReal = nextReal;
            zImaginary = nextImaginary;
            iteration++;
        }

        return iteration;
    }

    private static double mapCoordinate(int value, int size, double min, double max) {
        if (size == 1) {
            return (min + max) / 2.0;
        }
        return min + (value * (max - min)) / (size - 1.0);
    }
}
