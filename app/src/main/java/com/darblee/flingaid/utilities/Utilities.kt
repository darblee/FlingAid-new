package com.darblee.flingaid.utilities

import android.content.Context
import android.widget.Toast

/**
 * Toast message. Short length.
 *
 * @param context The context to use
 * @param message String based message to show
 */
fun gameToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}