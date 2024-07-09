package com.darblee.flingaid.utilities

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.ui.unit.Dp
import kotlin.random.Random

/**
 * Toast message. Short length.
 *
 * @param context The context to use
 * @param message String based message to show
 * @param displayLonger indicate if you need to display longer time length. If this is not provided,
 * it will use a shorter time length.
 */
fun gameToast(context: Context, message: String, displayLonger: Boolean = false) {
    if (displayLonger)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    else
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun Float.mapInRange(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
    return outMin + (((this - inMin) / (inMax - inMin)) * (outMax - outMin))
}

fun Dp.toPx() = value.dpToPx()

fun Float.dpToPx() = this * Resources.getSystem().displayMetrics.density


private val random = Random
fun Float.randomTillZero() = this * random.nextFloat()
fun randomInRange(min:Float,max:Float) = min + (max - min).randomTillZero()

/**
 * Percentage of time to return "true"
 *
 * @param trueProbabilityPercentage Probability to return "true"
 */
fun randomBoolean(trueProbabilityPercentage: Int) = random.nextFloat() < trueProbabilityPercentage/100f