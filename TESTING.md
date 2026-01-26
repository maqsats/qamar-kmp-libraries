# Running Tests

This document explains how to run tests for the Qamar KMP Libraries project.

## Quick Start

### Run All Tests (JVM/Desktop)
```bash
./gradlew test
```

### Run Tests for a Specific Module
```bash
# Test quran-api module
./gradlew :quran-api:test

# Test quran-core module
./gradlew :quran-core:test

# Test quran-translations module
./gradlew :quran-translations:test

# Test quran-transliteration module
./gradlew :quran-transliteration:test

# Test quran-search module
./gradlew :quran-search:test
```

## Platform-Specific Test Commands

### JVM/Desktop Tests
```bash
# Run all JVM tests
./gradlew jvmTest

# Run JVM tests for a specific module
./gradlew :quran-api:jvmTest
./gradlew :quran-api:desktopTest
```

### Android Tests
```bash
# Run all Android unit tests
./gradlew androidUnitTest

# Run Android tests for a specific module
./gradlew :quran-api:androidUnitTest
```

### JavaScript Tests
```bash
# Run all JS tests (Node.js)
./gradlew jsNodeTest

# Run JS tests for a specific module
./gradlew :quran-api:jsNodeTest

# Run JS tests in browser (requires browser)
./gradlew jsBrowserTest
./gradlew :quran-api:jsBrowserTest
```

### iOS Tests
```bash
# Run iOS tests (requires Xcode and iOS simulator)
./gradlew iosX64Test
./gradlew iosArm64Test
./gradlew iosSimulatorArm64Test

# Run for a specific module
./gradlew :quran-api:iosX64Test
```

## Running Specific Test Classes

### Using Gradle
```bash
# Run a specific test class (JVM)
./gradlew :quran-api:jvmTest --tests "com.qamar.quran.api.DefaultQuranApiTest"

# Run a specific test method
./gradlew :quran-api:jvmTest --tests "com.qamar.quran.api.DefaultQuranApiTest.testGetVerse"
```

### Using IntelliJ IDEA / Android Studio
1. Open the test file in the editor
2. Click the green arrow next to the test class or method
3. Select "Run 'TestName'"

Or use the keyboard shortcut:
- **Mac**: `Ctrl + Shift + R` (or `Cmd + Shift + R`)
- **Windows/Linux**: `Ctrl + Shift + F10`

## Running Tests with Coverage

```bash
# Generate test coverage report (JVM)
./gradlew :quran-api:jvmTest jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

## Common Test Tasks

### Clean and Test
```bash
./gradlew clean test
```

### Test with Verbose Output
```bash
./gradlew test --info
```

### Run Tests in Parallel (faster)
```bash
./gradlew test --parallel
```

### Skip Tests During Build
```bash
./gradlew build -x test
```

## Test Structure

Tests are located in `src/commonTest` for shared tests and platform-specific test directories:

- `src/commonTest/` - Shared tests (run on all platforms)
- `src/jvmTest/` - JVM-specific tests
- `src/androidUnitTest/` - Android-specific tests
- `src/iosTest/` - iOS-specific tests
- `src/jsTest/` - JavaScript-specific tests

## Troubleshooting

### Tests Fail with Database Errors
Make sure the test database drivers are properly configured. Check that:
- SQLDelight drivers are included in test dependencies
- Platform-specific test drivers are implemented

### iOS Tests Don't Run
- Ensure Xcode is installed
- iOS Simulator must be available
- Run: `xcodebuild -version` to verify Xcode installation

### JavaScript Tests Fail
- Ensure Node.js is installed: `node --version`
- For browser tests, ensure a browser is available

### Android Tests Fail
- Ensure Android SDK is configured
- Check `local.properties` has `sdk.dir` set correctly

## Continuous Integration

For CI/CD pipelines, use:

```bash
# Run all tests across all platforms
./gradlew test jvmTest androidUnitTest jsNodeTest
```

Note: iOS tests typically require macOS and Xcode, so they may be skipped in CI environments.
