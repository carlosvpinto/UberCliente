package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R

class SplashActivity : AppCompatActivity() {

    val DURACION : Long = 3000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //ACULTAR TOOL BAR
        supportActionBar?.hide()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        Glide.with(this).load(R.drawable.ic_logotaxiahora240x240svg).into(logo)

        cambiarActivity()
    }

    private fun cambiarActivity() {
        Handler().postDelayed(Runnable {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }, DURACION)
    }
}