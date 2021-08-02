package com.app.potatodiseasedetection

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*


class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        imgLogo.animation = AnimationUtils.loadAnimation(this, R.anim.fade)
        imgPod.animation = AnimationUtils.loadAnimation(this, R.anim.fade)
        imgName.animation = AnimationUtils.loadAnimation(this, R.anim.fade)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                startActivity(Intent(this@SplashScreenActivity, LangSelActivity::class.java))
                finish()
            }
        }, 3500)
    }
}