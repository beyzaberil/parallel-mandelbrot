package ceng479.mandelbrot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class StaticParallelMandelbrotRenderer implements MandelbrotRenderer, AutoCloseable {
    private final int threadCount;
    private final ExecutorService executor;

    public StaticParallelMandelbrotRenderer(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be positive");
        }
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public int[] render(MandelbrotConfig config) throws InterruptedException {
        int[] output = new int[config.getWidth() * config.getHeight()];
        List<Callable<Void>> tasks = createRowBandTasks(config, output);
        List<Future<Void>> futures = executor.invokeAll(tasks);

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException exception) {
                throw new IllegalStateException("Static parallel Mandelbrot task failed", exception);
            }
        }

        return output;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private List<Callable<Void>> createRowBandTasks(MandelbrotConfig config, int[] output) {
        List<Callable<Void>> tasks = new ArrayList<>();
        int rowsPerThread = (int) Math.ceil(config.getHeight() / (double) threadCount);

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            int startY = threadIndex * rowsPerThread;
            int endY = Math.min(startY + rowsPerThread, config.getHeight());
            if (startY >= endY) {
                continue;
            }

            tasks.add(() -> {
                renderRows(config, output, startY, endY);
                return null;
            });
        }

        return tasks;
    }

    private void renderRows(MandelbrotConfig config, int[] output, int startY, int endY) {
        int width = config.getWidth();
        for (int y = startY; y < endY; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                output[rowOffset + x] = MandelbrotMath.computePixel(config, x, y);
            }
        }
    }
}
