package com.chaitany.oralvisjetpack.data.repository

import com.chaitany.oralvisjetpack.data.dao.PatientCounterDao
import com.chaitany.oralvisjetpack.data.model.PatientCounter
import kotlinx.coroutines.flow.Flow

class PatientCounterRepository(private val patientCounterDao: PatientCounterDao) {

    val patientCounter: Flow<PatientCounter?> = patientCounterDao.getPatientCounterFlow()

    suspend fun getPatientCounter(): Int {
        return patientCounterDao.getPatientCounter()?.counter ?: 1
    }

    suspend fun updatePatientCounter(newCounter: Int) {
        val counter = patientCounterDao.getPatientCounter()
        if (counter != null) {
            patientCounterDao.updateCounter(newCounter)
        } else {
            patientCounterDao.insertPatientCounter(PatientCounter(id = 1, counter = newCounter))
        }
    }

    suspend fun incrementCounter() {
        val currentCounter = getPatientCounter()
        updatePatientCounter(currentCounter + 1)
    }
}

