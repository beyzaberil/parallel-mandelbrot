# Static vs Dynamic Parallel Benchmark Summary - Windows/Ryzen

## Purpose

This file summarizes the Windows benchmark results pushed by the teammate. The benchmark compares:

- Static parallel rendering: fixed horizontal row-band partitioning.
- Dynamic parallel rendering: 64x64 pixel tiles scheduled through the Java work-stealing pool.

These results complement the Apple M3 benchmark and show the behavior of the same Java implementation on a Windows/Ryzen system.

## Experimental Setup

- Machine: Windows/Ryzen teammate machine
- Exact CPU model: MISSING
- Physical CPU cores: MISSING
- Logical CPU threads: 16 thread counts were tested
- RAM: MISSING
- Windows version: MISSING
- Java version: MISSING
- Source CSV used for this summary: `report/data/windows-static-dynamic-benchmark.csv`
- Original pushed CSV: `results/csv/benchmark-20260613-121454.csv`

The CSV indicates:

- Preset: `full`
- Resolutions: 1000x1000, 2000x2000, 4000x4000
- Maximum iterations: 250, 500, 1000
- Thread counts: 1, 2, 4, 8, 16
- Tile size: 64

The CSV does not store the exact command line, repeat count, warm-up count, JVM version, or Windows hardware details. These should be confirmed with the teammate before final submission.

## 16-Thread Static vs Dynamic Summary

| Resolution | Max Iterations | Static Time (ms) | Static Speedup | Dynamic Time (ms) | Dynamic Speedup | Dynamic / Static Speedup Ratio |
|---|---:|---:|---:|---:|---:|---:|
| 1000x1000 | 250 | 56.043 | 5.3439 | 24.372 | 12.2882 | 2.30x |
| 1000x1000 | 500 | 109.714 | 5.1422 | 44.515 | 12.6739 | 2.46x |
| 1000x1000 | 1000 | 214.413 | 5.1582 | 97.222 | 11.3759 | 2.21x |
| 2000x2000 | 250 | 215.806 | 5.4199 | 96.268 | 12.1498 | 2.24x |
| 2000x2000 | 500 | 443.857 | 5.0591 | 180.159 | 12.4642 | 2.46x |
| 2000x2000 | 1000 | 887.047 | 4.8817 | 307.180 | 14.0969 | 2.89x |
| 4000x4000 | 250 | 838.914 | 5.5675 | 397.763 | 11.7423 | 2.11x |
| 4000x4000 | 500 | 1690.193 | 5.5743 | 843.657 | 11.1676 | 2.00x |
| 4000x4000 | 1000 | 3371.441 | 5.0245 | 1582.013 | 10.7077 | 2.13x |

## Key Result

The largest benchmark case is 4000x4000 resolution with 1000 maximum iterations:

- Sequential time: 16939.691 ms
- Static parallel time with 16 threads: 3371.441 ms
- Dynamic parallel time with 16 threads: 1582.013 ms
- Static speedup: 5.0245x
- Dynamic speedup: 10.7077x

For this largest case, dynamic scheduling achieves about 2.13x higher speedup than static row-band partitioning.

The best dynamic speedup in the Windows CSV is:

- Resolution: 2000x2000
- Max iterations: 1000
- Threads: 16
- Dynamic time: 307.180 ms
- Dynamic speedup: 14.0969x
- Efficiency: 0.8811

## Interpretation

The Windows/Ryzen results support the same conclusion as the Apple M3 benchmark: dynamic tile scheduling scales much better than static row-band partitioning. Static partitioning improves over the sequential baseline but plateaus because some row ranges contain heavier Mandelbrot regions than others. Dynamic scheduling creates smaller tasks and lets worker threads continue taking work, which reduces idle time and improves utilization.

## Generated Files

- CSV data: `report/data/windows-static-dynamic-benchmark.csv`
- Graphs: `report/assets/graphs-static-dynamic-windows/`

Recommended figures for the final report:

- `report/assets/graphs-static-dynamic-windows/speedup-4000x4000-iter1000.png`
- `report/assets/graphs-static-dynamic-windows/efficiency-4000x4000-iter1000.png`
- `report/assets/graphs-static-dynamic-windows/execution-time-4000x4000-iter1000.png`
