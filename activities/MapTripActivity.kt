package com.carlosvicente.uberkotlin.activities

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer

import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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
import com.google.firebase.firestore.ListenerRegistration
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.databinding.ActivityMapBinding
import com.carlosvicente.uberkotlin.databinding.ActivityMapTripBinding
import com.carlosvicente.uberkotlin.fragments.ModalBottomSheetTripInfo
//import com.carlosvicente.uberkotlin.fragments.ModalBottomSheetTripInfo
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.BookingProvider
import com.carlosvicente.uberkotlin.providers.DriverProvider
import com.carlosvicente.uberkotlin.providers.GeoProvider
import com.carlosvicente.uberkotlin.utils.CarMoveAnim
import com.tommasoberlose.progressdialog.ProgressDialogFragment
import org.imperiumlabs.geofirestore.extension.getLocation
import kotlin.math.log



class MapTripActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {


    private var listenerDriverLocation: ListenerRegistration? = null
    private var driverLocation: LatLng? = null
    private var endLatLng: LatLng? = null
    private var startLatLng: LatLng? = null

    private var listenerBooking: ListenerRegistration? = null
    private var progressDialog = ProgressDialogFragment
    private var markerDestination: Marker? = null
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var booking: Booking? = null
    private var markerOrigin: Marker? = null
    private var markerOriginFoto: Marker? = null
    private var bookingListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMapTripBinding
    private var googleMap: GoogleMap? = null
    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()

    private var driver: Driver? = null


    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil




    private var isDriverLocationFound = false
    private var isBookingLoaded = false

    //Moto
    val origin: String? = null
    private var isMoto = false
    private val driverProvider = DriverProvider()
    private var extraTipo = ""


    private var modalTrip = ModalBottomSheetTripInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)
        easyWayLocation?.startLocation()

        binding.imageViewInfo.setOnClickListener {
            binding.floatInfo.isClickable = false
            binding.imageViewInfo.isClickable = false
            showModalInfo() }
        binding.floatInfo.setOnClickListener{
            binding.floatInfo.isClickable = false
            binding.imageViewInfo.isClickable = false

            showModalInfo()}

        //RECIBE EL TIPO DE VEHICULO DE LA SEARCHACTIVITY**************
        extraTipo = intent.getStringExtra("tipo")!!
        Log.d("TIPOV", "VALOR DE EXTRATIPO TRAIDA PANTALLA SEARCHA:= $extraTipo")
        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))


    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido")


                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")

                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                }
            }
        }

    }


    //MENSAGE DE CONFIRMACION DE SALIDA*********************

    fun salirdelApp(){

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("Desea salir de la pantalla de navegacion?")
        builder.setPositiveButton("Salir", DialogInterface.OnClickListener { dialog, which ->

            goToMap()
        })
        builder.setNegativeButton("Cancelar",null )
        builder.show()

    }
    private fun goToMap() {
        val i = Intent(this, MapActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }


    private fun showModalInfo() {


            if (booking != null) {
                val bundle = Bundle()
                bundle.putString("booking", booking?.toJson())
                binding.floatInfo.isClickable = true
                binding.imageViewInfo.isClickable = true
                if (!modalTrip.isAdded) { // Verifica si el fragmento ya está agregado
                modalTrip.arguments = bundle
                //VERIFICA QUE ESTE EN UNA ACTIVIDAD VALIDA


                modalTrip.show(supportFragmentManager, ModalBottomSheetTripInfo.TAG)


                ///******   ***************************

            } else {
                    Toast.makeText(this, "Por favor espera que cargue la Informacion", Toast.LENGTH_SHORT)
                        .show()

            }
        }else {
                Toast.makeText(this, "No se pudo cargar la informacion", Toast.LENGTH_SHORT).show()
        }
    }


// VERIFICA LA LOCALIZACION DEL CONDUCTOR ****************
    private fun getLocationDriver() {
        if (booking != null) {
            listenerDriverLocation = geoProvider.getLocationWorking(booking?.idDriver!!).addSnapshotListener { document, e ->
                if (e != null) {
                    Log.d("FIRESTORE", "ERROR: ${e.message}")
                    return@addSnapshotListener
                }

                if (driverLocation != null) {
                    Log.d("FIRESTORE", "RATREADOR ${driverLocation}")
                    endLatLng = driverLocation
                }

                if (document?.exists()!!) {
                    var l = document?.get("l") as List<*>
                    val lat = l[0] as Double
                    val lng = l[1] as Double

                    driverLocation = LatLng(lat, lng)

                    if (!isDriverLocationFound && driverLocation != null) {
                        isDriverLocationFound = true
                        addDriverMarker(driverLocation!!)
                        easyDrawRoute(driverLocation!!, originLatLng!!)
                    }
/// AMIMACION PARA BUEN MOVIMIENTO (YO)
                    if (endLatLng != null) {
                        CarMoveAnim.carAnim(markerDriver!!, endLatLng!!, driverLocation!!)
                    }

                    Log.d("FIRESTORE", "LOCATION: $l")
                }

            }
        }

    }


        // OBTIENE LOS CAMBIO DEL BOOKING***************
    private fun getBooking() {
        listenerBooking = bookingProvider.getBooking().addSnapshotListener { document, e ->

            if (e != null) {
                Log.d("FIRESTORE", "ERROR: ${e.message}")
                return@addSnapshotListener
            }

            booking = document?.toObject(Booking::class.java)
            if(booking!=null){
                Log.d("FIRESTORE", "VALOR DEL BOOKING COMPLETO1: ${booking}")
                if (!isBookingLoaded) {
                    Log.d("FIRESTORE", "VALOR DEL BOOKING COMPLETO2: ${booking}")
                    isBookingLoaded = true
                    originLatLng = LatLng(booking?.originLat!!, booking?.originLng!!)
                    destinationLatLng = LatLng(booking?.destinationLat!!, booking?.destinationLng!!)
                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(originLatLng!!).zoom(13f).build()// ZOOM DE LA CAMARA AL ENTRAR
                    ))
                    getLocationDriver()
                    addOriginMarker(originLatLng!!)
                }

                if (booking?.status == "accept") {
                    binding.textViewStatus.text = "Aceptado"
                    binding.textViewStatus.textColors
                }
                else if (booking?.status == "started") {
                    binding.textViewStatus.text = "Iniciado"

                    startTrip()
                }
                else if (booking?.status == "finished") {
                    binding.textViewStatus.text = "Finalizado"
                    finishTrip()
                }
            }


        }
    }
