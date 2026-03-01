# Spring Boot Issue #47969 Reproduction

## Issue

https://github.com/spring-projects/spring-boot/issues/47969

## How to Run

### Prerequisites

- **Java 25+** (required to build the spring-boot main branch)

### Run Tests

`test-with-branch.sh` automatically clones the spring-boot fork, publishes to mavenLocal, and runs tests.

```bash
git clone https://github.com/neungs-2/Spring-Boot-Issue-47696.git
cd Spring-Boot-Issue-47696

# Interceptor branch (ALL PASSED)
SPRING_BOOT_JAVA_HOME=/path/to/java25 ./test-with-branch.sh fix-47969-repository-observed-interceptor

# Listener branch (1 FAILED)
SPRING_BOOT_JAVA_HOME=/path/to/java25 ./test-with-branch.sh fix-47969-repository-observed-listener
```

Set `SPRING_BOOT_JAVA_HOME` to the Java 25+ path. Can be omitted if `JAVA_HOME` is already Java 25+.

## Test Results

### `fix-47969-repository-observed-interceptor` — All 4 tests passed

### `fix-47969-repository-observed-listener` — 1 of 4 tests failed

**Failing test:** `observedOnRepositoryDurationIsInSimilarRangeToRepositoryMetricDuration`

**Reason:** The listener approach calls both `Observation.start()` and `Observation.stop()` inside `afterInvocation`, so the observation does not wrap the actual method execution. The observation duration does not include the method execution time, causing a significant gap from the repository metric duration.

The interceptor approach wraps the method execution with `start()` → `proceed()` → `stop()`, so the observation duration accurately reflects the actual execution time.
