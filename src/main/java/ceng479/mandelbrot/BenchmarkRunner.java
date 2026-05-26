package ceng479.mandelbrot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BenchmarkRunner {
    private final String preset;
    private final int[] threadCounts;
    private final int repeats;
    private final int warmups;
    private final int tileSize;

    public BenchmarkRunner(String preset, int[] threadCounts, int repeats, int warmups, int tileSize) {
        this.preset = preset;
        this.threadCounts = Arrays.copyOf(threadCounts, threadCounts.length);
        this.repeats = repeats;
        this.warmups = warmups;
        this.tileSize = tileSize;
    }

    public String run() throws IOException, InterruptedException {
        List<BenchmarkCase> cases = benchmarkCases(preset);
        List<BenchmarkResult> results = new ArrayList<>();

        System.out.printf("Running %s benchmark with threads=%s, repeats=%d, warmups=%d, tileSize=%d%n",
                preset, Arrays.toString(threadCounts), repeats, warmups, tileSize);

        for (BenchmarkCase benchmarkCase : cases) {
            MandelbrotConfig config = MandelbrotConfig.standard(
                    benchmarkCase.width,
                    benchmarkCase.height,
                    benchmarkCase.maxIterations,
                    tileSize);

            double sequentialMs = averageRenderTimeMs(new SequentialMandelbrotRenderer(), config);
            results.add(new BenchmarkResult(
                    preset,
                    "sequential",
                    config.getWidth(),
                    config.getHeight(),
                    config.getMaxIterations(),
                    1,
                    0,
                    sequentialMs,
                    1.0,
                    1.0));

            System.out.printf(Locale.US,
                    "Sequential %dx%d maxIter=%d: %.3f ms%n",
                    config.getWidth(),
                    config.getHeight(),
                    config.getMaxIterations(),
                    sequentialMs);

            for (int threadCount : threadCounts) {
                double parallelMs;
                try (ParallelMandelbrotRenderer renderer = new ParallelMandelbrotRenderer(threadCount)) {
                    parallelMs = averageRenderTimeMs(renderer, config);
                }
                double speedup = sequentialMs / parallelMs;
                double efficiency = speedup / threadCount;

                results.add(new BenchmarkResult(
                        preset,
                        "parallel",
                        config.getWidth(),
                        config.getHeight(),
                        config.getMaxIterations(),
                        threadCount,
                        tileSize,
                        parallelMs,
                        speedup,
                        efficiency));

                System.out.printf(Locale.US,
                        "Parallel   %dx%d maxIter=%d threads=%d: %.3f ms, speedup=%.4f, efficiency=%.4f%n",
                        config.getWidth(),
                        config.getHeight(),
                        config.getMaxIterations(),
                        threadCount,
                        parallelMs,
                        speedup,
                        efficiency);
            }
        }

        return writeCsv(results);
    }

    private double averageRenderTimeMs(MandelbrotRenderer renderer, MandelbrotConfig config)
            throws InterruptedException {
        for (int i = 0; i < warmups; i++) {
            renderer.render(config);
        }

        long totalNs = 0L;
        for (int i = 0; i < repeats; i++) {
            long start = System.nanoTime();
            renderer.render(config);
            long end = System.nanoTime();
            totalNs += end - start;
        }

        return totalNs / 1_000_000.0 / repeats;
    }

    private String writeCsv(List<BenchmarkResult> results) throws IOException {
        File directory = new File("results/csv");
        directory.mkdirs();

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
        File output = new File(directory, "benchmark-" + timestamp + ".csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write(BenchmarkResult.csvHeader());
            writer.newLine();
            for (BenchmarkResult result : results) {
                writer.write(result.toCsvRow());
                writer.newLine();
            }
        }

        return output.getPath();
    }

    private static List<BenchmarkCase> benchmarkCases(String preset) {
        if ("quick".equalsIgnoreCase(preset)) {
            return Arrays.asList(
                    new BenchmarkCase(800, 800, 250),
                    new BenchmarkCase(1200, 1200, 500));
        }

        if ("full".equalsIgnoreCase(preset)) {
            return Arrays.asList(
                    new BenchmarkCase(1000, 1000, 250),
                    new BenchmarkCase(1000, 1000, 500),
                    new BenchmarkCase(1000, 1000, 1000),
                    new BenchmarkCase(2000, 2000, 250),
                    new BenchmarkCase(2000, 2000, 500),
                    new BenchmarkCase(2000, 2000, 1000),
                    new BenchmarkCase(4000, 4000, 250),
                    new BenchmarkCase(4000, 4000, 500),
                    new BenchmarkCase(4000, 4000, 1000));
        }

        throw new IllegalArgumentException("Unknown preset: " + preset + ". Use quick or full.");
    }

    private static final class BenchmarkCase {
        private final int width;
        private final int height;
        private final int maxIterations;

        private BenchmarkCase(int width, int height, int maxIterations) {
            this.width = width;
            this.height = height;
            this.maxIterations = maxIterations;
        }
    }
}
