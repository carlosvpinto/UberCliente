package com.carlosvicente.uberkotlin.activities

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import com.google.android.gms.common.api.Status
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityMapBinding
import com.carlosvicente.uberkotlin.fragments.ModalBottomSheetMenu
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.Client
import com.carlosvicente.uberkotlin.models.DriverLocation
import com.carlosvicente.uberkotlin.providers.*
import com.carlosvicente.uberkotlin.utils.CarMoveAnim
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.*
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener
import org.jsoup.Jsoup
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


data class Direccion(val nombre: String, val latitud: Float, val longitud: Float)



class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding: ActivityMapBinding
    private var location: LatLng? = null
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val clientProvider = ClientProvider()
    private val bookingProvider = BookingProvider()
    private val configProvider = ConfigProvider()
    private var cliente: Client? = null

    // GOOGLE PLACES
    private var places: PlacesClient? = null
    private var autocompleteOrigin: AutocompleteSupportFragment? = null
    private var  autocompleteDestination: AutocompleteSupportFragment? = null
    private var originName = ""
    private var destinationName = ""
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var isLocationEnabled = false
    private var idDriver = ""


    // PARA MOTO
    private val driverMarkersMoto = ArrayList<Marker>()
    private val driversLocationMoto = ArrayList<DriverLocation>()

    private val driverMarkers = ArrayList<Marker>()
    private val driversLocation = ArrayList<DriverLocation>()
    private val modalMenu = ModalBottomSheetMenu()
    private val tipo = ""

    //PARA VERIFICAR CON GOOGLE
    private lateinit var auth : FirebaseAuth
    private var client: Client? = null
    private lateinit var clientExtra: Client
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private  val MAX_DIRECCIONES_GUARDADAS = 3

    data class Direccion(val nombre: String, val latitud: Float, val longitud: Float)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)



        //RECIBO EL BOOKING****************
        val extras = intent.extras
        if (extras != null) {
            val data = extras?.getString("client")
            clientExtra = Client.fromJson(data!!)!!
            Log.d("CLIENTE", "CLIENTE RECIBIDO EN MAP ACTIVITY: $clientExtra ")
            if(clientExtra?.image== null ||clientExtra?.image==""){
                Toast.makeText(this, "Por favor Agrege su Foto de perfil", Toast.LENGTH_LONG).show()
            }
        }


        //PARA ACTUALIZAR EL PRECIO DEL DOLAR SOLO CUANDO CARGA POR PRIMERA VEZ
        if(savedInstanceState== null){
            obtenerPrecioDolar()
        }

        //PARA VERIFICAR CON GOOGLE
        auth = FirebaseAuth.getInstance()


        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        binding.imageViewMenu.isClickable= false
        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        googleAnalytics()
        startGooglePlaces()
        removeBooking()
        createToken()
      //  startNetworkMonitoring(this)
        FirebaseAnalytics.getInstance(this)
        disableSSLVerification()


        binding.btnSolicitarMoto.setOnClickListener { goToTripMotoInfo() }
        binding.btnBuscarCarro.setOnClickListener { goToTripInfo() }
        binding.imageViewMenu.setOnClickListener { showModalMenu() }
        binding.imageViewSalir.setOnClickListener{salirdelApp()}
        binding.btnDirFrecuente.setOnClickListener {obtenerPrimeraDireccionGuardada() }
        binding.btnDirFrecuente2.setOnClickListener { obtenerSegundaDireccionGuardada() }
        binding.btnDirFrecuente3.setOnClickListener { obtenerTercerDireccionGuardada() }


    }

    override fun onStart() {

        mostrarBtnFrecuentes(client)
        super.onStart()
    }

    //ELIMINA LA MEMORIA DE DIRECCIONES************************************
    private fun clearSharedPreferences() {
        val sharedPreferences = getSharedPreferences("Direcciones", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }




    //ASINCRONO CORRUTINA BCV*************************

    private fun obtenerPrecioDolar() {
        CoroutineScope(Dispatchers.IO).launch {
            var intentos = 0
            val maxIntentos = 3
            var obtenido = false

                while (intentos < maxIntentos && !obtenido) {
                try {
                    val document = Jsoup.connect("https://www.bcv.org.ve/").timeout(60000).get()
                    val precioDolar = document.select("#dolar strong").first()?.text()
                    val valorDolar = precioDolar?.replace(",", ".")?.toDoubleOrNull()

                    withContext(Dispatchers.Main) {
                        if (valorDolar != null) {

                            PrecioDolarContainer.setPrecioDolar(valorDolar)

                           // actualizarPrecioDolar(valorDolar)
                            configProvider.updateTaza(valorDolar.toDouble()).addOnCompleteListener{
                              //  Toast.makeText(this@MapActivity, "Actualizo Precio BCV $valorDolar", Toast.LENGTH_SHORT).show()
                            }

                            // Marcar como obtenido y salir del bucle
                            obtenido = true
                        } else {
                            Toast.makeText(this@MapActivity, "Problemas de Internet", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        //binding.textTasa.text = "Error de conexión"
                    }
                }
                    // Incrementar el contador de intentos
                    intentos++

                    // Esperar un tiempo antes de realizar el próximo intento
                    delay(10000) // Esperar 10 segundos antes de volver a intentar obtener el precio del dólar
            }
        }
    }

    //PUBLICO PARA TODO EL PROYECTO
    object PrecioDolarContainer {
        private var precioDolar: Double? = null

        fun setPrecioDolar(valor: Double) {
            precioDolar = valor
        }

        fun getPrecioDolar(): Double? {
            return precioDolar
        }
    }
    

    //PARA LA SEGURIDAD*****************
    fun disableSSLVerification() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }

    //**************************************


    //realizar Google Analytics *****yo**************
    private fun googleAnalytics() {
        val analytics:FirebaseAnalytics=FirebaseAnalytics.getInstance(this)
        val bundle= Bundle()
        bundle.putString("menssage","Integracion de Firebase Analytics Completa")
        analytics.logEvent("InitScreen",bundle)
    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido")
                    if (easyWayLocation!=null){
                        easyWayLocation?.startLocation()
                    }

                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")
                    if (easyWayLocation!= null){
                        easyWayLocation?.startLocation()
                    }


                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                    Toast.makeText(this, "SIN LOS PERMISO DE UBICACION NO PUEDE FUNCIONAR", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
            }
        }

    }



    private fun mostrarBtnFrecuentes(clientE: Client?) {
        val direccionesGuardadas = obtenerDireccionesGuardadas()
        var btn= 0

        for (direccion in direccionesGuardadas) {
            // Realiza las acciones necesarias con cada dirección guardada
            btn++
            if (btn==1){
                binding.btnDirFrecuente.visibility= View.VISIBLE
                binding.btnDirFrecuente.text= direccionesGuardadas[0].nombre
                val myButton: Button = findViewById(R.id.btnDirFrecuente)
                ajustarTamañoLetraBoton(myButton, "boton1")

            }
            if (btn==2){

                binding.btnDirFrecuente2.visibility= View.VISIBLE
                binding.btnDirFrecuente2.text= direccionesGuardadas[1].nombre
                destinationLatLng= LatLng(direccion.latitud.toDouble(),direccion.longitud.toDouble())
                val myButton: Button = findViewById(R.id.btnDirFrecuente2)
                ajustarTamañoLetraBoton(myButton,"boton2")
            }
            if (btn==3){
                binding.btnDirFrecuente3.visibility= View.VISIBLE
                binding.btnDirFrecuente3.text= direccionesGuardadas[2].nombre
                destinationLatLng= LatLng(direccion.latitud.toDouble(),direccion.longitud.toDouble())
                val myButton: Button = findViewById(R.id.btnDirFrecuente3)
                ajustarTamañoLetraBoton(myButton, "boton3")
            }
        }
    }
    //MENSAGE DE CONFIRMACION DE SALIDA*********************

    fun salirdelApp(){

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("Desea salir de la aplicacion TaxiAhora?")
        builder.setPositiveButton("Salir", DialogInterface.OnClickListener { dialog, which ->
            easyWayLocation?.endUpdates()
            finishAffinity()
        })
        builder.setNegativeButton("Cancelar",null )
        builder.show()
    }
    //UBICA EN LA POCICION ACTUAL YO**********
    private fun irPosicionActual(){

        if (myLocationLatLng!=null){
            googleMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder().target(myLocationLatLng!!).zoom(13f).build()
                ))
        }

    }

    private fun createToken() {
        clientProvider.createToken(authProvider.getId())
    }
    private fun showModalMenu() {
        val fragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(ModalBottomSheetMenu.TAG)
        if (existingFragment != null) {
            // Si el fragmento ya existe, no lo agregues de nuevo
            return
        }
        val fragmentTransaction = fragmentManager.beginTransaction()
        modalMenu.show(fragmentTransaction, ModalBottomSheetMenu.TAG)
    }


    private fun removeBooking() {

        bookingProvider.getBooking().get().addOnSuccessListener { document ->
            Log.d("FIRESTORE", "VALOR DEL DOCUMENT  ${document} ")
            if (document.exists()) {
                val booking = document.toObject(Booking::class.java)
                if (booking?.status == "create" || booking?.status == "cancel" ) {// como estava  || booking?.status == "cancel"
                    bookingProvider.remove()
                }
            }

        }
    }
