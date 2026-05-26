package ceng479.mandelbrot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ParallelMandelbrotRenderer implements MandelbrotRenderer, AutoCloseable {
    private final int threadCount;
    private final ExecutorService executor;

    public ParallelMandelbrotRenderer(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be positive");
        }
        this.threadCount = threadCount;
        this.executor = Executors.newWorkStealingPool(threadCount);
    }

    @Override
    public int[] render(MandelbrotConfig config) throws InterruptedException {
        int[] output = new int[config.getWidth() * config.getHeight()];

        List<Callable<Void>> tasks = createTileTasks(config, output);
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException exception) {
                throw new IllegalStateException("Parallel Mandelbrot task failed", exception);
            }
        }

        return output;
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private List<Callable<Void>> createTileTasks(MandelbrotConfig config, int[] output) {
        List<Callable<Void>> tasks = new ArrayList<>();
        int tileSize = config.getTileSize();

        for (int startY = 0; startY < config.getHeight(); startY += tileSize) {
            int endY = Math.min(startY + tileSize, config.getHeight());
            for (int startX = 0; startX < config.getWidth(); startX += tileSize) {
                int endX = Math.min(startX + tileSize, config.getWidth());
                int tileStartX = startX;
                int tileEndX = endX;
                int tileStartY = startY;
                int tileEndY = endY;
                tasks.add(() -> {
                    renderTile(config, output, tileStartX, tileEndX, tileStartY, tileEndY);
                    return null;
                });
            }
        }

        return tasks;
    }

    private void renderTile(
            MandelbrotConfig config,
            int[] output,
            int startX,
            int endX,
            int startY,
            int endY) {
        int width = config.getWidth();
        for (int y = startY; y < endY; y++) {
            int rowOffset = y * width;
            for (int x = startX; x < endX; x++) {
                output[rowOffset + x] = MandelbrotMath.computePixel(config, x, y);
            }
        }
    }
}
