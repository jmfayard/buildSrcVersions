# gradle refreshVersions

> Life is too short to google for dependencies and versions

### Usage

```kotlin
// settings.gradle(.kts)
plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.11.0"
}
refreshVersions { // Optional: configure the plugin

}

// Migrate project
./gradlew refreshVersionsMigrate

// Find available updates in versions.properties
./gradlew refreshVersions

// Cleanup versions availability comments
./gradlew refreshVersionsCleanup
```

### Documentation is at [jmfayard.github.io/refreshVersions](https://jmfayard.github.io/refreshVersions/)

[![](https://raw.githubusercontent.com/jmfayard/refreshVersions/main/docs/img/screencast.png)](http://www.youtube.com/watch?v=VhYERonB8co "Gradle refreshVersions")
