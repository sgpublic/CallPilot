package io.github.sgpublic.callpilot

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sgpublic.android.core.sysservice.sysAudioManager

class CallPilotService: AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        Status = CallPilotService.Status.Idle
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        checkVolume()
        Status = CallPilotService.Status.Running
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.v("CallPilotService", "onAccessibilityEvent\n" +
                "  eventType: ${AccessibilityEvent.eventTypeToString(event.eventType)}\n" +
                "  packageName: ${event.packageName}")
        if (event.packageName == "com.android.incallui") {
            if (event.source?.also(::clickHandsFree) == null) {
                Log.w("CallPilotService", "event.source is null!")
            }
        }
    }

    /** 开启免提 */
    private fun clickHandsFree(source: AccessibilityNodeInfo) {
        try {
            val handsFree = source.findAccessibilityNodeInfosByViewId("com.android.incallui:id/audioButton")
                .firstOrNull() ?: return
            Log.d("CallPilotService#clickHandsFree", "clickHandsFree: $handsFree")
            if (!handsFree.isChecked) {
                Log.i("CallPilotService#clickHandsFree", "perform click!")
                handsFree.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } catch (e: Exception) {
            Log.e("CallPilotService#clickHandsFree", "error!", e)
        }
    }

    override fun onInterrupt() {
        Status = CallPilotService.Status.Interrupted
    }

    private val audioManager: AudioManager by lazy { sysAudioManager }
    private val streamVoiceCallIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_VOICE_CALL)
    private val streamNotificationIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_NOTIFICATION)
    private val streamAlarmIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_ALARM)
    private val streamRingIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_RING)
    private val streamSystemIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_SYSTEM)
    private val streamVoiceMusicIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_MUSIC)
    private fun checkTargetVolume(type: Int): Boolean {
        try {
            val max = audioManager.getStreamMaxVolume(type)
            if (audioManager.getStreamVolume(type) != max) {
                Log.i("CallPilotService#checkTargetVolume", "volume value is not max, set to max: $max")
                audioManager.setStreamVolume(type, max, 0)
            } else {
                Log.i("CallPilotService#checkTargetVolume", "volume value is already max: $max")
            }
            return true
        } catch (e: Exception) {
            Log.e("CallPilotService#checkTargetVolume", "cannot adjust volume: $type", e)
            return false
        }
    }
    private fun checkVolume() {
        repeatIf(5, {!streamVoiceCallIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamVoiceCall is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamVoiceCall adjust failed!")
            }
        }
        repeatIf(5, {!streamNotificationIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamSystem is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamSystem adjust failed!")
            }
        }
        repeatIf(5, {!streamAlarmIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamAlarm is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamAlarm adjust failed!")
            }
        }
        repeatIf(5, {!streamRingIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamRing is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamRing adjust failed!")
            }
        }
        repeatIf(5, {!streamSystemIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamSystem is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamSystem adjust failed!")
            }
        }
        repeatIf(5, {!streamVoiceMusicIsMax}) {
            if (it < 4) {
                Log.i("CallPilotService#checkVolume", "streamMusic is not max, adjust...")
            } else {
                Log.e("CallPilotService#checkVolume", "streamMusic adjust failed!")
            }
        }
    }

    private fun repeatIf(maxTime: Int, arg: () -> Boolean, block: (Int) -> Unit) {
        for (time in 0 until maxTime) {
            if (!arg()) {
                break
            }
            block(time)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("CallPilotService", "onKeyEvent: ${KeyEvent.keyCodeToString(event.keyCode)}")
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                checkVolume()
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Status = CallPilotService.Status.Stopped
    }

    enum class Status {
        Stopped,
        Idle,
        Running,
        Interrupted,
    }

    companion object {
        var Status: Status by mutableStateOf(CallPilotService.Status.Stopped)
    }
}