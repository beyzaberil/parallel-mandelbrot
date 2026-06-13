package ceng479.mandelbrot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class StaticMandelbrotRenderer implements MandelbrotRenderer, AutoCloseable {
    private final int threadCount;
    private final ExecutorService executor;

    public StaticMandelbrotRenderer(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be positive");
        }
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public int[] render(MandelbrotConfig config) throws InterruptedException {
        int[] output = new int[config.getWidth() * config.getHeight()];
        List<Callable<Void>> tasks = createStripTasks(config, output);
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException exception) {
                throw new IllegalStateException("Static parallel task failed", exception);
            }
        }
        return output;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private List<Callable<Void>> createStripTasks(MandelbrotConfig config, int[] output) {
        List<Callable<Void>> tasks = new ArrayList<>();
        int height = config.getHeight();
        int strips = Math.min(threadCount, height);

        for (int i = 0; i < strips; i++) {
            int startY = i * height / strips;
            int endY = (i + 1) * height / strips;
            tasks.add(() -> {
                renderStrip(config, output, startY, endY);
                return null;
            });
        }
        return tasks;
    }

    private void renderStrip(MandelbrotConfig config, int[] output, int startY, int endY) {
        int width = config.getWidth();
        for (int y = startY; y < endY; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                output[rowOffset + x] = MandelbrotMath.computePixel(config, x, y);
            }
        }
    }
}
