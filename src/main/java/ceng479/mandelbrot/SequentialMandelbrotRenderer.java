package ceng479.mandelbrot;

public final class SequentialMandelbrotRenderer implements MandelbrotRenderer {
    @Override
    public int[] render(MandelbrotConfig config) {
        int[] output = new int[config.getWidth() * config.getHeight()];

        for (int y = 0; y < config.getHeight(); y++) {
            int rowOffset = y * config.getWidth();
            for (int x = 0; x < config.getWidth(); x++) {
                output[rowOffset + x] = MandelbrotMath.computePixel(config, x, y);
            }
        }

        return output;
    }
}
