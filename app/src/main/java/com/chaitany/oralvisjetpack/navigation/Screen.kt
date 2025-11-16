package com.chaitany.oralvisjetpack.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Welcome : Screen("welcome") {
        fun createRoute() = "welcome"
    }
    object PatientEntry : Screen("patient_entry/{clinicId}") {
        fun createRoute(clinicId: String) = "patient_entry/$clinicId"
    }
    object ImageSequence : Screen("image_sequence/{folderName}/{clinicId}/{patientId}") {
        fun createRoute(folderName: String, clinicId: String, patientId: Int) = 
            "image_sequence/$folderName/$clinicId/$patientId"
    }
    object History : Screen("history/{clinicId}") {
        fun createRoute(clinicId: String) = "history/$clinicId"
    }
    object SessionDetail : Screen("session_detail/{patientId}/{clinicId}") {
        fun createRoute(patientId: String, clinicId: String) = "session_detail/$patientId/$clinicId"
    }
}

