package com.cying.floatingball

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.action_container.view.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by Cying on 17/9/27.
 */
const val CMD1 = "settings put secure enabled_accessibility_services com.cying.floatingball/com.cying.floatingball.FloatingBallService"
const val CMD2 = "settings put secure accessibility_enabled 1"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val deviceComponent = ComponentName(this, LockReceiver::class.java)

        val dpm = getDevicePolicyManager()
        long_press_vibrator.isChecked = ActionSettings.needVibrate
        long_press_vibrator.setOnCheckedChangeListener { _, isChecked -> ActionSettings.needVibrate = isChecked }
        open_device_permission.setOnClickListener {
            if (!dpm.isAdminActive(deviceComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceComponent)
                }
                startActivity(intent)
            }
        }

        open_alert_permission.setOnClickListener {
            val context = it.context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) run {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName))
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }

        open_accessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            if (intent.resolveActivity(it.context.packageManager) != null) {
                it.context.startActivity(intent)
            }
        }

        val mockActionArray = MockAction.getActionArray()

        arrayOf(action_click to GESTURE.CLICK,
                action_double_click to GESTURE.DOUBLE_CLICK,
                action_swipe_left to GESTURE.SWIPE_LEFT,
                action_swipe_top to GESTURE.SWIPE_TOP,
                action_swipe_right to GESTURE.SWIPE_RIGHT,
                action_swipe_bottom to GESTURE.SWIPE_BOTTOM)
                .forEach { (view, gesture) ->
                    view.action_name.text = gesture.label
                    view.action_value.setSelection(mockActionArray.indexOf(gesture.action))
                    view.action_value.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, itemView: View?, position: Int, id: Long) {
                            gesture.action = mockActionArray[position]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }
                    }
                }

    }


    override fun onResume() {
        super.onResume()
        val deviceComponent = ComponentName(this, LockReceiver::class.java)
        /* if (!isFloatingBallServiceEnabled(this)) {
             //execCommand("reboot")
             //execCommand(CMD1)
             //execCommand(CMD2)

         }*/
        // open_accessibility.visibility = if (isFloatingBallServiceEnabled(this)) View.GONE else View.VISIBLE
        open_device_permission.visibility = if (getDevicePolicyManager().isAdminActive(deviceComponent)) View.GONE else View.VISIBLE
        open_alert_permission.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) View.GONE else View.VISIBLE

    }
}

fun execCommand(cmd: String): Boolean {
    Log.i("execCommand", "cmd= $cmd")
    val process = Runtime.getRuntime().exec(cmd)
    process.inputStream.bufferedReader().use {
        it.readLines().forEach { Log.i("execCommand-SUCCESS", it) }
    }
    process.errorStream.bufferedReader().use {
        it.readLines().forEach { Log.i("execCommand-ERROR", it) }
    }
    process.destroy()
    return false
}

