package ceng479.mandelbrot;

public final class MandelbrotConfig {
    private final int width;
    private final int height;
    private final int maxIterations;
    private final int tileSize;
    private final double minReal;
    private final double maxReal;
    private final double minImaginary;
    private final double maxImaginary;

    public MandelbrotConfig(
            int width,
            int height,
            int maxIterations,
            int tileSize,
            double minReal,
            double maxReal,
            double minImaginary,
            double maxImaginary) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive");
        }
        if (tileSize <= 0) {
            throw new IllegalArgumentException("tileSize must be positive");
        }
        if (minReal >= maxReal) {
            throw new IllegalArgumentException("minReal must be smaller than maxReal");
        }
        if (minImaginary >= maxImaginary) {
            throw new IllegalArgumentException("minImaginary must be smaller than maxImaginary");
        }

        this.width = width;
        this.height = height;
        this.maxIterations = maxIterations;
        this.tileSize = tileSize;
        this.minReal = minReal;
        this.maxReal = maxReal;
        this.minImaginary = minImaginary;
        this.maxImaginary = maxImaginary;
    }

    public static MandelbrotConfig standard(int width, int height, int maxIterations, int tileSize) {
        return new MandelbrotConfig(width, height, maxIterations, tileSize, -2.0, 1.0, -1.5, 1.5);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public int getTileSize() {
        return tileSize;
    }

    public double getMinReal() {
        return minReal;
    }

    public double getMaxReal() {
        return maxReal;
    }

    public double getMinImaginary() {
        return minImaginary;
    }

    public double getMaxImaginary() {
        return maxImaginary;
    }
}
