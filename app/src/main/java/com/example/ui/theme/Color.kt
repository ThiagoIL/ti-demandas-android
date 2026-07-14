package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

var isDarkThemeGlobal by mutableStateOf(true)

val BgDark: Color
    get() = if (isDarkThemeGlobal) Color(0xFF050811) else Color(0xFFF8FAFC)

val SurfaceDark: Color
    get() = if (isDarkThemeGlobal) Color(0xFF0E1324) else Color(0xFFFFFFFF)

val SurfaceDarkVariant: Color
    get() = if (isDarkThemeGlobal) Color(0xFF161E36) else Color(0xFFF1F5F9)

val BlueAccent = Color(0xFF1E69FF)

val PriorityHigh = Color(0xFFEF4444)
val PriorityNormal = Color(0xFF3B82F6)
val PriorityLow = Color(0xFF64748B)

val StatusCompleted = Color(0xFF10B981)
val StatusPending = Color(0xFFF59E0B)

val TextWhite: Color
    get() = if (isDarkThemeGlobal) Color(0xFFFFFFFF) else Color(0xFF0F172A)

val TextGray: Color
    get() = if (isDarkThemeGlobal) Color(0xFF94A3B8) else Color(0xFF64748B)

val TextGrayLight: Color
    get() = if (isDarkThemeGlobal) Color(0xFFCBD5E1) else Color(0xFF334155)

