package com.carlosvicente.uberkotlin.activities

import java.util.UUID
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.carlosvicente.uberkotlin.databinding.ActivitySearchBinding
import com.carlosvicente.uberkotlin.models.*
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.BookingProvider
import com.carlosvicente.uberkotlin.providers.SolicitudesRealiProvider
import com.carlosvicente.uberkotlin.providers.GeoProvider
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener
import com.ekn.gruzer.gaugelibrary.Range
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout





import com.carlosvicente.uberkotlin.providers.*
import com.example.easywaylocation.EasyWayLocation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.log


class SearchActivity : AppCompatActivity() {
    private val conductor2Ani: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.ani_conductor_conseguido) }


    private var listenerBooking: ListenerRegistration? = null
    private lateinit var binding: ActivitySearchBinding
    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0
    private var extraTime = 0.0
    private var extraDistance = 0.0
    private var extratotalDollar = 0.0
    private var extratotalBs = 0.0
    private var extraTazaBcv = 0.0
    private var nroDeAsig = 0

    private var extraTipoDePago = ""

    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    //PARA CLASIFICAR BUSQUEDA DE TIPO MOTO
    private var extraTipo = ""

    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val clientProvider = ClientProvider()
    private val bookingProvider = BookingProvider()
    private val pagoMovilProvider = PagoMovilProvider()
    private val reciboCondutorProvider = ReciboCondutorlProvider()
    private val solicitudesRealiProvider = SolicitudesRealiProvider()
    private val notificationProvider = NotificationProvider()
    private val driverProvider = DriverProvider()
    private val historyCancelProvider = HistoryCancelProvider()
    private var booking: Booking? = null

    private var swTiempo = false


    // BUSQUEDA DEL CONDUCTOR
    private var radius = 0.2
    private var idDriver = ""
    private var driver: Driver? = null
    private var cliente: Client? = null
    private var isDriverFound = false
    private var conductor1Encontrado = false
    private var conductor2Encontrado = false
    private var conductor3Encontrado = false

    private var prioridadDisponible: Boolean = true
    private var suichePrioridad = false
    private var driverLatLng: LatLng? = null
    private var limitRadius = 30
    private var IntentosBusqueda = 1
    val date = Date()
    var countDownTimer: CountDownTimer? = null

    var valor = 50.0

    private var bookinglate: Booking? = null

    //Moto
    val origin: String? = null
    var isMoto = false

    //buscar 3 conductores
    private var numDriversFound = 0
    var idDriver1 = ""
    var idDriver2 = ""
    var idDriver3= ""
    private lateinit var layoutDatosConductor: FrameLayout
    private lateinit var layoutDatosConductor2: FrameLayout
    private lateinit var layoutDatosConductor3: FrameLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // EXTRAS
        extraTipoDePago = intent.getStringExtra("tipoDepago")!!
        extratotalDollar = intent.getDoubleExtra("total",0.0)
        extratotalBs = intent.getDoubleExtra("totalbs",0.0)
        extraTazaBcv = intent.getDoubleExtra("tazaBcv",0.0)
        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)
        extraTime = intent.getDoubleExtra("time", 0.0)
        extraDistance = intent.getDoubleExtra("distance", 0.0)
        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)
        extraTipo = intent.getStringExtra("tipo")!!

        layoutDatosConductor = findViewById(R.id.layoutdatosConductor)
        layoutDatosConductor2 = findViewById(R.id.layoutdatosConductor2)
        layoutDatosConductor3 = findViewById(R.id.layoutdatosConductor3)

        //SI EL USUARIO CANCELA LA BUSQUEDA
        binding.btnCancelBusqueda .setOnClickListener { mostrarDialog() }
        iniciarBusqueda()
        configurarGaude()

    }

    //INICIAR BUSQUEDA****YO**************
    private fun iniciarBusqueda(){
        //VERIFICA SI ES MOTO O CARRO/////
        //disconnectDriver()
        getClient()//OBTIENE LA INFORMCIONDEL CLIENTE ******* YO *****************
        SaberSiesMoto()

        //COLOCA LA ANIMACION CORREPONDIENTE A LA BUSQUEDA
        BuscaAnimacion(extraTipo)
        if (isMoto!= true){

            //getClosestDriver()
            buscarConductor1()  //PARA BUSCAR3
           // buscarConductor2()

        }
        if (isMoto!= false){
            // getClosestDriverMoto() INAVILITADO TEMPORAL HASTA TENER MOTOS ACTIVAS***********************************
            goToBotonMoto()
        }
        checkIfDriverAccept()
    }


    //ACTIVA LA ANIMACION DEL CONDUCTOR CONSEGUIDO
    private fun animationConductor() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.ani_conductor_conseguido)
        layoutDatosConductor.startAnimation(anim)
    }
    //ACTIVA LA ANIMACION DEL CONDUCTOR CONSEGUIDO REVERSE
    private fun animationConductorReverse() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.ani_conductor_conseguidoreverse)
        layoutDatosConductor.startAnimation(anim)
    }

    //ACTIVA LA ANIMACION DEL CONDUCTOR CONSEGUIDO
    private fun animationConductor2() {

        val anim = AnimationUtils.loadAnimation(this, R.anim.ani_conductor_conseguido2)
        layoutDatosConductor2.startAnimation(anim)

    }
    //ACTIVA LA ANIMACION DEL CONDUCTOR CONSEGUIDO
    private fun animationConductor3() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.ani_conductor_conseguido3)
        layoutDatosConductor3.startAnimation(anim)

    }

    // TEMPORORIZADOR DE ESPERA DE RESPUESTA DEL CONDUCTOR********************YO*********
    private fun activartiempo(){
        countDownTimer = object : CountDownTimer(100000,1000){
            override fun onTick(millisUntilFinished: Long) {
                val segundo = (millisUntilFinished/1000).toInt()
                binding.txtTiempoNro.text= segundo.toString()
                binding.fullGauge.value= segundo.toDouble()
                binding.fullGauge2.value= segundo.toDouble()
                binding.fullGauge3.value= segundo.toDouble()
            }

            override fun onFinish() {
               // disconnectDriver()//DESCONECTA AL CONDUCTOR
                if (!swTiempo){
                    swTiempo= false
                   // cancelBooking(authProvider.getId())
                    binding.txtTiempoNro.text= "SIN REPUESTA INTENTE DE NUEVO"
                }



            }

        }.start()
    }
    //configurar la barra de tiempo Gaude ***yo ******************
    private fun configurarGaude(){
        val range = Range()
        range.color = Color.parseColor("#ce0000")
        range.from = 0.0
        range.to = 20.0

        val range2 = Range()
        range2.color = Color.parseColor("#E3E500")
        range2.from = 20.0
        range2.to = 60.0

        val range3 = Range()
        range3.color = Color.parseColor("#00b20b")
        range3.from = 60.0
        range3.to = 100.0

        binding.fullGauge.minValue = 0.0
        binding.fullGauge.maxValue = 100.0
        binding.fullGauge.value = 100.0

        binding.fullGauge.addRange(range)
        binding.fullGauge.addRange(range2)
        binding.fullGauge.addRange(range3)

        binding.fullGauge.isUseRangeBGColor = true
        binding.fullGauge.isDisplayValuePoint = false

        binding.fullGauge2.minValue = 0.0
        binding.fullGauge2.maxValue = 100.0
        binding.fullGauge2.value = 100.0

        binding.fullGauge2 .addRange(range)
        binding.fullGauge2.addRange(range2)
        binding.fullGauge2.addRange(range3)

        binding.fullGauge2.isUseRangeBGColor = true
        binding.fullGauge2.isDisplayValuePoint = false

        //tercer conductor
        binding.fullGauge3.minValue = 0.0
        binding.fullGauge3.maxValue = 100.0
        binding.fullGauge3.value = 100.0

        binding.fullGauge3 .addRange(range)
        binding.fullGauge3.addRange(range2)
        binding.fullGauge3.addRange(range3)

        binding.fullGauge3.isUseRangeBGColor = true
        binding.fullGauge3.isDisplayValuePoint = false






    }

    private fun BuscaAnimacion(tipo:String){
        Log.d("TIPOV", "VALOR DE TIPO: $tipo")
      if (tipo=="Moto"){
          binding.imgJsonBuscarCarro.visibility = View.GONE
          binding.imgJsonBuscarMoto.visibility = View.VISIBLE
      }
        if (tipo=="Carro"){
            binding.imgJsonBuscarCarro.visibility = View.VISIBLE
            binding.imgJsonBuscarMoto.visibility = View.GONE
        }

    }

    //DESCONECTA AL CONDUCTOR QUE RECHAZO EL BOOKING
    private fun disconnectDriver() {

            if (isMoto != true){
               geoProvider.removeLocation(idDriver)
            }
        // DESCONECTAR MOTO

            if(isMoto!=false){
                geoProvider.removeLocationMoto(idDriver)
            }
    }

