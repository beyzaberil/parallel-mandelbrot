package ceng479.mandelbrot;

import java.util.HashMap;
import java.util.Map;

public final class CliOptions {
    private final Map<String, String> values;

    private CliOptions(Map<String, String> values) {
        this.values = values;
    }

    public static CliOptions parse(String[] args, int startIndex) {
        Map<String, String> values = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }

            int equalsIndex = arg.indexOf('=');
            if (equalsIndex < 0) {
                values.put(arg.substring(2), "true");
            } else {
                values.put(arg.substring(2, equalsIndex), arg.substring(equalsIndex + 1));
            }
        }
        return new CliOptions(values);
    }

    public String getString(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public int[] getIntList(String key, int[] defaultValue) {
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        String[] parts = value.split(",");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = Integer.parseInt(parts[i].trim());
        }
        return numbers;
    }
}
