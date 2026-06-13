package ceng479.mandelbrot;

import java.util.Arrays;

public final class CorrectnessVerifier {
    private CorrectnessVerifier() {
    }

    public static void verify() throws InterruptedException {
        MandelbrotConfig config = MandelbrotConfig.standard(900, 700, 500, 64);
        int cores = Runtime.getRuntime().availableProcessors();
        int[] sequential = new SequentialMandelbrotRenderer().render(config);

        int[] dynamic;
        try (ParallelMandelbrotRenderer renderer = new ParallelMandelbrotRenderer(cores)) {
            dynamic = renderer.render(config);
        }
        checkEqual("sequential", "dynamic-parallel", sequential, dynamic);

        int[] statik;
        try (StaticMandelbrotRenderer renderer = new StaticMandelbrotRenderer(cores)) {
            statik = renderer.render(config);
        }
        checkEqual("sequential", "static-parallel", sequential, statik);

        System.out.println("Correctness check passed: sequential, dynamic-parallel, and static-parallel outputs are identical.");
    }

    private static void checkEqual(String leftName, String rightName, int[] left, int[] right) {
        if (!Arrays.equals(left, right)) {
            int idx = firstMismatch(left, right);
            throw new IllegalStateException(
                    leftName + " and " + rightName + " differ at index " + idx
                            + ": " + leftName + "=" + left[idx]
                            + ", " + rightName + "=" + right[idx]);
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
