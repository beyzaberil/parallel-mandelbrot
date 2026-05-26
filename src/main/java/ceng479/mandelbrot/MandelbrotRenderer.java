package ceng479.mandelbrot;

public interface MandelbrotRenderer {
    int[] render(MandelbrotConfig config) throws InterruptedException;
}
