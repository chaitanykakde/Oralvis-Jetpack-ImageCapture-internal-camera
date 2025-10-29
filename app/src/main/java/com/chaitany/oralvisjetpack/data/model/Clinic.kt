package com.chaitany.oralvisjetpack.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clinic")
data class Clinic(
    @PrimaryKey
    val id: Int,
    val name: String,
    val clinicId: Int
)

