package ua.safetube.kids.parental

import android.app.Activity
import android.app.ActivityManager
import android.content.Context

/**
 * Android Screen Pinning: поки застосунок "приколотий", дитина не може вийти на
 * головний екран/у інший застосунок кнопками системи — тільки через PIN батьків
 * (див. екран налаштувань, який викликає stopLockTask() після ParentalControls.verifyPin).
 */
object ScreenPinHelper {

    fun isPinned(activity: Activity): Boolean {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun pin(activity: Activity) {
        if (!isPinned(activity)) activity.startLockTask()
    }

    fun unpin(activity: Activity) {
        if (isPinned(activity)) activity.stopLockTask()
    }
}
