package io.github.sgpublic.callpilot

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.sgpublic.android.Application.Companion.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity: AppCompatActivity(), ServiceConnection {
    private lateinit var batteryIntent: ActivityResultLauncher<Intent>
    private lateinit var notifyPriorityIntent: ActivityResultLauncher<Intent>
    private lateinit var permissionIntent: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindService(Intent(this, CallPilotService::class.java), this, Context.BIND_AUTO_CREATE)
        batteryIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                delay(200)
                checkBattery()
            }
        }
        notifyPriorityIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                delay(200)
                checkNotifyPriority()
            }
        }
        permissionIntent = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            var allGrant = true
            for ((permission, grant) in it) {
                if (grant == false) {
                    allGrant = false
                    break
                }
            }
            if (allGrant) {
                Toast(R.string.step_permission_granted)
            } else {
                Toast(R.string.step_permission_not_granted)
            }
        }

        setContent {
            MaterialTheme {
                MainView()
            }
        }

        lifecycleScope.launch {
            setPermission()
            checkBattery()
            checkNotifyPriority()
        }
    }

    private var batteryIgnored: Boolean by mutableStateOf(false)
    private var notifyPriority: Boolean by mutableStateOf(false)

    @Composable
    fun MainView() = Scaffold {
       Column(
           modifier = Modifier
               .padding(it)
               .fillMaxSize(),
       ) {
           Text(
               text = getString(when (CallPilotService.Status) {
                   CallPilotService.Status.Stopped -> R.string.service_not_running
                   CallPilotService.Status.Idle -> R.string.service_prepare
                   CallPilotService.Status.Running -> R.string.service_running
                   CallPilotService.Status.Interrupted -> R.string.service_error
               }),
               fontSize = 32.sp,
               modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
           )
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier
                   .padding(top = 16.dp)
                   .fillMaxWidth()
                   .height(64.dp)
                   .clickable { setBattery() }
                   .padding(horizontal = 16.dp),
           ) {
               Text(
                   text = getString(R.string.step_ignore_battery),
                   modifier = Modifier.weight(1f),
                   fontSize = 20.sp,
               )

               Switch(
                   checked = batteryIgnored,
                   onCheckedChange = { setBattery() },
               )
           }
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier
                   .fillMaxWidth()
                   .height(64.dp)
                   .clickable { setNotifyPriority() }
                   .padding(horizontal = 16.dp),
           ) {
               Text(
                   text = getString(R.string.step_notify_priority),
                   modifier = Modifier.weight(1f),
                   fontSize = 20.sp,
               )

               Switch(
                   checked = notifyPriority,
                   onCheckedChange = { setNotifyPriority() },
               )
           }
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier
                   .fillMaxWidth()
                   .height(64.dp)
                   .clickable { setPermission() }
                   .padding(horizontal = 16.dp),
           ) {
               Text(
                   text = getString(R.string.step_permission),
                   modifier = Modifier.weight(1f),
                   fontSize = 20.sp,
               )

               Icon(
                   imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                   contentDescription = null,
                   modifier = Modifier.size(32.dp),
               )
           }
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier
                   .fillMaxWidth()
                   .height(64.dp)
                   .clickable { setAccessibility() }
                   .padding(horizontal = 16.dp),
           ) {
               Text(
                   text = getString(R.string.step_accessibility),
                   modifier = Modifier.weight(1f),
                   fontSize = 20.sp,
               )

               Icon(
                   imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                   contentDescription = null,
                   modifier = Modifier.size(32.dp),
               )
           }
       }
    }

    private fun setBattery() {
        batteryIntent.launch(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
        )
    }
   private fun checkBattery() {
       val powerManager = getSystemService(PowerManager::class.java)
       batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) == true
   }

    private fun setAccessibility() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun setPermission() {
        permissionIntent.launch(arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        ))
    }

    private fun setNotifyPriority() {
        notifyPriorityIntent.launch(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        )
    }
    private fun checkNotifyPriority() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notifyPriority = notificationManager.isNotificationPolicyAccessGranted()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

    }

    override fun onServiceDisconnected(name: ComponentName?) {

    }

    fun LifecycleOwner.Toast(@StringRes content: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            val content = getString(content)
            android.widget.Toast.makeText(
                this@MainActivity, content,
                if (content.length > 10) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
