package com.appsonair.appsync.interfaces

interface UpdateCallBack {
    fun onSuccess(response: String?)
    fun onFailure(message: String?)
}