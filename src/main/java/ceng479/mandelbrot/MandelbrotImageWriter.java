package ceng479.mandelbrot;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class MandelbrotImageWriter {
    private MandelbrotImageWriter() {
    }

    public static void writePng(MandelbrotConfig config, int[] iterations, String outputPath) throws IOException {
        BufferedImage image = toBufferedImage(config, iterations);

        File output = new File(outputPath);
        File parent = output.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", output);
    }

    public static BufferedImage toBufferedImage(MandelbrotConfig config, int[] iterations) {
        BufferedImage image = new BufferedImage(config.getWidth(), config.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < config.getHeight(); y++) {
            int rowOffset = y * config.getWidth();
            for (int x = 0; x < config.getWidth(); x++) {
                int iteration = iterations[rowOffset + x];
                image.setRGB(x, y, colorFor(iteration, config.getMaxIterations()));
            }
        }

        return image;
    }

    private static int colorFor(int iteration, int maxIterations) {
        if (iteration >= maxIterations) {
            return Color.BLACK.getRGB();
        }

        float hue = 0.68f + (0.32f * iteration / maxIterations);
        float saturation = 0.85f;
        float brightness = iteration < maxIterations ? 1.0f : 0.0f;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }
}
