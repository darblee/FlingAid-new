package com.darblee.flingaid.utilities

import android.content.Context
import android.content.res.Resources
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
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

/**
 * Return a float number within a range of data
 */
fun Float.mapInRange(inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
    return outMin + (((this - inMin) / (inMax - inMin)) * (outMax - outMin))
}

/**
 * Convert from dp to Px
 */
fun Dp.toPx() = value.dpToPx()

/**
 * Convert float to Px
 */
fun Float.dpToPx() = this * Resources.getSystem().displayMetrics.density


private val random = Random

/**
 * Generate a random number
 */
fun Float.randomTillZero() = this * random.nextFloat()

/**
 * Generate a random float number between [min] and [max]
 *
 * @param min
 * @param max
 *
 * @return Random number between  [min] and [max]
 */
fun randomInRange(min: Float, max: Float) = min + (max - min).randomTillZero()

/**
 * Percentage of time to return "true"
 *
 * @param trueProbabilityPercentage Probability to return "true"
 */
fun randomBoolean(trueProbabilityPercentage: Int) =
    random.nextFloat() < trueProbabilityPercentage / 100f

/**
 * Perform haptic feedback.
 */
fun View.click() = run {
    this.let { this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)}
    this.playSoundEffect(SoundEffectConstants.CLICK)
}

/********************************* Singleton helper functions *************************************/

/**
 * SingletonHolder - This is used to pass parameter to Singleton class
 */
open class SingletonHolder<out T : Any, in A>(creator: (A) -> T) {

    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null
    protected fun getInstanceInternal(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) return checkInstance
        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) checkInstanceAgain
            else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

/**
 * If you need to pass only ONE argument to the constructor of the singleton class.
 * Make companion object extended from [SingleArgSingletonHolder] for best match.
 * Ex:
class AppRepository private constructor(private val db: Database) {
companion object : SingleArgSingletonHolder<AppRepository, Database>(::AppRepository)
}

 * Uses:
val appRepository =  AppRepository.getInstance(db)
 */
open class SingleArgSingletonHolder<out T : Any, in A>(creator: (A) -> T) :
    SingletonHolder<T, A>(creator) {
    fun getInstance(arg: A): T = getInstanceInternal(arg)
}


/**
 * If you need to pass TWO arguments to the constructor of the singleton class.
 * Extended from [PairArgsSingletonHolder] for best match.
 * Ex:
class AppRepository private constructor(private val db: Database, private val apiService: ApiService) {
companion object : PairArgsSingletonHolder<AppRepository, Database, ApiService>(::AppRepository)
}
 *
 * Uses:
val appRepository =  AppRepository.getInstance(db, apiService)
 */
open class PairArgsSingletonHolder<out T : Any, in A, in B>(creator: (A, B) -> T) :
    SingletonHolder<T, Pair<A, B>>(creator = { (a, b) -> creator(a, b) }) {
    fun getInstance(arg1: A, arg2: B) = getInstanceInternal(Pair(arg1, arg2))
}

