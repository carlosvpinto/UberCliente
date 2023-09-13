package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityMapBinding
import com.carlosvicente.uberkotlin.databinding.ActivityMotoTemporalBinding
import com.carlosvicente.uberkotlin.fragments.ModalBottomSheetMenu
import com.google.android.gms.maps.model.LatLng

private lateinit var binding: ActivityMotoTemporalBinding
private var extraOriginName = ""
private var extraDestinationName = ""
private val modalMenu = ModalBottomSheetMenu()

class MotoTemporalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMotoTemporalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!

        binding.btnSolicitarAceptarMoto.setOnClickListener {
            val driverTlf = "0584124603451"
            whatSapp(driverTlf) }

        binding.imageViewMenu.setOnClickListener { showModalMenu() }
    }
    private fun showModalMenu() {
        if (!modalMenu.isAdded()) {
            modalMenu.show(supportFragmentManager, ModalBottomSheetMenu.TAG)
        }

    }

    //ENVIAR MSJ DE WHATSAPP*******YO******
    private fun whatSapp (phone: String){
        var phone58 = phone
        val cantNrotlf = phone.length // devuelve 10

        try {
            // código que puede generar una excepción
            val phone58 = "058$phone"
            val i  = Intent(Intent.ACTION_VIEW);
            val  uri =  "whatsapp://send?phone="+phone+"&text="+"hola te escribo de la aplicacion TAXI AHORA, Necesito ir para " + extraDestinationName+" Estoy en "+ extraOriginName;
            i.setData(Uri.parse(uri))
            this.startActivity(i)
        } catch (e: Exception) {
            // código para manejar la excepción
            Toast.makeText(this, "Error al iniciar Whatsaap $e", Toast.LENGTH_SHORT).show()
            return
        }

    }
    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    override fun onBackPressed() {
        goToMap()

    }
}