package com.carlosvicente.uberkotlin.adapters

import android.app.Activity
import android.content.Intent
import android.graphics.Color.red
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.activities.HistoriesDetailActivity
import com.carlosvicente.uberkotlin.databinding.ActivityBancoprincipalBinding
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.History
import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.providers.*
import com.carlosvicente.uberkotlin.utils.RelativeTime
import com.google.firebase.firestore.ListenerRegistration
import com.tommasoberlose.progressdialog.ProgressDialogFragment

private val clientProvider = ClientProvider()
private var authProvider = AuthProvider()
private var totalDollarUpdate = 0.0
class PagoMovilAdapter(val context: Activity, var pagoMoviles: ArrayList<PagoMovil>): RecyclerView.Adapter<PagoMovilAdapter.PagoMovilAdapterViewHolder>() {


    //PARA SUMAR TODO LOS VALORES DEL ARRAYS PAGOMOVILES*********************
    private var itemCount: Int = 0 // variable para almacenar la cantidad de elementos en la lista

    init {
        var totalBs = 0.0
        var totalDollar= 0.0
        var totalSinVeriBs = 0.0
        var totalSinVeriBsDollar = 0.0

        for (item in pagoMoviles) {
            if (item.verificado!=true){
                Log.d("COUNTAR", "ADENTRO ADETRO VERIFICADO FALSE: ")
                totalSinVeriBs += item.montoBs!!.toDouble()
                totalSinVeriBsDollar +=item.montoDollar!!.toDouble()
            }

            if (item.verificado!= false){
                Log.d("COUNTAR", "ADENTRO VERIFICADO TRUE: ")
                totalBs +=item.montoBs!!.toDouble()
                totalDollar+= item.montoDollar!!.toDouble()
            }
        }
        totalDollarUpdate = totalDollar
        updateBilletera(authProvider.getId()!!)//llama a acuatlizar la billetera del cliente


        Log.d("COUNTAR", "pagoMoviles.size: ${pagoMoviles.size} ")
        val textView = context.findViewById<TextView>(R.id.txttotaldolares)
        val textViewSinVeri = context.findViewById<TextView>(R.id.txttotalSinveri)
        textViewSinVeri.text= totalSinVeriBsDollar.toString()// coloca el monto de los dolares sin verificar
        textView.text = totalDollar.toString()//coloca el monto de los dolares Verificados

        itemCount = pagoMoviles.size
    }
//**************************************************************

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoMovilAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_depositos, parent, false)
        return PagoMovilAdapterViewHolder(view)
    }

    // ESTABLECER LA INFORMACION
    override fun onBindViewHolder(holder: PagoMovilAdapterViewHolder, position: Int) {
        val pagoMovil =  pagoMoviles[position] // UN SOLO HISTORIAL
        holder.textViewFecha.text = pagoMovil.fechaPago
        holder.textViewMontoBs.text = pagoMovil.montoBs.toString()
        val montoDollar = pagoMovil.montoDollar.toString().toDouble()
        holder.textViewMontoDollar.text = pagoMovil.montoDollar.toString()
        holder.textViewNroRecibo.text = pagoMovil.nro.toString()
        if (pagoMovil.verificado!= null){
            holder.checkVerificacion.isChecked= pagoMovil.verificado!!
        }
        if (pagoMovil.timestamp != null) {
            holder.textViewtimestamp.text = RelativeTime.getTimeAgo(pagoMovil.timestamp!!, context)
        }
        if (montoDollar < 0) { // Verificar si el valor es negativo
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.greyligh)) //CAMBIA DE COLOR EL FON DEL CARDVIEW
            holder.textViewMontoDollar.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rojo))
            holder.textViewMontoBs.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rojo))
            holder.checkVerificacion.visibility = View.GONE
        }
       // holder.itemView.setOnClickListener { goToDetail(pagoMovil?.id!!) } //para no llamar al activity al gacer click
    }

    private fun goToDetail(idHistory: String) {
        val i = Intent(context, HistoriesDetailActivity::class.java)
        i.putExtra("id", idHistory)
        context.startActivity(i)
    }

    // EL TAMAñO DE LA LISTA QUE VAMOS A MOSTRAR
    override fun getItemCount(): Int {
        return pagoMoviles.size
    }
    fun updatePagosRealizados(pagosRealizadosList: List<PagoMovil> ){
        this.pagoMoviles = pagosRealizadosList as ArrayList<PagoMovil>
        notifyDataSetChanged()
    }

    class PagoMovilAdapterViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val textViewFecha: TextView
        val textViewMontoBs: TextView
        val textViewMontoDollar: TextView
        val textViewNroRecibo: TextView
        val textViewtimestamp: TextView
        val cardView: CardView // Nueva referencia a la CardView
        val checkVerificacion: CheckBox

        init {
            textViewFecha = view.findViewById(R.id.textViewFDeposito)
            textViewMontoBs = view.findViewById(R.id.textViewMontoBs)
            textViewMontoDollar = view.findViewById(R.id.textViewMontoDollar)
            textViewNroRecibo = view.findViewById(R.id.textViewNroRecibo)
            textViewtimestamp = view.findViewById(R.id.textViewTimestamp)
            cardView = view.findViewById(R.id.cardView) // Inicializar la referencia a la CardView
            checkVerificacion= view.findViewById(R.id.checkboxVerificacion)

        }
    }
    private fun updateBilletera(idDocument: String) {
        clientProvider.updateBilleteraClient(idDocument, totalDollarUpdate).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("BILLETERA", "totalDollarUpdate: ${totalDollarUpdate} ")
            }
            else {
                Log.d("BILLETERA", "FALLO ACTUALIZACION ${totalDollarUpdate} ")
            }
        }
    }
}