# Performance Baselines

This directory contains saved JMH benchmark results for comparison purposes.

## Creating a Baseline

Run benchmarks and save as a baseline:

```bash
# Run all benchmarks
./gradlew :performance:jmh

# Save results as a named baseline
./gradlew :performance:jmhSaveBaseline -PbaselineName=my-baseline

# Or with a specific date
./gradlew :performance:jmhSaveBaseline -PbaselineName=groovy-4.0.x-indy-20240115
```

## Comparing Against a Baseline

```bash
# Compare current results against most recent baseline
./gradlew :performance:jmh
./gradlew :performance:jmhCompareBaseline

# Compare against a specific baseline
./gradlew :performance:jmhCompareBaseline -PbaselineName=groovy-4.0.x-indy
```

## Baseline Naming Convention

Recommended naming pattern: `{version}-{mode}-{date}.txt`

Examples:
- `groovy-3.0.x-noindy.txt` - Groovy 3.x without invokedynamic
- `groovy-4.0.x-indy.txt` - Groovy 4.x with invokedynamic (default)
- `groovy-4.0.x-noindy.txt` - Groovy 4.x without invokedynamic
- `groovy-4.0.x-threshold-100.txt` - Groovy 4.x with custom threshold

## File Format

Baseline files are standard JMH text output format:

```
Benchmark                               Mode  Cnt    Score   Error  Units
MemoryAllocationBench.memory_create...  avgt   10   11.047 ± 0.493  us/op
MemoryAllocationBench.memory_poly...    avgt   10  253.352 ± 14.6   us/op
```
