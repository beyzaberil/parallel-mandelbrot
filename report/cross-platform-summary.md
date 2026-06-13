# Cross-Platform Benchmark Summary

## Compared Systems

| System | CPU / Architecture | Logical Threads Tested | Status |
|---|---|---:|---|
| Apple M3 macOS | Apple M3, arm64 | 1, 2, 4, 8 | Complete |
| Windows/Ryzen | Exact model MISSING | 1, 2, 4, 8, 16 | Benchmark CSV present, hardware metadata incomplete |

## Largest Case Comparison

The largest benchmark case is 4000x4000 resolution with 1000 maximum iterations.

| System | Thread Count | Sequential Time (ms) | Static Time (ms) | Static Speedup | Dynamic Time (ms) | Dynamic Speedup | Dynamic Efficiency |
|---|---:|---:|---:|---:|---:|---:|---:|
| Apple M3 | 8 | 7245.545 | 2530.984 | 2.8627 | 1171.609 | 6.1843 | 0.7730 |
| Windows/Ryzen | 8 | 16939.691 | 5650.813 | 2.9977 | 2134.856 | 7.9348 | 0.9919 |
| Windows/Ryzen | 16 | 16939.691 | 3371.441 | 5.0245 | 1582.013 | 10.7077 | 0.6692 |

## Observations

- Both systems show that dynamic tile-based scheduling outperforms static row-band partitioning.
- On Apple M3, dynamic scheduling achieves 6.1843x speedup with 8 threads for the largest case.
- On the Windows/Ryzen benchmark, dynamic scheduling achieves 7.9348x speedup with 8 threads and 10.7077x with 16 threads for the largest case.
- Static partitioning plateaus earlier on both systems because fixed row bands do not distribute Mandelbrot workload evenly.
- The Windows/Ryzen sequential time is higher than the Apple M3 sequential time for the largest case, but the 16-thread dynamic run obtains higher speedup due to the larger thread count.

## Missing Before Final Report

- Exact Windows CPU model
- Physical core count
- RAM amount
- Windows version
- Java/JDK version used on Windows
- Confirmation of benchmark command, warm-up count, and repeat count

## Additional Artifacts Added Later

- Example Mandelbrot image: `report/assets/mandelbrot-sample.png`
- Dynamic scheduling architecture diagram: `report/assets/dynamic-task-scheduling.svg`
- Mermaid source for the diagram: `report/assets/dynamic-task-scheduling.mmd`
- Tile-size sweep data: `report/data/m3-tile-size-sweep.csv`
- Tile-size sweep graph: `report/assets/tile-size-sweep-2000x2000-iter1000.svg`
