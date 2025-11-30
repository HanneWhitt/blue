// HabitRepository.kt
package com.example.blue.presentation

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.time.LocalDate

fun loadHabitData(context: Context, currentDate: LocalDate): HabitData {
    val savedDataFile = "saved_habit_data.json"
    val initialDataFile = "initial_habit_data.json"

    return try {
        // First, try to load from internal storage
        val file = context.getFileStreamPath(savedDataFile)
        if (file.exists()) {
            println("Loading data from internal storage: $savedDataFile")
            val jsonString = context.openFileInput(savedDataFile).bufferedReader().use { it.readText() }
            parseHabitDataJson(jsonString, currentDate)
        } else {
            // If saved file doesn't exist, load from assets
            println("Saved data not found, loading from assets: $initialDataFile")
            val inputStream = context.assets.open(initialDataFile)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            parseHabitDataJson(jsonString, currentDate)
        }
    } catch (e: Exception) {
        println("Error loading habit data: ${e.message}")
        e.printStackTrace()
        // Fallback to empty data if both loading attempts fail
        HabitData(emptyList(), emptyList(), AppSettings())
    }
}

fun parseHabitDataJson(jsonString: String, currentDate: LocalDate): HabitData {
    val jsonObject = JSONObject(jsonString)

    // Parse settings
    val settingsObject = jsonObject.optJSONObject("settings")
    val noDays = settingsObject?.optInt("no_days", 10) ?: 10
    val settings = AppSettings(noDays = noDays)

    println("Loading data for $noDays days ending on $currentDate")

    // Parse habits from dictionary format
    val habitsObject = jsonObject.getJSONObject("habits")
    val habits = mutableListOf<Habit>()
    val habitIdsIterator = habitsObject.keys()

    while (habitIdsIterator.hasNext()) {
        val habitIdString = habitIdsIterator.next()
        val habitId = habitIdString.toInt()
        val habitJson = habitsObject.getJSONObject(habitIdString)

        val type = habitJson.getString("type")
        val name = habitJson.getString("name")
        val abbreviation = habitJson.getString("abbreviation")

        when (type) {
            "Binary" -> {
                val colorHex = habitJson.getString("colorHex")
                val color = Color(android.graphics.Color.parseColor(colorHex))
                val enabled = habitJson.optBoolean("enabled", true)  // Default to true for backward compatibility
                habits.add(
                    Habit.BinaryHabit(
                        id = habitId,
                        name = name,
                        abbreviation = abbreviation,
                        color = color,
                        enabled = enabled
                    )
                )
            }
            "Time-based" -> {
                val colorHex = habitJson.getString("colorHex")
                val color = Color(android.graphics.Color.parseColor(colorHex))
                val targetTime = habitJson.optString("targetTime", "12:00")
                val enabled = habitJson.optBoolean("enabled", true)  // Default to true for backward compatibility
                habits.add(
                    Habit.TimeBasedHabit(
                        id = habitId,
                        name = name,
                        abbreviation = abbreviation,
                        color = color,
                        targetTime = targetTime,
                        enabled = enabled
                    )
                )
            }
            "Multiple" -> {
                val colorHex = habitJson.getString("colorHex")
                val color = Color(android.graphics.Color.parseColor(colorHex))
                val completionsPerDay = habitJson.optInt("completionsPerDay", 3)  // Default to 3
                val enabled = habitJson.optBoolean("enabled", true)  // Default to true for backward compatibility
                habits.add(
                    Habit.MultipleHabit(
                        id = habitId,
                        name = name,
                        abbreviation = abbreviation,
                        color = color,
                        completionsPerDay = completionsPerDay,
                        enabled = enabled
                    )
                )
            }
            else -> {
                println("Unknown habit type: $type for habit ID $habitId")
            }
        }
    }

    // Generate list of dates for the last noDays (going backwards from currentDate)
    val dateList = mutableListOf<LocalDate>()
    for (i in 0 until noDays) {
        dateList.add(currentDate.minusDays(i.toLong()))
    }

    // Parse completions from new date-based format
    val completionsObject = jsonObject.getJSONObject("completions")
    val completions = mutableListOf<HabitCompletion>()

    // Iterate through each habit ID
    val habitIds = completionsObject.keys()
    while (habitIds.hasNext()) {
        val habitIdString = habitIds.next()
        val habitId = habitIdString.toInt()
        val habitDatesObject = completionsObject.getJSONObject(habitIdString)

        // For each date in our range, check if data exists
        dateList.forEachIndexed { dayIndex, date ->
            val dateString = date.toString() // Format: YYYY-MM-DD

            if (habitDatesObject.has(dateString)) {
                val dateData = habitDatesObject.getJSONObject(dateString)
                val isCompleted = if (dateData.isNull("iscompleted")) {
                    null
                } else {
                    dateData.getBoolean("iscompleted")
                }
                val completionCount = if (dateData.has("completionCount")) {
                    dateData.getInt("completionCount")
                } else {
                    null
                }
                val completionTime = if (dateData.has("completionTime")) {
                    dateData.getString("completionTime")
                } else {
                    null
                }

                completions.add(
                    HabitCompletion(
                        habitId = habitId,
                        dayIndex = dayIndex,
                        isCompleted = isCompleted,
                        completionCount = completionCount,
                        completionTime = completionTime
                    )
                )
            }
            // If date doesn't exist in data, we don't add a completion entry
        }
    }

    return HabitData(habits, completions, settings)
}

