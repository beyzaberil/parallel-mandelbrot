package ceng479.mandelbrot;

public final class BenchmarkResult {
    private final String preset;
    private final String mode;
    private final int width;
    private final int height;
    private final int maxIterations;
    private final int threadCount;
    private final int tileSize;
    private final double averageTimeMs;
    private final double speedup;
    private final double efficiency;
    private final double minTimeMs;
    private final double maxTimeMs;
    private final double stdDevTimeMs;

    public BenchmarkResult(
            String preset,
            String mode,
            int width,
            int height,
            int maxIterations,
            int threadCount,
            int tileSize,
            double averageTimeMs,
            double speedup,
            double efficiency,
            double minTimeMs,
            double maxTimeMs,
            double stdDevTimeMs) {
        this.preset = preset;
        this.mode = mode;
        this.width = width;
        this.height = height;
        this.maxIterations = maxIterations;
        this.threadCount = threadCount;
        this.tileSize = tileSize;
        this.averageTimeMs = averageTimeMs;
        this.speedup = speedup;
        this.efficiency = efficiency;
        this.minTimeMs = minTimeMs;
        this.maxTimeMs = maxTimeMs;
        this.stdDevTimeMs = stdDevTimeMs;
    }

    public String toCsvRow() {
        return String.join(",",
                preset,
                mode,
                Integer.toString(width),
                Integer.toString(height),
                Integer.toString(maxIterations),
                Integer.toString(threadCount),
                Integer.toString(tileSize),
                String.format(java.util.Locale.US, "%.3f", averageTimeMs),
                String.format(java.util.Locale.US, "%.4f", speedup),
                String.format(java.util.Locale.US, "%.4f", efficiency),
                String.format(java.util.Locale.US, "%.3f", minTimeMs),
                String.format(java.util.Locale.US, "%.3f", maxTimeMs),
                String.format(java.util.Locale.US, "%.3f", stdDevTimeMs));
    }

    public static String csvHeader() {
        return "preset,mode,width,height,maxIter,threadCount,tileSize,averageTimeMs,speedup,efficiency,minTimeMs,maxTimeMs,stdDevTimeMs";
    }
}
