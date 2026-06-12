# Benchmark Summary - Apple M3

This file summarizes the original sequential vs dynamic parallel benchmark. For the later static row-band vs dynamic tile-based comparison, see `report/static-vs-dynamic-summary.md`.

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

The benchmark excludes image writing from the measured render time. Each reported value is the average of three measured runs after one warm-up run.

## Benchmark Matrix

- Resolutions: 1000x1000, 2000x2000, 4000x4000
- Maximum iterations: 250, 500, 1000
- Thread counts: 1, 2, 4, 8
- Tile size: 64x64 pixels

## 8-Thread Summary

| Resolution | Max Iterations | Sequential Time (ms) | Parallel Time, 8 Threads (ms) | Speedup | Efficiency |
|---|---:|---:|---:|---:|---:|
| 1000x1000 | 250 | 132.643 | 31.796 | 4.1717 | 0.5215 |
| 1000x1000 | 500 | 211.421 | 51.484 | 4.1065 | 0.5133 |
| 1000x1000 | 1000 | 421.776 | 73.856 | 5.7108 | 0.7138 |
| 2000x2000 | 250 | 428.889 | 74.041 | 5.7926 | 0.7241 |
| 2000x2000 | 500 | 860.222 | 146.314 | 5.8793 | 0.7349 |
| 2000x2000 | 1000 | 1718.074 | 288.314 | 5.9590 | 0.7449 |
| 4000x4000 | 250 | 1740.368 | 292.362 | 5.9528 | 0.7441 |
| 4000x4000 | 500 | 3453.745 | 577.713 | 5.9783 | 0.7473 |
| 4000x4000 | 1000 | 6923.048 | 1138.574 | 6.0805 | 0.7601 |

## Key Observations

- The parallel renderer consistently improves execution time as the number of threads increases.
- Larger workloads produce more stable speedup because task scheduling overhead becomes small compared with pixel computation time.
- The strongest measured result is for 4000x4000 resolution with 1000 maximum iterations: 6923.048 ms sequential time versus 1138.574 ms parallel time with 8 threads, giving a 6.0805x speedup.
- Efficiency at 8 threads reaches 0.7601 in the largest benchmark case, which is reasonable for a Java thread-based implementation with dynamic tile scheduling.
- Small workloads show more variation because thread pool scheduling overhead and JVM behavior are more visible when the total computation time is short.

## Generated Files

- CSV data: `report/data/m3-full-benchmark.csv`
- Graphs: `report/assets/graphs/`

Recommended figures for the final report:

- `report/assets/graphs/execution-time-4000x4000-iter1000.png`
- `report/assets/graphs/speedup-4000x4000-iter1000.png`
- `report/assets/graphs/efficiency-4000x4000-iter1000.png`
