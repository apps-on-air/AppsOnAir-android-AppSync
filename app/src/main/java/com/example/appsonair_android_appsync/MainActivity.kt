package com.example.appsonair_android_appsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.appsonair.appsync.interfaces.UpdateCallBack
import com.appsonair.appsync.services.AppSyncService
import com.example.appsonair_android_appsync.ui.theme.AppsOnAirAndroidAppSyncTheme

class MainActivity : ComponentActivity() {
    @Suppress("PrivatePropertyName")
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppsOnAirAndroidAppSyncTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        AppSyncService.sync(
            this,
            options = mapOf("showNativeUI" to true),
            callBack = object : UpdateCallBack {
                override fun onSuccess(response: String?) {
                    Log.d(TAG, "onSuccess: ")
                }

                override fun onFailure(message: String?) {
                    Log.d(TAG, "onFailure: ")
                }
            },
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppsOnAirAndroidAppSyncTheme {
        Greeting("Android")
    }
}