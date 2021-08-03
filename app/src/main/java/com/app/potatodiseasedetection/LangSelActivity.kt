package com.app.potatodiseasedetection

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_lang_sel.*
import java.util.*


class LangSelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lang_sel)

        var topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        langAnim.animation = topAnim

        var bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        mainLay.animation = bottomAnim

            btnEng.setOnClickListener {

                saveLanguage("eng")

                changeLang("en", this)
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("lang", "en")
                startActivity(intent)
                finish()
            }

            btnHin.setOnClickListener {

                saveLanguage("hin")

                changeLang("hi", this)
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("lang", "hi")
                startActivity(intent)
                finish()

        }


    }

    fun changeLang(str: String, context: Context){

        val locale = Locale(str) //constructs a locale from str
        Locale.setDefault(locale) //sets it as default locale
        val configuration = context.resources.configuration
        configuration.locale = locale
        context.createConfigurationContext(configuration) //load resources with new configuration
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics) //store resources with new configuration

    }

    fun saveLanguage(type: String?) {
        val editor = getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE).edit()
        editor.putString("myLanguage", type)
        editor.apply()
        editor.commit()
    }

    override fun onStart() {
        super.onStart()

        val prefs = getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE)
        if(prefs.getString("myLanguage","").equals("hin")) {
            changeLang("hi", this)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("lang", "hi")
            startActivity(intent)
            finish()
            mainLay.visibility = View.GONE
        }
        else if(prefs.getString("myLanguage","").equals("eng")) {
            changeLang("en", this)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("lang", "en")
            startActivity(intent)
            finish()
            mainLay.visibility = View.GONE
        }

        else{
            mainLay.visibility = View.VISIBLE
        }

    }

}
