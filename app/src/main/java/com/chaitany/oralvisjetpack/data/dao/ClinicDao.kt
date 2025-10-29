package com.chaitany.oralvisjetpack.data.dao

import androidx.room.*
import com.chaitany.oralvisjetpack.data.model.Clinic
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicDao {
    @Query("SELECT * FROM clinic LIMIT 1")
    suspend fun getClinic(): Clinic?

    @Query("SELECT * FROM clinic LIMIT 1")
    fun getClinicFlow(): Flow<Clinic?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClinic(clinic: Clinic)

    @Query("DELETE FROM clinic")
    suspend fun deleteAllClinics()
}

