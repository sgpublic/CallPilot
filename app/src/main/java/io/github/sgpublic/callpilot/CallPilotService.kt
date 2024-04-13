package io.github.sgpublic.callpilot

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sgpublic.android.core.sysservice.sysAudioManager

class CallPilotService: AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        checkVolume()
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
        val handsFree = source.findAccessibilityNodeInfosByViewId("com.android.incallui:id/audioButton")
            .firstOrNull() ?: return
        Log.d("CallPilotService#clickHandsFree", "clickHandsFree: $handsFree")
        if (!handsFree.isChecked) {
            Log.i("CallPilotService#clickHandsFree", "perform click!")
            handsFree.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun onInterrupt() {

    }

    private val audioManager: AudioManager by lazy { sysAudioManager }
    private val streamVoiceCallIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_VOICE_CALL)
    private val streamNotificationIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_NOTIFICATION)
    private val streamRingIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_RING)
    private val streamSystemIsMax: Boolean get() = checkTargetVolume(AudioManager.STREAM_SYSTEM)
    private fun checkTargetVolume(type: Int): Boolean {
        try {
            if (audioManager.getStreamVolume(type) ==
                audioManager.getStreamMaxVolume(type)) {
                return true
            }
            audioManager.adjustStreamVolume(type, AudioManager.ADJUST_RAISE, 0)
            return false
        } catch (e: Exception) {
            Log.w("CallPilotService#checkTargetVolume", "cannot adjust volume: $type", e)
            return true
        }
    }
    private fun checkVolume() {
        while (!streamVoiceCallIsMax) {
            Log.i("CallPilotService#checkVolume", "streamVoiceCall is not max, adjust...")
        }
        while (!streamNotificationIsMax) {
            Log.i("CallPilotService#checkVolume", "streamNotification is not max, adjust...")
        }
        while (!streamRingIsMax) {
            Log.i("CallPilotService#checkVolume", "streamRing is not max, adjust...")
        }
        while (!streamSystemIsMax) {
            Log.i("CallPilotService#checkVolume", "streamSystem is not max, adjust...")
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
}