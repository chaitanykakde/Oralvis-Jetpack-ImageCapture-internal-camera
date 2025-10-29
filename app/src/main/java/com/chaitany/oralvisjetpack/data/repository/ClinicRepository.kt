package com.chaitany.oralvisjetpack.data.repository

import com.chaitany.oralvisjetpack.data.dao.ClinicDao
import com.chaitany.oralvisjetpack.data.model.Clinic
import kotlinx.coroutines.flow.Flow

class ClinicRepository(private val clinicDao: ClinicDao) {

    val clinic: Flow<Clinic?> = clinicDao.getClinicFlow()

    suspend fun getClinic(): Clinic? {
        return clinicDao.getClinic()
    }

    suspend fun insertClinic(clinic: Clinic) {
        clinicDao.deleteAllClinics() // Ensure only one clinic exists
        clinicDao.insertClinic(clinic)
    }

    suspend fun deleteAllClinics() {
        clinicDao.deleteAllClinics()
    }
}

