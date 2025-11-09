package com.chaitany.oralvisjetpack.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object ClinicEntry : Screen("clinic_entry")
    object Welcome : Screen("welcome/{clinicName}/{clinicId}") {
        fun createRoute(clinicName: String, clinicId: Int) = "welcome/$clinicName/$clinicId"
    }
    object PatientEntry : Screen("patient_entry/{clinicName}/{clinicId}") {
        fun createRoute(clinicName: String, clinicId: Int) = "patient_entry/$clinicName/$clinicId"
    }
    object ImageSequence : Screen("image_sequence/{folderName}/{clinicId}/{patientId}") {
        fun createRoute(folderName: String, clinicId: Int, patientId: Int) = 
            "image_sequence/$folderName/$clinicId/$patientId"
    }
    object History : Screen("history/{clinicId}") {
        fun createRoute(clinicId: Int) = "history/$clinicId"
    }
    object SessionDetail : Screen("session_detail/{patientId}/{clinicId}") {
        fun createRoute(patientId: String, clinicId: Int) = "session_detail/$patientId/$clinicId"
    }
}

