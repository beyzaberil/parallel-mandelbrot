package ceng479.mandelbrot;

import java.io.IOException;
import java.util.Arrays;

public final class MandelbrotApp {
    private MandelbrotApp() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        if (args.length == 0 || "help".equalsIgnoreCase(args[0]) || "--help".equalsIgnoreCase(args[0])) {
            printHelp();
            return;
        }

        switch (args[0]) {
            case "verify":
                CorrectnessVerifier.verify();
                break;
            case "render":
                render(CliOptions.parse(args, 1));
                break;
            case "benchmark":
                benchmark(CliOptions.parse(args, 1));
                break;
            case "graphs":
                graphs(CliOptions.parse(args, 1));
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void render(CliOptions options) throws IOException, InterruptedException {
        String mode = options.getString("mode", "parallel");
        int width = options.getInt("width", 1920);
        int height = options.getInt("height", 1080);
        int maxIterations = options.getInt("maxIter", 1000);
        int tileSize = options.getInt("tileSize", 64);
        int threads = options.getInt("threads", Runtime.getRuntime().availableProcessors());
        String output = options.getString("output", "results/images/mandelbrot.png");

        MandelbrotConfig config = MandelbrotConfig.standard(width, height, maxIterations, tileSize);
        MandelbrotRenderer renderer = rendererFor(mode, threads);

        int[] iterations;
        long start;
        long end;
        try {
            start = System.nanoTime();
            iterations = renderer.render(config);
            end = System.nanoTime();
        } finally {
            if (renderer instanceof ParallelMandelbrotRenderer) {
                ((ParallelMandelbrotRenderer) renderer).close();
            }
        }

        MandelbrotImageWriter.writePng(config, iterations, output);

        double renderMs = (end - start) / 1_000_000.0;
        System.out.printf("Rendered %s in %.3f ms. Image written to %s%n", mode, renderMs, output);
    }

    private static void benchmark(CliOptions options) throws IOException, InterruptedException {
        String preset = options.getString("preset", "quick");
        int[] defaultThreads = defaultThreadCounts();
        int[] threads = options.getIntList("threads", defaultThreads);
        int repeats = options.getInt("repeats", 3);
        int warmups = options.getInt("warmups", 1);
        int tileSize = options.getInt("tileSize", 64);

        BenchmarkRunner runner = new BenchmarkRunner(preset, threads, repeats, warmups, tileSize);
        String csvPath = runner.run();
        System.out.println("Benchmark CSV written to " + csvPath);
    }

    private static void graphs(CliOptions options) throws IOException {
        String input = options.getString("input", "latest");
        String outputDirectory = options.getString("outputDir", "results/graphs");

        String resolvedInput = "latest".equalsIgnoreCase(input)
                ? BenchmarkGraphGenerator.latestCsvPath()
                : input;
        System.out.println("Generating graphs from " + resolvedInput);

        for (String output : BenchmarkGraphGenerator.generate(input, outputDirectory)) {
            System.out.println("Graph written to " + output);
        }
    }

    private static MandelbrotRenderer rendererFor(String mode, int threads) {
        if ("sequential".equalsIgnoreCase(mode)) {
            return new SequentialMandelbrotRenderer();
        }
        if ("parallel".equalsIgnoreCase(mode)) {
            return new ParallelMandelbrotRenderer(threads);
        }
        throw new IllegalArgumentException("Unknown render mode: " + mode);
    }

    private static int[] defaultThreadCounts() {
        int max = Runtime.getRuntime().availableProcessors();
        int[] candidates = {1, 2, 4, 8, 16, 32};
        return Arrays.stream(candidates)
                .filter(value -> value <= max)
                .toArray();
    }

    private static void printHelp() {
        System.out.println("CENG-479 Mandelbrot Parallel Benchmark");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  verify");
        System.out.println("  render --mode=parallel --width=1920 --height=1080 --maxIter=1000 --threads=8 --tileSize=64 --output=results/images/parallel.png");
        System.out.println("  benchmark --preset=quick --threads=1,2,4,8 --repeats=3 --warmups=1 --tileSize=64");
        System.out.println("  graphs --input=latest --outputDir=results/graphs");
    }
}
