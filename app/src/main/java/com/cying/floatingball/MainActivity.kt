package com.cying.floatingball

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity

/**
 * Created by Cying on 17/9/27.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dpm = getDevicePolicyManager()
        val deviceComponent = ComponentName(this, LockReceiver::class.java)
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

        mock_home.setOnClickListener {
            MockAction.HOME.trigger()
        }

        mock_back.setOnClickListener {
            MockAction.BACK.trigger()
        }

        mock_recents.setOnClickListener {
            MockAction.RECENTS.trigger()
        }
        mock_lock.setOnClickListener {
            MockAction.LOCK.trigger()
        }
    }

}