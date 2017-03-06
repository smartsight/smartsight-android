package com.smartsight.app.util

import android.content.Context
import android.widget.Toast

fun showToast(context: Context, resId: Int) =
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