//CREAMOS UN MARCADOR PARA LA MOTO CONECTADA
    private fun getNearbyDriversMoto() {

        if (myLocationLatLng == null) return

        geoProvider.getNearbyDriversMoto(myLocationLatLng!!, 170.0).addGeoQueryEventListener(object: GeoQueryEventListener {

            override fun onKeyEntered(documentID: String, location: GeoPoint) {

                Log.d("FIRESTORE", "Document id: $documentID")
                Log.d("FIRESTORE", "location: $location")

                for (marker in driverMarkersMoto) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            return
                        }
                    }
                }
                // CREAMOS UN NUEVO MARCADOR PARA LA MOTO CONECTADA
                val driverLatLng = LatLng(location.latitude, location.longitude)
                if (driverLatLng!= null){// yo eliminando el error del null********
                    val marker = googleMap?.addMarker(
                        MarkerOptions().position(driverLatLng).title(idDriver).icon(
                            BitmapDescriptorFactory.fromResource(R.drawable.ic_motorverde)
                        )
                    )

                    marker?.tag = documentID
                    driverMarkersMoto.add(marker!!)

                    val dl = DriverLocation()
                    dl.id = documentID
                    driversLocationMoto.add(dl)
                }

            }

            override fun onKeyExited(documentID: String) {
                for (marker in driverMarkersMoto) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            marker.remove()
                            driverMarkersMoto.remove(marker)
                            driversLocationMoto.removeAt(getPositionDriverMoto(documentID))
                            return
                        }
                    }
                }
            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

                for (marker in driverMarkersMoto) {

                    val start = LatLng(location.latitude, location.longitude)
                    var end: LatLng? = null
                    val position = getPositionDriverMoto(marker.tag.toString())

                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
//                            marker.position = LatLng(location.latitude, location.longitude)

                            if (driversLocationMoto[position].latlng != null) {
                                end = driversLocationMoto[position].latlng
                            }
                            driversLocationMoto[position].latlng = LatLng(location.latitude, location.longitude)
                            if (end  != null) {
                                CarMoveAnim.carAnim(marker, end, start)
                            }

                        }
                    }
                }

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {

            }

        })
    }


    //CREAMOS UN MARCADOR PARA LOS CARROS CONECTADA
    private fun getNearbyDrivers() {

        if (myLocationLatLng == null) return

        geoProvider.getNearbyDrivers(myLocationLatLng!!, 170.0).addGeoQueryEventListener(object: GeoQueryEventListener {

            override fun onKeyEntered(documentID: String, location: GeoPoint) {

                Log.d("FIRESTORE", "Document id: $documentID")
                Log.d("FIRESTORE", "location: $location")
                idDriver = documentID

                for (marker in driverMarkers) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            return
                        }
                    }
                }
                // CREAMOS UN NUEVO MARCADOR PARA EL CONDUCTOR CONECTADO
                val driverLatLng = LatLng(location.latitude, location.longitude)
                val marker = googleMap?.addMarker(
                    MarkerOptions().position(driverLatLng).title(idDriver).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.uber_carverde)
                    )
                )

                marker?.tag = documentID
                driverMarkers.add(marker!!)

                val dl = DriverLocation()
                dl.id = documentID
                driversLocation.add(dl)
            }

            override fun onKeyExited(documentID: String) {
                for (marker in driverMarkers) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            marker.remove()
                            driverMarkers.remove(marker)
                            driversLocation.removeAt(getPositionDriver(documentID))
                            return
                        }
                    }
                }
            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {

                for (marker in driverMarkers) {

                    val start = LatLng(location.latitude, location.longitude)
                    var end: LatLng? = null
                    val position = getPositionDriver(marker.tag.toString())

                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
//                            marker.position = LatLng(location.latitude, location.longitude)

                            if (driversLocation[position].latlng != null) {
                                end = driversLocation[position].latlng
                            }
                            driversLocation[position].latlng = LatLng(location.latitude, location.longitude)
                            if (end  != null) {
                                CarMoveAnim.carAnim(marker, end, start)
                            }

                        }
                    }
                }

            }

            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {

            }

        })
    }


    private fun goToTripMotoInfo() {
        irPosicionActual()
        if (originLatLng != null && destinationLatLng != null) {
            val i = Intent(this, TripInfoActivity::class.java) //ELIMINE EL ACTIVITY TRIP MOTO PARA COLOCAR CAMBIAR EL TRIPCTIVITYmOTO
            i.putExtra("origin", originName)
            i.putExtra("destination", destinationName)
            i.putExtra("origin_lat", myLocationLatLng?.latitude)// para dejar en la posicion origen del celular*********yo***********
            i.putExtra("origin_lng", myLocationLatLng?.longitude)//***************yo*************
//            i.putExtra("origin_lat", originLatLng?.latitude)
//            i.putExtra("origin_lng", originLatLng?.longitude)
            i.putExtra("destination_lat", destinationLatLng?.latitude)
            i.putExtra("destination_lng", destinationLatLng?.longitude)
            i.putExtra("tipo", "Moto")
            startActivity(i)
        }
        else {
            Toast.makeText(this, "Debes seleccionar el origin y el destino", Toast.LENGTH_LONG).show()
        }

    }
    private fun goToTripInfo() {
        irPosicionActual()

        if (originLatLng != null && destinationLatLng != null) {

            val i = Intent(this, TripInfoActivity::class.java)
            i.putExtra("origin", originName)
            i.putExtra("destination", destinationName)
            i.putExtra("origin_lat", myLocationLatLng?.latitude)
            i.putExtra("origin_lng", myLocationLatLng?.longitude)
            i.putExtra("destination_lat", destinationLatLng?.latitude!!.toDouble())
            i.putExtra("destination_lng", destinationLatLng?.longitude)
            i.putExtra("tipo", "Carro")
            startActivity(i)
        }
        else {
            Toast.makeText(this, "Debes seleccionar el origin y el destino", Toast.LENGTH_LONG).show()
        }

    }


    //PARA MOTO
    private fun getPositionDriverMoto(id: String): Int {
        var position = 0
        for (i in driversLocationMoto.indices) {
            if (id == driversLocationMoto[i].id) {
                position = i
                break
            }
        }
        return position
    }

    //POSICION DEL CONDUCTOR
    private fun getPositionDriver(id: String): Int {
        var position = 0
        for (i in driversLocation.indices) {
            if (id == driversLocation[i].id) {
                position = i
                break
            }
        }
        return position
    }



        //POSICION DE LA CAMARA
    private fun  onCameraMove() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                if (originLatLng != null) {
                    val addressList = geocoder.getFromLocation(originLatLng?.latitude!!, originLatLng?.longitude!!, 1)
                    if (addressList!!.size > 0) {
                        val city = addressList[0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)
                        originName = "$address $city"
                        autocompleteOrigin?.setText("$address $city")
                        Log.d("PLACES", "Address onCameraMove ORIGEN: $originName")
                        Log.d("PLACES", "LAT onCameraMove ORIGEN: ${originLatLng?.latitude}")
                        Log.d("PLACES", "LNG onCameraMove ORIGEN: ${originLatLng?.longitude}")
                    }
                }

            } catch (e: Exception) {
                Log.d("ERROR", "Mensaje error:Listener ${e.message}")
            }
        }
    }
    //POSICION DE LA CAMARA sin mover el origen del telefono
    private fun onCameraMove2() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                if (originLatLng != null) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                        if (location != null) {
                            originLatLng = LatLng(location.latitude, location.longitude)
                        }
                    }

                    val addressList = geocoder.getFromLocation(originLatLng!!.latitude, originLatLng!!.longitude, 1)

                    if (addressList!!.size > 0) {
                        val city = addressList[0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)
                        originName = "$address $city"
                        autocompleteOrigin?.setText("$address $city")
                    }
                }
            } catch (e: Exception) {
                Log.d("ERROR", "Mensaje error:Listener ${e.message}")
            }
        }
    }

    //INICIA EL BUSCADOR DE GOOGLE
    private fun startGooglePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
        }

        places = Places.createClient(this)
        instanceAutocompleteOrigin()
        instanceAutocompleteDestination()
    }

