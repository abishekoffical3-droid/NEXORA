package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RealCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    completedDates: Set<String> = emptySet(),
    taskDueDates: Set<String> = emptySet()
) {
    var currentYearMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate))
    }

    val today = remember { LocalDate.now() }
    val daysOfWeek = remember {
        listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Month & Year Header with Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real Planning & Attendance Calendar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Jump to Today
                    IconButton(
                        onClick = {
                            currentYearMonth = YearMonth.from(today)
                            onDateSelected(today)
                        },
                        modifier = Modifier
                            .testTag("calendar_today_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Today",
                            tint = NexoraPurpleHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Previous Month
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier
                            .testTag("calendar_prev_month")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Next Month
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier
                            .testTag("calendar_next_month")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day of Week Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (dayOfWeek == DayOfWeek.SUNDAY || dayOfWeek == DayOfWeek.SATURDAY) {
                            NexoraPurpleHighlight
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of days
            val firstDayOfMonth = currentYearMonth.atDay(1)
            val firstDayOfWeekIndex = daysOfWeek.indexOf(firstDayOfMonth.dayOfWeek)
            val daysInMonth = currentYearMonth.lengthOfMonth()

            // Total grid cells needed (multiples of 7)
            val totalCells = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (weekIndex in 0 until totalCells / 7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayCol in 0 until 7) {
                            val cellIndex = weekIndex * 7 + dayCol
                            val dayNumber = cellIndex - firstDayOfWeekIndex + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentYearMonth.atDay(dayNumber)
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == today
                                val dateStr = cellDate.toString()
                                val hasCompleted = completedDates.contains(dateStr)
                                val hasTaskDue = taskDueDates.contains(dateStr)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> NexoraPurplePrimary
                                                isToday -> NexoraPurpleHighlight.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                            color = if (isToday && !isSelected) NexoraPurpleHighlight else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onDateSelected(cellDate) }
                                        .testTag("calendar_day_$dateStr"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> NexoraPurpleHighlight
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )

                                        // Attendance & Task indicators
                                        if (hasCompleted || hasTaskDue) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                if (hasCompleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else NexoraSuccessGreen)
                                                    )
                                                }
                                                if (hasTaskDue) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else NexoraPurpleHighlight)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Empty spacer cell for days outside the month
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NexoraSuccessGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Attendance / Habit Done",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NexoraPurpleHighlight)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Scheduled Tasks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
