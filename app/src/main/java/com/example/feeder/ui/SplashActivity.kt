package com.example.feeder.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.feeder.ui.LoginActivity
import com.example.feeder.R
import com.example.feeder.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    private val SPLASH_DELAY: Long = 3000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DELAY)


        val text = "UtilityNet App"
        val spannable = SpannableString(text)

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)),
            0,
            10,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.black)),
            11,
            text.length,
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


    private fun navigateToLogin() {

    }
}