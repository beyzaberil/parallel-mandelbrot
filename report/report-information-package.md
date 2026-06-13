# CENG-479 Submission 2 Report Information Package

Project topic: **Dynamic Parallelization of Mandelbrot Set Fractal Generation Using Java Threads**

This package is based on the repository state inspected at commit `9744884` (`Add benchmark variance and chunk-size report artifacts`). It only uses information visible in the repository, generated benchmark files, source code, and local verification commands. When information is not available, it is marked as **MISSING**.

## A. Repository Overview

| Item | Detail |
|---|---|
| Project name | Dynamic Parallel Mandelbrot Generation |
| Main objective | Implement and compare sequential Mandelbrot rendering, static row-band parallel rendering, and dynamic tile-based parallel rendering using Java threads. |
| GitHub repository | `https://github.com/beyzaberil/parallel-mandelbrot.git` |
| Current commit | `9744884 Add benchmark variance and chunk-size report artifacts` |
| Main language | Java |
| Java/JDK used locally | Oracle JDK `21.0.11`, `javac 21.0.11` |
| Build system | No Maven/Gradle. Compilation is done with `javac` through `scripts/compile.sh`. |
| External dependencies | None. The project uses only the Java standard library. |
| Main CLI entry point | `ceng479.mandelbrot.MandelbrotApp` |
| GUI entry point | `ceng479.mandelbrot.MandelbrotGuiApp` |

Main folders and files:

| Path | Purpose |
|---|---|
| `src/main/java/ceng479/mandelbrot/` | Java source code |
| `scripts/compile.sh` | macOS/Linux compilation script |
| `results/csv/` | Generated benchmark CSV files |
| `results/graphs*/` | Generated benchmark graph PNG files |
| `results/images/` | Generated render/demo images |
| `report/data/` | Report-ready CSV data |
| `report/assets/` | Report-ready graphs, diagrams, and images |
| `report/*.md` | Report summaries and analysis notes |

Important classes:

| Class | Responsibility |
|---|---|
| `MandelbrotApp` | CLI dispatcher for `verify`, `render`, `benchmark`, `graphs`, and `tile-sweep` commands |
| `MandelbrotGuiApp` | Swing GUI demo for rendering and comparing modes |
| `MandelbrotConfig` | Immutable configuration: image size, max iterations, tile size, complex-plane bounds |
| `MandelbrotMath` | Pixel-to-complex mapping and Mandelbrot iteration calculation |
| `MandelbrotRenderer` | Renderer interface returning an `int[]` iteration buffer |
| `SequentialMandelbrotRenderer` | Baseline nested-loop renderer |
| `StaticParallelMandelbrotRenderer` | Fixed horizontal row-band renderer using `Executors.newFixedThreadPool` |
| `ParallelMandelbrotRenderer` | Dynamic tile renderer using `Executors.newWorkStealingPool` |
| `CorrectnessVerifier` | Pixel-by-pixel comparison of sequential, static, and dynamic outputs |
| `TimingStats` | Measures average, min, max, and standard deviation using `System.nanoTime()` |
| `BenchmarkRunner` | Runs full/quick benchmark matrices and writes benchmark CSV |
| `BenchmarkResult` | Formats benchmark rows and CSV headers |
| `BenchmarkGraphGenerator` | Generates execution time, speedup, and efficiency PNG graphs |
| `TileSizeSweepRunner` | Tests dynamic renderer with different tile sizes |
| `MandelbrotImageWriter` | Converts iteration buffers to `BufferedImage` and writes PNG files |
| `CliOptions` | Parses `--key=value` CLI options |

Compile command:

```bash
./scripts/compile.sh
```

