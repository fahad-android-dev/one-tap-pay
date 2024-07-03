package com.orbits.paymentapp.mvvm.splash.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.orbits.paymentapp.R
import com.orbits.paymentapp.databinding.ActivitySplashBinding
import com.orbits.paymentapp.mvvm.main.view.MainActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Your Code
            startActivity(Intent(this, MainActivity::class.java))

            finish()
        }, 3000)

    }
}