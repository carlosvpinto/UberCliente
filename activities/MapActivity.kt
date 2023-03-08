package com.carlosvicente.uberkotlin.activities

import android.Manifest
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
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityMapBinding
import com.carlosvicente.uberkotlin.fragments.ModalBottomSheetMenu
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.DriverLocation
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.BookingProvider
import com.carlosvicente.uberkotlin.providers.ClientProvider
import com.carlosvicente.uberkotlin.providers.GeoProvider
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
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

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

    // GOOGLE PLACES
    private var places: PlacesClient? = null
    private var autocompleteOrigin: AutocompleteSupportFragment? = null
    private var autocompleteDestination: AutocompleteSupportFragment? = null
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





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


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


        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        googleAnalytics()
        startGooglePlaces()
        removeBooking()
        createToken()
        FirebaseAnalytics.getInstance(this)
        binding.btnSolicitarMoto.setOnClickListener { goToTripMotoInfo() }
        binding.btnBuscarCarro.setOnClickListener { goToTripInfo() }
        binding.imageViewMenu.setOnClickListener { showModalMenu() }
        binding.imageViewSalir.setOnClickListener{salirdelApp()}


       // binding.txtposicionActual.setOnClickListener{irPosicionActual()}
    }


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
        if (!modalMenu.isAdded()) {
                modalMenu.show(supportFragmentManager, ModalBottomSheetMenu.TAG)
        }

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
            //i.putExtra("origin_lat", originLatLng?.latitude)
            i.putExtra("origin_lng", originLatLng?.longitude)
            i.putExtra("destination_lat", destinationLatLng?.latitude)
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

    //ON CAMARA INMOVIL *****YO******
    private fun onCameraMoveNo() {

        if (myLocationLatLng!=null){
            googleMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder().target(myLocationLatLng!!).zoom(13f).build()
                ))
        }
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                if (myLocationLatLng != null) {
                    val addressList = geocoder.getFromLocation(myLocationLatLng?.latitude!!, myLocationLatLng?.longitude!!, 1)
                    if (addressList.size > 0) {
                        val city = addressList[0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)
                        originName = "$address $city"
                        autocompleteOrigin?.setText("$address $city")
                    }
                }

            } catch (e: Exception) {
                Log.d("ERROR", "Mensaje error: ${e.message}")
            }
        }

    }

        //POSICION DE LA CAMARA
    private fun onCameraMove() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target

                if (originLatLng != null) {
                    val addressList = geocoder.getFromLocation(originLatLng?.latitude!!, originLatLng?.longitude!!, 1)
                    if (addressList.size > 0) {
                        val city = addressList[0].locality
                        val country = addressList[0].countryName
                        val address = addressList[0].getAddressLine(0)
                        originName = "$address $city"
                        autocompleteOrigin?.setText("$address $city")
                    }
                }

            } catch (e: Exception) {
                Log.d("ERROR", "Mensaje error: ${e.message}")
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
                Log.d("PLACES", "Address: $originName")
                Log.d("PLACES", "LAT: ${originLatLng?.latitude}")
                Log.d("PLACES", "LNG: ${originLatLng?.longitude}")
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
                destinationName = place.name!!
                destinationLatLng = place.latLng
                Log.d("PLACES", "Address: $destinationName")
                Log.d("PLACES", "LAT: ${destinationLatLng?.latitude}")
                Log.d("PLACES", "LNG: ${destinationLatLng?.longitude}")
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
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMoveNo()
//        easyWayLocation?.startLocation();

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
        Log.d("MAPAS", "VALOS DE myLocationLatLng: ${myLocationLatLng}")
        myLocationLatLng = LatLng(location.latitude, location.longitude) // LAT Y LONG DE LA POSICION ACTUAL

        if (!isLocationEnabled) { // UNA SOLA VEZ
            isLocationEnabled = true
            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).zoom(14f).build()
            ))
            getNearbyDrivers()
            getNearbyDriversMoto()
            limitSearch()
        }
    }

    override fun locationCancelled() {

    }


}