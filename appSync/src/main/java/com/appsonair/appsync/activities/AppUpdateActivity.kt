package com.appsonair.appsync.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.appsonair.appsync.R
import org.json.JSONException
import org.json.JSONObject

class AppUpdateActivity : AppCompatActivity() {
    private var activityClose: Boolean = false
    private lateinit var callback: OnBackPressedCallback

    @SuppressLint("NewApi", "DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_update)
        try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle1 = ai.metaData
            var icon = 0

            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(applicationInfo) as String

            if (bundle1 != null) {
                icon = bundle1.getInt("com.appsonair.icon")
            }

            val bundle = this.intent.extras
            val data = bundle!!.getString("res") // NullPointerException.
            val jsonObject = JSONObject(data.toString())
            if (jsonObject.isNull(data)) {
                val updateData = jsonObject.getJSONObject("updateData")
                val isAndroidForcedUpdate = updateData.getBoolean("isAndroidForcedUpdate")
                val isAndroidUpdate = updateData.getBoolean("isAndroidUpdate")
                val androidBuildNumber = updateData.getString("androidBuildNumber")
                val playStoreURL = updateData.getString("androidUpdateLink")
                val info = applicationContext.packageManager.getPackageInfo(
                    packageName, 0
                )
                @Suppress("DEPRECATION") val versionCode = info.versionCode
                var buildNum = 0

                @Suppress("SENSELESS_COMPARISON")
                if (androidBuildNumber != null) {
                    buildNum = androidBuildNumber.toInt()
                }
                val isUpdate = versionCode < buildNum

                val imgIcon = findViewById<ImageView>(R.id.img_icon)

                if ((isAndroidForcedUpdate || isAndroidUpdate) && (isUpdate)) {
                    val txtTitle = findViewById<TextView>(R.id.txt_title)
                    val txtDes = findViewById<TextView>(R.id.txt_des)
                    val txtNoThanks = findViewById<TextView>(R.id.txt_no_thanks)
                    val btnUpdate = findViewById<TextView>(R.id.btn_update)
                    if (icon != 0) {
                        imgIcon.visibility = View.VISIBLE
                        imgIcon.setImageResource(icon)
                    }
                    txtTitle.text = buildString {
                        append(appName)
                        append(" ")
                        append(getString(R.string.update_title))
                    }
                    if (isAndroidForcedUpdate) {
                        txtNoThanks.visibility = View.GONE
                        txtDes.text = getString(R.string.update_force_dsc)
                    } else {
                        txtNoThanks.visibility = View.VISIBLE
                        txtDes.text = getString(R.string.update_dsc, appName)
                        txtNoThanks.setOnClickListener {
                            activityClose = true
                            callback.isEnabled = true
                            callback.handleOnBackPressed()
                        }
                    }
                    btnUpdate.setOnClickListener {
                        try {
                            val marketIntent =
                                Intent(Intent.ACTION_VIEW, Uri.parse(playStoreURL))
                            startActivity(marketIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activityClose) {
                    finish()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)
    }
}
