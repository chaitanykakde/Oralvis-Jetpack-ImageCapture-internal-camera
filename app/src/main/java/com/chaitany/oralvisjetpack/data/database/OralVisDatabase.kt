package com.chaitany.oralvisjetpack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chaitany.oralvisjetpack.data.dao.ClinicDao
import com.chaitany.oralvisjetpack.data.dao.PatientCounterDao
import com.chaitany.oralvisjetpack.data.model.Clinic
import com.chaitany.oralvisjetpack.data.model.PatientCounter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Clinic::class, PatientCounter::class],
    version = 1,
    exportSchema = false
)
abstract class OralVisDatabase : RoomDatabase() {

    abstract fun clinicDao(): ClinicDao
    abstract fun patientCounterDao(): PatientCounterDao

    companion object {
        @Volatile
        private var INSTANCE: OralVisDatabase? = null

        fun getDatabase(context: Context): OralVisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OralVisDatabase::class.java,
                    "oralvis_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize patient counter to 1
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.patientCounterDao().insertPatientCounter(
                                        PatientCounter(id = 1, counter = 1)
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

