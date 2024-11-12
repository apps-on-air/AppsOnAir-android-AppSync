# AppsOnAir-android-AppSync


## How it works? 

- App Update 📤 
>By enabling App Update feature, users will be able to see  any new releases published in this app.

- App Maintenance 🛠️ 
>By enabling Maintenance mode feature, users won’t be able to access the app and will be noted as the app is under Maintenance mode.


## How to use?

Add meta-data to the app's AndroidManifest.xml file under the application tag.

>Make sure meta-data name is “appId”.

>Provide your application id in meta-data value.


```sh
</application>
    ...
    <meta-data
        android:name="appId"
        android:value="********-****-****-****-************" />
</application>
```

>Make sure meta-data name is “com.appsonair.icon”.

>Provide your application logo in meta-data value.

```sh
</application>
    ...
    <meta-data
       android:name="com.appsonair.icon"
       android:resource="@mipmap/ic_launcher" />
</application>
```

Add AppsOnAir AppSync dependency to your gradle.

```sh
dependencies {
   implementation 'com.github.apps-on-air:AppsOnAir-android-AppSync:TAG'
}
```

Add below code to setting.gradle.

```sh
dependencyResolutionManagement {
   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
   repositories {
       google()
       mavenCentral()
       maven {
           url = uri("https://jitpack.io")
       }
   }
}
```

## Example :

Keep the "showNativeUI" true, if you want to display the native(default) UI for app updates. Conversely, if you want to display your custom UI for it, it will be set to false.

You can show your custom UI for app update in onSuccess() method.

```sh
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
```