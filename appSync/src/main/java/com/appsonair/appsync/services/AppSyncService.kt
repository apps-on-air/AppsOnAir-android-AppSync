package com.appsonair.appsync.services

import android.content.Context
import android.content.Intent
import android.util.Log
import com.appsonair.appsync.BuildConfig
import com.appsonair.appsync.R
import com.appsonair.appsync.activities.AppUpdateActivity
import com.appsonair.appsync.activities.MaintenanceActivity
import com.appsonair.appsync.interfaces.UpdateCallBack
import com.appsonair.core.services.CoreService
import com.appsonair.core.services.NetworkService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper


class AppSyncService {
    companion object {
        private var appId: String = ""
        private var showNativeUI: Boolean = true
        private var isResponseReceived: Boolean = false
        private var isNetworkConnected: Boolean = false
        private const val TAG = "AppSyncService"

        private fun getResponse(
            response: Response,
            context: Context,
            callBack: UpdateCallBack? = null,
            isFromCDN: Boolean
        ) {
            try {
                if (response.code == 200) {
                    val myResponse = response.body!!.string()
                    val jsonObject = JSONObject(myResponse)
                    val updateData = jsonObject.getJSONObject("updateData")
                    val isAndroidUpdate = updateData.getBoolean("isAndroidUpdate")
                    val isMaintenance = jsonObject.getBoolean("isMaintenance")
                    if (isMaintenance && showNativeUI) {
                        val intent = Intent(context, MaintenanceActivity::class.java)
                        intent.putExtra("res", myResponse)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else if (isAndroidUpdate) {
                        val isAndroidForcedUpdate = updateData.getBoolean("isAndroidForcedUpdate")
                        val androidBuildNumber = updateData.getString("androidBuildNumber")
                        val info = context.packageManager.getPackageInfo(context.packageName, 0)
                        @Suppress("DEPRECATION") val versionCode = info.versionCode
                        var buildNum = 0

                        @Suppress("SENSELESS_COMPARISON")
                        if (androidBuildNumber != null) {
                            buildNum = androidBuildNumber.toInt()
                        }
                        val isUpdate = versionCode < buildNum
                        if (showNativeUI && isUpdate && (isAndroidForcedUpdate || isAndroidUpdate)) {
                            val intent = Intent(context, AppUpdateActivity::class.java)
                            intent.putExtra("res", myResponse)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }

                    // Creating a new object with modified keys
                    val newUpdateData = JSONObject().apply {
                        put("isUpdateEnabled", updateData.getBoolean("isAndroidUpdate"))
                        put("buildNumber", updateData.getString("androidBuildNumber"))
                        put("minBuildVersion", updateData.optString("androidMinBuildVersion", ""))
                        put("updateLink", updateData.getString("androidUpdateLink"))
                        put("isForcedUpdate", updateData.getBoolean("isAndroidForcedUpdate")) }

                    // Updating the original JSON
                    jsonObject.put("updateData", newUpdateData)
                    callBack?.onSuccess(jsonObject.toString())
                    isResponseReceived = true
                } else if (isFromCDN) {
                    callServiceApi(context, callBack)
                }
            } catch (e: Exception) {
                callBack?.onFailure(e.message)
                isResponseReceived = false
                Log.d(TAG, "getResponse: " + e.message)
            }
        }

        private fun callCDNServiceApi(context: Context, callBack: UpdateCallBack? = null) {
            val baseUrl = BuildConfig.CDN_BASE_URL

            val pathSegment = buildString {
                append(appId)
                append(".json")
            }

            val urlBuilder: HttpUrl.Builder = baseUrl.toHttpUrlOrNull()!!.newBuilder()
            val unixTime = System.currentTimeMillis() / 1000L
            urlBuilder.addPathSegment(pathSegment)
            urlBuilder.addQueryParameter("now", unixTime.toString())
            val url: String = urlBuilder.build().toString()

            val client = OkHttpClient().newBuilder().build()
            val request: Request = Request.Builder().url(url).method("GET", null).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "onFailure: " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    getResponse(response, context, callBack, true)
                }
            })
        }

        private fun callServiceApi(context: Context, callBack: UpdateCallBack? = null) {
            val url: String = buildString {
                append(BuildConfig.BASE_URL)
                append(appId)
            }
            val client = OkHttpClient().newBuilder().build()
            val request: Request = Request.Builder().url(url).method("GET", null).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "onFailure: " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    getResponse(response, context, callBack, false)
                }
            })
        }

        @JvmStatic
        @JvmOverloads
        fun sync(
            context: Context,
            options: Map<String, Any> = emptyMap(),
            callBack: UpdateCallBack? = null
        ) {
            Debounce.debounce {
                val applicationId: String = CoreService.getAppId(context)
                appId = applicationId

                if (appId.isEmpty()) {
                    Log.d(TAG, "AppId: " + context.getString(R.string.error_something_wrong))
                } else {
                    if (options.isNotEmpty()
                        && options.containsKey(key = "showNativeUI")
                        && options["showNativeUI"] is Boolean
                    ) {
                        showNativeUI = options["showNativeUI"] as Boolean
                    }
                    if (isResponseReceived) {
                        if (isNetworkConnected) {
                            callCDNServiceApi(context, callBack)
                        }
                    } else {
                        NetworkService.checkConnectivity(
                            context
                        ) { isAvailable: Boolean ->
                            run {
                                isNetworkConnected = isAvailable
                                if (isAvailable ) {
                                    if (!isResponseReceived) {
                                        callCDNServiceApi(context, callBack)
                                    }
                                } else {
                                    Log.d(TAG, "Please check your internet connection!")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// Debounce object to handle debounce logic
object Debounce {
    private var lastCallTime: Long = 0
    private const val DEBOUNCE_DELAY_MS = 300L // Adjust the delay as needed
    private val handler = Handler(Looper.getMainLooper())
    private var currentTime: Long = 0

    // Function to debounce the provided action
    fun debounce(action: () -> Unit) {
        currentTime = System.currentTimeMillis()
        // Check if enough time has passed since the last call
        if (currentTime - lastCallTime > DEBOUNCE_DELAY_MS) {
            // Remove any pending callbacks
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                action.invoke()
                lastCallTime = System.currentTimeMillis()
            }, DEBOUNCE_DELAY_MS)
        }
    }
}