fun saveHabitDataToFile(context: Context, habitData: HabitData, currentDate: LocalDate) {
    try {
        val output_data_file = "saved_habit_data.json"
        val numDays = habitData.settings.noDays

        // Load existing data if it exists to preserve historical records
        val existingCompletionsObject = try {
            val file = context.getFileStreamPath(output_data_file)
            if (file.exists()) {
                val jsonString = context.openFileInput(output_data_file).bufferedReader().use { it.readText() }
                val existingJsonObject = JSONObject(jsonString)
                existingJsonObject.getJSONObject("completions")
            } else {
                JSONObject()
            }
        } catch (e: Exception) {
            println("No existing data to merge, creating new file")
            JSONObject()
        }

        // Create JSON object
        val jsonObject = JSONObject()

        // Add settings
        val settingsObject = JSONObject()
        settingsObject.put("no_days", habitData.settings.noDays)
        jsonObject.put("settings", settingsObject)

        // Add habits dictionary
        val habitsObject = JSONObject()
        habitData.habits.forEach { habit ->
            val habitJson = JSONObject()
            habitJson.put("name", habit.name)
            habitJson.put("abbreviation", habit.abbreviation)

            when (habit) {
                is Habit.BinaryHabit -> {
                    habitJson.put("type", "Binary")
                    habitJson.put("colorHex", "#1565C0")
                    habitJson.put("enabled", habit.enabled)
                }
                is Habit.TimeBasedHabit -> {
                    habitJson.put("type", "Time-based")
                    habitJson.put("colorHex", "#1565C0")
                    habitJson.put("targetTime", habit.targetTime)
                    habitJson.put("enabled", habit.enabled)
                }
                is Habit.MultipleHabit -> {
                    habitJson.put("type", "Multiple")
                    habitJson.put("colorHex", "#1565C0")
                    habitJson.put("completionsPerDay", habit.completionsPerDay)
                    habitJson.put("enabled", habit.enabled)
                }
            }

            habitsObject.put(habit.id.toString(), habitJson)
        }
        jsonObject.put("habits", habitsObject)

        // Generate list of dates for the last numDays (going backwards from currentDate)
        val dateList = mutableListOf<LocalDate>()
        for (i in 0 until numDays) {
            dateList.add(currentDate.minusDays(i.toLong()))
        }

        // Merge new completions with existing historical data
        val completionsObject = JSONObject()

        // Group new completions by habit ID
        val completionsByHabit = habitData.completions.groupBy { it.habitId }

        habitData.habits.forEach { habit ->
            val habitIdString = habit.id.toString()

            // Get existing dates for this habit (if any)
            val habitDatesObject = if (existingCompletionsObject.has(habitIdString)) {
                existingCompletionsObject.getJSONObject(habitIdString)
            } else {
                JSONObject()
            }

            // Add/update new completion data
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            habitCompletions.forEach { completion ->
                if (completion.dayIndex < dateList.size) {
                    val date = dateList[completion.dayIndex]
                    val dateString = date.toString() // Format: YYYY-MM-DD
                    val dateData = JSONObject()

                    if (completion.isCompleted != null) {
                        dateData.put("iscompleted", completion.isCompleted)
                    } else {
                        dateData.put("iscompleted", JSONObject.NULL)
                    }

                    if (completion.completionCount != null) {
                        dateData.put("completionCount", completion.completionCount)
                    }

                    if (completion.completionTime != null) {
                        dateData.put("completionTime", completion.completionTime)
                    }

                    // This will add new dates or update existing ones
                    habitDatesObject.put(dateString, dateData)
                }
            }

            completionsObject.put(habitIdString, habitDatesObject)
        }

        jsonObject.put("completions", completionsObject)

        // Write to internal storage
        context.openFileOutput(output_data_file, Context.MODE_PRIVATE).use { outputStream ->
            outputStream.write(jsonObject.toString(2).toByteArray())
        }

        println("Data saved successfully to $output_data_file")
    } catch (e: Exception) {
        println("Error saving data: ${e.message}")
        e.printStackTrace()
    }
}

