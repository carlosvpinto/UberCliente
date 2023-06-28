package com.carlosvicente.uberkotlin.fragments


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.carlosvicente.uberkotlin.R


import android.util.Log
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import com.carlosvicente.uberkotlin.activities.*

import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.models.Prices
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.ClientProvider
import com.carlosvicente.uberkotlin.providers.ConfigProvider
import com.carlosvicente.uberkotlin.providers.PagoMovilProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tommasoberlose.progressdialog.ProgressDialogFragment
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class VerificacionFragment : Fragment() {

    private var nrorecibo: String? = null
    private var monto: String? = null
    private var MontoBigDecimal : BigDecimal? = null
    private var tazaBcv: String? = null

    private var btnAceptar: Button? = null
    private var btnCancelar: Button? = null
    private var txtFecha: TextView? = null
    private var txtBcv: TextView? = null
    private var txtCi: TextView? = null


    private var textViewMonto: TextView? = null
    private var textViewBanco: TextView? = null
    private var textViewTlf: TextView? = null
    private var textViewTasa: TextView? = null
    private var textViewSaldo: TextView? = null
    private var txtTlfFragment: TextView? = null
    private var btnPortaPapelCi: ImageButton? = null
    private var btnPortaPapelTlf: ImageButton? = null
    private var imgAtras : ImageView? = null
    private var MontoBs = ""
    private var pagoMoviles = ArrayList<PagoMovil>()
    private var totalBs = 0.0
    private var totalDollar= 0.0
    private var totalSinVeriBs = 0.0
    private var totalSinVeriBsDollar = 0.0
    private var progressDialog = ProgressDialogFragment

    private val authProvider = AuthProvider()
    private val clientProvider = ClientProvider()
    private var pagoMovilProvider = PagoMovilProvider()
    private val configProvider = ConfigProvider()

    private var textView5Ultmios: TextView? = null

    private var onSumResultListener: OnSumResultListener? = null
    private lateinit var button: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        arguments?.let {
            tazaBcv = it.getString(TAZA_BCV)
            monto = it.getString(MONTO_BUNDLE)

            Log.d("Framentando", "Valor LLEGANDO AL FRAGMENT Recivo y Valor de  tazaBcv $tazaBcv y monto $monto")
        }
    }
    //INTERFACE PARA COMUNICAR CON EL ACTIVITY
    interface OnSumResultListener {
        fun onSumResult(
            totalDepBs: Double,
            totalDepDollar: Double,
            telefoFragmnet:String,
            fechaDepFragmen:String,
            reci5UltimosFragment:String)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.cardview_pagomovil, container, false)
        btnAceptar = view?.findViewById(R.id.btnFragValidar)
        btnCancelar = view?.findViewById(R.id.btnFragCancelar)
        txtFecha= view?.findViewById(R.id.textViewDateFija)
        txtBcv= view?.findViewById(R.id.textTasaCard)
        textViewMonto= view?.findViewById(R.id.textViewMontoBs)
        textViewBanco= view?.findViewById(R.id.textBanco)
        textViewTlf= view?.findViewById(R.id.textTlf)
        textView5Ultmios= view?.findViewById(R.id.text5Ultimos)
        txtCi= view?.findViewById(R.id.textCedula)
        txtTlfFragment= view?.findViewById(R.id.textViewtelefono)

        textViewSaldo= view?.findViewById(R.id.txtSaldo)
        btnPortaPapelTlf= view?.findViewById(R.id.btnPortaPapelTlf)
        btnPortaPapelCi= view?.findViewById(R.id.btnPortaPapelCi)
        imgAtras = view?.findViewById(R.id.imageViewBack)

        //ANIMACION BOTON COPIAR CI*********************************
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.white)
        val pressedColor = ContextCompat.getColor(requireContext(), R.color.green)
        val colorAnimationCi = ValueAnimator.ofArgb(defaultColor, pressedColor)
        colorAnimationCi.addUpdateListener { animator ->
            btnPortaPapelCi?.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimationCi.duration = 200 // Duración de la transición de colores en milisegundos
        val scaleAnimationCiUp = ObjectAnimator.ofPropertyValuesHolder(
            btnPortaPapelCi,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f)
        )
        scaleAnimationCiUp.duration = 100 // Duración de la transición de escala en milisegundos

        val scaleAnimationCiDown = ObjectAnimator.ofPropertyValuesHolder(
            btnPortaPapelCi,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f, 1.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f, 1.0f)
        )
        scaleAnimationCiDown.duration = 100 // Duración de la transición de escala hacia abajo en milisegundos
        val animatorCiSet = AnimatorSet()
        animatorCiSet.playTogether(scaleAnimationCiUp,colorAnimationCi, scaleAnimationCiDown)
       //***********************************************************



       //ANIMACION BOTON COPIAR TLF*********************************
        val colorAnimationTlf = ValueAnimator.ofArgb(defaultColor, pressedColor)
        colorAnimationTlf.addUpdateListener { animator ->
            btnPortaPapelTlf?.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimationTlf.duration = 300 // Duración de la transición en milisegundos
        val scaleAnimationTlfUp = ObjectAnimator.ofPropertyValuesHolder(
            btnPortaPapelTlf,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f)
        )
        scaleAnimationTlfUp.duration = 100 // Duración de la transición de escala en milisegundos
        val scaleAnimationTlfDown = ObjectAnimator.ofPropertyValuesHolder(
            btnPortaPapelTlf,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f, 1.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f, 1.0f)
        )
        scaleAnimationTlfDown.duration = 100 // Duración de la transición de escala hacia abajo en milisegundos

        val animatorTlfSet = AnimatorSet()
        animatorTlfSet.playTogether(scaleAnimationTlfUp,colorAnimationTlf, scaleAnimationTlfDown)
        //**************************************************************


        btnCancelar?.setOnClickListener { cerrarFragmentoConAnimacion() }

        btnAceptar?.setOnClickListener { validar() }

        btnPortaPapelTlf?.setOnClickListener {
            animatorTlfSet.start()
            copiarTlf() }

        btnPortaPapelCi?.setOnClickListener {
            animatorCiSet.start()
            copiarCi() }
        imgAtras?.setOnClickListener { cerrarFragmentoConAnimacion() }


        // Aquí puedes añadir la animación de aparición hacia abajo lentamente
        val animation = AnimationUtils.loadAnimation(context, R.anim.appear_from_top)
        view.startAnimation(animation)

        //********************************************
        obtenerBCV()
        validarFecha()
        return view

    }



    fun setOnSumResultListener(listener: OnSumResultListener) {
        onSumResultListener = listener
    }


    //obtener precio del dollar y Coloca el Total en el TextView del Monto
    private fun obtenerBCV() {
       txtBcv?.text = tazaBcv

        //COLOCA EL PRECIO A DEPOSITAR*****************
        val montoBs = monto.toString().toDouble()*txtBcv?.text.toString().toDouble()
        val montoRedondeado = String.format("%.2f", montoBs)
        textViewMonto?.text = montoRedondeado
    }

    //CREA EL PAGO MOVIL
    private fun createPagomovil() {
        //QUITA LOS DECIMALES*********************


        Log.d("precioBs", "datos de los textView: ${textViewMonto?.text.toString().toDouble()} y tazaBcv.toString().toDouble() ${tazaBcv.toString().toDouble()}")

        val montoBsFragment = textViewMonto?.text.toString().toDouble()
        val montoDolarFragment = textViewMonto?.text.toString().toDouble()/txtBcv?.text.toString().toDouble()
        val tlfFragment = txtTlfFragment?.text.toString()
        val fechaFragment = txtFecha?.text.toString()
        val reci5ultimosFragment = textView5Ultmios?.text.toString()

        val df = DecimalFormat("#.##")
        val extratotalDollarRedondeado = df.format(montoDolarFragment).toDouble()
        val extratotalBsRedondeado = df.format(montoBsFragment).toDouble()

        //*****************************************

        val pagoMovil = PagoMovil(

            idClient = authProvider.getId(),
            nro= reci5ultimosFragment,
            montoBs = extratotalBsRedondeado,
            montoDollar = extratotalDollarRedondeado,
            tlfPago= tlfFragment,
            fechaPago = fechaFragment,
            tazaCambiaria = txtBcv?.text.toString().toDouble(),
            timestamp = Date().time,
            verificado = true,
            date = Date()
        )
        pagoMovilProvider.create(pagoMovil).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(requireContext(), "Datos Enviados para Validar", Toast.LENGTH_LONG).show()
               // onSumResultListener?.onSumResult(montoRedondeadoBs.toDouble(), montoRedondeadoDollar.toDouble()) // Pasa el resultado al listener del Activity
                totalizaPagos()

            } else {
                Toast.makeText(requireContext(), "Error al crear los datos", Toast.LENGTH_LONG).show()
            }
        }
    }



    //PARA CERRA EL FRAGMENT CON ANIMACION***************
    private fun cerrarFragmentoConAnimacion() {
        var animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {

                parentFragmentManager.beginTransaction()
                    .remove(this@VerificacionFragment)
                    .commitAllowingStateLoss()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        view?.startAnimation(animation)
    }

    private fun copiarTlf() {
        copyToClipboard(requireContext(), textViewTlf?.text.toString(),"El Telefono")
    }

    //COPIAR LOS DATOS AL PORTAPEPEL
    private fun copiarCi() {
        copyToClipboard(requireContext(), txtCi?.text.toString(), "La Cedula")

    }

    //FUNCION PARA COPIAR AL PORTA PAPEL
    fun copyToClipboard(context: Context, text: String, titulo: String) {
        // Obtener el servicio del portapapeles
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Crear un objeto ClipData para guardar el texto
        val clipData = ClipData.newPlainText("text", text)

        // Copiar el objeto ClipData al portapapeles
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Se a Copiado $titulo", Toast.LENGTH_SHORT).show()
    }


    //TOTALIZA TODOS LOS RECIBOS DEL CLIENTE
    fun totalizaPagos(){
        pagoMoviles.clear()
        var total = 0.0
        pagoMovilProvider.getPagoMovil(authProvider.getId()).get().addOnSuccessListener { query ->
            if (query != null) {
                if (query.documents.size > 0) {
                    val documents = query.documents

                    for (d in documents) {
                        var pagoMovil = d.toObject(PagoMovil::class.java)
                        pagoMovil?.id = d.id
                        pagoMoviles.add(pagoMovil!!)
                        if (pagoMovil.verificado != true) {
                            totalSinVeriBs += pagoMovil.montoBs!!.toDouble()
                            totalSinVeriBsDollar += pagoMovil.montoDollar!!.toDouble()

                        }

                        if (pagoMovil.verificado != false) {
                            totalBs += pagoMovil.montoBs!!.toDouble()
                            totalDollar += pagoMovil.montoDollar!!.toDouble()
                        }
                    }
                }
            }

            val activity = activity as? TripInfoActivity //CASTEA EL TEXTVIEW SALDO PARA PASAR EL RESULTADO DE LA SUMA DE PAGOMOVIL
            activity?.txtSaldo?.text = totalDollar.toString()
            updateBilletera(authProvider.getId(),totalDollar)

            //CIERRA EL FRAGMENT
            cerrarFragmentoConAnimacion()
            //LLAMA LA FUNCION goto Search del TripMapActivitu para Inicar la Busqueda
                if (activity is TripInfoActivity) {
                    // El Activity es del tipo esperado (MiActivity)
                    val miActivity = activity as TripInfoActivity
                    // Llama a la función en MiActivity
                    miActivity.goToSearchDriver()
                }
        }

    }

    //ACTUALIZA EL EL MONTO EN LA BILLETERA
    private fun updateBilletera(idDocument: String,totalDolar: Double) {
        clientProvider.updateBilleteraClient(idDocument, totalDolar).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("BILLETERA", "totalDollarUpdate: ${totalDolar} ")
            }
            else {
                Log.d("BILLETERA", "FALLO ACTUALIZACION ${totalDolar} ")
            }
        }
    }

    //VALIDA LA FECHA INTRODUCIDA CARDVIEW
    private fun validarFecha() {
        Log.d("FECHA", "VALIDAR FECHA CON/ ")
        txtFecha?.addTextChangedListener(object : TextWatcher {
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

    private fun validar() {

        val banco = textViewBanco?.text.toString()
        MontoBs = textViewMonto?.text.toString()
        val telefono = txtTlfFragment?.text.toString()
        val fecha = txtFecha?.text.toString()
        val recibo = textView5Ultmios?.text.toString()

        if (isValidForm(banco, MontoBs,telefono,fecha,recibo)) {
            mandaInfoAlActivity()
            createPagomovil()
        }
    }

    private fun mandaInfoAlActivity() {
        //MANDA LA INFORMACION DE FRAGMNET AL ACTIVITY
        val montoBsFragment = textViewMonto?.text.toString().toDouble()
        val montoDolarFragment = textViewMonto?.text.toString().toDouble()/txtBcv?.text.toString().toDouble()
        val tlfFragment = txtTlfFragment?.text.toString()
        val fechaFragment = txtFecha?.text.toString()
        val reci5ultimosFragment = textView5Ultmios?.text.toString()
        onSumResultListener?.onSumResult(
            montoBsFragment,
            montoDolarFragment,
            tlfFragment,
            fechaFragment,
            reci5ultimosFragment )
        //****************************************
    }


    private fun isValidForm(banco: String, monto: String, telefono: String, fecha: String, recibo: String): Boolean {

        if (banco.isEmpty()) {

            Toast.makeText(requireContext(), "Ingresa tu Banco", Toast.LENGTH_SHORT).show()

            return false
        }

        if (monto.isEmpty()) {
            textViewMonto?.requestFocus()
            Toast.makeText(requireContext(), "Ingresa el Monto", Toast.LENGTH_SHORT).show()
            return false
        }

        if (telefono.isEmpty()) {
            textViewTlf?.requestFocus()
            Toast.makeText(requireContext(), "Ingresa el Telefono", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fecha.isEmpty()) {
            txtFecha?.requestFocus()
            Toast.makeText(requireContext(), "Ingresa la fecha", Toast.LENGTH_SHORT).show()
            return false
        } else {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")
            dateFormat.isLenient = false

            try {
                val date = dateFormat.parse(fecha)
                val currentDate = Calendar.getInstance().time

                if (date.after(currentDate)) {
                    Toast.makeText(requireContext(), "La fecha no puede ser mayor a la fecha actual", Toast.LENGTH_SHORT).show()
                    txtFecha?.text=""
                    return false
                }
            } catch (e: ParseException) {
                Toast.makeText(requireContext(), "Fecha Invalida", Toast.LENGTH_SHORT).show()
                txtFecha?.text= ""
                return false
            }
        }

        if (recibo.isEmpty()) {
            textView5Ultmios?.requestFocus()
            Toast.makeText(requireContext(), "Ingresa el Nro de Recibo", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    companion object {

        const val TAZA_BCV = "taza_bcv"
        const val MONTO_BUNDLE = "monto_bundle"
    }
}
