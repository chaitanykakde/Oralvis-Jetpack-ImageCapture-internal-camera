package com.chaitany.oralvisjetpack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_counter")
data class PatientCounter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val counter: Int
)