Windows PowerShell compile command:

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java/*.java)
```

Run sequential render:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=sequential --width=1920 --height=1080 --maxIter=1000 --output=results/images/sequential.png
```

Run dynamic parallel render:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=dynamic --width=1920 --height=1080 --maxIter=1000 --threads=8 --tileSize=64 --output=results/images/dynamic.png
```

Run static parallel render:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=static --width=1920 --height=1080 --maxIter=1000 --threads=8 --tileSize=64 --output=results/images/static.png
```

Run correctness validation:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp verify
```

Run report benchmark:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=full --threads=1,2,4,8 --repeats=3 --warmups=1 --tileSize=64
```

Run chunk-size sweep:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp tile-sweep --width=4000 --height=4000 --maxIter=1000 --threads=8 --tileSizes=16,32,64,128,256 --repeats=3 --warmups=1
```

Generate graphs:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp graphs --input=report/data/m3-full-benchmark-with-stats.csv --outputDir=report/assets/graphs-with-stats-m3
```

## B. Problem and Algorithm Summary

The project computes and renders the Mandelbrot set. Each output pixel is mapped to a point `c = real + imaginary*i` in the complex plane. The standard configuration in `MandelbrotConfig.standard(...)` uses:

| Parameter | Value |
|---|---:|
| Real range | `[-2.0, 1.0]` |
| Imaginary range | `[-1.5, 1.5]` |
| Default tile size | `64` when not overridden |

Pixel mapping is implemented in `MandelbrotMath.computePixel(...)`. The x coordinate is mapped from `0..width-1` to `minReal..maxReal`. The y coordinate is mapped from `0..height-1` to `maxImaginary..minImaginary`, so the image is vertically oriented in the usual mathematical direction.

The iteration starts with `z = 0` and repeatedly applies:

```text
z(n+1) = z(n)^2 + c
```

The implementation stores real and imaginary parts separately:

```text
nextReal = zReal * zReal - zImaginary * zImaginary + real
nextImaginary = 2.0 * zReal * zImaginary + imaginary
```

Escape condition:

```text
zReal^2 + zImaginary^2 > 4.0
```

The loop stops when the point escapes or when `maxIterations` is reached. The returned value is the number of iterations performed for that pixel.

Color generation is implemented in `MandelbrotImageWriter.colorFor(...)`. Points that reach `maxIterations` are colored black. Escaped points are colored using HSB color with:

```text
hue = 0.68 + 0.32 * iteration / maxIterations
saturation = 0.85
brightness = 1.0
```

Computational complexity:

```text
O(width * height * maxIterations)
```

In practice, some pixels escape quickly, while points inside or near the set require many iterations. This makes Mandelbrot rendering suitable for parallelization because each pixel is independent from all other pixels, but the cost per pixel is uneven.

## C. Sequential Baseline Implementation

Main file/class/method:

| Item | Detail |
|---|---|
| File | `src/main/java/ceng479/mandelbrot/SequentialMandelbrotRenderer.java` |
| Class | `SequentialMandelbrotRenderer` |
| Method | `int[] render(MandelbrotConfig config)` |

Algorithm flow:

1. Allocate `int[] output` with size `width * height`.
2. Iterate rows from `y = 0` to `height - 1`.
3. Compute `rowOffset = y * width`.
4. Iterate columns from `x = 0` to `width - 1`.
5. Store `MandelbrotMath.computePixel(config, x, y)` at `output[rowOffset + x]`.
6. Return the iteration buffer.

Pixel traversal order is row-major: top-to-bottom, left-to-right.

Parameters:

| Parameter | Source |
|---|---|
| `width` | CLI `--width`, benchmark preset, or GUI spinner |
| `height` | CLI `--height`, benchmark preset, or GUI spinner |
| `maxIterations` | CLI `--maxIter`, benchmark preset, or GUI spinner |
| complex range | `MandelbrotConfig.standard`: `[-2.0, 1.0] x [-1.5, 1.5]` |
| output format | `int[]` iteration buffer; PNG only when `MandelbrotImageWriter` is called |

Timing details:

| Question | Answer |
|---|---|
| Is image generation included in benchmark timing? | No. Benchmarks time renderer computation only. |
| Is PNG file I/O included in benchmark timing? | No. |
| Is output buffer allocation included? | Yes. The renderer allocates `int[]` inside the timed render call. |
| Timing method | `System.nanoTime()` in `TimingStats.measure(...)` |
| Warm-up strategy | Configurable `warmups`; report runs use one warm-up |
| Repeats | Configurable `repeats`; report runs use three measured repeats |
| Min/max/stddev | Included in `m3-full-benchmark-with-stats.csv` |

Memory considerations:

The renderer stores one `int` per pixel. For a `4000x4000` image, the output array contains 16,000,000 integers, which is about 64 MB before Java object overhead. PNG conversion creates an additional `BufferedImage`, but that is not part of benchmark timing.

Sequential pseudocode:

```text
function renderSequential(config):
    output = new int[config.width * config.height]
    for y from 0 to config.height - 1:
        rowOffset = y * config.width
        for x from 0 to config.width - 1:
            output[rowOffset + x] = computePixel(config, x, y)
    return output
```

Report-ready paragraph:

The sequential baseline is implemented in `SequentialMandelbrotRenderer`. It allocates a one-dimensional integer array and fills it in row-major order using two nested loops over the image height and width. For each pixel, the renderer calls `MandelbrotMath.computePixel`, which maps the pixel to the complex plane and performs the Mandelbrot escape-time iteration. This baseline provides the reference execution time used for speedup and efficiency calculations.

## D. Parallel Implementation

The repository contains two parallel implementations: a static row-band version and the main dynamic tile-based version.

### Static Parallel Renderer

| Item | Detail |
|---|---|
| File | `src/main/java/ceng479/mandelbrot/StaticParallelMandelbrotRenderer.java` |
| Threading API | `ExecutorService`, `Executors.newFixedThreadPool(threadCount)`, `Callable<Void>`, `Future<Void>` |
| Decomposition | Horizontal row bands |
| Rows per task | `ceil(height / threadCount)` |
| Scheduling type | Static partitioning |

Static pseudocode:

```text
function renderStatic(config, threadCount):
    output = new int[width * height]
    rowsPerThread = ceil(height / threadCount)
    for each threadIndex:
        startY = threadIndex * rowsPerThread
        endY = min(startY + rowsPerThread, height)
        submit task renderRows(startY, endY)
    wait for all tasks
    return output
```

### Dynamic Parallel Renderer

| Item | Detail |
|---|---|
| File | `src/main/java/ceng479/mandelbrot/ParallelMandelbrotRenderer.java` |
| Threading API | `ExecutorService`, `Executors.newWorkStealingPool(threadCount)`, `Callable<Void>`, `Future<Void>` |
| Main data structure | Shared `int[] output` |
| Work decomposition | 2D tiles |
| Default tile size | `64x64` |
| Scheduling type | Java work-stealing executor with dynamically scheduled tile tasks |
| Manual work stealing? | No. The project uses Java's work-stealing executor instead of implementing a custom work-stealing queue. |

The dynamic renderer creates one `Callable<Void>` per tile. Each task computes a rectangular tile region and writes results to the shared output array. Because tiles are disjoint, two tasks do not write to the same pixel index.

Thread count selection:

| Context | Thread count |
|---|---|
| CLI render | `--threads`, default `Runtime.getRuntime().availableProcessors()` |
| CLI benchmark | `--threads`, default powers of two up to available processors |
| Correctness verifier | `Runtime.getRuntime().availableProcessors()` |
| GUI | thread spinner |

Synchronization and race prevention:

| Question | Answer |
|---|---|
| Are locks used for pixels? | No. |
| Why are locks avoided safely? | Each tile covers a disjoint pixel range. |
| How is completion handled? | `executor.invokeAll(tasks)` submits all tasks and waits for completion; each `Future.get()` is checked. |
| How are task failures handled? | `ExecutionException` is wrapped in `IllegalStateException`. |
| How is thread lifecycle managed? | Renderers implement `AutoCloseable`; `close()` calls `executor.shutdown()`. |

Dynamic pseudocode:

```text
function renderDynamic(config, threadCount):
    output = new int[width * height]
    tasks = []
    for startY from 0 to height - 1 step tileSize:
        endY = min(startY + tileSize, height)
        for startX from 0 to width - 1 step tileSize:
            endX = min(startX + tileSize, width)
            tasks.add(task renderTile(startX, endX, startY, endY))
    futures = workStealingExecutor.invokeAll(tasks)
    for each future in futures:
        future.get()
    return output
```

Expected performance benefits:

Dynamic scheduling improves load balance because Mandelbrot tiles have unequal cost. When a worker finishes an easier tile, it can continue with another tile instead of waiting for the slowest row band. This is especially useful near dense fractal regions where many pixels require more iterations.

Expected overheads:

| Overhead | Explanation |
|---|---|
| Task creation | Many `Callable` tile tasks are allocated. |
| Scheduling | The executor must schedule and coordinate tasks. |
| Future handling | `invokeAll` and `Future.get()` add coordination overhead. |
| Context switching | More worker activity can increase runtime overhead. |
| Memory writes | All modes write a full `int[]` output buffer. |

Report-ready paragraph:

The main parallel implementation is `ParallelMandelbrotRenderer`, which divides the image into rectangular tiles and submits each tile as a `Callable<Void>` to `Executors.newWorkStealingPool(threadCount)`. This design relies on Java's work-stealing executor rather than a manually implemented queue. Since each tile writes to a unique region of the output array, no locks are required for pixel writes. The dynamic task granularity improves load balancing compared with static row-band partitioning, especially because Mandelbrot pixels have uneven iteration counts.

## E. Correctness Validation

| Item | Detail |
|---|---|
| File | `src/main/java/ceng479/mandelbrot/CorrectnessVerifier.java` |
| Command | `java -cp out ceng479.mandelbrot.MandelbrotApp verify` |
| Validation config | `900x700`, `maxIterations=500`, `tileSize=64` |
| Compared modes | Sequential vs static parallel, sequential vs dynamic parallel |
| Comparison type | Pixel-by-pixel array comparison using `Arrays.equals(...)` |
| Checksum/hash | Not implemented |
| Visual comparison | Images exist, but correctness is verified numerically |
| Current verification output | `Correctness check passed: sequential, static parallel, and dynamic parallel outputs are identical.` |

Known differences:

No numerical differences are known from the current correctness check. The static and dynamic outputs match the sequential output exactly for the validation case.

Output images:

| Path | Purpose |
|---|---|
| `report/assets/sample-mandelbrot.png` | Report-ready Mandelbrot output image |
| `report/assets/mandelbrot-sample.png` | Earlier sample Mandelbrot image |
| `results/images/parallel-sample.png` | Generated sample output |
| `results/images/gui-check.png` | GUI check image |

Report-ready paragraph:

Correctness is validated by comparing the sequential output against both the static and dynamic parallel outputs on a `900x700` image with `500` maximum iterations. The validation uses pixel-by-pixel comparison of the integer iteration arrays. Since the parallel renderers write the same iteration values as the sequential renderer and the arrays are identical, the implementation confirms that parallel execution does not change the computed Mandelbrot result.

## F. Benchmark Methodology

### Local Apple M3 System

| Item | Detail |
|---|---|
| Machine | MacBook Air |
| Chip | Apple M3 |
| Cores | 8 total cores: 4 performance and 4 efficiency |
| Logical processors available | 8, confirmed by `getconf _NPROCESSORS_ONLN` |
| RAM | 16 GB |
| Architecture | `arm64` |
| OS | macOS `15.7.1` (`24G231`) |
| Java | Oracle JDK `21.0.11` LTS |
| Compiler | `javac 21.0.11` |
| IDE | MISSING, not relevant to command-line benchmark |

### Windows/Ryzen System

The repository contains Windows/Ryzen benchmark CSV data, but hardware metadata is incomplete.

| Item | Status |
|---|---|
| CPU model | MISSING |
| Physical core count | MISSING |
| Logical thread count | CSV includes tests up to 16 threads |
| RAM | MISSING |
| Windows version | MISSING |
| Java/JDK version | MISSING |
| Exact benchmark command | MISSING |
| Repeat/warm-up count | MISSING in CSV |

PowerShell commands to collect missing Windows data:

```powershell
Get-CimInstance Win32_Processor | Select-Object Name,NumberOfCores,NumberOfLogicalProcessors
Get-CimInstance Win32_ComputerSystem | Select-Object TotalPhysicalMemory
Get-ComputerInfo | Select-Object WindowsProductName,WindowsVersion,OsBuildNumber
java -version
javac -version
git rev-parse HEAD
```

### Benchmark Parameters

Updated M3 benchmark command:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=full --threads=1,2,4,8 --repeats=3 --warmups=1 --tileSize=64
```

Benchmark matrix for `--preset=full`:

| Resolutions | Max iterations |
|---|---|
| `1000x1000` | `250`, `500`, `1000` |
| `2000x2000` | `250`, `500`, `1000` |
| `4000x4000` | `250`, `500`, `1000` |

Thread counts:

| System/data file | Thread counts |
|---|---|
| Updated M3 full-stats benchmark | `1`, `2`, `4`, `8` |
| Windows/Ryzen benchmark CSV | `1`, `2`, `4`, `8`, `16` |

Chunk sizes:

| Test | Tile sizes |
|---|---|
| Main benchmark | `64x64` |
| Main chunk-size sweep | `16x16`, `32x32`, `64x64`, `128x128`, `256x256` |

Timing:

| Question | Answer |
|---|---|
| Timing API | `System.nanoTime()` |
| Warm-up runs | Yes, report M3 benchmark uses `1` warm-up |
| Measured repeats | Yes, report M3 benchmark uses `3` repeats |
| Average calculated | Yes |
| Min/max/stddev calculated | Yes for updated M3 benchmark and chunk-size sweep |
| Standard deviation formula | Population standard deviation: `sqrt(variance / repeats)` |
| Garbage collection controlled? | MISSING. No explicit GC control is implemented. |
| Image I/O excluded? | Yes for benchmark and tile sweep |
| Renderer allocation included? | Yes, each render allocates the output array |

Generated CSV/result files:

| File | Description |
|---|---|
| `report/data/m3-full-benchmark-with-stats.csv` | Updated M3 full benchmark with average, min, max, stddev |
| `report/data/chunk-size-sweep.csv` | Main `4000x4000`, `maxIter=1000`, 8-thread chunk-size sweep |
| `report/data/m3-static-dynamic-benchmark.csv` | Earlier M3 static vs dynamic benchmark without min/max/stddev |
| `report/data/windows-static-dynamic-benchmark.csv` | Windows/Ryzen benchmark without min/max/stddev |
| `report/data/m3-tile-size-sweep.csv` | Earlier `2000x2000`, `maxIter=1000` tile-size sweep |

Generated graph/image files:

| Path | Description |
|---|---|
| `report/assets/graphs-with-stats-m3/` | 27 updated M3 graphs from full-stats CSV |
| `report/assets/graphs-static-dynamic-m3/` | 27 earlier M3 static/dynamic graphs |
| `report/assets/graphs-static-dynamic-windows/` | 27 Windows/Ryzen graphs |
| `report/assets/chunk-size-sweep-4000x4000-iter1000.svg` | Main chunk-size sweep graph |
| `report/assets/sample-mandelbrot.png` | Report-ready Mandelbrot output image |
| `report/assets/dynamic-scheduling-architecture.png` | Report-ready architecture diagram |
| `report/assets/dynamic-task-scheduling.svg` | SVG source architecture diagram |
| `report/assets/dynamic-task-scheduling.mmd` | Mermaid source architecture diagram |

## G. Performance Results

### Updated Apple M3 Full Benchmark

Source file: `report/data/m3-full-benchmark-with-stats.csv`

The updated CSV contains 81 measured rows plus a header. It records sequential, static, and dynamic times for all full-preset workloads.

Best dynamic speedup in updated M3 benchmark:

| Resolution | Max iter | Threads | Tile size | Avg ms | Min ms | Max ms | Stddev ms | Speedup | Efficiency |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `4000x4000` | 1000 | 8 | 64 | 1123.402 | 1122.098 | 1124.603 | 1.025 | 5.9583 | 0.7448 |

Representative largest workload, `4000x4000`, `maxIter=1000`, `tileSize=64`:

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

Cases where a parallel run was slightly slower than the sequential baseline in the updated M3 benchmark:

| Mode | Workload | Threads | Speedup |
|---|---|---:|---:|
| Static | `1000x1000`, `maxIter=250` | 1 | 0.9936 |
| Static | `1000x1000`, `maxIter=500` | 1 | 0.9983 |
| Static | `1000x1000`, `maxIter=1000` | 1 | 0.9995 |
| Dynamic | `1000x1000`, `maxIter=1000` | 1 | 0.9988 |
| Dynamic | `4000x4000`, `maxIter=1000` | 1 | 0.9989 |

These cases occur at one thread and reflect parallel framework overhead rather than useful parallel execution.

### Chunk-Size Sweep

Source file: `report/data/chunk-size-sweep.csv`

Workload: `4000x4000`, `maxIter=1000`, dynamic renderer, `8` threads.

Sequential baseline for this sweep:

| Avg ms | Min ms | Max ms | Stddev ms |
|---:|---:|---:|---:|
| 6858.946 | 6846.927 | 6875.659 | 12.190 |

Dynamic results:

| Tile size | Avg ms | Min ms | Max ms | Stddev ms | Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|---:|
| 16x16 | 4357.135 | 2509.324 | 7074.894 | 1962.773 | 1.5742 | 0.1968 |
| 32x32 | 1237.027 | 1148.287 | 1402.208 | 116.909 | 5.5447 | 0.6931 |
| 64x64 | 1161.253 | 1151.264 | 1178.959 | 12.554 | 5.9065 | 0.7383 |
| 128x128 | 1185.322 | 1165.986 | 1214.358 | 20.905 | 5.7866 | 0.7233 |
| 256x256 | 1194.133 | 1191.992 | 1196.556 | 1.874 | 5.7439 | 0.7180 |

Best chunk size in this sweep:

```text
64x64
```

Precise interpretation: `64x64` was the best measured tile size for the main high-workload sweep and is a balanced default for this project. It should not be claimed as universally optimal for every machine or workload.

### Windows/Ryzen Results

Source file: `report/data/windows-static-dynamic-benchmark.csv`

The Windows CSV does not include min/max/stddev columns. It contains 99 measured rows plus a header.

Best dynamic speedup in Windows CSV:

| Resolution | Max iter | Threads | Avg ms | Speedup | Efficiency |
|---|---:|---:|---:|---:|---:|
| `2000x2000` | 1000 | 16 | 307.180 | 14.0969 | 0.8811 |

Largest Windows workload, `4000x4000`, `maxIter=1000`:

| Mode | Threads | Avg ms | Speedup | Efficiency |
|---|---:|---:|---:|---:|
| Sequential | 1 | 16939.691 | 1.0000 | 1.0000 |
| Static | 8 | 5650.813 | 2.9977 | 0.3747 |
| Dynamic | 8 | 2134.856 | 7.9348 | 0.9919 |
| Static | 16 | 3371.441 | 5.0245 | 0.3140 |
| Dynamic | 16 | 1582.013 | 10.7077 | 0.6692 |

### Cross-Platform Largest Case

Source file: `report/cross-platform-summary.md`

| System | Thread count | Sequential ms | Static ms | Static speedup | Dynamic ms | Dynamic speedup | Dynamic efficiency |
|---|---:|---:|---:|---:|---:|---:|---:|
| Apple M3 earlier run | 8 | 7245.545 | 2530.984 | 2.8627 | 1171.609 | 6.1843 | 0.7730 |
| Windows/Ryzen | 8 | 16939.691 | 5650.813 | 2.9977 | 2134.856 | 7.9348 | 0.9919 |
| Windows/Ryzen | 16 | 16939.691 | 3371.441 | 5.0245 | 1582.013 | 10.7077 | 0.6692 |

Notes:

- Both platforms show dynamic tile scheduling outperforming static row-band partitioning.
- Static partitioning plateaus because fixed row bands do not distribute work evenly.
- Dynamic scheduling scales better as thread count increases.
- The updated M3 benchmark with min/max/stddev has slightly different absolute values from the earlier M3 run, but it supports the same conclusion.

## H. Graph and Figure Suggestions

| Figure/table | X-axis | Y-axis | Data source | Suggested title | Demonstrates |
|---|---|---|---|---|---|
| Execution time vs thread count | Thread count | Average time (ms) | `report/data/m3-full-benchmark-with-stats.csv` | Execution Time by Thread Count | Parallel execution reduces time as threads increase |
| Speedup vs thread count | Thread count | Speedup | `report/data/m3-full-benchmark-with-stats.csv` | Speedup by Thread Count | Dynamic scaling is stronger than static scaling |
| Efficiency vs thread count | Thread count | Efficiency | `report/data/m3-full-benchmark-with-stats.csv` | Parallel Efficiency by Thread Count | Efficiency decreases as overhead and imbalance increase |
| Static vs dynamic comparison | Thread count | Speedup or average time | `report/data/m3-full-benchmark-with-stats.csv` | Static vs Dynamic Parallelization | Dynamic scheduling reduces load imbalance |
| Effect of image resolution | Resolution | Execution time or speedup | `report/data/m3-full-benchmark-with-stats.csv` | Effect of Resolution on Performance | Larger workloads amortize overhead better |
| Effect of max iterations | Max iterations | Execution time | `report/data/m3-full-benchmark-with-stats.csv` | Effect of Maximum Iterations | Higher iteration limits increase compute cost |
| Effect of chunk size | Tile size | Average time (ms) | `report/data/chunk-size-sweep.csv` | Chunk-Size Sweep for Dynamic Renderer | Very small tiles add overhead; very large tiles reduce load balancing |
| Example output image | N/A | N/A | `report/assets/sample-mandelbrot.png` | Sample Mandelbrot Set Image | Implementation visually renders the fractal |
| Architecture diagram | N/A | N/A | `report/assets/dynamic-scheduling-architecture.png` | Dynamic Tile-Based Task Scheduling Architecture | Image is split into tiles, tasks go to work-stealing pool, workers write disjoint pixels |

Recommended report figures:

| Figure | File |
|---|---|
| Main speedup graph | `report/assets/graphs-with-stats-m3/speedup-4000x4000-iter1000.png` |
| Main execution-time graph | `report/assets/graphs-with-stats-m3/execution-time-4000x4000-iter1000.png` |
| Main efficiency graph | `report/assets/graphs-with-stats-m3/efficiency-4000x4000-iter1000.png` |
| Chunk-size graph | `report/assets/chunk-size-sweep-4000x4000-iter1000.svg` |
| Mandelbrot output | `report/assets/sample-mandelbrot.png` |
| Architecture diagram | `report/assets/dynamic-scheduling-architecture.png` |

Suggested captions:

```text
Figure X. Sample Mandelbrot set image generated by the implemented renderer.
Figure X. Dynamic tile-based task scheduling architecture used in the parallel Mandelbrot renderer.
Figure X. Execution time comparison for the 4000x4000, maxIter=1000 workload.
Figure X. Speedup comparison between static row-band partitioning and dynamic tile scheduling.
Figure X. Effect of tile size on dynamic parallel rendering performance.
```

## I. Code Quality and Documentation

Code quality observations:

| Aspect | Evaluation |
|---|---|
| Modularity | Good. Computation, rendering, benchmarking, graphing, image writing, CLI, GUI, and validation are separated. |
| Naming | Clear class names such as `SequentialMandelbrotRenderer`, `ParallelMandelbrotRenderer`, and `BenchmarkRunner`. |
| Comments | Limited comments, but the code is readable and method names are descriptive. |
| Separation of concerns | Strong. Rendering code does not write images during benchmarks; image writing is in `MandelbrotImageWriter`. |
| Reusability | Good. All modes implement `MandelbrotRenderer`. |
| Error handling | Basic but appropriate: invalid parameters throw `IllegalArgumentException`; task failures are wrapped in `IllegalStateException`. |
| CLI usability | Good. Commands cover verify, render, benchmark, graph generation, and tile sweep. |
| Documentation | README gives compile/run commands. Report summaries provide benchmark interpretation. |
| Tests | No formal JUnit tests. Correctness is verified through a CLI command. |
| Code smells | `BenchmarkGraphGenerator` is relatively large and could be split later, but it is acceptable for a project utility. |

Report-ready paragraph:

The implementation is organized into small classes with clear responsibilities. The renderers share a common `MandelbrotRenderer` interface, while benchmarking, graph generation, correctness validation, and image writing are separated into dedicated classes. This modular structure makes it easy to compare sequential, static parallel, and dynamic parallel strategies using the same computational kernel. The project does not use a formal unit-testing framework, but it includes a deterministic correctness verifier that compares outputs pixel by pixel.

## J. Challenges and Solutions

| Challenge | Problem | Implemented solution | Effect |
|---|---|---|---|
| Load imbalance | Mandelbrot pixels have uneven iteration counts. Static row bands can leave some workers with heavier regions. | Added dynamic tile-based renderer using `Executors.newWorkStealingPool`. | Dynamic scheduling outperforms static partitioning, especially at higher thread counts. |
| Comparing naive and proposed parallelization | Only comparing sequential vs parallel would not show why dynamic scheduling matters. | Added `StaticParallelMandelbrotRenderer` as a row-band baseline. | Report can compare sequential, static parallel, and dynamic parallel. |
| Task granularity | Very small tiles increase scheduling overhead; very large tiles reduce load balancing. | Added `TileSizeSweepRunner` and tested `16,32,64,128,256`. | `64x64` was best for the main `4000x4000`, `maxIter=1000`, 8-thread sweep. |
| Benchmark accuracy | Single measurements are noisy and JVM JIT can affect results. | Added warm-up runs and repeated measurements through `TimingStats`. | CSV now includes average, min, max, and stddev for M3 full benchmark. |
| Correctness under parallel writes | Parallel workers share one output array. | Tasks write disjoint tile or row-band regions. | No locks are needed; pixel-by-pixel verification passes. |
| Cross-platform evaluation | Performance depends on hardware and OS. | Added teammate Windows/Ryzen benchmark CSV and graphs. | Report can discuss M3 and Windows/Ryzen behavior. |
| Visual presentation | Numeric tables alone are harder to explain in a presentation. | Added generated graphs, sample Mandelbrot image, and architecture diagram. | Report and presentation become clearer. |

MISSING or partially missing challenge evidence:

| Item | Status |
|---|---|
| Actual bug/fix history | MISSING. Commits show feature additions, but no documented bug-fix narrative is present. |
| Garbage collection analysis | MISSING. No GC logs or explicit GC controls are used. |
| Windows benchmark metadata | MISSING. Hardware/JDK details need teammate confirmation. |

## K. Academic Background Support

No formal academic references are stored in the repository. Therefore, the final report's reference list is **MISSING** and should be added before submission.

Recommended topics to cite:

| Topic | Why it is relevant |
|---|---|
| Mandelbrot set and escape-time algorithm | Explains the mathematical basis of the computation |
| Embarrassingly parallel workloads | Supports the claim that pixels can be computed independently |
| Dynamic scheduling and load balancing | Supports the static-vs-dynamic comparison |
| Java `ExecutorService` and work-stealing pools | Supports the implementation design |
| Amdahl's Law | Explains why speedup is not perfectly linear |
| Speedup and efficiency metrics | Defines performance formulas used in the benchmark |

Possible reference directions to verify and format in APA:

| Reference type | Suggested source |
|---|---|
| Java concurrency documentation | Official Oracle Java documentation for `ExecutorService`, `Executors`, and `ForkJoinPool` |
| Parallel performance theory | Amdahl's Law original paper or a parallel computing textbook |
| Mandelbrot background | A credible mathematical or computer graphics source explaining the Mandelbrot set |
| Load balancing/dynamic scheduling | Parallel computing textbook or peer-reviewed article |

Do not submit the report without replacing this section with verified APA references.

## L. Report-Ready Draft Material

### 1. Introduction

This project implements Mandelbrot set fractal generation as a parallel programming case study in Java. Mandelbrot rendering is computationally expensive because each pixel may require many complex-number iterations before it either escapes or reaches the maximum iteration limit. At the same time, each pixel can be computed independently, which makes the problem suitable for parallel execution. The project implements a sequential baseline, a static row-band parallel renderer, and a dynamic tile-based parallel renderer. The main goal is to evaluate whether dynamic task scheduling improves performance compared with both sequential execution and naive static partitioning.

### 2. Sequential Baseline Implementation

The sequential baseline is implemented in `SequentialMandelbrotRenderer`. It traverses the image in row-major order and computes the Mandelbrot escape-time value for every pixel using `MandelbrotMath.computePixel`. The result is stored in a one-dimensional integer array of size `width * height`. This baseline is used as the reference time for all speedup calculations. Benchmark timing includes renderer computation and output array allocation, but excludes image conversion and file I/O.

### 3. Parallel Implementation

The main parallel renderer is `ParallelMandelbrotRenderer`. It divides the image into rectangular tiles and submits each tile as a `Callable<Void>` task to a Java work-stealing executor created with `Executors.newWorkStealingPool(threadCount)`. Each task computes the pixels inside one tile and writes to a disjoint region of the shared output array, so no locks are required for pixel writes. The project also includes `StaticParallelMandelbrotRenderer`, which divides the image into fixed horizontal row bands. This static version serves as a naive parallel baseline for evaluating the benefit of dynamic scheduling.

### 4. Benchmark Methodology

Benchmarks were executed from the command line using the `benchmark` command in `MandelbrotApp`. The full benchmark tested image resolutions of `1000x1000`, `2000x2000`, and `4000x4000`, with maximum iteration values of `250`, `500`, and `1000`. The updated Apple M3 benchmark used thread counts `1`, `2`, `4`, and `8`, a tile size of `64x64`, one warm-up run, and three measured repetitions. Execution time was measured using `System.nanoTime()`, and the generated CSV records average, minimum, maximum, and standard deviation values. Image writing was excluded from benchmark timing.

### 5. Performance Comparison

The benchmark results show that dynamic tile-based scheduling provides better scalability than static row-band partitioning. For the largest updated Apple M3 workload, `4000x4000` with `1000` maximum iterations, the sequential renderer required `6693.590 ms`, while the dynamic renderer with 8 threads completed in `1123.402 ms`, achieving a speedup of `5.9583`. The static renderer with 8 threads completed in `2436.174 ms`, corresponding to a lower speedup of `2.7476`. These results support the conclusion that dynamic scheduling reduces load imbalance in Mandelbrot rendering.

### 6. Challenges and Solutions

The main challenge was load imbalance. Although Mandelbrot pixels are independent, their computation costs are not uniform because some regions escape quickly while others require many iterations. A static row-band decomposition can assign heavier regions to some workers, causing other workers to wait. The project addresses this by using smaller tile tasks and Java's work-stealing executor, allowing workers to continue processing new tiles as they finish earlier ones. Benchmark reliability was improved by adding warm-up runs, repeated measurements, and min/max/standard deviation reporting.

### 7. Conclusion and Future Improvements

The project successfully implements and benchmarks sequential, static parallel, and dynamic parallel Mandelbrot renderers in Java. The results show that dynamic tile-based scheduling provides the best performance among the implemented strategies, especially for larger workloads. The chunk-size sweep indicates that `64x64` was the best tile size for the main `4000x4000`, `maxIter=1000`, 8-thread Apple M3 experiment. Future improvements could include collecting complete Windows hardware metadata, adding formal JUnit tests, testing more tile sizes and thread counts, recording garbage collection behavior, and extending the visualization with workload heatmaps.

## M. Missing Information Checklist

| Item | Status | What to do |
|---|---|---|
| Updated M3 benchmark with min/max/stddev | Complete | Use `report/data/m3-full-benchmark-with-stats.csv`. |
| Main chunk-size sweep | Complete | Use `report/data/chunk-size-sweep.csv`. |
| Graphs for updated M3 benchmark | Complete | Use `report/assets/graphs-with-stats-m3/`. |
| Example Mandelbrot image | Complete | Use `report/assets/sample-mandelbrot.png`. |
| Architecture diagram | Complete | Use `report/assets/dynamic-scheduling-architecture.png`. |
| GitHub link | Complete | `https://github.com/beyzaberil/parallel-mandelbrot.git` |
| Correctness validation | Complete | `CorrectnessVerifier` compares arrays pixel by pixel. |
| Windows CPU model | MISSING | Ask teammate to run hardware commands. |
| Windows physical core count | MISSING | Ask teammate to run hardware commands. |
| Windows RAM | MISSING | Ask teammate to run hardware commands. |
| Windows version | MISSING | Ask teammate to run hardware commands. |
| Windows Java/JDK version | MISSING | Ask teammate to run `java -version` and `javac -version`. |
| Windows min/max/stddev | MISSING | Re-run Windows benchmark after latest code if these values are needed. |
| Exact Windows benchmark command | MISSING | Confirm with teammate. |
| APA references | MISSING | Add verified academic and official documentation references. |
| Garbage collection analysis | MISSING | Optional. Add GC logs only if the report needs deeper methodology. |
| Formal unit tests | MISSING | Optional. Current correctness validation is CLI-based. |

## N. Final Output Notes

Use these files first in the final report:

| Purpose | File |
|---|---|
| Main benchmark CSV | `report/data/m3-full-benchmark-with-stats.csv` |
| Main benchmark summary | `report/variance-benchmark-summary.md` |
| Main chunk-size CSV | `report/data/chunk-size-sweep.csv` |
| Main chunk-size summary | `report/chunk-size-sweep-summary.md` |
| M3 graphs | `report/assets/graphs-with-stats-m3/` |
| Windows CSV | `report/data/windows-static-dynamic-benchmark.csv` |
| Windows summary | `report/windows-static-dynamic-summary.md` |
| Cross-platform summary | `report/cross-platform-summary.md` |
| Sample output image | `report/assets/sample-mandelbrot.png` |
| Architecture diagram | `report/assets/dynamic-scheduling-architecture.png` |

Recommended wording for the benchmark methodology:

> Each experiment was repeated three times after one warm-up run, and the average, minimum, maximum, and standard deviation values were recorded. The measured time includes the computation of the Mandelbrot iteration buffer but excludes PNG image generation and file I/O.

Recommended wording for the dynamic scheduling claim:

> The implementation uses Java's built-in work-stealing executor through `Executors.newWorkStealingPool(threadCount)`. Therefore, the project relies on Java work-stealing support rather than implementing a custom work-stealing queue manually.

