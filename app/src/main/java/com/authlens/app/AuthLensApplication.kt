package com.authlens.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

/**
 * Application entry point.
 *
 * Initializes the OpenCV native library as early as possible so that all image
 * processing analyzers (ELA, noise, template matching) are ready to use.
 *
 * Annotated with [HiltAndroidApp] so the Hilt dependency graph is built once on startup.
 */
@HiltAndroidApp
class AuthLensApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initOpenCv()
    }

    /**
     * Loads the OpenCV native library. We log the result but never crash the app if it
     * fails — analyzers handle a missing OpenCV backend gracefully (they will report
     * "unable to analyze" rather than throwing).
     */
    private fun initOpenCv() {
        val loaded = runCatching { OpenCVLoader.initLocal() }.getOrDefault(false)
        isOpenCvLoaded = loaded
    }

    companion object {
        /** Whether the OpenCV native library was successfully loaded on app start. */
        @Volatile
        var isOpenCvLoaded: Boolean = false
            private set
    }
}
