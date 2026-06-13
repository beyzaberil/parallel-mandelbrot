# Chunk-Size Sweep Summary

This summary documents the dynamic renderer tile-size experiment for the main high-workload case.

## Command

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp tile-sweep --width=4000 --height=4000 --maxIter=1000 --threads=8 --tileSizes=16,32,64,128,256 --repeats=3 --warmups=1
```

## Methodology

- Workload: `4000x4000`, `maxIter=1000`
- Renderer: dynamic tile-based parallel renderer
- Thread count: `8`
- Tile sizes: `16x16`, `32x32`, `64x64`, `128x128`, `256x256`
- Repeats: `3`
- Warm-ups: `1`
- Timing method: `System.nanoTime()`
- Image I/O: excluded
- Output CSV: `report/data/chunk-size-sweep.csv`
- Graph: `report/assets/chunk-size-sweep-4000x4000-iter1000.svg`

## Results

Sequential baseline for this sweep:

| Avg ms | Min ms | Max ms | Stddev ms |
|---:|---:|---:|---:|
| 6858.946 | 6846.927 | 6875.659 | 12.190 |

Dynamic renderer results:

| Tile size | Avg ms | Min ms | Max ms | Stddev ms | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|---:|
| 16x16 | 4357.135 | 2509.324 | 7074.894 | 1962.773 | 1.5742 | 0.1968 |
| 32x32 | 1237.027 | 1148.287 | 1402.208 | 116.909 | 5.5447 | 0.6931 |
| 64x64 | 1161.253 | 1151.264 | 1178.959 | 12.554 | 5.9065 | 0.7383 |
| 128x128 | 1185.322 | 1165.986 | 1214.358 | 20.905 | 5.7866 | 0.7233 |
| 256x256 | 1194.133 | 1191.992 | 1196.556 | 1.874 | 5.7439 | 0.7180 |

## Best Chunk Size

For this `4000x4000`, `maxIter=1000`, 8-thread M3 experiment, the best measured tile size was `64x64`, with an average time of `1161.253 ms` and a speedup of `5.9065`.

The `16x16` tile size produced much higher variance and a worse average time, which suggests that overly small tasks can introduce scheduling overhead and measurement instability. Larger tiles (`128x128` and `256x256`) were stable but slightly slower than `64x64`, which is consistent with weaker load balancing as task granularity becomes coarser.

Report wording should be precise: `64x64` was the best tile size for this main high-workload sweep, and it is a balanced default for the project. It should not be claimed as universally optimal for every possible machine or workload.
