package com.smartsight.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment

class Prefs(context: Context) {
    private val mPrefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    companion object {
        fun newInstance(context: Context) = Prefs(context)
    }

    var savePhotosFolder: String
        get() = mPrefs.getString(SAVE_PHOTOS, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString())
        set(path) = mPrefs.edit().putString(SAVE_PHOTOS, path).apply()
}
