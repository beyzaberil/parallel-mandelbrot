package ceng479.mandelbrot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import javax.imageio.ImageIO;

public final class BenchmarkGraphGenerator {
    private static final int IMAGE_WIDTH = 1200;
    private static final int IMAGE_HEIGHT = 800;
    private static final int LEFT_MARGIN = 95;
    private static final int RIGHT_MARGIN = 45;
    private static final int TOP_MARGIN = 80;
    private static final int BOTTOM_MARGIN = 95;

    private BenchmarkGraphGenerator() {
    }

    public static List<String> generate(String inputPath, String outputDirectory) throws IOException {
        File inputFile = resolveInputFile(inputPath);
        File outputDir = new File(outputDirectory);
        outputDir.mkdirs();

        List<BenchmarkRecord> records = readCsv(inputFile);
        Map<String, BenchmarkGroup> groups = groupByCase(records);
        List<String> outputs = new ArrayList<>();

        for (BenchmarkGroup group : groups.values()) {
            group.comparisonRecords.sort(Comparator.comparingInt(record -> record.threadCount));
            if (group.sequentialRecord == null || group.comparisonRecords.isEmpty()) {
                continue;
            }

            String slug = group.slug();
            outputs.add(drawExecutionTimeChart(group, new File(outputDir, "execution-time-" + slug + ".png")));
            outputs.add(drawSpeedupChart(group, new File(outputDir, "speedup-" + slug + ".png")));
            outputs.add(drawEfficiencyChart(group, new File(outputDir, "efficiency-" + slug + ".png")));
        }

        return outputs;
    }

    public static String latestCsvPath() {
        return resolveInputFile("latest").getPath();
    }

    private static File resolveInputFile(String inputPath) {
        if (!"latest".equalsIgnoreCase(inputPath)) {
            return new File(inputPath);
        }

        File directory = new File("results/csv");
        File[] files = directory.listFiles((dir, name) -> name.startsWith("benchmark-") && name.endsWith(".csv"));
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No benchmark CSV files found in results/csv.");
        }

