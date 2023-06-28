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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.AsyncTask
import com.carlosvicente.uberkotlin.models.*
import com.carlosvicente.uberkotlin.providers.*
import com.google.firebase.firestore.ListenerRegistration
import com.tommasoberlose.progressdialog.ProgressDialogFragment
import kotlinx.coroutines.*
import okhttp3.OkHttpClient




import okhttp3.Request
import org.jsoup.nodes.Element
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


private lateinit var binding: ActivityBankBinding
private val authProvider = AuthProvider()
private val pagoMovilProvider = PagoMovilProvider()
private val configProvider = ConfigProvider()
private var configListener: ListenerRegistration? = null

private var progressDialog = ProgressDialogFragment

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
        binding.btnPortaPapelCi.setOnClickListener {
            copiarCi()
        }
        binding.btnPortaPapelTlf.setOnClickListener {
            copiarTlf()
        }
        obtenerBCV()
        validarFecha()

    }



    private fun obtenerBCV(){
        progressDialog.showProgressBar(this)
        configProvider.getPrices().addOnSuccessListener { document ->

            var taza = 0.0

            if (document.exists()) {
                val prices = document.toObject(Prices::class.java) // DOCUMENTO CON LA INFORMACION

                    taza = prices?.taza!!

                Log.d("PRICE", "VALOS FINAL DE TOTAL: $taza ")
                val tazaRedondeado = String.format("%.2f", taza)
                //  val maxTotalString = String.format("%.1f", maxTotal)
                binding.textTasa.text = "$tazaRedondeado"

                progressDialog.hideProgressBar(this)
            }


        }.addOnFailureListener{
            Toast.makeText(this, "Problemas de conexion Verifique el acceso a internet", Toast.LENGTH_SHORT).show()
            return@addOnFailureListener
        }
    }


    private fun copiarTlf() {
        copyToClipboard(this, binding.textTlf.text.toString(),"El Telefono")
    }

    //COPIAR LOS DATOS AL PORTAPEPEL
    private fun copiarCi() {
        copyToClipboard(this, binding.textCedula.text.toString(), "La Cedula")

    }

    //FUNCION PARA COPIAR AL PORTA PAPEL
    fun copyToClipboard(context: Context, text: String, titulo: String) {
        // Obtener el servicio del portapapeles
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Crear un objeto ClipData para guardar el texto
        val clipData = ClipData.newPlainText("text", text)

        // Copiar el objeto ClipData al portapapeles
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "Se a Copiado $titulo", Toast.LENGTH_SHORT).show()
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
    //CONVIERTE DE BS A DOLART SEGUN LA TAZA BCV EN EL TXT
    private fun covertir(taza:Double) {
        var montoBs = 0.0
        var montoDollar= 0.0
        montoBs= binding.textViewMonto.text.toString().toDouble()
        montoDollar= montoBs/taza
       // Toast.makeText(this, "Convertor", Toast.LENGTH_SHORT).show()
        val valorDolar = montoDollar
        val valorDoilarRendondeado = String.format("%.2f", valorDolar)

        binding.TxtMontoDollar.text = valorDoilarRendondeado.toString()
    }

    //CREA LA EL PAGO MOVIL***************
    private fun createPagomovil() {

        val montoBs = binding.textViewMonto .toString().toDouble()
        val montoDollar = binding.TxtMontoDollar.toString().toDouble()
        val montoBRedondeado = String.format("%.2f", montoBs)
        val montoDollarRedondeado = String.format("%.2f", montoDollar)


        val pagoMovil = PagoMovil(

            idClient = authProvider.getId(),
            nro= binding.text5Ultimos.text.toString(),
            montoBs = montoBRedondeado.toDouble(),
            montoDollar = montoDollarRedondeado.toDouble(),
            fechaPago = binding.textViewDateFija.text.toString(),
            tlfPago = binding.textTlf.text.toString(),
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
            binding.textBanco.requestFocus()
            Toast.makeText(this, "Ingresa tu Banco", Toast.LENGTH_SHORT).show()
            return false
        }

        if (monto.isEmpty()) {
            binding.textViewMonto.requestFocus()
            Toast.makeText(this, "Ingresa el Monto", Toast.LENGTH_SHORT).show()
            return false
        }

        if (telefono.isEmpty()) {
            Toast.makeText(this, "Ingresa el Telefono", Toast.LENGTH_SHORT).show()
            // Establecer el foco en el EditText
            binding.textTlf.requestFocus()
            return false
        }

        if (fecha.isEmpty()) {
            binding.textViewDateFija.requestFocus()
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
            binding.text5Ultimos.requestFocus()
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


