/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.bench.profiling;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Post-processes JFR output to generate flame graph compatible output.
 * <p>
 * This class converts JFR recording data to the collapsed stack format
 * that can be consumed by flame graph tools like async-profiler's
 * converter or Brendan Gregg's flamegraph.pl.
 * <p>
 * Usage:
 * <pre>
 * # First, convert JFR to text using jfr tool
 * jfr print --events jdk.ExecutionSample recording.jfr > stacks.txt
 *
 * # Then use this tool to convert to collapsed format
 * java FlameGraphGenerator stacks.txt > collapsed.txt
 *
 * # Finally generate SVG using flamegraph.pl
 * flamegraph.pl collapsed.txt > profile.svg
 * </pre>
 */
public class FlameGraphGenerator {

    private static final Pattern STACK_FRAME_PATTERN =
            Pattern.compile("^\\s+(.+)$");

    private static final Pattern EVENT_SEPARATOR_PATTERN =
            Pattern.compile("^jdk\\.(ExecutionSample|NativeMethodSample|ObjectAllocationInNewTLAB|ObjectAllocationOutsideTLAB)");

    /**
     * Main entry point for command-line usage.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        switch (command) {
            case "collapse":
                if (args.length < 2) {
                    System.err.println("Usage: FlameGraphGenerator collapse <jfr-text-file>");
                    System.exit(1);
                }
                collapseStacks(args[1], System.out);
                break;

            case "filter":
                if (args.length < 3) {
                    System.err.println("Usage: FlameGraphGenerator filter <collapsed-file> <pattern>");
                    System.exit(1);
                }
                filterStacks(args[1], args[2], System.out);
                break;

            case "top":
                if (args.length < 2) {
                    System.err.println("Usage: FlameGraphGenerator top <collapsed-file> [n]");
                    System.exit(1);
                }
                int n = args.length > 2 ? Integer.parseInt(args[2]) : 20;
                topMethods(args[1], n, System.out);
                break;

            case "diff":
                if (args.length < 3) {
                    System.err.println("Usage: FlameGraphGenerator diff <baseline-file> <current-file>");
                    System.exit(1);
                }
                diffProfiles(args[1], args[2], System.out);
                break;

            default:
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("FlameGraphGenerator - JFR profile analysis tool for Groovy benchmarks");
        System.err.println();
        System.err.println("Commands:");
        System.err.println("  collapse <jfr-text>     Convert JFR text output to collapsed stacks");
        System.err.println("  filter <collapsed> <pattern>  Filter stacks matching pattern");
        System.err.println("  top <collapsed> [n]     Show top n methods by sample count");
        System.err.println("  diff <baseline> <current>  Show difference between profiles");
        System.err.println();
        System.err.println("Workflow:");
        System.err.println("  1. jfr print --events jdk.ExecutionSample recording.jfr > stacks.txt");
        System.err.println("  2. java FlameGraphGenerator collapse stacks.txt > collapsed.txt");
        System.err.println("  3. flamegraph.pl collapsed.txt > profile.svg");
        System.err.println();
        System.err.println("Analysis:");
        System.err.println("  java FlameGraphGenerator filter collapsed.txt 'groovy' > groovy-only.txt");
        System.err.println("  java FlameGraphGenerator top collapsed.txt 30");
        System.err.println("  java FlameGraphGenerator diff baseline.txt current.txt");
    }

    /**
     * Convert JFR text output to collapsed stack format.
     */
    public static void collapseStacks(String inputFile, PrintStream out) throws IOException {
        Map<String, Long> stackCounts = new LinkedHashMap<>();
        List<String> currentStack = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher eventMatcher = EVENT_SEPARATOR_PATTERN.matcher(line);
                if (eventMatcher.find()) {
                    // Start of new event - save previous stack if any
                    if (!currentStack.isEmpty()) {
                        String collapsed = collapseStack(currentStack);
                        stackCounts.merge(collapsed, 1L, Long::sum);
                        currentStack.clear();
                    }
                    continue;
                }

                Matcher frameMatcher = STACK_FRAME_PATTERN.matcher(line);
                if (frameMatcher.matches()) {
                    String frame = frameMatcher.group(1).trim();
                    // Clean up frame format
                    frame = cleanFrame(frame);
                    if (!frame.isEmpty()) {
                        currentStack.add(frame);
                    }
                }
            }

