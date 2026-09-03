// Tells Room how to store our enums (as their name string) and read them back.
package com.example.asgm.data.local

import androidx.room.TypeConverter
import com.example.asgm.data.local.entity.AlertPriority
import com.example.asgm.data.local.entity.ReportStatus
import com.example.asgm.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromReportStatus(status: ReportStatus): String = status.name

    @TypeConverter
    fun toReportStatus(value: String): ReportStatus = ReportStatus.valueOf(value)

    @TypeConverter
    fun fromAlertPriority(priority: AlertPriority): String = priority.name

    @TypeConverter
    fun toAlertPriority(value: String): AlertPriority = AlertPriority.valueOf(value)
}