// LIMITA LA BUSQUEDA A UN ESPACIO REDUCIDO
    private fun limitSearch() {
        val northSide = SphericalUtil.computeOffset(myLocationLatLng, 5000.0, 0.0)
        val southSide = SphericalUtil.computeOffset(myLocationLatLng, 5000.0, 180.0)

        autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))

        autocompleteDestination?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
    }

    private fun instanceAutocompleteOrigin() {
        autocompleteOrigin = supportFragmentManager.findFragmentById(R.id.placesAutocompleteOrigin) as AutocompleteSupportFragment
        autocompleteOrigin?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteOrigin?.setHint("Lugar de recogida")
        autocompleteOrigin?.setCountry("VE")

        autocompleteOrigin?.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                originName = place.name!!
                originLatLng = place.latLng
            }

            override fun onError(p0: Status) {

            }
        })
    }

    private fun instanceAutocompleteDestination() {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        autocompleteDestination?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteDestination?.setHint("Indique el Destino")
        autocompleteDestination?.setCountry("VE")
        autocompleteDestination?.setOnPlaceSelectedListener(object: PlaceSelectionListener {

            override fun onPlaceSelected(place: Place) {
                autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment

                destinationName = place.name!!
                destinationLatLng = place.latLng

                //USO PARA DIRECIONES FRECUENTES*********************************
                guardarDireccionEnSharedPreferences(destinationName,destinationLatLng!!)

            }

            override fun onError(p0: Status) {

            }
        })
    }

    override fun onResume() {
        super.onResume() // ABRIMOS LA PANTALLA ACTUAL
    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
       // stopNetworkMonitoring()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMove2() // para evitar que se muevan los datos al mover la pantalla
        //easyWayLocation?.startLocation();

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap?.isMyLocationEnabled = true
        binding.imageViewMenu.isClickable = true

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

    override fun currentLocation(location: Location) { // ACTUALIZACION DE LA POSICION EN TIEMPO REAL
        myLocationLatLng = LatLng(location.latitude, location.longitude) // LAT Y LONG DE LA POSICION ACTUAL

        if (!isLocationEnabled) { // UNA SOLA VEZ
            isLocationEnabled = true
            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).zoom(13f).build()
            ))
            getNearbyDrivers()
            getNearbyDriversMoto()
            limitSearch()
        }
    }

    override fun locationCancelled() {

    }



    private fun guardarDireccionEnSharedPreferences(destino: String, coordenadas: LatLng) {
        val sharedPreferences = getSharedPreferences("Direcciones", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val direccionesGuardadas = obtenerDireccionesGuardadas()
        Log.d("DIRECCIONESG", "direccionesGuardadas ${direccionesGuardadas} ")

        // Verifica si la dirección ya está guardada
        if (!direccionesGuardadas.any { it.nombre == destino }) {
            val nuevaDireccion = Direccion(destino, coordenadas.latitude.toFloat(), coordenadas.longitude.toFloat())
            val primerDrireccion: Direccion? = direccionesGuardadas.getOrNull(0)
            val segundaDireccion: Direccion? = direccionesGuardadas.getOrNull(1)


            // Agrega la dirección a la lista de direcciones guardadas
            direccionesGuardadas.add(0, nuevaDireccion)
            if (primerDrireccion!=null) direccionesGuardadas.add(1, primerDrireccion)
            if (segundaDireccion!=null)direccionesGuardadas.add(2, segundaDireccion)
            Log.d("DIRECCIONESG", "direccionesGuardadas ${direccionesGuardadas} ")






            // Limita el tamaño de la lista a las últimas 3 direcciones
            if (direccionesGuardadas.size > MAX_DIRECCIONES_GUARDADAS) {
                direccionesGuardadas.removeAt(direccionesGuardadas.size - 1)
            }

            // Actualiza la lista de direcciones guardadas en SharedPreferences
            val gson = Gson()
            val direccionesGuardadasStringSet = direccionesGuardadas.map { gson.toJson(it) }.toSet()
            editor.putStringSet("direcciones", direccionesGuardadasStringSet)
            editor.apply()
        }
    }

    private fun  obtenerDireccionesGuardadas(): MutableList<Direccion> {
        val sharedPreferences = getSharedPreferences("Direcciones", Context.MODE_PRIVATE)
        val direccionesGuardadasSet = sharedPreferences.getStringSet("direcciones", emptySet())
        val direccionesGuardadas = mutableListOf<Direccion>()
        val gson = Gson()
        val direccionType = object : TypeToken<Direccion>() {}.type

        direccionesGuardadasSet?.forEach { direccionString ->
            val direccion = gson.fromJson<Direccion>(direccionString, direccionType)
            direccionesGuardadas.add(direccion)
        }

        return direccionesGuardadas
    }

    private fun obtenerPrimeraDireccionGuardada(): Direccion? {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        val direccionesGuardadas = obtenerDireccionesGuardadas()
        val nombreDirecion = direccionesGuardadas.firstOrNull()
        autocompleteDestination!!.setText(nombreDirecion?.nombre)
        destinationLatLng= LatLng(nombreDirecion!!.latitud.toDouble(),nombreDirecion.longitud.toDouble())
        destinationName= nombreDirecion?.nombre!!

        return direccionesGuardadas.firstOrNull()
    }
    private fun obtenerSegundaDireccionGuardada(): Direccion? {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        val direccionesGuardadas = obtenerDireccionesGuardadas()
        val nombreDirecion = direccionesGuardadas.getOrNull(1)
        autocompleteDestination!!.setText(nombreDirecion?.nombre)
        destinationName= nombreDirecion?.nombre!!
        destinationLatLng= LatLng(nombreDirecion!!.latitud.toDouble(),nombreDirecion.longitud.toDouble())
        return direccionesGuardadas.getOrNull(1)
    }
    private fun obtenerTercerDireccionGuardada(): Direccion? {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination) as AutocompleteSupportFragment
        val direccionesGuardadas = obtenerDireccionesGuardadas()
        val nombreDirecion = direccionesGuardadas.getOrNull(2)
        autocompleteDestination!!.setText(nombreDirecion?.nombre)
        destinationLatLng= LatLng(nombreDirecion!!.latitud.toDouble(),nombreDirecion.longitud.toDouble())
        destinationName= nombreDirecion?.nombre!!
        return direccionesGuardadas.getOrNull(2)
    }

    //AJUSTA EL TAMAÑO DE LA LETRA BOTON*************************
    fun ajustarTamañoLetraBoton(boton: Button, origen: String) {
        val tamañoBoton = 200/ boton.text.length
        val numerodeLetras = boton.text.length
        var tamañoLetra = (tamañoBoton * 1.2).toFloat() // Puedes ajustar el valor según tus necesidades

        if (numerodeLetras in 1..10){
            tamañoLetra = 23.0f
        }

        if (numerodeLetras in 10..15){
            tamañoLetra = 21.0f
        }
        if (numerodeLetras in 15..25){
            tamañoLetra = 18.0f
        }
        if (numerodeLetras in 15..25){
            tamañoLetra = 17.0f
        }

        if (numerodeLetras in 25..100){
            tamañoLetra = 16.0f
        }

        boton.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamañoLetra)
        Log.d("contraletra", "AJUSTAR boton.text.length ${boton.text.length} tamañoBoton $tamañoBoton tamañoLetra $tamañoLetra origen $origen ")
    }
    //************************************************************



    // Llama a este método para comenzar a monitorear el estado de la red
//    private fun startNetworkMonitoring(context: Context) {
//        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//        val networkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .build()
//
//        networkCallback = object : ConnectivityManager.NetworkCallback() {
//            override fun onAvailable(network: Network) {
//                // La conexión a internet está disponible
//
//                binding.imageViewInternet.setImageResource(R.drawable.ic_internet)
//                Toast.makeText(context, "Conexion a Internet Disponible", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onLost(network: Network) {
//                // La conexión a internet se perdió
//                binding.imageViewInternet.setImageResource(R.drawable.ic_no_internet_rojo)
//                Toast.makeText(context, "Sin Conexion a Internet ", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//    }
//
//    // Llama a este método para detener el monitoreo del estado de la red
//    private fun stopNetworkMonitoring() {
//        connectivityManager.unregisterNetworkCallback(networkCallback)
//    }

}