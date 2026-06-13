package ceng479.mandelbrot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public final class TileSizeSweepRunner {
    private final int width;
    private final int height;
    private final int maxIterations;
    private final int threadCount;
    private final int[] tileSizes;
    private final int repeats;
    private final int warmups;

    public TileSizeSweepRunner(
            int width,
            int height,
            int maxIterations,
            int threadCount,
            int[] tileSizes,
            int repeats,
            int warmups) {
        this.width = width;
        this.height = height;
        this.maxIterations = maxIterations;
        this.threadCount = threadCount;
        this.tileSizes = Arrays.copyOf(tileSizes, tileSizes.length);
        this.repeats = repeats;
        this.warmups = warmups;
    }

    public String run() throws IOException, InterruptedException {
        File directory = new File("results/csv");
        directory.mkdirs();

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        File output = new File(directory, "tile-sweep-" + timestamp + ".csv");

        MandelbrotConfig baselineConfig = MandelbrotConfig.standard(width, height, maxIterations, 64);
        TimingStats sequentialStats = TimingStats.measure(new SequentialMandelbrotRenderer(), baselineConfig, warmups, repeats);
        double sequentialMs = sequentialStats.getAverageTimeMs();

        System.out.printf(Locale.US,
                "Tile sweep %dx%d maxIter=%d threads=%d tileSizes=%s repeats=%d warmups=%d%n",
                width,
                height,
                maxIterations,
                threadCount,
                Arrays.toString(tileSizes),
                repeats,
                warmups);
        System.out.printf(Locale.US,
                "Sequential baseline: avg=%.3f ms, min=%.3f, max=%.3f, std=%.3f%n",
                sequentialStats.getAverageTimeMs(),
                sequentialStats.getMinTimeMs(),
                sequentialStats.getMaxTimeMs(),
                sequentialStats.getStdDevTimeMs());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write("width,height,maxIter,threadCount,tileSize,averageTimeMs,speedup,efficiency,minTimeMs,maxTimeMs,stdDevTimeMs");
            writer.newLine();

            for (int tileSize : tileSizes) {
                MandelbrotConfig config = MandelbrotConfig.standard(width, height, maxIterations, tileSize);
                TimingStats dynamicStats;
                try (ParallelMandelbrotRenderer renderer = new ParallelMandelbrotRenderer(threadCount)) {
                    dynamicStats = TimingStats.measure(renderer, config, warmups, repeats);
                }

                double speedup = sequentialMs / dynamicStats.getAverageTimeMs();
                double efficiency = speedup / threadCount;

                writer.write(String.join(",",
                        Integer.toString(width),
                        Integer.toString(height),
                        Integer.toString(maxIterations),
                        Integer.toString(threadCount),
                        Integer.toString(tileSize),
                        String.format(Locale.US, "%.3f", dynamicStats.getAverageTimeMs()),
                        String.format(Locale.US, "%.4f", speedup),
                        String.format(Locale.US, "%.4f", efficiency),
                        String.format(Locale.US, "%.3f", dynamicStats.getMinTimeMs()),
                        String.format(Locale.US, "%.3f", dynamicStats.getMaxTimeMs()),
                        String.format(Locale.US, "%.3f", dynamicStats.getStdDevTimeMs())));
                writer.newLine();

                System.out.printf(Locale.US,
                        "Dynamic tileSize=%d: avg=%.3f ms, min=%.3f, max=%.3f, std=%.3f, speedup=%.4f, efficiency=%.4f%n",
                        tileSize,
                        dynamicStats.getAverageTimeMs(),
                        dynamicStats.getMinTimeMs(),
                        dynamicStats.getMaxTimeMs(),
                        dynamicStats.getStdDevTimeMs(),
                        speedup,
                        efficiency);
            }
        }

        return output.getPath();
    }
}
