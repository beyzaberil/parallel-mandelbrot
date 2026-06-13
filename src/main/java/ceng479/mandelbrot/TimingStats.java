package ceng479.mandelbrot;

public final class TimingStats {
    private final double averageTimeMs;
    private final double minTimeMs;
    private final double maxTimeMs;
    private final double stdDevTimeMs;

    private TimingStats(double averageTimeMs, double minTimeMs, double maxTimeMs, double stdDevTimeMs) {
        this.averageTimeMs = averageTimeMs;
        this.minTimeMs = minTimeMs;
        this.maxTimeMs = maxTimeMs;
        this.stdDevTimeMs = stdDevTimeMs;
    }

    public static TimingStats measure(MandelbrotRenderer renderer, MandelbrotConfig config, int warmups, int repeats)
            throws InterruptedException {
        if (warmups < 0) {
            throw new IllegalArgumentException("warmups must be zero or positive");
        }
        if (repeats <= 0) {
            throw new IllegalArgumentException("repeats must be positive");
        }

        for (int i = 0; i < warmups; i++) {
            renderer.render(config);
        }

        double[] measurementsMs = new double[repeats];
        double totalMs = 0.0;
        double minMs = Double.MAX_VALUE;
        double maxMs = -Double.MAX_VALUE;

        for (int i = 0; i < repeats; i++) {
            long start = System.nanoTime();
            renderer.render(config);
            long end = System.nanoTime();

            double elapsedMs = (end - start) / 1_000_000.0;
            measurementsMs[i] = elapsedMs;
            totalMs += elapsedMs;
            minMs = Math.min(minMs, elapsedMs);
            maxMs = Math.max(maxMs, elapsedMs);
        }

        double averageMs = totalMs / repeats;
        double variance = 0.0;
        for (double measurementMs : measurementsMs) {
            double delta = measurementMs - averageMs;
            variance += delta * delta;
        }
        double stdDevMs = Math.sqrt(variance / repeats);

        return new TimingStats(averageMs, minMs, maxMs, stdDevMs);
    }

    public double getAverageTimeMs() {
        return averageTimeMs;
    }

    public double getMinTimeMs() {
        return minTimeMs;
    }

    public double getMaxTimeMs() {
        return maxTimeMs;
    }

    public double getStdDevTimeMs() {
        return stdDevTimeMs;
    }
}
