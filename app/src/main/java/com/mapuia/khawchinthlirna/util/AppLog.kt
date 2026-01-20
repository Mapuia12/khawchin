package com.mapuia.khawchinthlirna.util

import android.util.Log
import com.mapuia.khawchinthlirna.BuildConfig

/**
 * Application-wide logging utility.
 * 
 * Only logs in DEBUG builds to avoid:
 * - Performance overhead in production
 * - Potential data exposure in release builds
 * - Log spam in user devices
 */
object AppLog {
    
    /** Debug level log - only in debug builds */
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }
    
    /** Info level log - only in debug builds */
    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg)
        }
    }
    
    /** Warning level log - only in debug builds */
    fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg)
        }
    }
    
    /** Error level log - only in debug builds */
    fun e(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg)
        }
    }
    
    /** Error level log with throwable - only in debug builds */
    fun e(tag: String, msg: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg, throwable)
        }
    }
}