//NOTIFICACIONES PUSH
    private fun sendNotification() {
        val map = HashMap<String, String>()
        map.put("title", "SOLICITUD DE VIAJE")
        map.put(
            "body",
            "Un cliente esta solicitando un viaje a " +
                    "${String.format("%.1f",extraDistance)}km y " +
                    "${String.format("%.1f", extraTime)}Min"
        )
        map.put("idBooking", authProvider.getId())

        val body = FCMBody(
            to = driver?.token!!,
            priority = "high",
            ttl = "4500s",
            data = map
        )

        notificationProvider.sendNotification(body).enqueue(object: Callback<FCMResponse> {
            override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                if (response.body() != null) {

                    if (response.body()!!.success == 1) {
                        Toast.makeText(this@SearchActivity, "Se envio la notificacion", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this@SearchActivity, "No se pudo enviar la notificacion", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    Toast.makeText(this@SearchActivity, "hubo un error enviando la notificacion", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                Log.d("NOTIFICATION", "ERROR: ${t.message}")
            }

        })
    }

    //ESPERA LA RESPUESTA DEL CONDUCTOR CAMBIA A ESTADO "aceptado" y envia al goToMapTrip
    private fun checkIfDriverAccept() {
        listenerBooking = bookingProvider.getBooking().addSnapshotListener { snapshot, e ->
            if (e != null) {

                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                booking = snapshot.toObject(Booking::class.java)
                Log.d("VERASIGNADO", "booking: $booking")
                if (booking?.status == "accept") {


                    Toast.makeText(this@SearchActivity, "Viaje aceptado", Toast.LENGTH_LONG).show()
                    swTiempo= true
                    if (countDownTimer != null && countDownTimer?.onFinish()!= null ){
                        swTiempo= true
                        countDownTimer?.cancel()
                    }
                    creaReciboCobro()

                    listenerBooking?.remove()
                    goToMapTrip()
                }
//                if (booking?.idDriverAsig== "Id1Cancelo"){
//
//                }
                else if (booking?.status == "accept" && booking?.idDriverAsig != authProvider.getId()) {

                    //SI EL CONDUCTOR CANCELA LA PETICION DE BOOKING(RESERVA)
                    Toast.makeText(this@SearchActivity, "Viaje cancelado por el conductor Intente de Nuevo", Toast.LENGTH_LONG).show()
                    countDownTimer?.cancel()
                    listenerBooking?.remove()
                    removeBooking()
                    goToMap()
                }

            }
        }
    }

    //crea recibo de cobro para descortar del saldo
    private fun creaReciboCobro() {

    val df = DecimalFormat("#.##")
    val montoDollarRedondeado = df.format(extratotalDollar).toDouble()
    val montoBsRedondeado = df.format(montoDollarRedondeado*extraTazaBcv)

        if (extraTipoDePago== "Billetera") {
            val pagoMovil = PagoMovil(

                idClient = authProvider.getId(),
                nro= "viaje",
                montoBs = -montoBsRedondeado.toDouble(),//ACOMODAR ES TEMPORAL
                montoDollar = -montoDollarRedondeado,
                fechaPago =Date().toString(),
                tazaCambiaria = extraTazaBcv,
                timestamp = Date().time,
                verificado = true,
                destino = extraDestinationName,
                origen = extraOriginName,
                date = Date()
            )
            pagoMovilProvider.create(pagoMovil).addOnCompleteListener {
                if (it.isSuccessful) {
                    creaReciboConductor()
                    Toast.makeText(this@SearchActivity, "Saldo Descontado", Toast.LENGTH_LONG).show()

                } else {
                    Toast.makeText(this@SearchActivity, "Error al Descontar", Toast.LENGTH_LONG).show()
                }
            }
        }else{
            creaReciboConductor()
        }

    }

    //CREA UN RECIBO PARA EL CONDUCTOR QUE ACEPTO LA CARRERA
    private fun creaReciboConductor() {
        //para redondear a 2 decimales
        val df = DecimalFormat("#.##")
        val montoDollarRedondeado = df.format(extratotalDollar).toDouble()
        val montoBsRedondeado = df.format(montoDollarRedondeado*extraTazaBcv)
        val uniqueId: String = UUID.randomUUID().toString()
        val last10Digits = uniqueId.takeLast(10)

        Log.d("BILLETERA", "(booking?.status: ${booking?.status} booking?.asignado: ${booking?.asignado} booking?.idDriverAsig ${booking?.idDriverAsig}")
            val reciboConductor = ReciboConductor(

                idClient = authProvider.getId(),
                idDriver = booking?.idDriverAsig,
                nro =  last10Digits,
                montoBs = montoBsRedondeado.toDouble(),
                montoDollar = montoDollarRedondeado,
                fechaPago =Date().toString(),
                tazaCambiaria = extraTazaBcv,
                timestamp = Date().time,
                tipoPago = extraTipoDePago ,
                verificado = false,
                destino = extraDestinationName,
                origen = extraOriginName,
                date = Date()
            )

            reciboCondutorProvider.crear(reciboConductor).addOnCompleteListener {
                if (it.isSuccessful) {

                    Toast.makeText(this@SearchActivity, "Recibo Creado al Conductor", Toast.LENGTH_LONG).show()

                } else {
                    Toast.makeText(this@SearchActivity, "Error alcrear recibo", Toast.LENGTH_LONG).show()
                }
            }

    }

    // ENVIA A LA PANTALLAS MAPTRIPACTIVITY Y ENVIA EL VALOR DEL TIPO DE VEHICULO********
    private fun goToMapTrip() {
        val i = Intent(this, MapTripActivity::class.java)
        if (extraTipo=="Moto"){
            i.putExtra("tipo", "Moto")
            startActivity(i)
        }

        if (extraTipo == "Carro"){
            i.putExtra("tipo", "Carro")
            startActivity(i)
        }

    }


    //PARA MANDAR A BUSCAR CARRO(MOTO) TEMPORAR HASTA TENER MOTOS
    private fun goToBotonMoto() {
        val i = Intent(this, MotoTemporalActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        i.putExtra("origin", extraOriginName)
        i.putExtra("destination", extraDestinationName)

        //PARA MANDAR A BUSCAR CARRO(MOTO)

        startActivity(i)
    }

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    //MODIFICA EL IDDRIVER2 DEL BOOKING
    private fun updateBookingDriver2(idDriver2: String){
        bookingProvider.editarIdDriver2(authProvider.getId(),idDriver2)
    }

    //MODIFICA EL IDDRIVER3 DEL BOOKING
    private fun updateBookingDriver3(idDriver3: String){
        bookingProvider.editarIdDriver3(authProvider.getId(),idDriver3)
    }

    //CREA LA SOLICITUD DE VIAJE CON ESTATUS "create"
    private fun createBooking(idDriver: String) {
        //para redondear a 2 decimales
        val df = DecimalFormat("#.##")
        val montoDollarRedondeado = df.format(extratotalDollar).toDouble()

        val booking = Booking(
            idClient = authProvider.getId(),
            asignado = false,
            idDriver = idDriver,
            status = "create",
            destination = extraDestinationName,
            origin = extraOriginName,
            time = extraTime,
            km = extraDistance,
            originLat = extraOriginLat,
            originLng = extraOriginLng,
            destinationLat = extraDestinationLat,
            destinationLng = extraDestinationLng,
            price = montoDollarRedondeado,
            priceBs = extratotalBs,
            tipoPago = extraTipoDePago,
            date= Date()
        )
        bookinglate = booking
        bookingProvider.create2(booking).addOnCompleteListener {
            if (it.isSuccessful) {
                //buscarConductor2()
               // Toast.makeText(this@SearchActivity, "Datos del viaje creados", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@SearchActivity, "Error al crear los datos", Toast.LENGTH_LONG).show()
            }
        }
    }

    //CREA LA  registro de solicitudes con ESTATUS "create"****** YO ********************
    private fun createSolicitudes(idCliente: String) {
        Log.d("CLIENTE", "Dentro de creando solicitudes: ${cliente?.email} ${cliente?.name} ${cliente?.lastname}")
        val solicitudes = SolicitudesRealizadas(
            idClient = idCliente,
            idDriver = idDriver,
            email = cliente?.email,
            phone = cliente?.phone,
            name = cliente?.name,
            lastname = cliente?.lastname,
            image = cliente?.image,
            destination = extraDestinationName,
            origin = extraOriginName,
            time = Date().time,
            price = extratotalDollar,
            km = extraDistance,
            fecha= Date()
        )

        solicitudesRealiProvider.create(solicitudes).addOnCompleteListener {
            if (it.isSuccessful) {
                // Toast.makeText(this@SearchActivity, "Datos del viaje creados", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@SearchActivity, "Error al crear los datos", Toast.LENGTH_LONG).show()
            }
        }
    }
    //********************************************************************************************



    //OBTIENE LA INFORMACION DEL CONDUCTOR
    private fun getDriverInfo() {

        driverProvider.getDriver(idDriver).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  getDriverInfo")
                //CARGA LOS DATOS DEL CONDUCTOR A LA BUSQUEDA***********

                Log.d("CONDUCTOR", "CONDUCTOR ENCONTRADO EN datosConductorencontrado: ${driver?.id} y:  ${driver?.name}")
                animationConductor()
                binding.layoutdatosConductor.visibility = View.VISIBLE
                if (conductor2Encontrado){
                    binding.layoutdatosConductor2.visibility= View.VISIBLE
                }
                binding.layoutMarcadorTiempo.visibility= View.VISIBLE
                binding.imgJsonBuscarCarro.visibility = View.GONE

                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(this@SearchActivity).load(driver?.image).into(binding.circleImageConductor)
                    }
                }
                binding.txtNombreConductor.text=driver?.name.toString()
                activartiempo()
                sendNotification() //PRUEVa de 3 CONDUCTORES

                buscarConductor2()
                createSolicitudes(authProvider.getId() )
                //*************************************************

            }
        }

    }
    //OBTIENE LA INFORMACION DEL CONDUCTOR
    private fun getDriver2Info() {

        driverProvider.getDriver(idDriver2).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  getDriverInfo2")
                //CARGA LOS DATOS DEL CONDUCTOR A LA BUSQUEDA***********

                Log.d("CONDUCTOR", "CONDUCTOR ENCONTRADO EN datosConductorencontrado: ${driver?.id} y:  ${driver?.name}")
                animationConductor2()
                binding.layoutdatosConductor.visibility = View.VISIBLE

                binding.layoutdatosConductor2.visibility= View.VISIBLE
                //binding.layoutdatosConductor3.visibility= View.VISIBLE

                binding.layoutMarcadorTiempo.visibility= View.VISIBLE
                binding.imgJsonBuscarCarro.visibility = View.GONE

                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(this@SearchActivity).load(driver?.image).into(binding.circleImageConductor2)
                    }
                }
                binding.txtNombreConductor2.text=driver?.name.toString()
               // activartiempo()
                 sendNotification() //PRUEVa de 3 CONDUCTORES

              //  buscarConductor3()  TEMPORALMENTE PARA NO BUSCAR EL TERCER
                //createSolicitudes(authProvider.getId() )
                //*************************************************

            }
        }

    }

    //OBTIENE LA INFORMACION DEL CONDUCTOR
    private fun getDriver3Info() {

        driverProvider.getDriver(idDriver3).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  getDriverInfo3")
                //CARGA LOS DATOS DEL CONDUCTOR A LA BUSQUEDA***********

                Log.d("CONDUCTOR", "CONDUCTOR ENCONTRADO EN datosConductorencontrado: ${driver?.id} y:  ${driver?.name}")
                animationConductor3()
                binding.layoutdatosConductor.visibility = View.VISIBLE

                binding.layoutdatosConductor2.visibility= View.VISIBLE
                binding.layoutdatosConductor3.visibility= View.VISIBLE

                binding.layoutMarcadorTiempo.visibility= View.VISIBLE
                binding.imgJsonBuscarCarro.visibility = View.GONE

                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(this@SearchActivity).load(driver?.image).into(binding.circleImageConductor3)
                    }
                }
                binding.txtNombreConductor3.text=driver?.name.toString()
                // activartiempo()

                 sendNotification() //PRUEVa de 3 CONDUCTORES

               // createSolicitudes(authProvider.getId() )
                //*************************************************

            }
        }

    }



