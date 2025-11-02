// MainActivity.kt
package com.example.blue.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            HabitTrackerDisplay()
        }
    }
}

// Data classes
data class HabitCompletion(
    val habitId: Int,
    val dayIndex: Int, // 0-9 for 10 days
    val isCompleted: Boolean?  // null = no data, true = completed, false = not completed
)

data class Habit(
    val id: Int,
    val name: String,
    val color: Color
)

data class HabitData(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>
)

@Composable
fun HabitTrackerDisplay() {
    // Current date tracking
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    // Selection state
    var selectedDayIndex by remember { mutableStateOf(0) }
    var selectedHabitIndex by remember { mutableStateOf(0) }
    var scrollAccumulator by remember { mutableStateOf(0f) }
    val scrollThreshold = 25f  // Adjust this value to control sensitivity

    // Spacing configuration variables
    val outerMarginPx = 20f      // Distance from screen edge to outer habit layer
    val innerMarginPx = 40f      // Distance from screen center to inner habit layer

    // Define your three colors
    val darkBlue = Color(0xFF1565C0)      // Dark blue for completed
    val paleBlue = Color(0xFFBBDEFB)      // Pale blue for not completed
    val paleGrey = Color(0xFFE0E0E0)      // Pale grey for no data

    // Selection colors
    val lightGreen = Color(0xFF81C784)    // Light green for selected (no data or not completed)
    val darkGreen = Color(0xFF2E7D32)     // Dark green for selected (completed)

    val context = LocalContext.current
    val numDays = 10

    // Load habit data - reloads when currentDate changes
    var habitData by remember { mutableStateOf(loadHabitData(context, currentDate, numDays)) }

    // Mutable completions state
    var completions by remember { mutableStateOf(habitData.completions) }

    // Reload data when date changes
    LaunchedEffect(currentDate) {
        habitData = loadHabitData(context, currentDate, numDays)
        completions = habitData.completions
    }

    val density = LocalDensity.current
    val screenSize = 200.dp // Approximate watch screen size
    val focusRequester = remember { FocusRequester() }

    val numHabits = habitData.habits.size

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Monitor lifecycle events to check for date changes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                val newDate = LocalDate.now()
                if (newDate != currentDate) {
                    println("DATE HAS CHANGED!")
                    currentDate = newDate
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Save data whenever completions change
    LaunchedEffect(completions) {
        saveHabitDataToFile(
            context,
            HabitData(
                habits = habitData.habits,
                completions = completions
            ),
            currentDate,
            numDays
        )
    }

    Canvas(
        modifier = Modifier
            .size(screenSize)
            .onRotaryScrollEvent { event ->

                // Accumulate scroll delta
                scrollAccumulator += event.verticalScrollPixels

                // Check if we've accumulated enough to trigger a selection change
                if (scrollAccumulator >= scrollThreshold) {
                    // Scroll up: increase habit index
                    if (selectedHabitIndex < numHabits - 1) {
                        selectedHabitIndex++
                    } else {
                        // Wrapped to next day
                        selectedHabitIndex = 0
                        if (selectedDayIndex < numDays - 1) {
                            selectedDayIndex++
                        }
                    }
                    scrollAccumulator = 0f
                } else if (scrollAccumulator <= -scrollThreshold) {
                    // Scroll down: decrease habit index
                    if (selectedHabitIndex > 0) {
                        selectedHabitIndex--
                    } else {
                        // Wrapped to previous day
                        selectedHabitIndex = numHabits - 1
                        if (selectedDayIndex > 0) {
                            selectedDayIndex--
                        }
                    }
                    scrollAccumulator = 0f
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        // Find the selected habit's ID

                        val selectedHabit = habitData.habits.getOrNull(selectedHabitIndex)
                        if (selectedHabit != null) {
                            // Find existing completion
                            val existingCompletion = completions.find {
                                it.habitId == selectedHabit.id && it.dayIndex == selectedDayIndex
                            }

                            // Toggle completion status
                            val newCompletions = completions.toMutableList()
                            if (existingCompletion != null) {
                                newCompletions.remove(existingCompletion)
                                val newStatus = when (existingCompletion.isCompleted) {
                                    true -> false    // Completed -> Not completed
                                    else -> true     // No data or not completed -> Completed
                                }
                                newCompletions.add(
                                    existingCompletion.copy(isCompleted = newStatus)
                                )
                            } else {
                                // No existing entry, create new one as completed
                                newCompletions.add(
                                    HabitCompletion(
                                        habitId = selectedHabit.id,
                                        dayIndex = selectedDayIndex,
                                        isCompleted = true
                                    )
                                )
                            }
                            completions = newCompletions
                        }
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f - outerMarginPx

        drawHabitTracker(
            center = center,
            maxRadius = maxRadius,
            innerMargin = innerMarginPx,
            habits = habitData.habits,
            completions = completions,
            darkBlue = darkBlue,
            paleBlue = paleBlue,
            paleGrey = paleGrey,
            lightGreen = lightGreen,
            darkGreen = darkGreen,
            selectedDayIndex = selectedDayIndex,
            selectedHabitIndex = selectedHabitIndex
        )
    }
}

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

    // Parse habits (unchanged)
    val habitsArray = jsonObject.getJSONArray("habits")
    val habits = mutableListOf<Habit>()
    for (i in 0 until habitsArray.length()) {
        val habitJson = habitsArray.getJSONObject(i)
        val colorHex = habitJson.getString("colorHex")
        val color = Color(android.graphics.Color.parseColor(colorHex))
        habits.add(
            Habit(
                id = habitJson.getInt("id"),
                name = habitJson.getString("name"),
                color = color
            )
        )
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

        // Add habits array
        val habitsArray = JSONArray()
        habitData.habits.forEach { habit ->
            val habitJson = JSONObject()
            habitJson.put("id", habit.id)
            habitJson.put("name", habit.name)
            habitJson.put("colorHex", "#1565C0")
            habitsArray.put(habitJson)
        }
        jsonObject.put("habits", habitsArray)

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

fun DrawScope.drawHabitTracker(
    center: Offset,
    maxRadius: Float,
    innerMargin: Float,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    darkBlue: Color,
    paleBlue: Color,
    paleGrey: Color,
    lightGreen: Color,
    darkGreen: Color,
    selectedDayIndex: Int,
    selectedHabitIndex: Int
) {
    val numDays = 10
    val numHabits = habits.size

    // Arc configuration variables
    val startingAngle = -90f  // Start at 12 o'clock (vertical, top of screen)
    val gapAngle = 45f        // Gap size in degrees (adjustable)

    val totalArcAngle = 360f - gapAngle  // Available degrees for the days
    val segmentAngle = totalArcAngle / numDays  // Each day gets equal portion

    val availableRadius = maxRadius - innerMargin
    val habitLayerThickness = availableRadius / numHabits

    // Draw each habit layer (habit 0 is innermost, highest index is outermost)
    habits.forEachIndexed { habitIndex, habit ->
        val reversedIndex = numHabits - 1 - habitIndex
        val outerRadius = maxRadius - (reversedIndex * habitLayerThickness)
        val innerRadius = outerRadius - habitLayerThickness * 0.8f // Leave gap between layers

        // Draw each day segment for this habit
        for (dayIndex in 0 until numDays) {
            // Calculate angle for this day (going anticlockwise from starting position)
            val dayStartAngle = startingAngle - (dayIndex * segmentAngle)

            // Find completion status for this habit/day
            val completion = completions.find {
                it.habitId == habit.id && it.dayIndex == dayIndex
            }

            // Check if this is the selected segment
            val isSelected = (habitIndex == selectedHabitIndex && dayIndex == selectedDayIndex)

            val segmentColor = if (isSelected) {
                // Selected segment uses green colors
                when (completion?.isCompleted) {
                    true -> darkGreen     // Completed - dark green
                    else -> lightGreen    // No data or not completed - light green
                }
            } else {
                // Non-selected segments use original blue/grey colors
                when (completion?.isCompleted) {
                    null -> paleGrey      // No data - pale grey
                    true -> darkBlue      // Completed - dark blue
                    false -> paleBlue     // Not completed - pale blue
                }
            }

            // Draw the arc segment
            drawArc(
                color = segmentColor,
                startAngle = dayStartAngle,
                sweepAngle = -segmentAngle + 2f, // Negative for anticlockwise, small gap between segments
                useCenter = false,
                topLeft = Offset(
                    center.x - outerRadius,
                    center.y - outerRadius
                ),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = outerRadius - innerRadius)
            )
        }
    }
}