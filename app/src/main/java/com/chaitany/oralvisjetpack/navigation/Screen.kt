package com.chaitany.oralvisjetpack.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Welcome : Screen("welcome") {
        fun createRoute() = "welcome"
    }
    object PatientEntry : Screen("patient_entry/{clinicId}") {
        fun createRoute(clinicId: Int) = "patient_entry/$clinicId"
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

