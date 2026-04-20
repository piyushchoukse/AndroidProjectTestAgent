package com.example.androidtestagent

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry-point required by Hilt for dependency injection.
 * Must be declared in AndroidManifest.xml via android:name.
 */
@HiltAndroidApp
class MyApplication : Application()
