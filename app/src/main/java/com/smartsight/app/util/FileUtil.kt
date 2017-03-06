package com.smartsight.app.util

import android.content.Context
import com.smartsight.app.data.SM_PHOTO_NAME
import com.smartsight.app.data.Prefs
import java.io.File
import java.io.IOException


fun getOutputMediaFile(context: Context): File {
    val mediaStorageDir = File(Prefs.newInstance(context).savePhotosFolder)

    if (!mediaStorageDir.exists()) {
        if (!mediaStorageDir.mkdirs()) {
            throw IOException()
        }
    }

    return File("${mediaStorageDir.path}${File.separator}$SM_PHOTO_NAME")
}
