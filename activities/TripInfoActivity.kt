package com.carlosvicente.uberkotlin.activities

import android.content.DialogInterface
import java.time.LocalTime
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityTripInfoBinding
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.models.Prices
import com.carlosvicente.uberkotlin.providers.*
import com.google.firebase.firestore.ListenerRegistration
import com.tommasoberlose.progressdialog.ProgressDialogFragment


//import com.carlosvicente.uberkotlin.models.Prices
//import com.carlosvicente.uberkotlin.providers.ConfigProvider

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {

    private lateinit var binding: ActivityTripInfoBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    //PARA VERIFICAR SI TIENE BOOKING ACTIVO
    private val bookingProvider = BookingProvider()
    private var listenerBooking: ListenerRegistration? = null
    private var booking: Booking? = null
    private var isBookingLoaded = false
    private var activo = "true"
    private var extraTipo = ""
    private val driverProvider = DriverProvider()
    private var driver: Driver? = null
    private val authProvider = AuthProvider()
    private var pagoMoviles = ArrayList<PagoMovil>()
    private var pagoMovilProvider = PagoMovilProvider()
    private val clientProvider = ClientProvider()
    private var totalBs = 0.0
    private var totalDollar= 0.0
    private var totalSinVeriBs = 0.0
    private var totalSinVeriBsDollar = 0.0
    private var tipoDepago = ""

    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil

    private var markerOrigin: Marker? = null
    private var markerDestination: Marker? = null


    var distance = 0.0
    var time = 0.0
    var total = 0.0

    private var progressDialog = ProgressDialogFragment

    //CARRO O MOTO
    private var tipoVehiculo = ""
    private val geoProvider = GeoProvider()

    private var configProvider = ConfigProvider()
    //VERIFICA SI ES DE NOCHE
    private var esNoturno = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        progressDialog.showProgressBar(this)
        // EXTRAS
        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!
        extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
        extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)
        tipoVehiculo = intent.getStringExtra("tipo")!!
        originLatLng = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)
        totalizaPagos()

        Log.d("PLACESTRIP", "onCreate:destinationLatLng= $destinationLatLng  y destinationLatLng!!.latitude= ${destinationLatLng!!.latitude} ")
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }


        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

       // binding.textViewOrigin.text = extraOriginName
        binding.textViewDestination.text = extraDestinationName


        binding.imageViewBack.setOnClickListener { finish() }
        binding.btnConfirmRequest.setOnClickListener { goToSearchDriver()}

    }

    //TOTALIZA TODOS LOS RECIBOS DEL CLIENTE
    private fun totalizaPagos(){
        pagoMoviles.clear()
        Log.d("PAGOMOVIL", "getPagosMoviles: ")
        var total = 0.0
        pagoMovilProvider.getPagoMovil(authProvider.getId()).get().addOnSuccessListener { query ->
            Log.d("PAGOMOVIL", "authProviderA: ${authProvider.getId()}")
            if (query != null) {
                if (query.documents.size > 0) {
                    val documents = query.documents

                    for (d in documents) {
                        var pagoMovil = d.toObject(PagoMovil::class.java)
                        pagoMovil?.id = d.id
                        pagoMoviles.add(pagoMovil!!)
                        if (pagoMovil.verificado != true) {
                            Log.d("COUNTAR", "ADENTRO ADETRO VERIFICADO FALSE:${pagoMovil.verificado} y $totalDollar ")
                            totalSinVeriBs += pagoMovil.montoBs!!.toDouble()
                            totalSinVeriBsDollar += pagoMovil.montoDollar!!.toDouble()
                        }

                        if (pagoMovil.verificado != false) {
                            Log.d("COUNTAR", "ADENTRO VERIFICADO TRUE: ${pagoMovil.verificado} y $totalDollar ")
                            totalBs += pagoMovil.montoBs!!.toDouble()
                            totalDollar += pagoMovil.montoDollar!!.toDouble()
                        }
                    }
                }
            }
            val totalVerdes = totalDollar
            binding.txtSaldo.text = totalDollar.toString() + "$"
            progressDialog.hideProgressBar(this)
            updateBilletera(authProvider.getId(),totalVerdes)
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




    private fun goToSearchDriver() {
        //verifica que el boton de pago con billetera esta activo
        if(binding.optBilletera.isChecked){
            tipoDepago = "Billetera"
            if (total>totalDollar){
                //MENSAGE DE CONFIRMACION DE SALIDA*********************

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Recargar")
                    builder.setMessage("Saldo Insuficiente, Desea Recargar Ahora??")
                    builder.setPositiveButton("Si", DialogInterface.OnClickListener { dialog, which ->

                        goToBankActivity()
                    })
                    builder.setNegativeButton("No",null )
                    builder.show()


                return
            }else{

                if (originLatLng != null && destinationLatLng != null) {
                    val i = Intent(this, SearchActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    i.putExtra("origin", extraOriginName)
                    i.putExtra("destination", extraDestinationName)
                    i.putExtra("origin_lat", originLatLng?.latitude)
                    i.putExtra("origin_lng", originLatLng?.longitude)
                    i.putExtra("destination_lat", destinationLatLng?.latitude)
                    i.putExtra("destination_lng", destinationLatLng?.longitude)
                    i.putExtra("time", time)
                    i.putExtra("distance", distance)
                    //PARA MANDAR A BUSCAR CARRO(MOTO)
                    i.putExtra("tipo",tipoVehiculo)
                    i.putExtra("total",total)
                    i.putExtra("tipoDepago",tipoDepago)
                    startActivity(i)
                }
                else {
                    Toast.makeText(this, "Debes seleccionar el origin y el destino", Toast.LENGTH_LONG).show()
                }

            }
        }else{
            tipoDepago= "Efectivo"
            if (originLatLng != null && destinationLatLng != null) {
                val i = Intent(this, SearchActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                i.putExtra("origin", extraOriginName)
                i.putExtra("destination", extraDestinationName)
                i.putExtra("origin_lat", originLatLng?.latitude)
                i.putExtra("origin_lng", originLatLng?.longitude)
                i.putExtra("destination_lat", destinationLatLng?.latitude)
                i.putExtra("destination_lng", destinationLatLng?.longitude)
                i.putExtra("time", time)
                i.putExtra("distance", distance)
                //PARA MANDAR A BUSCAR CARRO(MOTO)
                i.putExtra("tipo",tipoVehiculo)
                i.putExtra("total",total)
                i.putExtra("tipoDepago",tipoDepago)
                startActivity(i)
            }
            else {
                Toast.makeText(this, "Debes seleccionar el origin y el destino", Toast.LENGTH_LONG).show()
            }
        }
    }

        //Va al banco para ReCargar
    private fun goToBankActivity() {
        val i = Intent(this, BankActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)

    }

    //OPTIENE EL PRECIO DE VIAJE!(YO)**********
    private fun getPrices(distance: Double, time: Double) {
        //** verifica si es horario nocturno*************
            val horaActual = LocalTime.now()
            val horaLimite1 = LocalTime.of(23, 0) // 11:00 PM
            val horaLimite2 = LocalTime.of(6, 0) // 6:00 AM

            if (horaActual.isAfter(horaLimite1) || horaActual.isBefore(horaLimite2)) {
               esNoturno=true
                binding.textTipoTarifa.text= "Nocturna"

            } else {
                esNoturno= false
                binding.textTipoTarifa.text= "Diurna"
            }
        //***********************************************


        configProvider.getPrices().addOnSuccessListener { document ->

            var CcortaMoto = 0.0
            var CmediaMoto = 0.0
            var ClargaMoto = 0.0
            var CcortaCarro = 0.0
            var CmediaCarro = 0.0
            var ClargaCarro = 0.0
            var kmCarro = 0.0
            var kmMoto = 0.0
            if (document.exists()) {
                val prices = document.toObject(Prices::class.java) // DOCUMENTO CON LA INFORMACION
                if (esNoturno){
                    CcortaMoto = prices?.CcortaMoto!!.toDouble()*1.5
                     CmediaMoto = prices?.CmediaMoto!!.toDouble()*1.5
                     ClargaMoto = prices?.CLargaMoto!!.toDouble()*1.5
                     CcortaCarro = prices?.CcortaCarro!!.toDouble()*1.5
                     CmediaCarro = prices?.CMediaCarro!!.toDouble()*1.5
                     ClargaCarro = prices?.CLargaCarro!!.toDouble()*1.5
                     kmCarro = prices?.kmCarro!!.toDouble()*1.5
                     kmMoto = prices?.kmMoto!!.toDouble()*1.5
                }else{

                }
                if (!esNoturno){
                     CcortaMoto = prices?.CcortaMoto!!.toDouble()
                     CmediaMoto = prices?.CmediaMoto!!.toDouble()
                     ClargaMoto = prices?.CLargaMoto!!.toDouble()
                     CcortaCarro = prices?.CcortaCarro!!.toDouble()
                     CmediaCarro = prices?.CMediaCarro!!.toDouble()
                     ClargaCarro = prices?.CLargaCarro!!.toDouble()
                     kmCarro = prices?.kmCarro!!.toDouble()
                     kmMoto = prices?.kmMoto!!.toDouble()
                }


                if (tipoVehiculo == "Carro"){

                    if (distance<3.2) {
                        total = CcortaCarro!!.toDouble()
                    }
                    if (distance>3.2 && distance<5) {
                        total = CmediaCarro!!.toDouble()
                    }
                    if (distance>5 && distance<7){
                        total = ClargaCarro!!.toDouble()
                    }
                    if (distance>7 ){ // FALTA CALCULAR BIEN DESPUES DE 12KM
                        total =  ClargaCarro!!.toDouble()+ (distance-7)*kmCarro!!.toDouble()
                    }

                }

                if (tipoVehiculo == "Moto"){
                    if (distance<3) {
                        total = CcortaMoto!!.toDouble()
                    }
                    if (distance>3 && distance<5) {
                        total = CmediaMoto!!.toDouble()
                    }
                    if (distance>5 && distance<7){
                        total = ClargaMoto!!.toDouble()
                    }
                    if (distance>7){ // FALTA CALCULAR BIEN DESPUES DE 12KM
                        total =  ClargaMoto!!.toDouble() + (distance-7)*kmMoto!!.toDouble()
                    }

                }
                Log.d("PRICE", "VALOS FINAL DE TOTAL: $total ")
                val minTotalString = String.format("%.1f", total)
                //  val maxTotalString = String.format("%.1f", maxTotal)
                binding.textViewPrice.text = "$minTotalString$"

                progressDialog.hideProgressBar(this)
            }


        }.addOnFailureListener{
            Toast.makeText(this, "Problemas de conexion Verifique el acceso a internet", Toast.LENGTH_SHORT).show()
            return@addOnFailureListener
        }
    }



    private fun addOriginMarker() {
        markerOrigin = googleMap?.addMarker(MarkerOptions().position(originLatLng!!).title("Mi posicion")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
    }

    private fun addDestinationMarker() {
        markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("LLegada")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))

    }

    private fun easyDrawRoute() {

        if (originLatLng != null && destinationLatLng != null && originLatLng!!.latitude != 0.0 && originLatLng!!.longitude != 0.0 && destinationLatLng!!.latitude != 0.0 && destinationLatLng!!.longitude != 0.0) {
        wayPoints.add(originLatLng!!)
            Log.d("PLACESTRIP", "easyDrawRoute:$destinationLatLng Y ${destinationLatLng!!.latitude} ")
            Log.d("PLACESTRIP", "wayPoints:$wayPoints Y $googleMap ")
        wayPoints.add(destinationLatLng!!)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLng!!)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.green)
            .setPolyLineWidth(15)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLng!!)
            .build()

        directionUtil.initPath()
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

// ACTIVA LA POSCION DE LA CAMARA (YO)
        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(originLatLng!!).zoom(12f).build()
            ))
        easyDrawRoute()
        addOriginMarker()
        addDestinationMarker()

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )
            if (!success!!) {
                Log.d("MAPAS", "No se pudo encontrar el estilo")
            }

        } catch (e: Resources.NotFoundException) {
            Log.d("MAPAS", "Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        Log.d("PLACESTRIP", "pathFindFinish:distance;$distance time:$time polyLineDetailsArray[1] ${polyLineDetailsArray[1]}" )
        distance = polyLineDetailsArray[1].distance.toDouble() // METROS
        time = polyLineDetailsArray[1].time.toDouble() // SEGUNDOS
        distance = if (distance < 1000.0) 1000.0 else distance // SI ES MENOS DE 1000 METROS EN 1 KM
        time = if (time < 60.0) 60.0 else time

        distance = distance / 1000 // KM
        time = time / 60 // MIN

        val timeString = String.format("%.2f", time)
        val distanceString = String.format("%.2f", distance)


        getPrices(distance, time)
        binding.textViewTimeAndDistance.text = "$timeString mins - $distanceString km"
        Log.d("verpatFInd2", "pathFindFinish:distance;$distance time:$time polyLineDetailsArray[1] ${polyLineDetailsArray[1]}")

    trazarlinea()



    }
    private fun trazarlinea (){

        try {
            // Código que puede generar una excepción
            directionUtil.drawPath(WAY_POINT_TAG);
        } catch (e: NullPointerException) {
            Toast.makeText(this, "Error locacion $e", Toast.LENGTH_LONG).show()
            Log.d("PLACES", "Mensaje error: ${e.message}")
            Log.d("CAMPO", "VALOR Null: NullPointerException: $e")
            return
        }
    }
}