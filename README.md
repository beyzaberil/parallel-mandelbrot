# Dynamic Parallel Mandelbrot Generation

CENG-479 Parallel Programming project for comparing a sequential Mandelbrot renderer with static and dynamically scheduled parallel versions implemented with Java threads.

## Project Goal

The project renders the Mandelbrot set by computing each pixel independently. The sequential baseline processes pixels with nested loops. The static parallel implementation divides the image into horizontal row bands, while the dynamic parallel implementation splits the image into small tiles and schedules those tiles on a Java work-stealing thread pool.

The benchmark records:

- Execution time
- Speedup: `T_sequential / T_parallel`
- Efficiency: `speedup / threadCount`

The static vs dynamic comparison is included to show why dynamic scheduling is useful for Mandelbrot rendering's uneven workload distribution.

## Requirements

- JDK 21 is recommended.
- JDK 17 or newer should also work.
- No external Java libraries are required.

## Compile

macOS / Linux:

```bash
./scripts/compile.sh
```

Windows PowerShell:

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java/*.java)
```

## Verify Correctness

Compares the sequential and parallel outputs pixel by pixel.

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp verify
```

## Render an Image

Sequential:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=sequential --width=1920 --height=1080 --maxIter=1000 --output=results/images/sequential.png
```

Legacy parallel alias, mapped to dynamic parallel:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=parallel --width=1920 --height=1080 --maxIter=1000 --threads=8 --tileSize=64 --output=results/images/parallel.png
```

Static parallel:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=static --width=1920 --height=1080 --maxIter=1000 --threads=8 --output=results/images/static.png
```

Dynamic parallel:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp render --mode=dynamic --width=1920 --height=1080 --maxIter=1000 --threads=8 --tileSize=64 --output=results/images/dynamic.png
```

## Run the GUI Demo

```bash
java -cp out ceng479.mandelbrot.MandelbrotGuiApp
```

## Run Benchmarks

Quick benchmark for testing:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=quick
```

Full benchmark for the report:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp benchmark --preset=full --threads=1,2,4,8,16 --repeats=5 --warmups=2 --tileSize=64
```

CSV files are written under `results/csv/`.

## Run Tile-Size Sweep

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp tile-sweep --width=2000 --height=2000 --maxIter=1000 --threads=8 --tileSizes=16,32,64,128,256 --repeats=3 --warmups=1
```

The tile-size sweep measures the dynamic renderer with different tile sizes and writes a CSV file under `results/csv/`.

## Generate Graphs

Create PNG charts from the latest benchmark CSV:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp graphs --input=latest
```

Or specify a CSV manually:

```bash
java -cp out ceng479.mandelbrot.MandelbrotApp graphs --input=results/csv/benchmark-YYYYMMDD-HHMMSS.csv
```

Charts are written under `results/graphs/`.

## Report Artifacts

Final Apple M3 benchmark data, graphs, and a short result summary are stored under `report/`.
The latest report artifacts include static row-band parallel results and dynamic tile-based parallel results for comparison.

## Suggested GitHub Workflow

Create a private GitHub repository, invite your teammate as a collaborator, then connect this local project to the remote:

```bash
git remote add origin <repo-url>
git branch -M main
git push -u origin main
```
