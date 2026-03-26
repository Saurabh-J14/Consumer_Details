package com.example.feeder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.feeder.R
import com.example.feeder.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    private val SPLASH_DELAY: Long = 3000
    private val STARTUP_PERMISSION_REQ = 1101
    private val startupHandler = Handler(Looper.getMainLooper())
    private var hasNavigated = false
    private val startupPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startupHandler.postDelayed({
            requestStartupPermissionsIfNeeded()
        }, SPLASH_DELAY)


        val text = "PhiTech"
        val spannable = SpannableString(text)

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)),
            0,
            7,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        binding.tvHeader.text = spannable

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleFadeIn = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in)

        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        binding.img.startAnimation(rotateAnim)

        binding.tvHeader.startAnimation(scaleFadeIn)
        fadeIn.startOffset = 500
        binding.imageView.startAnimation(fadeIn)

    }


    private fun requestStartupPermissionsIfNeeded() {
        val missingPermissions = startupPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            navigateToLogin()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            missingPermissions.toTypedArray(),
            STARTUP_PERMISSION_REQ
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STARTUP_PERMISSION_REQ) {
            navigateToLogin()
        }
    }

    override fun onDestroy() {
        startupHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun navigateToLogin() {
        if (hasNavigated) return
        hasNavigated = true
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
