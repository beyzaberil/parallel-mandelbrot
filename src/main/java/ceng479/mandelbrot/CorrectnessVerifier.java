package ceng479.mandelbrot;

import java.util.Arrays;

public final class CorrectnessVerifier {
    private CorrectnessVerifier() {
    }

    public static void verify() throws InterruptedException {
        MandelbrotConfig config = MandelbrotConfig.standard(900, 700, 500, 64);
        int[] sequential = new SequentialMandelbrotRenderer().render(config);
        int[] parallel;
        try (ParallelMandelbrotRenderer renderer =
                     new ParallelMandelbrotRenderer(Runtime.getRuntime().availableProcessors())) {
            parallel = renderer.render(config);
        }

        if (!Arrays.equals(sequential, parallel)) {
            int mismatchIndex = firstMismatch(sequential, parallel);
            throw new IllegalStateException(
                    "Sequential and parallel outputs differ at index " + mismatchIndex
                            + ": sequential=" + sequential[mismatchIndex]
                            + ", parallel=" + parallel[mismatchIndex]);
        }

        System.out.println("Correctness check passed: sequential and parallel outputs are identical.");
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
