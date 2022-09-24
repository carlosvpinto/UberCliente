package com.carlosvicente.uberkotlin.activities


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.carlosvicente.uberkotlin.databinding.ActivitySearchBinding
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.BookingProvider
import com.carlosvicente.uberkotlin.providers.GeoProvider
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener


import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.models.FCMBody
import com.carlosvicente.uberkotlin.models.FCMResponse
import com.carlosvicente.uberkotlin.providers.*
import com.example.easywaylocation.EasyWayLocation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SearchActivity : AppCompatActivity() {

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

    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    //PARA CLASIFICAR BUSQUEDA DE TIPO MOTO
    private var extraTipo = ""

    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()
    private val notificationProvider = NotificationProvider()
    private val driverProvider = DriverProvider()


    // BUSQUEDA DEL CONDUCTOR
    private var radius = 0.2
    private var idDriver = ""
    private var driver: Driver? = null
    private var isDriverFound = false
    private var driverLatLng: LatLng? = null
    private var limitRadius = 30
    private var IntentosBusqueda = 1

    //Moto
    val origin: String? = null
    var isMoto = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // EXTRAS
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




        //SI EL USUARIO CANCELA LA BUSQUEDA
        binding.btnCancelBusqueda .setOnClickListener { cancelSolicitud() }

        //VERIFICA SI ES MOTO O CARRO/////

        SaberSiesMoto()

        //COLOCA LA ANIMACION CORREPONDIENTE A LA BUSQUEDA
        BuscaAnimacion(extraTipo)


        Log.d("TIPOV", "isMoto: $isMoto")
        if (isMoto!= true){
            getClosestDriver()
        }
        if (isMoto!= false){
            getClosestDriverMoto()
        }
        checkIfDriverAccept()
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

    // PARA VOLVER HACER LA BUSQUEDA SI EL CONDUCTOR CANCELA
    private fun vuelveBuscarSiCancela(){

        disconnectDriver()
        isDriverFound = false
        //VERIFICA SI ES MOTO O CARRO/////
        SaberSiesMoto()
        Log.d("INTENTOS", "isMoto: $isMoto")
        if (isMoto!= true){

            getClosestDriver()
        }
        if (isMoto!= false){

            getClosestDriverMoto()
        }
        checkIfDriverAccept()
    }

    //DESCONECTA AL CONDUCTOR QUE RECHAZO EL BOOKING
    private fun disconnectDriver() {



        Log.d("INTENTOS", "isMoto: $isMoto")


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
                Log.d("FIRESTORE", "ERROR: ${e.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val booking = snapshot.toObject(Booking::class.java)

                if (booking?.status == "accept") {
                    Toast.makeText(this@SearchActivity, "Viaje aceptado", Toast.LENGTH_SHORT).show()
                    listenerBooking?.remove()
                    goToMapTrip()
                }
                else if (booking?.status == "cancel") {

                    //SI EL CONDUCTOR CANCELA LA PETICION DE BOOKING(RESERVA)
                    Toast.makeText(this@SearchActivity, "Viaje cancelado", Toast.LENGTH_SHORT).show()
                    listenerBooking?.remove()
                    removeBooking()
                    if (IntentosBusqueda <5){
                        IntentosBusqueda++
                        Log.d("INTENTOS", "IntentosBusqueda: ${IntentosBusqueda}")
                        vuelveBuscarSiCancela()
                    }else{
                        goToMap()
                    }

                }

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

    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    //CREA LA SOLICITUD DE VIAJE CON ESTATUS "create"
    private fun createBooking(idDriver: String) {

        val booking = Booking(
            idClient = authProvider.getId(),
            idDriver = idDriver,
            status = "create",
            destination = extraDestinationName,
            origin = extraOriginName,
            time = extraTime,
            km = extraDistance,
            originLat = extraOriginLat,
            originLng = extraOriginLng,
            destinationLat = extraDestinationLat,
            destinationLng = extraDestinationLng
        )

        bookingProvider.create(booking).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this@SearchActivity, "Datos del viaje creados", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this@SearchActivity, "Error al crear los datos", Toast.LENGTH_LONG).show()
            }
        }
    }



    //OBTIENE LA INFORMACION DEL CONDUCTOR
    private fun getDriverInfo() {

        driverProvider.getDriver(idDriver).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                sendNotification()
            }
        }

    }

    //PARA BUSCAR SOLO A MOTOS
    private fun getClosestDriverMoto() {
        geoProvider.getNearbyDriversMoto(originLatLng!!, radius).addGeoQueryEventListener(object: GeoQueryEventListener {

            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                if (!isDriverFound) {
                    isDriverFound = true
                    idDriver = documentID
                    getDriverInfo()
                    Log.d("FIRESTORE", "Conductor id: $idDriver")
                    driverLatLng = LatLng(location.latitude, location.longitude)
                    //VERICA CUANTOS INTENTOS DE BUSQUEDA LLEVA

                        binding.textViewSearch.text = "MOTO ENCONTRADA\nESPERANDO RESPUESTA (BUSQUEDA $IntentosBusqueda)/5 $idDriver"



                    createBooking(documentID)
                }
            }

            override fun onKeyExited(documentID: String) {

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                if (!isDriverFound) {
                    radius = radius + 0.2

                    if (radius > limitRadius) {
                        binding.textViewSearch.text = "NO SE ENCONTRO NINGUNA MOTO"
                        goToMap()
                        return

                    }
                    else {
                        getClosestDriverMoto()
                    }
                }
            }

        })
    }


// BUSCA SOLO CARRO
    private fun getClosestDriver() {
        geoProvider.getNearbyDrivers(originLatLng!!, radius).addGeoQueryEventListener(object: GeoQueryEventListener {

            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                if (!isDriverFound) {
                    isDriverFound = true
                    idDriver = documentID
                    getDriverInfo()
                    Log.d("FIRESTORE", "Conductor id: $idDriver")
                    driverLatLng = LatLng(location.latitude, location.longitude)
                    binding.textViewSearch.text = "MOTO ENCONTRADA\nESPERANDO RESPUESTA (BUSQUEDA $IntentosBusqueda)/5 $idDriver"

                    //CREA EL BOOKING EN ESTADO CREADO
                    createBooking(documentID)
                }
            }

            override fun onKeyExited(documentID: String) {

            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() { // TERMINA LA BUSQUEDA
                if (!isDriverFound) {
                    radius = radius + 0.2

                    if (radius > limitRadius) {
                        binding.textViewSearch.text = "NO SE ENCONTRO NINGUN CONDUCTOR"
                        return
                    }
                    else {
                        getClosestDriver()
                    }
                }
            }

        })
    }

    //BORRA EL ESCUCHADOR DEL CLIENTE
    override fun onDestroy() {
        super.onDestroy()
        listenerBooking?.remove()
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
                if (booking?.status == "create" || booking?.status == "cancel") {
                    bookingProvider.remove()
                }
            }

        }
    }



}

