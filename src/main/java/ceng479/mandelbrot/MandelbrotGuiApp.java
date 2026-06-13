package ceng479.mandelbrot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class MandelbrotGuiApp {
    private final JFrame frame;
    private final JComboBox<String> modeCombo;
    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;
    private final JSpinner maxIterSpinner;
    private final JSpinner threadSpinner;
    private final JSpinner tileSizeSpinner;
    private final JButton renderButton;
    private final JButton compareButton;
    private final JButton saveButton;
    private final JLabel statusLabel;
    private final JLabel renderTimeLabel;
    private final JLabel speedupLabel;
    private final ImagePanel imagePanel;

    private BufferedImage lastImage;

    private MandelbrotGuiApp() {
        frame = new JFrame("CENG-479 Mandelbrot Renderer");
        modeCombo = new JComboBox<>(new String[]{"Dynamic Parallel", "Static Parallel", "Sequential"});
        widthSpinner = new JSpinner(new SpinnerNumberModel(1200, 200, 8000, 100));
        heightSpinner = new JSpinner(new SpinnerNumberModel(800, 200, 8000, 100));
        maxIterSpinner = new JSpinner(new SpinnerNumberModel(500, 50, 5000, 50));
        threadSpinner = new JSpinner(new SpinnerNumberModel(
                Runtime.getRuntime().availableProcessors(),
                1,
                Math.max(64, Runtime.getRuntime().availableProcessors()),
                1));
        tileSizeSpinner = new JSpinner(new SpinnerNumberModel(64, 8, 512, 8));
        renderButton = new JButton("Render");
        compareButton = new JButton("Compare");
        saveButton = new JButton("Save PNG");
        statusLabel = new JLabel("Ready");
        renderTimeLabel = new JLabel("Render time: -");
        speedupLabel = new JLabel("Speedup: -");
        imagePanel = new ImagePanel();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            new MandelbrotGuiApp().show();
        });
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // The default Swing look and feel is still usable.
        }
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setLayout(new BorderLayout());
        frame.add(controlPanel(), BorderLayout.WEST);
        frame.add(imagePanel, BorderLayout.CENTER);
        frame.add(statusPanel(), BorderLayout.SOUTH);

        renderButton.addActionListener(event -> renderSelectedMode());
        compareButton.addActionListener(event -> compareSequentialAndParallel());
        saveButton.addActionListener(event -> saveImage());
        saveButton.setEnabled(false);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel controlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(280, 620));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 10, 0);
        constraints.weightx = 1.0;

        JLabel title = new JLabel("Mandelbrot Demo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        panel.add(title, constraints);

        addLabeledControl(panel, constraints, "Mode", modeCombo);
        addLabeledControl(panel, constraints, "Width", widthSpinner);
        addLabeledControl(panel, constraints, "Height", heightSpinner);
        addLabeledControl(panel, constraints, "Max Iterations", maxIterSpinner);
        addLabeledControl(panel, constraints, "Threads", threadSpinner);
        addLabeledControl(panel, constraints, "Tile Size", tileSizeSpinner);

        constraints.gridy++;
        constraints.insets = new Insets(18, 0, 8, 0);
        panel.add(renderButton, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(compareButton, constraints);

        constraints.gridy++;
        panel.add(saveButton, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(20, 0, 8, 0);
        panel.add(renderTimeLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        panel.add(speedupLabel, constraints);

        constraints.gridy++;
        constraints.weighty = 1.0;
        panel.add(new JPanel(), constraints);

        return panel;
    }

    private void addLabeledControl(JPanel panel, GridBagConstraints constraints, String label, java.awt.Component control) {
        constraints.gridy++;
        constraints.insets = new Insets(12, 0, 4, 0);
        panel.add(new JLabel(label), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 4, 0);
        panel.add(control, constraints);
    }

    private JPanel statusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.add(statusLabel);
        return panel;
    }

    private void renderSelectedMode() {
        RenderSettings settings = readSettings();
        RenderMode mode = selectedRenderMode();
        runWorker("Rendering " + modeCombo.getSelectedItem() + "...", () -> {
            RenderResult result = render(settings, mode);
            return new DemoResult(result.image, result.elapsedMs, null);
        });
    }

    private void compareSequentialAndParallel() {
        RenderSettings settings = readSettings();
        RenderMode comparisonMode = selectedRenderMode();
        if (comparisonMode == RenderMode.SEQUENTIAL) {
            comparisonMode = RenderMode.DYNAMIC;
        }
        RenderMode selectedComparisonMode = comparisonMode;
        runWorker("Comparing sequential and parallel...", () -> {
            RenderResult sequential = render(settings, RenderMode.SEQUENTIAL);
            RenderResult parallel = render(settings, selectedComparisonMode);
            double speedup = sequential.elapsedMs / parallel.elapsedMs;
            return new DemoResult(parallel.image, parallel.elapsedMs, speedup);
        });
    }

    private void runWorker(String status, DemoTask task) {
        setBusy(true);
        statusLabel.setText(status);

        SwingWorker<DemoResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DemoResult doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    DemoResult result = get();
                    lastImage = result.image;
                    imagePanel.setImage(result.image);
                    renderTimeLabel.setText(String.format("Render time: %.3f ms", result.elapsedMs));
                    if (result.speedup == null) {
                        speedupLabel.setText("Speedup: -");
                    } else {
                        speedupLabel.setText(String.format("Speedup: %.4fx", result.speedup));
                    }
                    statusLabel.setText("Ready");
                    saveButton.setEnabled(true);
                } catch (Exception exception) {
                    statusLabel.setText("Failed");
                    JOptionPane.showMessageDialog(
                            frame,
                            exception.getMessage(),
                            "Render Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        };
        worker.execute();
    }

    private RenderResult render(RenderSettings settings, RenderMode mode) throws InterruptedException {
        MandelbrotConfig config = MandelbrotConfig.standard(
                settings.width,
                settings.height,
                settings.maxIterations,
                settings.tileSize);

        MandelbrotRenderer renderer = rendererFor(mode, settings.threadCount);

        long start;
        long end;
        int[] iterations;
        try {
            start = System.nanoTime();
            iterations = renderer.render(config);
            end = System.nanoTime();
        } finally {
            closeRenderer(renderer);
        }

        return new RenderResult(
                MandelbrotImageWriter.toBufferedImage(config, iterations),
                (end - start) / 1_000_000.0);
    }

    private RenderMode selectedRenderMode() {
        Object selected = modeCombo.getSelectedItem();
        if ("Static Parallel".equals(selected)) {
            return RenderMode.STATIC;
        }
        if ("Sequential".equals(selected)) {
            return RenderMode.SEQUENTIAL;
        }
        return RenderMode.DYNAMIC;
    }

    private MandelbrotRenderer rendererFor(RenderMode mode, int threadCount) {
        return switch (mode) {
            case SEQUENTIAL -> new SequentialMandelbrotRenderer();
            case STATIC -> new StaticParallelMandelbrotRenderer(threadCount);
            case DYNAMIC -> new ParallelMandelbrotRenderer(threadCount);
        };
    }

    private void closeRenderer(MandelbrotRenderer renderer) {
        if (renderer instanceof ParallelMandelbrotRenderer parallelRenderer) {
            parallelRenderer.close();
        } else if (renderer instanceof StaticParallelMandelbrotRenderer staticRenderer) {
            staticRenderer.close();
        }
    }

    private RenderSettings readSettings() {
        return new RenderSettings(
                (Integer) widthSpinner.getValue(),
                (Integer) heightSpinner.getValue(),
                (Integer) maxIterSpinner.getValue(),
                (Integer) threadSpinner.getValue(),
                (Integer) tileSizeSpinner.getValue());
    }

    private void saveImage() {
        if (lastImage == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        chooser.setSelectedFile(new File("mandelbrot-demo.png"));

        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File output = chooser.getSelectedFile();
        if (!output.getName().toLowerCase().endsWith(".png")) {
            output = new File(output.getParentFile(), output.getName() + ".png");
        }

        try {
            ImageIO.write(lastImage, "png", output);
            statusLabel.setText("Saved " + output.getName());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(frame, exception.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setBusy(boolean busy) {
        renderButton.setEnabled(!busy);
        compareButton.setEnabled(!busy);
        modeCombo.setEnabled(!busy);
        widthSpinner.setEnabled(!busy);
        heightSpinner.setEnabled(!busy);
        maxIterSpinner.setEnabled(!busy);
        threadSpinner.setEnabled(!busy);
        tileSizeSpinner.setEnabled(!busy);
        saveButton.setEnabled(!busy && lastImage != null);
    }

    private interface DemoTask {
        DemoResult run() throws Exception;
    }

    private enum RenderMode {
        SEQUENTIAL,
        STATIC,
        DYNAMIC
    }

    private record RenderSettings(int width, int height, int maxIterations, int threadCount, int tileSize) {
    }

    private record RenderResult(BufferedImage image, double elapsedMs) {
    }

    private record DemoResult(BufferedImage image, double elapsedMs, Double speedup) {
    }

    private static final class ImagePanel extends JPanel {
        private BufferedImage image;

        private ImagePanel() {
            setPreferredSize(new Dimension(820, 620));
            setBackground(new Color(22, 24, 28));
        }

        private void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (image == null) {
                graphics2D.setColor(new Color(235, 235, 235));
                graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 22f));
                String text = "Mandelbrot Preview";
                int x = (getWidth() - graphics2D.getFontMetrics().stringWidth(text)) / 2;
                int y = getHeight() / 2;
                graphics2D.drawString(text, x, y);
                graphics2D.dispose();
                return;
            }

            double scale = Math.min(
                    getWidth() / (double) image.getWidth(),
                    getHeight() / (double) image.getHeight());
            int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int x = (getWidth() - drawWidth) / 2;
            int y = (getHeight() - drawHeight) / 2;

            Image scaled = image.getScaledInstance(drawWidth, drawHeight, Image.SCALE_SMOOTH);
            graphics2D.drawImage(scaled, x, y, null);
            graphics2D.dispose();
        }
    }
}
