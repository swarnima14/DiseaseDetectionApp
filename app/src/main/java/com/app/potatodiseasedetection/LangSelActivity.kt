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
        langAnim.animation = topAnim //the world animation

        var bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        mainLay.animation = bottomAnim //layout with text to choose language and buttons to select language

        //to select english as language
            btnEng.setOnClickListener {

                //saves the language type in string form in shared preferences so that it can be accessed in other activities
                saveLanguage("eng")

                changeLang("en", this)
                val intent = Intent(this, MainActivity::class.java)
                //intent.putExtra("lang", "en")
                startActivity(intent)
                finish()
            }

        //to select hindi as language
            btnHin.setOnClickListener {

                saveLanguage("hin")

                changeLang("hi", this)
                val intent = Intent(this, MainActivity::class.java)
                //intent.putExtra("lang", "hi")
                startActivity(intent)
                finish()

        }


    }

    fun changeLang(str: String, context: Context){

        //hold ctrl and click on any function name to see about the function and the values it returns

        val locale = Locale(str) //constructs a locale from str
        Locale.setDefault(locale) //sets it as default locale
        val configuration = context.resources.configuration //returns present configuration
        configuration.locale = locale //stores variable value of locale as new locale


        context.createConfigurationContext(configuration) //creates resources of new configuration, API level<17
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics) //stores newly updated configuration, API level>=17
                //resources are the resources used in current application
                //display metrics gives details about the display(size, density,etc)
    }

    fun saveLanguage(type: String?) {
        //shared preferences stores small data in the form of key value pairs
        val editor = getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE).edit() //MY_LANGUAGE IS THE FILE NAME
        // and mode_private is used so that only this application can access it
        // and no other application has access to this file
        editor.putString("myLanguage", type) //specify the key and value to be stored
        editor.apply() //saves data

    }

    override fun onStart() {
        super.onStart()

        //check if the language was already selected or not and if not then display the layout for selecting language

        val prefs = getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE) //to retrieve string
        if(prefs.getString("myLanguage","").equals("hin")) {
            changeLang("hi", this)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("lang", "hi")
            startActivity(intent)
            finish()
            mainLay.visibility = View.GONE //always choose gone instead of invisible
        // because invisible only makes the layout element invisible and its place will always be there as a gap
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
