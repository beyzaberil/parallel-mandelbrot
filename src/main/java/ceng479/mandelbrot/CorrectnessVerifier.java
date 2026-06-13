package ceng479.mandelbrot;

import java.util.Arrays;

public final class CorrectnessVerifier {
    private CorrectnessVerifier() {
    }

    public static void verify() throws InterruptedException {
        MandelbrotConfig config = MandelbrotConfig.standard(900, 700, 500, 64);
        int cores = Runtime.getRuntime().availableProcessors();
        int[] sequential = new SequentialMandelbrotRenderer().render(config);
        int[] staticParallel;
        try (StaticParallelMandelbrotRenderer renderer =
                     new StaticParallelMandelbrotRenderer(Runtime.getRuntime().availableProcessors())) {
            staticParallel = renderer.render(config);
        }
        int[] parallel;
        try (ParallelMandelbrotRenderer renderer =
                     new ParallelMandelbrotRenderer(Runtime.getRuntime().availableProcessors())) {
            parallel = renderer.render(config);
        }

        assertEqual(sequential, staticParallel, "static parallel");
        assertEqual(sequential, parallel, "dynamic parallel");

        System.out.println("Correctness check passed: sequential, static parallel, and dynamic parallel outputs are identical.");
    }

    private static void assertEqual(int[] sequential, int[] parallel, String mode) {
        if (!Arrays.equals(sequential, parallel)) {
            int mismatchIndex = firstMismatch(sequential, parallel);
            throw new IllegalStateException(
                    "Sequential and " + mode + " outputs differ at index " + mismatchIndex
                            + ": sequential=" + sequential[mismatchIndex]
                            + ", parallel=" + parallel[mismatchIndex]);
        }
    }

    private static int firstMismatch(int[] left, int[] right) {
        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return i;
            }
        }
        return -1;
    }
}