//CANCELA LA SOLICITUD DE VIAJE Y MANDA A GENERAR LA HISTORIA
    fun cancelBooking(idClient: String) {
        bookingProvider.updateStatus(idClient, "cancel").addOnCompleteListener {

            createHistoryCancel()//CREA HISTORIA DE BOOKING CANCELADOS*******************


        }
    }

    //CREA HISTORIA DE BOOKING CANCELADOS!!!!**************************
    private fun createHistoryCancel() {
        Log.d("PRICE", "VALOR DE TOTAL  ")
        val historyCancel = HistoryDriverCancel(
            idDriver = bookinglate?.idDriver,
            idClient = authProvider.getId(),
            origin = bookinglate?.origin,
            destination = bookinglate?.destination,
            originLat = bookinglate?.originLat,
            originLng = bookinglate?.originLng,
            destinationLat = bookinglate?.destinationLat,
            destinationLng = bookinglate?.destinationLng,
            timestamp = Date().time,
            causa = "Tiempo de Respuesta Condutor",
            fecha = date

        )
        historyCancelProvider.create(historyCancel).addOnCompleteListener {
            if (it.isSuccessful) {

                Log.d("HISTOCANCEL", "LA HISTORIA DE CANCEL $historyCancel ")

            }
        }
    }
    //OBTIENE LA INFOR DEL CLIENTE ****YO***********************************TRAIDO
    private fun getClient() {
        Log.d("CLIENTE", "fuera de getClient: ${authProvider.getId()}")
        clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
            if (document.exists()) {
                val client = document.toObject(Client::class.java)
                cliente = client
                Log.d("CLIENTE", "en getcliente: ${client?.email} ${client?.name} ${client?.lastname}")

            }
        }
    }

    // Función buscarConductor1()
    private fun buscarConductor1(): Boolean {
        var isDriverFound = false // Agregamos una variable local para controlar si se encuentra un conductor
        Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  buscarConductor1 isDriverFound $isDriverFound ")
        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                if (!isDriverFound) {
                    Log.d("ANIMACIONCOND", "DENTRO DEL if  onKeyEntered isDriverFound $isDriverFound ")
                    isDriverFound = true
                    conductor1Encontrado= true
                    numDriversFound++
                    idDriver = documentID
                    idDriver1= documentID
                    nroDeAsig = 1
                    getDriverInfo()

                    driverLatLng = LatLng(location.latitude, location.longitude)
                    binding.textViewSearch.text = "UN VEHICULO ENCONTRADO 1/3"
                    createBooking(documentID)
                }
            }

            override fun onKeyExited(documentID: String) {}

            override fun onKeyMoved(documentID: String, location: GeoPoint) {}

            override fun onGeoQueryError(exception: Exception) {}

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                if (!isDriverFound) {
                    radius = radius + 0.2

                    if (radius > limitRadius) {
                        Log.d("ANIMACIONCOND", "DENTRO DEL Limite radius radius:$radius  isDriverFound:$isDriverFound ")
                        binding.textViewSearch.text = "NO SE ENCONTRO NINGUN VEHICULO"
                        goToMap()
                        return

                    } else {
                     buscarConductor1() // Recursivamente llamamos a la función para continuar la búsqueda
                    }
                }
            }
        })

        return true
    }
    
    //BUSCA CONDUCTOR 2
    private fun buscarConductor2():Boolean {
        var isDriverFound = false // Agregamos una variable local para controlar si se encuentra un conductor
        Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  buscarConductor2")

        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                if (!isDriverFound && idDriver1!= documentID) {
                    isDriverFound = true
                    conductor2Encontrado= true
                    numDriversFound++
                    idDriver2 = documentID
                    nroDeAsig = 2
                    getDriver2Info()

                    driverLatLng = LatLng(location.latitude, location.longitude)
                    binding.textViewSearch.text = "SEGUNDO VEHICULOS ENCONTRADOS 2/3"
                    updateBookingDriver2(idDriver2)
                }
            }

            override fun onKeyExited(documentID: String) {}

            override fun onKeyMoved(documentID: String, location: GeoPoint) {}

            override fun onGeoQueryError(exception: Exception) {}

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                if (!isDriverFound) {
                    radius = radius + 0.3

                    if (radius > limitRadius) {
                        binding.textViewSearch.text = "NO SE ENCONTRO NINGUN VEHICULO"
                        goToMap()
                        return

                    } else {
                         buscarConductor2() // Recursivamente llamamos a la función para continuar la búsqueda
                    }
                }
            }
        })

        return true
    }

    //BUSCA CONDUCTOR 3
    private fun buscarConductor3():Boolean {
        var isDriverFound = false // Agregamos una variable local para controlar si se encuentra un conductor
        Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION  buscarConductor3")

        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                Log.d("ANIMACIONCOND", "DENTRO DE LA FUNCION onKeyEntered DE buscarConductor3 isDriverFound: $isDriverFound idDriver1:$idDriver1 idDriver2:$idDriver2 documentID:$documentID")
                if (!isDriverFound && idDriver1!= documentID && idDriver2!= documentID) {
                    isDriverFound = true
                    conductor3Encontrado= true
                    numDriversFound++
                    idDriver3 = documentID
                    nroDeAsig= 3
                    getDriver3Info()

                    driverLatLng = LatLng(location.latitude, location.longitude)
                    binding.textViewSearch.text = "TERCER VEHICULOS ENCONTRADOS 3/3"
                    updateBookingDriver3(idDriver3)
                }
            }

            override fun onKeyExited(documentID: String) {}

            override fun onKeyMoved(documentID: String, location: GeoPoint) {}

            override fun onGeoQueryError(exception: Exception) {}

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                if (!isDriverFound) {
                    radius = radius + 0.3

                    if (radius > limitRadius) {
                       // binding.textViewSearch.text = "NO SE ENCONTRO NINGUN VEHICULO"
                       // goToMap()
                        Log.d("ANIMACIONCOND", "NO SE CONSIGUIO CONDUCTOR3 onGeoQueryReady ")
                        return

                    } else {
                        buscarConductor3() // Recursivamente llamamos a la función para continuar la búsqueda
                    }
                }
            }
        })

        return true
    }


   
    // BUSCA SOLO CARRO/**************************
    private fun  getClosestDriver() {
        //BUSCA AL CONDUCTOR PRIORIDAD() ********
        if (!suichePrioridad){
            suichePrioridad= true
            geoProvider.getLocatioPrioridad("hrypyVnj1UQI673pQwgtpHmEqWh2").addOnSuccessListener { document ->
                if (document.exists()) {
                    if (document.contains("l")) {
                        prioridadDisponible = true
                    }else{
                        prioridadDisponible= false
                    }
                }
            }
        }

        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object: GeoQueryEventListener {

            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                Log.d("prioridad", "override onKeyEntered: IDDRIVER $idDriver")
                if (!isDriverFound) {
                    //LOCALIZA AL TAXI CON PRIORIDAD*****YO***************

                    if (prioridadDisponible){
                        val idDriverorioridad = "hrypyVnj1UQI673pQwgtpHmEqWh2"

                        if (documentID== idDriverorioridad){
                            isDriverFound = true
                            idDriver = documentID
                            getDriverInfo()

                            driverLatLng = LatLng(location.latitude, location.longitude)
                            binding.textViewSearch.text = "VEHICULO ENCONTRADO\n" +
                                    "ESPERANDO RESPUESTA DEL CONDUCTOR S/N:$idDriver"
                            suichePrioridad= false
                            createBooking(documentID)

                        }

                    }else{
                        if (!prioridadDisponible){
                            isDriverFound = true
                            idDriver = documentID
                            getDriverInfo()

                            Log.d("Prioridad", "Conductor CUANDO NO ESTA ACTIVO PRIORITY: $idDriver y prioridadDisponible $prioridadDisponible")
                            driverLatLng = LatLng(location.latitude, location.longitude)
                            binding.textViewSearch.text = "VEHICULO ENCONTRADO\nESPERANDO RESPUESTA DEL CONDUCTOR S/N:$idDriver"

                            suichePrioridad = false
                            //CREA EL BOOKING EN ESTADO CREADO
                            createBooking(documentID)
                            //  createSolicitudes(authProvider.getId() )
                        }

                    }

                }
            }

            override fun onKeyExited(documentID: String) {

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                Log.d("prioridad", "onGeoQueryReady: IDDRIVER y RADIUS $idDriver y: $radius")
                if (!isDriverFound) {
                    radius = radius + 0.2
                    if(radius>12.0 && prioridadDisponible ==true){
                        Log.d("Prioridad", "ENTRO A RADIUS MAS DE 12:$prioridadDisponible y Radius: $radius")
                        prioridadDisponible= false
                        radius = 0.0
                    }

                    if (radius > limitRadius) {
                        Log.d("Prioridad", "entro a limit radius $radius")
                        binding.textViewSearch.text = "NO SE ENCONTRO NINGUN CONDUCTOR"
                        removeBooking()//REMUEVE EL BOOKING PORQ NO CONSIGUIO EL CONDUCTOR ******YO*******
                        return
                    }
                    else {
                        getClosestDriver()
                    }
                }
            }

        })
    }


    // VERIFICA SI ES CARRO O MOTO
    private fun SaberSiesMoto(){

        if (extraTipo != "Carro"){
             isMoto = true
        }
        if (extraTipo != "Moto"){
                    isMoto = false
        }
        Log.d("TIPOV", "Moto o Carro: $isMoto")
    }

    //MENSAGE DE CONFIRMACION DE SALIDA*********************

    fun mostrarDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cancelar Solitudud")
        builder.setMessage("Desea cancelar la Busqueda")
        builder.setPositiveButton("Si", DialogInterface.OnClickListener { dialog, which ->
            cancelSolicitud()
        })
        builder.setNegativeButton("No",null )
        builder.show()
    }

    // BORRA EL BOOKING Y VUELVE AL MAP ACTIVITY
    private fun cancelSolicitud(){
        removeBooking()
        goToMap()
    }

    //ELIMINA EL BOOKING
    private fun removeBooking() {

        bookingProvider.getBooking().get().addOnSuccessListener { document ->

            if (document.exists()) {
                val booking = document.toObject(Booking::class.java)
                if (booking?.status == "create"|| booking?.status == "cancel" ) { // Como iba antes || booking?.status == "cancel"
                    bookingProvider.remove()
                }
            }

        }
    }
    //************************************************************************
    override fun onBackPressed() {
        mostrarDialog()
        return
        super.onBackPressed()
    }

    //BORRA EL ESCUCHADOR DEL CLIENTE
    override fun onDestroy() {
        super.onDestroy()
        listenerBooking?.remove()
    }


}

