package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_apps")
data class ExcludedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String
)
