# Tile-Size Sweep Summary - Apple M3

## Purpose

This experiment addresses the chunk-size sensitivity question for the dynamic tile-based Mandelbrot renderer. The renderer was tested with several tile sizes while keeping the image size, maximum iteration count, and thread count fixed.

## Experimental Setup

- Machine: Apple M3
- Resolution: 2000x2000
- Maximum iterations: 1000
- Thread count: 8
- Dynamic renderer only
- Warm-up runs: 1
- Measured repeats: 3
- Timing columns: average, minimum, maximum, and standard deviation
- CSV data: `report/data/m3-tile-size-sweep.csv`

Command:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp tile-sweep --width=2000 --height=2000 --maxIter=1000 --threads=8 --tileSizes=16,32,64,128,256 --repeats=3 --warmups=1
```

## Results

| Tile Size | Average Time (ms) | Min Time (ms) | Max Time (ms) | Std Dev (ms) | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|---:|
| 16 | 308.714 | 301.027 | 321.832 | 9.321 | 5.5471 | 0.6934 |
| 32 | 294.637 | 291.730 | 296.403 | 2.072 | 5.8121 | 0.7265 |
| 64 | 309.164 | 297.636 | 325.132 | 11.656 | 5.5390 | 0.6924 |
| 128 | 319.027 | 302.215 | 345.047 | 18.659 | 5.3678 | 0.6710 |
| 256 | 362.770 | 355.614 | 366.627 | 5.065 | 4.7205 | 0.5901 |

## Interpretation

In this experiment, 32x32 tiles gave the best average time and the lowest standard deviation. Very large tiles, especially 256x256, reduced scheduling flexibility and produced slower execution. Very small tiles also introduced additional scheduling overhead. The result supports using moderately sized tiles for dynamic Mandelbrot rendering.

Recommended figure:

- `report/assets/tile-size-sweep-2000x2000-iter1000.svg`
