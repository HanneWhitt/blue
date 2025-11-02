# Blue

A Wear OS habit tracking application built with Jetpack Compose for Wear OS devices.

A unique circular display shows days as sectors, and different habits as different concentric layers, enabling you to see at a glance how you are progress across all the different habits you are working to improve.

You can scroll through the habits and days easily to record when you have completed your habits!

The main Kotlin file is app/src/main/java/com/example/blue/presentation/MainActivity.kt

## Data Storage

The app uses a date-based JSON format to track habit completions over time.

### Data Format
- **Habits**: Array of habit objects with id, name, and colorHex
- **Completions**: Nested dictionary structure organized by habit ID and date
  - First level: habit IDs (e.g., "0", "1", "2")
  - Second level: ISO date strings (e.g., "2025-11-02")
  - Third level: completion data object with "iscompleted" key (true/false/null)

### Loading Behavior
- **First run**: Loads from `assets/initial_habit_data.json`
- **Subsequent runs**: Loads from `/data/data/com.example.blue/files/saved_habit_data.json`
- Only the last 10 days (ending on current date) are loaded into memory and displayed
- The top segment represents the current day; earlier segments (anticlockwise) represent progressively older days

### Saving Behavior
- Saves automatically on any completion change
- Merges new data with existing historical data in `saved_habit_data.json`
- Preserves all historical data indefinitely (not just the last 10 days)
- Updates existing date entries or adds new ones without overwriting older data

### Date Awareness
- The app tracks the current date and automatically reloads data when the date changes
- This ensures the display always shows the most recent 10 days
- Useful for users accessing the app around midnight when the day transitions


## Features

- Habit tracking with visual progress indicators
- Designed specifically for Wear OS smartwatches
- Built with Jetpack Compose for modern UI
- Standalone app (no phone companion required)

