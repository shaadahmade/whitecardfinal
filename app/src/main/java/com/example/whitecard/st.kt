package com.example.whitecard

import android.app.Application
import com.example.whitecard.firebase.FirestoreManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirestoreManager.initialize(this)
        // Initialize any app-wide components here
    }
}

