// HabitRepository.kt
package com.example.blue.presentation

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.time.LocalDate

fun loadHabitData(context: Context, currentDate: LocalDate, numDays: Int): HabitData {
    val savedDataFile = "saved_habit_data.json"
    val initialDataFile = "initial_habit_data.json"

    println("Loading data for $numDays days ending on $currentDate")

    return try {
        // First, try to load from internal storage
        val file = context.getFileStreamPath(savedDataFile)
        if (file.exists()) {
            println("Loading data from internal storage: $savedDataFile")
            val jsonString = context.openFileInput(savedDataFile).bufferedReader().use { it.readText() }
            parseHabitDataJson(jsonString, currentDate, numDays)
        } else {
            // If saved file doesn't exist, load from assets
            println("Saved data not found, loading from assets: $initialDataFile")
            val inputStream = context.assets.open(initialDataFile)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            parseHabitDataJson(jsonString, currentDate, numDays)
        }
    } catch (e: Exception) {
        println("Error loading habit data: ${e.message}")
        e.printStackTrace()
        // Fallback to empty data if both loading attempts fail
        HabitData(emptyList(), emptyList())
    }
}

fun parseHabitDataJson(jsonString: String, currentDate: LocalDate, numDays: Int): HabitData {
    val jsonObject = JSONObject(jsonString)

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
                habits.add(
                    Habit.BinaryHabit(
                        id = habitId,
                        name = name,
                        abbreviation = abbreviation,
                        color = color
                    )
                )
            }
            else -> {
                println("Unknown habit type: $type for habit ID $habitId")
            }
        }
    }

    // Generate list of dates for the last numDays (going backwards from currentDate)
    val dateList = mutableListOf<LocalDate>()
    for (i in 0 until numDays) {
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

                completions.add(
                    HabitCompletion(
                        habitId = habitId,
                        dayIndex = dayIndex,
                        isCompleted = isCompleted
                    )
                )
            }
            // If date doesn't exist in data, we don't add a completion entry
        }
    }

    return HabitData(habits, completions)
}

fun saveHabitDataToFile(context: Context, habitData: HabitData, currentDate: LocalDate, numDays: Int) {
    try {
        val output_data_file = "saved_habit_data.json"

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