fun createHabit(
    context: Context,
    currentDate: LocalDate,
    name: String,
    abbreviation: String,
    type: HabitType,
    completionsPerDay: Int = 3,
    targetTime: String = "12:00"
): HabitData {
    val habitData = loadHabitData(context, currentDate)

    // Find the next available ID
    val nextId = if (habitData.habits.isEmpty()) {
        0
    } else {
        (habitData.habits.maxOf { it.id }) + 1
    }

    // Create the new habit based on type
    val color = Color(android.graphics.Color.parseColor("#1565C0"))
    val newHabit = when (type) {
        HabitType.BINARY -> Habit.BinaryHabit(nextId, name, abbreviation, color)
        HabitType.TIME_BASED -> Habit.TimeBasedHabit(nextId, name, abbreviation, color, targetTime)
        HabitType.MULTIPLE -> Habit.MultipleHabit(nextId, name, abbreviation, color, completionsPerDay)
    }

    // Add new habit to the list
    val updatedHabits = habitData.habits + newHabit
    val updatedHabitData = HabitData(updatedHabits, habitData.completions, habitData.settings)

    // Save to file
    saveHabitDataToFile(context, updatedHabitData, currentDate)

    return updatedHabitData
}

fun updateHabit(
    context: Context,
    currentDate: LocalDate,
    habitId: Int,
    name: String,
    abbreviation: String,
    type: HabitType,
    completionsPerDay: Int = 3,
    targetTime: String = "12:00",
    enabled: Boolean = true
): HabitData {
    val habitData = loadHabitData(context, currentDate)

    // Update the habit
    val color = Color(android.graphics.Color.parseColor("#1565C0"))
    val updatedHabits = habitData.habits.map { habit ->
        if (habit.id == habitId) {
            when (type) {
                HabitType.BINARY -> Habit.BinaryHabit(habitId, name, abbreviation, color, enabled)
                HabitType.TIME_BASED -> Habit.TimeBasedHabit(habitId, name, abbreviation, color, targetTime, enabled)
                HabitType.MULTIPLE -> Habit.MultipleHabit(habitId, name, abbreviation, color, completionsPerDay, enabled)
            }
        } else {
            habit
        }
    }

    val updatedHabitData = HabitData(updatedHabits, habitData.completions, habitData.settings)

    // Save to file
    saveHabitDataToFile(context, updatedHabitData, currentDate)

    return updatedHabitData
}

fun deleteHabit(
    context: Context,
    currentDate: LocalDate,
    habitId: Int
): HabitData {
    val habitData = loadHabitData(context, currentDate)

    // Remove the habit and its completions
    val updatedHabits = habitData.habits.filter { it.id != habitId }
    val updatedCompletions = habitData.completions.filter { it.habitId != habitId }

    val updatedHabitData = HabitData(updatedHabits, updatedCompletions, habitData.settings)

    // Save to file
    saveHabitDataToFile(context, updatedHabitData, currentDate)

    return updatedHabitData
}

fun resetHabitData(context: Context): Boolean {
    return try {
        val savedDataFile = "saved_habit_data.json"
        val file = context.getFileStreamPath(savedDataFile)

        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                println("Reset successful: $savedDataFile deleted")
            } else {
                println("Reset failed: Could not delete $savedDataFile")
            }
            deleted
        } else {
            println("Reset: No saved data file to delete")
            true // Consider it successful if there's nothing to delete
        }
    } catch (e: Exception) {
        println("Error during reset: ${e.message}")
        e.printStackTrace()
        false
    }
}

