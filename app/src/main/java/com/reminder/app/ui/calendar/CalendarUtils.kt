package com.reminder.app.ui.calendar

import java.time.LocalDate
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

enum class CalendarViewType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

object CalendarUtils {
    
    fun getMonthText(date: LocalDate): String {
        return date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    }
    
    fun getDaysInMonth(date: LocalDate): List<Int> {
        return (1..date.lengthOfMonth()).toList()
    }
    
    fun getWeekStart(date: LocalDate): LocalDate {
        return date.with(DayOfWeek.MONDAY)
    }
    
    fun getWeekDays(date: LocalDate): List<Int> {
        val weekStart = getWeekStart(date)
        return (0..6).map { weekStart.plusDays(it.toLong()).dayOfMonth }
    }
    
    fun getMonthsInYear(): List<String> {
        return listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                   "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}