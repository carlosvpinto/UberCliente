package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.adapters.HistoriesAdapter
import com.carlosvicente.uberkotlin.adapters.PagoMovilAdapter
import com.carlosvicente.uberkotlin.databinding.ActivityBancoprincipalBinding
import com.carlosvicente.uberkotlin.databinding.ActivityBankBinding
import com.carlosvicente.uberkotlin.databinding.ActivityHistoryBinding
import com.carlosvicente.uberkotlin.models.History
import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.HistoryProvider
import com.carlosvicente.uberkotlin.providers.PagoMovilProvider
import com.google.firebase.firestore.ListenerRegistration
import com.tommasoberlose.progressdialog.ProgressDialogFragment

private lateinit var binding: ActivityBancoprincipalBinding
class BancoprincipalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBancoprincipalBinding
    private var pagoMovilProvider = PagoMovilProvider()
    private var pagoMoviles = ArrayList<PagoMovil>()
    private  var adapter: PagoMovilAdapter? = null
    private val authProvider = AuthProvider()
    private var pagoMovilListener: ListenerRegistration? = null
    private var isActivityVisible = false
    private var progressDialog = ProgressDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBancoprincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressDialog.showProgressBar(this)
        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerDepositos.layoutManager = linearLayoutManager

        //CORREGIR EL ACTION BAR
        val actionBar = (this as AppCompatActivity).supportActionBar
        if (actionBar != null) {
            // El tema actual utiliza ActionBar
            Log.d("TEMA", "ENTRO A TEMA CON ACTION VAR")
        } else {
            Log.d("TEMA", "ENTRO A TEMA SIN ACTION VAR")
            setSupportActionBar(binding.toolbar)
        }
            //*********************************

        supportActionBar?.title = "Depositos Realizados"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitleTextColor(Color.BLACK)
        binding.floataddbtn.setOnClickListener{
            goToBankActivity()
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        listenerpagomovil()
        //getPagosMoviles()
    }

    private fun goToBankActivity() {
        val i = Intent(this, BankActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)

    }
    private fun goToMapActivity() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)

    }


    private fun listenerpagomovil(){
        //************************
        pagoMovilListener = pagoMovilProvider.getPagoMovil(authProvider.getId())
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("pagomovil", "Error al escuchar cambios en la base de datos", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && isActivityVisible && binding.recyclerDepositos.isVisible) {

                    pagoMoviles.clear() // Limpiar la lista antes de agregar los nuevos elementos

                    if (querySnapshot.documents.size > 0) {
                        val documents = querySnapshot.documents

                        for (d in documents) {
                            var pagoMovil = d.toObject(PagoMovil::class.java)
                            pagoMovil?.id = d.id
                            pagoMoviles.add(pagoMovil!!)
                        }
                        adapter?.updatePagosRealizados(pagoMoviles)
                        adapter = PagoMovilAdapter(this@BancoprincipalActivity, pagoMoviles)
                        binding.recyclerDepositos.adapter = adapter
                        progressDialog.hideProgressBar(this)
                        //adapter?.notifyDataSetChanged() // Notificar al adaptador después de asignar la nueva lista

                    }else{
                        Toast.makeText(this, "Sin deposito", Toast.LENGTH_SHORT).show()
                        progressDialog.hideProgressBar(this)
                    }



                }else{
                    Toast.makeText(this, "No tiene ningun deposito", Toast.LENGTH_SHORT).show()
                    progressDialog.hideProgressBar(this)
                }
            }
    }

    override fun onBackPressed() {
        goToMapActivity()
        super.onBackPressed()
    }
    override fun onDestroy() {
        super.onDestroy()

        // Eliminar el listener de Firebase cuando se destruye la actividad
        pagoMovilListener?.remove()
        pagoMovilListener = null
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }
}