            // Don't forget the last stack
            if (!currentStack.isEmpty()) {
                String collapsed = collapseStack(currentStack);
                stackCounts.merge(collapsed, 1L, Long::sum);
            }
        }

        // Output in collapsed format
        for (Map.Entry<String, Long> entry : stackCounts.entrySet()) {
            out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    private static String collapseStack(List<String> stack) {
        // Reverse because JFR shows leaf first, but flame graphs want root first
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(stack.get(i));
        }
        return sb.toString();
    }

    private static String cleanFrame(String frame) {
        // Remove line numbers and source file info
        frame = frame.replaceAll("\\(.*\\)", "");
        // Remove module prefixes like java.base/
        frame = frame.replaceAll("^[\\w.]+/", "");
        return frame.trim();
    }

    /**
     * Filter stacks to only those matching a pattern.
     */
    public static void filterStacks(String inputFile, String pattern, PrintStream out) throws IOException {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (p.matcher(line).find()) {
                    out.println(line);
                }
            }
        }
    }

    /**
     * Show top methods by sample count.
     */
    public static void topMethods(String inputFile, int n, PrintStream out) throws IOException {
        Map<String, Long> methodCounts = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lastSpace = line.lastIndexOf(' ');
                if (lastSpace < 0) continue;

                String stackPart = line.substring(0, lastSpace);
                long count = Long.parseLong(line.substring(lastSpace + 1).trim());

                // Extract individual methods from stack
                String[] frames = stackPart.split(";");
                for (String frame : frames) {
                    methodCounts.merge(frame, count, Long::sum);
                }
            }
        }

        // Sort by count descending
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(methodCounts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        out.println("Top " + n + " methods by sample count:");
        out.println("====================================");
        int shown = 0;
        for (Map.Entry<String, Long> entry : sorted) {
            if (shown >= n) break;
            out.printf("%8d  %s%n", entry.getValue(), entry.getKey());
            shown++;
        }
    }

    /**
     * Compare two profiles and show differences.
     */
    public static void diffProfiles(String baselineFile, String currentFile, PrintStream out) throws IOException {
        Map<String, Long> baseline = loadCollapsed(baselineFile);
        Map<String, Long> current = loadCollapsed(currentFile);

        // Find methods with biggest changes
        Map<String, Double> changes = new HashMap<>();

        Set<String> allMethods = new HashSet<>();
        allMethods.addAll(baseline.keySet());
        allMethods.addAll(current.keySet());

        long baselineTotal = baseline.values().stream().mapToLong(Long::longValue).sum();
        long currentTotal = current.values().stream().mapToLong(Long::longValue).sum();

        for (String method : allMethods) {
            long baseCount = baseline.getOrDefault(method, 0L);
            long currCount = current.getOrDefault(method, 0L);

            double basePct = baselineTotal > 0 ? (baseCount * 100.0 / baselineTotal) : 0;
            double currPct = currentTotal > 0 ? (currCount * 100.0 / currentTotal) : 0;

            double diff = currPct - basePct;
            if (Math.abs(diff) > 0.1) { // Only show significant changes
                changes.put(method, diff);
            }
        }

        // Sort by absolute change
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(changes.entrySet());
        sorted.sort((a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));

        out.println("Profile Comparison (positive = regression, negative = improvement)");
        out.println("================================================================");
        out.printf("Baseline total samples: %d%n", baselineTotal);
        out.printf("Current total samples: %d%n", currentTotal);
        out.println();

        int shown = 0;
        for (Map.Entry<String, Double> entry : sorted) {
            if (shown >= 30) break;
            String sign = entry.getValue() > 0 ? "+" : "";
            out.printf("%s%.2f%%  %s%n", sign, entry.getValue(), entry.getKey());
            shown++;
        }
    }

    private static Map<String, Long> loadCollapsed(String file) throws IOException {
        Map<String, Long> methodCounts = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lastSpace = line.lastIndexOf(' ');
                if (lastSpace < 0) continue;

                String stackPart = line.substring(0, lastSpace);
                long count = Long.parseLong(line.substring(lastSpace + 1).trim());

                String[] frames = stackPart.split(";");
                for (String frame : frames) {
                    methodCounts.merge(frame, count, Long::sum);
                }
            }
        }

        return methodCounts;
    }

    /**
     * Programmatic API for generating collapsed stacks from JFR.
     */
    public static Map<String, Long> collapseFromJfr(Path jfrTextFile) throws IOException {
        Map<String, Long> stackCounts = new LinkedHashMap<>();
        List<String> currentStack = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(jfrTextFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher eventMatcher = EVENT_SEPARATOR_PATTERN.matcher(line);
                if (eventMatcher.find()) {
                    if (!currentStack.isEmpty()) {
                        String collapsed = collapseStack(currentStack);
                        stackCounts.merge(collapsed, 1L, Long::sum);
                        currentStack.clear();
                    }
                    continue;
                }

                Matcher frameMatcher = STACK_FRAME_PATTERN.matcher(line);
                if (frameMatcher.matches()) {
                    String frame = cleanFrame(frameMatcher.group(1).trim());
                    if (!frame.isEmpty()) {
                        currentStack.add(frame);
                    }
                }
            }

            if (!currentStack.isEmpty()) {
                String collapsed = collapseStack(currentStack);
                stackCounts.merge(collapsed, 1L, Long::sum);
            }
        }

        return stackCounts;
    }

    /**
     * Filter collapsed stacks programmatically.
     */
    public static Map<String, Long> filterByPattern(Map<String, Long> stacks, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Map<String, Long> filtered = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry : stacks.entrySet()) {
            if (p.matcher(entry.getKey()).find()) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    /**
     * Get top N methods from collapsed stacks.
     */
    public static List<Map.Entry<String, Long>> getTopMethods(Map<String, Long> stacks, int n) {
        Map<String, Long> methodCounts = new HashMap<>();

        for (Map.Entry<String, Long> entry : stacks.entrySet()) {
            String[] frames = entry.getKey().split(";");
            for (String frame : frames) {
                methodCounts.merge(frame, entry.getValue(), Long::sum);
            }
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(methodCounts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        return sorted.subList(0, Math.min(n, sorted.size()));
    }
}
