# Groovy 4 InvokeDynamic Performance Testing Harness Plan

## Problem Summary

Based on investigation of the mailing list, JIRA issues, and Grails reports, the key issues with Groovy 4's invokedynamic implementation are:

1. **4x performance regression** in data-intensive operations (Grails 7 vs Grails 6)
2. **2.4x average slowdown** in test suites, with some tests **10x slower**
3. **3x memory consumption** (1.2GB vs 460MB) with **13+ million AtomicReference objects**
4. **One-time call overhead** - indy callsite generation is much more expensive than classic callsite code
5. **Polymorphic dispatch problems** - inline cache invalidation causes constant deoptimization
6. **High INDY_OPTIMIZE_THRESHOLD** (10,000 calls before optimization)

## Sources

- [Groovy - invoke dynamic performance problems (Grails Issue #15293)](https://github.com/apache/grails-core/issues/15293)
- [GROOVY-10307: Groovy 4 runtime performance 2.4x slower than Groovy 3](https://issues.apache.org/jira/browse/GROOVY-10307)
- [GROOVY-8298: Slow Performance Caused by Invoke Dynamic](https://issues.apache.org/jira/browse/GROOVY-8298)
- [Groovy 4.0.0-beta-1 Performance Discussion](https://www.mail-archive.com/dev@groovy.apache.org/msg05759.html)
- [InvokeDynamic Support Documentation](http://docs.groovy-lang.org/docs/next/html/documentation/invokedynamic-support.html)

## Existing Infrastructure

The `subprojects/performance` directory already has:
- JMH benchmarks with `CallsiteBench` testing mono/poly/megamorphic dispatch
- Compiler performance tests comparing Groovy 2.5/3.0/4.0
- Support for `-Pindy=true` flag to toggle invokedynamic
- Filtering via `-PbenchInclude=`

## Proposed Testing Harness

### Phase 1: Expand JMH Benchmarks for Identified Problem Areas

#### 1.1 First-Time/Cold Call Benchmarks
Create benchmarks that measure the cost of initial callsite creation:

```
src/jmh/groovy/org/apache/groovy/bench/indy/
├── ColdCallBench.java         # Measures first invocation overhead
├── ColdCallPatterns.groovy    # Various cold call scenarios
├── WarmupBehaviorBench.java   # Measures warmup curve over N invocations
└── ThresholdSensitivityBench.java  # Tests different optimize/fallback thresholds
```

**Key scenarios:**
- Single invocation of many different methods
- Method called 1, 10, 100, 1000, 10000 times (around threshold boundaries)
- New object creation + immediate method call (common in web apps)

#### 1.2 GORM-Like Access Patterns
Create benchmarks simulating ORM patterns:

```
src/jmh/groovy/org/apache/groovy/bench/orm/
├── PropertyAccessBench.java   # Dynamic property get/set
├── DynamicFinderBench.java    # Simulates findByX() patterns
├── EntityTraversalBench.java  # Object graph navigation
└── CollectionOperationsBench.java  # .each, .collect on domain objects
```

**Patterns to test:**
- `object.propertyName` access (heavy in Grails views)
- `collection*.property` spread operator
- Builder patterns with many method calls
- Method missing / property missing handling

#### 1.3 Polymorphic Dispatch Stress Tests
Expand `CallsiteBench` with:

```
src/jmh/groovy/org/apache/groovy/bench/dispatch/
├── CallsiteBench.java              # (existing)
├── CacheInvalidationBench.java     # Tests cache thrashing scenarios
├── MixedTypeCollectionBench.java   # Collections with varied types
└── InterfaceDispatchBench.java     # Interface-based polymorphism
```

**Focus on:**
- Changing receiver types mid-iteration (cache invalidation)
- Collections with 2-20 different implementation types
- MetaClass changes during execution

#### 1.4 Memory Pressure Benchmarks

```
src/jmh/groovy/org/apache/groovy/bench/memory/
├── AtomicReferenceAllocationBench.java  # Track AtomicReference creation
├── CallSiteCacheGrowthBench.java        # Measure cache memory over time
└── LongRunningSessionBench.java         # Simulate web session lifecycle
```

### Phase 2: Grails-Realistic Scenario Tests

#### 2.1 Create a "Mini Grails" Test Fixture

```
src/jmh/groovy/org/apache/groovy/bench/grailslike/
├── model/
│   ├── DomainObject.groovy       # Base with dynamic properties
│   ├── Person.groovy             # Sample domain class
│   ├── Order.groovy
│   └── OrderItem.groovy
├── service/
│   ├── DynamicService.groovy     # Service with method dispatch
│   └── TransactionalSimulator.groovy
├── controller/
│   ├── ControllerSimulator.groovy
│   └── RequestLifecycleBench.java  # Full request simulation
└── view/
    └── TemplateRenderBench.java    # GSP-like property access
```

**Test scenarios:**
- HTTP request lifecycle (create objects -> process -> render -> discard)
- 50 different domain classes with varied inheritance
- Service method calls through dynamic dispatch
- View rendering with heavy property access

#### 2.2 Real-World Workload Profiles

```
src/jmh/resources/workloads/
├── crud-heavy.json           # Config for CRUD-intensive tests
├── read-heavy.json           # Mostly reads (common in APIs)
├── batch-processing.json     # Large data processing
└── mixed-traffic.json        # Realistic web app mix
```

### Phase 3: Comparison Infrastructure

#### 3.1 Indy vs Non-Indy Comparison Framework

Modify build to support side-by-side comparison:

```groovy
// In build-logic
tasks.register('jmhCompare') {
    dependsOn 'jmhIndy', 'jmhNonIndy'
    doLast {
        // Generate comparison report
    }
}

tasks.register('jmhIndy', JmhExec) {
    jvmArgs = ['-Dgroovy.target.indy=true']
}

tasks.register('jmhNonIndy', JmhExec) {
    jvmArgs = ['-Dgroovy.target.indy=false']
}
```

#### 3.2 Threshold Parameter Sweep

```groovy
tasks.register('jmhThresholdSweep') {
    // Run benchmarks with varying thresholds
    [0, 100, 1000, 10000, 100000].each { threshold ->
        // Run with -Dgroovy.indy.optimize.threshold=$threshold
    }
}
```

### Phase 4: Profiling Integration

#### 4.1 JFR/Async-Profiler Integration

```
src/jmh/groovy/org/apache/groovy/bench/profiling/
├── ProfiledBenchmarkRunner.java   # Wraps benchmarks with profiling
└── FlameGraphGenerator.java       # Post-process JFR output
```

Add Gradle tasks:
```groovy
tasks.register('jmhProfile') {
    jvmArgs += [
        '-XX:+FlightRecorder',
        '-XX:StartFlightRecording=duration=60s,filename=build/jmh.jfr'
    ]
}
```

#### 4.2 Memory Analysis Hooks

```java
@State(Scope.Benchmark)
public class MemoryTrackingState {
    @Setup(Level.Trial)
    public void captureBeforeMemory() {
        // Record heap state, AtomicReference count
    }

    @TearDown(Level.Trial)
    public void captureAfterMemory() {
        // Compare and report delta
    }
}
```

### Phase 5: Continuous Benchmarking

#### 5.1 CI Integration

```yaml
# .github/workflows/performance.yml
- name: Run Performance Regression Tests
  run: ./gradlew :perf:jmhBaseline

- name: Compare Against Baseline
  run: ./gradlew :perf:jmhCompareBaseline
```

#### 5.2 Baseline Management

```
subprojects/performance/baselines/
├── groovy-3.0.x.json         # Known-good Groovy 3 numbers
├── groovy-4.0.x-indy.json    # Groovy 4 with indy
└── groovy-4.0.x-noindy.json  # Groovy 4 without indy
```

## Implementation Priority

| Priority | Component | Rationale |
|----------|-----------|-----------|
| **P0** | Cold call benchmarks | Addresses Jochen's key concern |
| **P0** | Threshold sensitivity tests | Quick wins for configuration tuning |
| **P1** | GORM-like property access | Reproduces Grails pain point |
| **P1** | Cache invalidation benchmarks | Addresses GROOVY-8298 |
| **P2** | Memory tracking | Addresses 3x memory consumption |
| **P2** | Mini-Grails fixture | Full scenario reproduction |
| **P3** | CI integration | Long-term regression prevention |

## Expected Outputs

1. **Benchmark results** showing exact overhead of indy vs classic callsites
2. **Threshold recommendations** for `groovy.indy.optimize.threshold` and `groovy.indy.fallback.threshold`
3. **Hot spot identification** - which call patterns are most problematic
4. **Memory profiles** showing AtomicReference allocation patterns
5. **Reproducible test cases** that can be attached to JIRA tickets

## Running the Harness

```bash
# Full benchmark suite (indy)
./gradlew -Pindy=true :perf:jmh

# Specific benchmark
./gradlew -Pindy=true -PbenchInclude=ColdCall :perf:jmh

# Compare indy vs non-indy
./gradlew :perf:jmhCompare

# Threshold sweep
./gradlew :perf:jmhThresholdSweep

# With profiling
./gradlew -Pindy=true :perf:jmhProfile
```
