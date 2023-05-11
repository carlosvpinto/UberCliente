package com.carlosvicente.uberkotlin.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityBankBinding
import com.carlosvicente.uberkotlin.databinding.ActivityCalificationBinding
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.FCMBody
import com.carlosvicente.uberkotlin.models.FCMResponse
import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.BookingProvider
import com.carlosvicente.uberkotlin.providers.NotificationProvider
import com.carlosvicente.uberkotlin.providers.PagoMovilProvider
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*



private lateinit var binding: ActivityBankBinding
private val authProvider = AuthProvider()
private val pagoMovilProvider = PagoMovilProvider()

private val notificationProvider = NotificationProvider()
private var MontoBs = ""
private var banco = ""
var previousLength = 0
private val TAG = "Banco"
class BankActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBankBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnValidar.setOnClickListener{
            validar()
            }

        binding.textViewMonto.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                // Aquí puedes ejecutar la función que desees al perder el foco
                // Por ejemplo, si tu función se llama "miFuncion", puedes llamarla así:

                covertir(binding.textTasa.text.toString().toDouble())
            }
        }
        binding.imageViewBack.setOnClickListener {
            goToBancoPrincipal()
        }
        validarFecha()
    }

    private fun validarFecha() {

        binding.textViewDateFija.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 2 || s?.length == 5) {
                    s.append("/")
                } else if (s?.length ?: 0 < previousLength && (s?.length ?: 0) % 3 == 2) {
                    s?.delete(s.length - 1, s.length)
                }
                previousLength = s?.length ?: 0
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })



    }

    private fun covertir(taza:Double) {
        var montoBs = 0.0
        var montoDollar= 0.0
        montoBs= binding.textViewMonto.text.toString().toDouble()
        montoDollar= montoBs/taza
        Toast.makeText(this, "Convertor", Toast.LENGTH_SHORT).show()
        binding.TxtMontoDollar.text = montoDollar.toString()



    }

    //CREA LA SOLICITUD DE VIAJE CON ESTATUS "create"
    private fun createPagomovil() {

        val pagoMovil = PagoMovil(

            idClient = authProvider.getId(),
            nro= binding.text5Ultimos.text.toString(),
            montoBs = binding.textViewMonto.text.toString().toDouble(),
            montoDollar = binding.TxtMontoDollar.text.toString().toDouble(),
            fechaPago = binding.textViewDateFija.text.toString(),
            tazaCambiaria = binding.textTasa.text.toString().toDouble(),
            timestamp = Date().time,
            verificado = false,
            date = Date()



        )
        pagoMovilProvider.create(pagoMovil).addOnCompleteListener {
            if (it.isSuccessful) {
                 Toast.makeText(this@BankActivity, "Datos Enviados para Validar", Toast.LENGTH_LONG).show()
                limpiarEditTexts(this)
            } else {
                Toast.makeText(this@BankActivity, "Error al crear los datos", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun isValidForm(banco: String, monto: String, telefono: String, fecha: String, recibo: String): Boolean {

        if (banco.isEmpty()) {
            Toast.makeText(this, "Ingresa tu Banco", Toast.LENGTH_SHORT).show()
            return false
        }

        if (monto.isEmpty()) {
            Toast.makeText(this, "Ingresa el Monto", Toast.LENGTH_SHORT).show()
            return false
        }

        if (telefono.isEmpty()) {
            Toast.makeText(this, "Ingresa el Telefono", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fecha.isEmpty()) {
            Toast.makeText(this, "Ingresa la fecha", Toast.LENGTH_SHORT).show()
            return false
        } else {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")
            dateFormat.isLenient = false

            try {
                val date = dateFormat.parse(fecha)
                val currentDate = Calendar.getInstance().time

                if (date.after(currentDate)) {
                    Toast.makeText(this, "La fecha no puede ser mayor a la fecha actual", Toast.LENGTH_SHORT).show()
                    binding.textViewDateFija.text.clear()
                    return false
                }
            } catch (e: ParseException) {
                Toast.makeText(this, "Fecha Invalida", Toast.LENGTH_SHORT).show()
                binding.textViewDateFija.text.clear()
                return false
            }
        }

        if (recibo.isEmpty()) {
            Toast.makeText(this, "Ingresa el Nro de Recibo", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun validar() {

        val banco = binding.textBanco.text.toString()
         MontoBs = binding.textViewMonto.text.toString()
        val telefono = binding.textViewtelefono.text.toString()
        val fecha = binding.textViewDateFija.text.toString()
        val recibo = binding.text5Ultimos.text.toString()

        if (isValidForm(banco, MontoBs,telefono,fecha,recibo)) {
            createPagomovil()
            goToBancoPrincipal()

        }
    }

    private fun goToBancoPrincipal() {
        val i = Intent(this, BancoprincipalActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    fun limpiarEditTexts(activity: Activity) {
        Log.d("Cover", "Limpiar")
        val rootView = activity.window.decorView.rootView
        val editTexts = ArrayList<View>()
        rootView.findViewsWithText(editTexts, "", View.FIND_VIEWS_WITH_TEXT)
        for (view in editTexts) {
            if (view is EditText) {
                view.setText("")
            }
        }
    }

    override fun onBackPressed() {
        goToBancoPrincipal()
        super.onBackPressed()
    }


}


