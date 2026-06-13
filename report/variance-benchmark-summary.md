# Benchmark Variance Summary

This summary documents the updated benchmark run that records average, minimum, maximum, and standard deviation values for each measured case.

## Command

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=full --threads=1,2,4,8 --repeats=3 --warmups=1 --tileSize=64
```

## Methodology

- Preset: `full`
- Thread counts: `1, 2, 4, 8`
- Tile size: `64x64`
- Repeats: `3`
- Warm-ups: `1`
- Timing method: `System.nanoTime()`
- Measured operation: renderer computation only
- Image I/O: excluded from benchmark timing
- Output CSV: `report/data/m3-full-benchmark-with-stats.csv`
- Updated graphs: `report/assets/graphs-with-stats-m3/`

The generated CSV includes these timing columns for every sequential, static parallel, and dynamic parallel measurement:

```text
averageTimeMs,minTimeMs,maxTimeMs,stdDevTimeMs
```

Report-ready sentence:

> Each experiment was repeated three times after one warm-up run, and the average, minimum, maximum, and standard deviation values were recorded.

## Representative High-Workload Case

The table below shows the largest tested workload: `4000x4000`, `maxIter=1000`, `tileSize=64`.

| Mode | Threads | Avg ms | Min ms | Max ms | Stddev ms | Speedup | Efficiency |
|---|---:|---:|---:|---:|---:|---:|---:|
| Sequential | 1 | 6693.590 | 6690.228 | 6695.878 | 2.429 | 1.0000 | 1.0000 |
| Static | 1 | 6687.072 | 6686.603 | 6687.653 | 0.436 | 1.0010 | 1.0010 |
| Dynamic | 1 | 6700.707 | 6700.136 | 6701.295 | 0.473 | 0.9989 | 0.9989 |
| Static | 2 | 3426.452 | 3426.219 | 3426.603 | 0.167 | 1.9535 | 0.9768 |
| Dynamic | 2 | 3434.370 | 3432.727 | 3437.432 | 2.167 | 1.9490 | 0.9745 |
| Static | 4 | 3368.262 | 3361.930 | 3373.011 | 4.660 | 1.9873 | 0.4968 |
| Dynamic | 4 | 1868.305 | 1868.284 | 1868.327 | 0.018 | 3.5827 | 0.8957 |
| Static | 8 | 2436.174 | 2420.554 | 2445.967 | 11.163 | 2.7476 | 0.3434 |
| Dynamic | 8 | 1123.402 | 1122.098 | 1124.603 | 1.025 | 5.9583 | 0.7448 |

## Interpretation

For the largest workload, the dynamic renderer with 8 threads achieved the best time among the measured configurations: `1123.402 ms`, corresponding to a speedup of `5.9583`. Static partitioning also improved over the sequential baseline, but its 8-thread speedup was lower (`2.7476`) because horizontal row-based partitioning suffers from load imbalance in the Mandelbrot workload.

The low standard deviation for the dynamic 8-thread result (`1.025 ms`) indicates that the result was stable across the three measured repetitions in this run.
