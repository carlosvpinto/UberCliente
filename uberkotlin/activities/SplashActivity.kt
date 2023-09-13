package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        getClient()
        //ACULTAR TOOL BAR
        supportActionBar?.hide()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        Glide.with(this).load(R.drawable.ic_logotaxiahora240x240svg).into(logo)



    }

    private fun cambiarActivity() {
        Handler().postDelayed(Runnable {
            val intent = Intent(this,MainActivity::class.java)

            startActivity(intent)
        }, DURACION)
    }

    //OBTIENE LA INFOR DEL CLIENTE ****YO***********************************TRAIDO
    private fun getClient() {
        clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
            if (document.exists()) {
                val client = document.toObject(Client::class.java)
                cliente = client
                irMainActivity()
                Log.d("CLIENTE", "en getcliente: ${client?.email} ${client?.name} ${client?.lastname}")

            }
        }
    }
    private fun irMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putString("client", cliente?.toJson())
        intent.putExtras(bundle)
        Log.d("CLIENTE", "en SALIENDO DE SPLASACTIVITY:bundle $bundle")
        startActivity(intent)
    }
}
