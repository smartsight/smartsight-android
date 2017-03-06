package com.smartsight.app.util

import android.hardware.Camera

@Throws(Exception::class)
fun createCamera(): Camera {
    val camera: Camera = Camera.open()
    val parameters: Camera.Parameters = camera.parameters

    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
    camera.parameters = parameters

    return camera
}

fun restartCamera(camera: Camera) = camera.startPreview()
