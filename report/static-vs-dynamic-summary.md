# Static vs Dynamic Parallel Benchmark Summary - Apple M3

For the teammate's Windows/Ryzen benchmark and the cross-platform comparison, see `report/windows-static-dynamic-summary.md` and `report/cross-platform-summary.md`.

## Purpose

This benchmark compares two Java thread-based parallelization strategies for Mandelbrot set generation:

- Static parallel rendering: the image is divided into fixed horizontal row bands.
- Dynamic parallel rendering: the image is divided into 64x64 pixel tiles and scheduled through a work-stealing thread pool.

The comparison supports the project proposal's claim that static partitioning can suffer from load imbalance because Mandelbrot pixels have uneven iteration costs.

## Experimental Setup

- Machine: Apple M3
- Physical CPU cores: 8
- Logical CPU cores: 8
- Memory: 16 GB
- Java version: Oracle JDK 21.0.11 LTS
- Benchmark date: 2026-06-12
- Benchmark command:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=full --threads=1,2,4,8 --repeats=3 --warmups=1 --tileSize=64
```

## 8-Thread Static vs Dynamic Summary

| Resolution | Max Iterations | Static Time (ms) | Static Speedup | Dynamic Time (ms) | Dynamic Speedup | Dynamic / Static Speedup Ratio |
|---|---:|---:|---:|---:|---:|---:|
| 1000x1000 | 250 | 39.808 | 2.6832 | 29.998 | 3.5606 | 1.33x |
| 1000x1000 | 500 | 78.665 | 2.6928 | 37.714 | 5.6167 | 2.09x |
| 1000x1000 | 1000 | 158.491 | 2.7345 | 74.749 | 5.7980 | 2.12x |
| 2000x2000 | 250 | 157.318 | 2.9562 | 75.530 | 6.1574 | 2.08x |
| 2000x2000 | 500 | 317.032 | 2.8906 | 148.424 | 6.1742 | 2.14x |
| 2000x2000 | 1000 | 631.133 | 2.8398 | 295.772 | 6.0596 | 2.13x |
| 4000x4000 | 250 | 621.845 | 2.9173 | 294.956 | 6.1505 | 2.11x |
| 4000x4000 | 500 | 1256.942 | 2.8879 | 579.776 | 6.2609 | 2.17x |
| 4000x4000 | 1000 | 2530.984 | 2.8627 | 1171.609 | 6.1843 | 2.16x |

## Key Result

The strongest benchmark case is 4000x4000 resolution with 1000 maximum iterations:

- Sequential time: 7245.545 ms
- Static parallel time with 8 threads: 2530.984 ms
- Dynamic parallel time with 8 threads: 1171.609 ms
- Static speedup: 2.8627x
- Dynamic speedup: 6.1843x

In this case, dynamic scheduling achieves about 2.16x higher speedup than static row-band partitioning.

## Interpretation

Static row-band partitioning leaves some worker threads with heavier row ranges than others because Mandelbrot computation is spatially uneven. Rows crossing dense fractal regions contain many pixels that require high iteration counts, while rows mostly outside the set finish much faster.

Dynamic tile scheduling reduces this imbalance by creating many smaller tasks. Worker threads that finish early can immediately take another tile, so CPU utilization stays higher until the full image is complete.

## Generated Files

- CSV data: `report/data/m3-static-dynamic-benchmark.csv`
- Graphs: `report/assets/graphs-static-dynamic-m3/`
- Example Mandelbrot image: `report/assets/mandelbrot-sample.png`
- Report-ready example Mandelbrot image: `report/assets/sample-mandelbrot.png`
- Report-ready dynamic scheduling diagram: `report/assets/dynamic-scheduling-architecture.png`
- Dynamic scheduling diagram: `report/assets/dynamic-task-scheduling.svg`
- Mermaid source for the diagram: `report/assets/dynamic-task-scheduling.mmd`
- Tile-size sweep summary: `report/tile-size-sweep-summary.md`
- Updated benchmark variance summary: `report/variance-benchmark-summary.md`
- Main chunk-size sweep summary: `report/chunk-size-sweep-summary.md`

Recommended figures for the final report:

- `report/assets/graphs-static-dynamic-m3/speedup-4000x4000-iter1000.png`
- `report/assets/graphs-static-dynamic-m3/efficiency-4000x4000-iter1000.png`
- `report/assets/graphs-static-dynamic-m3/execution-time-4000x4000-iter1000.png`
