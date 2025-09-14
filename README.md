# Blue

A Wear OS habit tracking application built with Jetpack Compose for Wear OS devices.

## Features

- Habit tracking with visual progress indicators
- Designed specifically for Wear OS smartwatches
- Built with Jetpack Compose for modern UI
- Standalone app (no phone companion required)

## Technical Details

- **Target Platform**: Wear OS (API 30+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose for Wear OS
- **Min SDK**: 30
- **Target SDK**: 35
- **Build System**: Gradle with Kotlin DSL

## Project Structure

- `app/src/main/java/com/example/blue/presentation/` - Main UI components and activities
- `app/src/main/java/com/example/blue/presentation/theme/` - App theming

## Building

This project uses Gradle. To build:

```bash
./gradlew build
```

To install on a connected Wear OS device:

```bash
./gradlew installDebug
```

## Requirements

- Android Studio with Wear OS support
- Wear OS device or emulator (API 30+)
- JDK 11+