package com.smartsight.app.util

import android.content.Context
import android.widget.Toast

fun showToast(context: Context, resId: Int, length: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(context, resId, length).show()