// LLAMA AL ACTIVITY CALIFICACION *****************************
    private fun finishTrip() {
        listenerDriverLocation?.remove()//FINALIZA EL LISTENER
        binding.textViewStatus.text = "Finalizado"
        val i = Intent(this, CalificationActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }

    private fun startTrip() {
        binding.textViewStatus.text = "Iniciado"
        googleMap?.clear()
        if (driverLocation != null) {
            addDriverMarker(driverLocation!!)
            addDestinationMarker()
            easyDrawRoute(driverLocation!!, destinationLatLng!!)
        }
    }

    private fun easyDrawRoute(originLatLng: LatLng, destinationLatLng: LatLng) {
        wayPoints.clear()
        wayPoints.add(originLatLng)
        wayPoints.add(destinationLatLng)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLng)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.green)
            .setPolyLineWidth(15)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLng)
            .build()

        directionUtil.initPath()
    }

    private fun addOriginMarker(position: LatLng) {
        markerOrigin = googleMap?.addMarker(MarkerOptions().position(position).title("Recoger aqui")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))

    }


    private fun addDriverMarker(position: LatLng) {
        //PARA CAMBIA EL ICONO A UNA MOTO**************************
        //SaberSiesMoto()
        Log.d("TIPOV", "VARIABLE extraTipo $extraTipo")
        if (extraTipo== "Moto"){
            markerDriver = googleMap?.addMarker(MarkerOptions().position(position).title("Tu Moto")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_motorverde)))
        }
        if (extraTipo== "Carro"){
            markerDriver = googleMap?.addMarker(MarkerOptions().position(position).title("Tu conductor")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.uber_carverde)))
        }


    }


    private fun addDestinationMarker() {
        if (destinationLatLng != null) {
            markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("Recoger aqui")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
        }
    }


    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            70,
            150,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,70,150)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
        listenerBooking?.remove()
        listenerDriverLocation?.remove()

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        getBooking()
        //getBookingModi()//MODIFICANDO MEJORA YO************************

        easyWayLocation?.startLocation();

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
        googleMap?.isMyLocationEnabled = false

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
        ///PARA ACTUALIZAR POSOSCION 8-4-23
//        bookingProvider.updatePosicion(authProvider.getId(), location.latitude,location.longitude).addOnCompleteListener {
//
//            Log.d("POSISIONREAL", "ACTUALIZACION DE DE LA POSICION DEL BOOKIN ${location.latitude} Y ${location.longitude} ")//CREA HISTORIA DE BOOKING CANCELADOS*******************
//        }
    }

    override fun locationCancelled() {

    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        directionUtil.drawPath(WAY_POINT_TAG)
    }
    // VERIFICA SI ES CARRO O MOTO
    private fun SaberSiesMoto(){
        var extraTipo = "Moto"

        Log.d("TIPOV", "Valor de Extratipo:=: $extraTipo")
                if (extraTipo == "Carro"){
                    isMoto = false
                }
                if (extraTipo=="Moto"){
                    isMoto = true
                }

    }
    override fun onBackPressed() {
        // Aquí puedes colocar el código para manejar la acción del botón "Atrás"
        // Por ejemplo, puedes finalizar la actividad actual:
        salirdelApp()
        easyWayLocation?.endUpdates()
        listenerBooking?.remove()
    }


}