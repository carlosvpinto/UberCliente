package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.models.Client
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.ClientProvider
private val authProvider = AuthProvider()
private val clientProvider = ClientProvider()
private var cliente: Client? = null

class SplashActivity : AppCompatActivity() {


    val DURACION : Long = 3000
    override fun onCreate(savedInstanceState: Bundle?) {
        //para usar apa splash******************
        val screenSplas = installSplashScreen()
        //*************************************
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        getClient()

        screenSplas.setKeepOnScreenCondition {true}
        //ACULTAR TOOL BAR
//        supportActionBar?.hide()
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

//        val logo = findViewById<ImageView>(R.id.logoSplash)
//        Glide.with(this).load(R.drawable.ic_logotaxiahora240x240svg).into(logo)



    }

    //OBTIENE LA INFOR DEL CLIENTE ****YO***********************************TRAIDO
    private fun getClient() {
        if (authProvider.existSession()) {
            clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
                if (document.exists()) {
                    val client = document.toObject(Client::class.java)
                    cliente = client
                    irMainActivity(Registrado = true)
                    Log.d("CLIENTE", "en getcliente Splash: ${client?.email} ${client?.name} ${client?.lastname}")

                }
            }
        }else{
            irMainActivity(Registrado = false)
        }
    }
    private fun irMainActivity(Registrado: Boolean){
        val intent = Intent(this, MainActivity::class.java)
        if (Registrado){

            val bundle = Bundle()
            bundle.putString("client", cliente?.toJson())
            intent.putExtras(bundle)
            Log.d("CLIENTE", "en SALIENDO DE SPLASACTIVITY:bundle $bundle")
            startActivity(intent)
        }else{
            val bundle = Bundle()
            bundle.putString("client", null)
            intent.putExtras(bundle)
            Log.d("CLIENTE", "en SALIENDO DE SPLASACTIVITY:bundle $bundle")
            startActivity(intent)
        }
        finish()

    }
}
