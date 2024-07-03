package com.example.onetappay.helper

import android.app.Application


class AppController : Application() {
    companion object {
        lateinit var instance: AppController
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

    }
}