        File latest = files[0];
        for (File file : files) {
            if (file.lastModified() > latest.lastModified()) {
                latest = file;
            }
        }
        return latest;
    }

    private static List<BenchmarkRecord> readCsv(File inputFile) throws IOException {
        List<BenchmarkRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return records;
            }

            Map<String, Integer> columns = columnsByName(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                records.add(BenchmarkRecord.fromCsv(line, columns));
            }
        }

        return records;
    }

    private static Map<String, Integer> columnsByName(String headerLine) {
        String[] names = headerLine.split(",");
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            columns.put(names[i].trim(), i);
        }
        return columns;
    }

    private static Map<String, BenchmarkGroup> groupByCase(List<BenchmarkRecord> records) {
        Map<String, BenchmarkGroup> groups = new LinkedHashMap<>();
        for (BenchmarkRecord record : records) {
            String key = record.width + "x" + record.height + "-iter" + record.maxIterations;
            BenchmarkGroup group = groups.computeIfAbsent(
                    key,
                    ignored -> new BenchmarkGroup(record.width, record.height, record.maxIterations));

            if ("sequential".equalsIgnoreCase(record.mode)) {
                group.sequentialRecord = record;
            } else if (isComparisonMode(record.mode)) {
                group.comparisonRecords.add(record.normalized());
            }
        }
        return groups;
    }

    private static String drawExecutionTimeChart(BenchmarkGroup group, File outputFile) throws IOException {
        List<Series> series = new ArrayList<>();
        List<BenchmarkRecord> dynamicRecords = group.recordsFor("dynamic");
        List<BenchmarkRecord> staticRecords = group.recordsFor("static");

        if (!dynamicRecords.isEmpty()) {
            series.add(new Series(
                "Dynamic parallel",
                new Color(41, 98, 255),
                dynamicRecords.stream()
                        .map(record -> new Point(record.threadCount, record.averageTimeMs))
                        .toList()));
        }
        if (!staticRecords.isEmpty()) {
            series.add(new Series(
                    "Static parallel",
                    new Color(239, 108, 0),
                    staticRecords.stream()
                            .map(record -> new Point(record.threadCount, record.averageTimeMs))
                            .toList()));
        }

        List<Point> baseline = new ArrayList<>();
        int minThread = group.minThreadCount();
        int maxThread = group.maxThreadCount();
        baseline.add(new Point(minThread, group.sequentialRecord.averageTimeMs));
        baseline.add(new Point(maxThread, group.sequentialRecord.averageTimeMs));
        series.add(new Series("Sequential baseline", new Color(198, 40, 40), baseline, true));

        drawChart(
                outputFile,
                "Execution Time - " + group.title(),
                "Thread Count",
                "Average Time (ms)",
                series,
                false,
                false);
        return outputFile.getPath();
    }

    private static String drawSpeedupChart(BenchmarkGroup group, File outputFile) throws IOException {
        List<Series> series = new ArrayList<>();
        List<BenchmarkRecord> dynamicRecords = group.recordsFor("dynamic");
        List<BenchmarkRecord> staticRecords = group.recordsFor("static");

        if (!dynamicRecords.isEmpty()) {
            series.add(new Series(
                "Dynamic speedup",
                new Color(0, 121, 107),
                dynamicRecords.stream()
                        .map(record -> new Point(record.threadCount, record.speedup))
                        .toList()));
        }
        if (!staticRecords.isEmpty()) {
            series.add(new Series(
                    "Static speedup",
                    new Color(239, 108, 0),
                    staticRecords.stream()
                            .map(record -> new Point(record.threadCount, record.speedup))
                            .toList()));
        }
        series.add(new Series(
                "Ideal speedup",
                new Color(117, 117, 117),
                group.threadCounts().stream()
                        .map(threadCount -> new Point(threadCount, threadCount))
                        .toList(),
                true));

        drawChart(
                outputFile,
                "Speedup - " + group.title(),
                "Thread Count",
                "Speedup",
                series,
                false,
                false);
        return outputFile.getPath();
    }

    private static String drawEfficiencyChart(BenchmarkGroup group, File outputFile) throws IOException {
        List<Series> series = new ArrayList<>();
        List<BenchmarkRecord> dynamicRecords = group.recordsFor("dynamic");
        List<BenchmarkRecord> staticRecords = group.recordsFor("static");

        if (!dynamicRecords.isEmpty()) {
            series.add(new Series(
                "Dynamic efficiency",
                new Color(123, 31, 162),
                dynamicRecords.stream()
                        .map(record -> new Point(record.threadCount, record.efficiency))
                        .toList()));
        }
        if (!staticRecords.isEmpty()) {
            series.add(new Series(
                    "Static efficiency",
                    new Color(239, 108, 0),
                    staticRecords.stream()
                            .map(record -> new Point(record.threadCount, record.efficiency))
                            .toList()));
        }

        List<Point> ideal = new ArrayList<>();
        int minThread = group.minThreadCount();
        int maxThread = group.maxThreadCount();
        ideal.add(new Point(minThread, 1.0));
        ideal.add(new Point(maxThread, 1.0));
        series.add(new Series("Ideal efficiency", new Color(117, 117, 117), ideal, true));

        drawChart(
                outputFile,
                "Efficiency - " + group.title(),
                "Thread Count",
                "Efficiency",
                series,
                false,
                true);
        return outputFile.getPath();
    }

    private static boolean isComparisonMode(String mode) {
        return "parallel".equalsIgnoreCase(mode)
                || "dynamic".equalsIgnoreCase(mode)
                || "static".equalsIgnoreCase(mode);
    }

    private static void drawChart(
            File outputFile,
            String title,
            String xAxisLabel,
            String yAxisLabel,
            List<Series> series,
            boolean forceZeroX,
            boolean capYAtOne) throws IOException {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        configureGraphics(graphics);

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            Bounds bounds = boundsFor(series, forceZeroX, capYAtOne);
            int plotX = LEFT_MARGIN;
            int plotY = TOP_MARGIN;
            int plotWidth = IMAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
            int plotHeight = IMAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;

            drawTitle(graphics, title);
            drawGridAndAxes(
                    graphics,
                    bounds,
                    xTicksFor(series, bounds),
                    plotX,
                    plotY,
                    plotWidth,
                    plotHeight,
                    xAxisLabel,
                    yAxisLabel);
            drawSeries(graphics, bounds, plotX, plotY, plotWidth, plotHeight, series);
            drawLegend(graphics, series, plotX + plotWidth - 260, plotY + 15);
        } finally {
            graphics.dispose();
        }

        File parent = outputFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", outputFile);
    }

    private static void configureGraphics(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static Bounds boundsFor(List<Series> series, boolean forceZeroX, boolean capYAtOne) {
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = 0.0;
        double maxY = capYAtOne ? 1.0 : -Double.MAX_VALUE;

        for (Series currentSeries : series) {
            for (Point point : currentSeries.points) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
            }
        }

        if (forceZeroX) {
            minX = 0.0;
        }
        if (minX == maxX) {
            maxX = minX + 1.0;
        }
        if (maxY <= minY) {
            maxY = minY + 1.0;
        }

        double yPadding = (maxY - minY) * 0.12;
        return new Bounds(minX, maxX, minY, maxY + yPadding);
    }

    private static void drawTitle(Graphics2D graphics, String title) {
        graphics.setColor(new Color(30, 30, 30));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(title, (IMAGE_WIDTH - metrics.stringWidth(title)) / 2, 42);
    }

    private static void drawGridAndAxes(
            Graphics2D graphics,
            Bounds bounds,
            List<Double> xTicks,
            int plotX,
            int plotY,
            int plotWidth,
            int plotHeight,
            String xAxisLabel,
            String yAxisLabel) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        graphics.setStroke(new BasicStroke(1f));

        for (int i = 0; i <= 5; i++) {
            double yValue = bounds.minY + (bounds.maxY - bounds.minY) * i / 5.0;
            int y = toPixelY(yValue, bounds, plotY, plotHeight);

            graphics.setColor(new Color(232, 236, 240));
            graphics.drawLine(plotX, y, plotX + plotWidth, y);
            graphics.setColor(new Color(80, 80, 80));
            String label = formatNumber(yValue);
            graphics.drawString(label, plotX - 12 - graphics.getFontMetrics().stringWidth(label), y + 5);
        }

        for (double xValue : xTicks) {
            int x = toPixelX(xValue, bounds, plotX, plotWidth);

            graphics.setColor(new Color(232, 236, 240));
            graphics.drawLine(x, plotY, x, plotY + plotHeight);
            graphics.setColor(new Color(80, 80, 80));
            String label = formatNumber(xValue);
            graphics.drawString(label, x - graphics.getFontMetrics().stringWidth(label) / 2, plotY + plotHeight + 28);
        }

        graphics.setColor(new Color(30, 30, 30));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawLine(plotX, plotY + plotHeight, plotX + plotWidth, plotY + plotHeight);
        graphics.drawLine(plotX, plotY, plotX, plotY + plotHeight);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(
                xAxisLabel,
                plotX + (plotWidth - metrics.stringWidth(xAxisLabel)) / 2,
                IMAGE_HEIGHT - 35);

        graphics.rotate(-Math.PI / 2.0);
        graphics.drawString(
                yAxisLabel,
                -(plotY + (plotHeight + metrics.stringWidth(yAxisLabel)) / 2),
                32);
        graphics.rotate(Math.PI / 2.0);
    }

    private static List<Double> xTicksFor(List<Series> series, Bounds bounds) {
        TreeSet<Double> ticks = new TreeSet<>();
        for (Series currentSeries : series) {
            for (Point point : currentSeries.points) {
                ticks.add(point.x);
            }
        }

        if (ticks.size() > 0 && ticks.size() <= 12) {
            return new ArrayList<>(ticks);
        }

        List<Double> generatedTicks = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            generatedTicks.add(bounds.minX + (bounds.maxX - bounds.minX) * i / 5.0);
        }
        return generatedTicks;
    }

    private static void drawSeries(
            Graphics2D graphics,
            Bounds bounds,
            int plotX,
            int plotY,
            int plotWidth,
            int plotHeight,
            List<Series> series) {
        for (Series currentSeries : series) {
            graphics.setColor(currentSeries.color);
            graphics.setStroke(currentSeries.dashed
                    ? new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[]{10f, 8f}, 0f)
                    : new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 1; i < currentSeries.points.size(); i++) {
                Point previous = currentSeries.points.get(i - 1);
                Point current = currentSeries.points.get(i);
                graphics.drawLine(
                        toPixelX(previous.x, bounds, plotX, plotWidth),
                        toPixelY(previous.y, bounds, plotY, plotHeight),
                        toPixelX(current.x, bounds, plotX, plotWidth),
                        toPixelY(current.y, bounds, plotY, plotHeight));
            }

            graphics.setStroke(new BasicStroke(2f));
            for (Point point : currentSeries.points) {
                int x = toPixelX(point.x, bounds, plotX, plotWidth);
                int y = toPixelY(point.y, bounds, plotY, plotHeight);
                graphics.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
            }
        }
    }

    private static void drawLegend(Graphics2D graphics, List<Series> series, int x, int y) {
        int width = 275;
        int height = 24 + series.size() * 28;
        graphics.setColor(new Color(255, 255, 255, 235));
        graphics.fillRoundRect(x, y, width, height, 8, 8);
        graphics.setColor(new Color(210, 210, 210));
        graphics.drawRoundRect(x, y, width, height, 8, 8);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        for (int i = 0; i < series.size(); i++) {
            Series currentSeries = series.get(i);
            int rowY = y + 26 + i * 28;
            graphics.setColor(currentSeries.color);
            graphics.setStroke(currentSeries.dashed
                    ? new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[]{8f, 6f}, 0f)
                    : new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.drawLine(x + 14, rowY - 5, x + 52, rowY - 5);
            graphics.fill(new Ellipse2D.Double(x + 29, rowY - 10, 10, 10));
            graphics.setColor(new Color(35, 35, 35));
            graphics.drawString(currentSeries.name, x + 64, rowY);
        }
    }

    private static int toPixelX(double x, Bounds bounds, int plotX, int plotWidth) {
        double ratio = (x - bounds.minX) / (bounds.maxX - bounds.minX);
        return plotX + (int) Math.round(ratio * plotWidth);
    }

    private static int toPixelY(double y, Bounds bounds, int plotY, int plotHeight) {
        double ratio = (y - bounds.minY) / (bounds.maxY - bounds.minY);
        return plotY + plotHeight - (int) Math.round(ratio * plotHeight);
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.format(Locale.US, "%.0f", value);
        }
        if (Math.abs(value) >= 100.0) {
            return String.format(Locale.US, "%.1f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private static final class BenchmarkGroup {
        private final int width;
        private final int height;
        private final int maxIterations;
        private BenchmarkRecord sequentialRecord;
        private final List<BenchmarkRecord> comparisonRecords = new ArrayList<>();

        private BenchmarkGroup(int width, int height, int maxIterations) {
            this.width = width;
            this.height = height;
            this.maxIterations = maxIterations;
        }

        private String title() {
            return width + "x" + height + ", maxIter=" + maxIterations;
        }

        private String slug() {
            return width + "x" + height + "-iter" + maxIterations;
        }

        private List<BenchmarkRecord> recordsFor(String mode) {
            return comparisonRecords.stream()
                    .filter(record -> mode.equals(record.mode))
                    .sorted(Comparator.comparingInt(record -> record.threadCount))
                    .toList();
        }

        private List<Integer> threadCounts() {
            return comparisonRecords.stream()
                    .map(record -> record.threadCount)
                    .distinct()
                    .sorted()
                    .toList();
        }

        private int minThreadCount() {
            return threadCounts().get(0);
        }

        private int maxThreadCount() {
            List<Integer> threadCounts = threadCounts();
            return threadCounts.get(threadCounts.size() - 1);
        }
    }

    private static final class BenchmarkRecord {
        private final String mode;
        private final int width;
        private final int height;
        private final int maxIterations;
        private final int threadCount;
        private final double averageTimeMs;
        private final double speedup;
        private final double efficiency;

        private BenchmarkRecord(
                String mode,
                int width,
                int height,
                int maxIterations,
                int threadCount,
                double averageTimeMs,
                double speedup,
                double efficiency) {
            this.mode = mode;
            this.width = width;
            this.height = height;
            this.maxIterations = maxIterations;
            this.threadCount = threadCount;
            this.averageTimeMs = averageTimeMs;
            this.speedup = speedup;
            this.efficiency = efficiency;
        }

        private static BenchmarkRecord fromCsv(String line, Map<String, Integer> columns) {
            String[] values = line.split(",");
            return new BenchmarkRecord(
                    value(values, columns, "mode"),
                    integer(values, columns, "width"),
                    integer(values, columns, "height"),
                    integer(values, columns, "maxIter"),
                    integer(values, columns, "threadCount"),
                    decimal(values, columns, "averageTimeMs"),
                    decimal(values, columns, "speedup"),
                    decimal(values, columns, "efficiency"));
        }

        private BenchmarkRecord normalized() {
            String normalizedMode = "parallel".equalsIgnoreCase(mode) ? "dynamic" : mode.toLowerCase(Locale.US);
            return new BenchmarkRecord(
                    normalizedMode,
                    width,
                    height,
                    maxIterations,
                    threadCount,
                    averageTimeMs,
                    speedup,
                    efficiency);
        }

        private static String value(String[] values, Map<String, Integer> columns, String columnName) {
            Integer index = columns.get(columnName);
            if (index == null || index >= values.length) {
                throw new IllegalArgumentException("Missing CSV column: " + columnName);
            }
            return values[index].trim();
        }

        private static int integer(String[] values, Map<String, Integer> columns, String columnName) {
            return Integer.parseInt(value(values, columns, columnName));
        }

        private static double decimal(String[] values, Map<String, Integer> columns, String columnName) {
            return Double.parseDouble(value(values, columns, columnName));
        }
    }

    private static final class Point {
        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Series {
        private final String name;
        private final Color color;
        private final List<Point> points;
        private final boolean dashed;

        private Series(String name, Color color, List<Point> points) {
            this(name, color, points, false);
        }

        private Series(String name, Color color, List<Point> points, boolean dashed) {
            this.name = name;
            this.color = color;
            this.points = points;
            this.dashed = dashed;
        }
    }

    private static final class Bounds {
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;

        private Bounds(double minX, double maxX, double minY, double maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}
