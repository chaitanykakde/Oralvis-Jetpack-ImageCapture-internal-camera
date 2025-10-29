package com.chaitany.oralvisjetpack.data.dao

import androidx.room.*
import com.chaitany.oralvisjetpack.data.model.PatientCounter
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientCounterDao {
    @Query("SELECT * FROM patient_counter LIMIT 1")
    suspend fun getPatientCounter(): PatientCounter?

    @Query("SELECT * FROM patient_counter LIMIT 1")
    fun getPatientCounterFlow(): Flow<PatientCounter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatientCounter(counter: PatientCounter)

    @Update
    suspend fun updatePatientCounter(counter: PatientCounter)

    @Query("UPDATE patient_counter SET counter = :newCounter WHERE id = 1")
    suspend fun updateCounter(newCounter: Int)
}

