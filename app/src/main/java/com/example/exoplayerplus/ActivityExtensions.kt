package com.example.exoplayerplus

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch


open class PiPActivity(val activity: ComponentActivity) : DefaultLifecycleObserver {

    fun initPip()
    {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d("TAG", "onCreate: OnCreate")
        activity.apply {
            addOnPictureInPictureModeChangedListener {
                if (it.isInPictureInPictureMode)
                {
                    lifecycleScope.invokeEventBus(PipEvents.IS_IN_PIP_MODE)
                }
                else{
                    lifecycleScope.invokeEventBus(PipEvents.IS_NOT_IN_PIP_MODE)
                }
            }

            onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (activity.doesSupportPip())
                    {
                        Log.d("TAG", "handleOnBackPressed: ")
                        activity.initPip()
                    }
                }

            })
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (activity.doesSupportPip())
        {
            Log.d("TAG", "handleOnBackPressed: ")
            activity.initPip()
        }
    }




   /* override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        addOnPictureInPictureModeChangedListener {
            if (it.isInPictureInPictureMode)
            {
                lifecycleScope.invokeEventBus(PipEvents.IS_IN_PIP_MODE)
            }
            else{
                lifecycleScope.invokeEventBus(PipEvents.IS_NOT_IN_PIP_MODE)
            }
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activity.doesSupportPip())
                {
                    Log.d("TAG", "handleOnBackPressed: ")
                    activity.initPip()
                }
            }

        })
    }

    override fun onActivityStarted(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityResumed(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityPaused(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        TODO("Not yet implemented")
    }


    override fun onUserLeaveHint() {
        if (doesSupportPip()) {
            lifecycleScope.launch {
                EventBus.invokeEvent(PipEvents.ENABLE_PIP_MODE)
            }
        }
    }*/

}



private fun Activity.checkPipMode(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycleScope.launch {
        EventBus.events.collectLatest { event ->
            if (event ==  Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && this@checkPipMode.isInPictureInPictureMode) {
                finishAndRemoveTask()
            }
        }
    }
}


fun Activity.initPip(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
        )
    }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
        this.enterPictureInPictureMode()
    }else{
       // Before that no PIP Mode
    }

}

fun Context.doesSupportPip(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val packageManager = this.applicationContext?.packageManager
        packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) ?: false
    } else {
        false
    }
}


enum class PipEvents {
    ENABLE_PIP_MODE, IS_IN_PIP_MODE, IS_NOT_IN_PIP_MODE
}

object EventBus {
    private val sharedEvents = MutableSharedFlow<Any>()
    val events = sharedEvents.asSharedFlow()
    suspend fun invokeEvent(event: Any) = sharedEvents.emit(event)

    @OptIn(DelicateCoroutinesApi::class)
    suspend inline fun <reified T> subscribe(crossinline onEvent: (T) -> Unit) {
        events.filterIsInstance<T>().collectLatest { event ->
            GlobalScope.coroutineContext.ensureActive()
            onEvent(event)
        }
    }
}

fun LifecycleCoroutineScope.invokeEventBus(event:Any)
{
    this.launch {
        EventBus.invokeEvent(event)
    }
}