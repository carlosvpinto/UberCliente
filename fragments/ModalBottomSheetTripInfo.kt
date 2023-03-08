package com.carlosvicente.uberkotlin.fragments


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.activities.*
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.providers.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.hdodenhof.circleimageview.CircleImageView

class ModalBottomSheetTripInfo: BottomSheetDialogFragment() {

    private var driver: Driver? = null
    private lateinit var booking: Booking
    val driverProvider = DriverProvider()
    val authProvider = AuthProvider()
    var textViewClientName: TextView? = null
    var textViewOrigin: TextView? = null
    var textViewDestination: TextView? = null
    var imageViewPhone: ImageView? = null
    var circleImageClient: CircleImageView? = null
    var circleImageWhatsaap: CircleImageView? = null

    val REQUEST_PHONE_CALL = 30


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.modal_bottom_sheet_trip_info, container, false)

        textViewClientName = view.findViewById(R.id.textViewClientName)
        textViewOrigin = view.findViewById(R.id.textViewOrigin)
        textViewDestination = view.findViewById(R.id.textViewDestination)
        imageViewPhone = view.findViewById(R.id.imageViewPhone)
        circleImageClient = view.findViewById(R.id.circleImageClient)
        circleImageWhatsaap = view.findViewById(R.id.logowhatsapp)

//        getDriver()
        val data = arguments?.getString("booking")
        booking = Booking.fromJson(data!!)!!

        textViewOrigin?.text = booking.origin
        textViewDestination?.text = booking.destination
        circleImageWhatsaap?.setOnClickListener{
            if (driver?.phone!= null){
                whatSapp(driver?.phone!!)
            }
        }
        imageViewPhone?.setOnClickListener {
            if (driver?.phone != null) {

                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CALL_PHONE), REQUEST_PHONE_CALL)
                }

                call(driver?.phone!!)
            }

        }

        getDriverInfo()
        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PHONE_CALL) {
            if (driver?.phone != null) {
                call(driver?.phone!!)
            }
        }

    }


    //ENVIAR MSJ DE WHATSAPP*******YO******
    private fun whatSapp (phone: String){
        var phone58 = phone
        val cantNrotlf = phone.length // devuelve 10
        if (cantNrotlf<=11){
            try {
                // Código que puede producir una excepción
                val phone58 = "058$phone"
                val i  = Intent(Intent.ACTION_VIEW);
                val  uri = "whatsapp://send?phone="+phone58+"&text="+ driver?.name +" hola estoy esperando por ti en:";
                i.setData(Uri.parse(uri))
                requireActivity().startActivity(i)
            } catch (e: Exception) {
                // Código para manejar la excepción
                Toast.makeText(requireContext(), "No se pudo iniciar Whatsaap $e", Toast.LENGTH_SHORT).show()
                Log.d("whatsapp", "whatSapp: error: $e")
            }


        }else{
            try {
                // Código que puede producir una excepción
                val i  = Intent(Intent.ACTION_VIEW);
                val  uri = "whatsapp://send?phone="+phone+"&text="+"hola estoy esperando por ti en:";
                i.setData(Uri.parse(uri))
                requireActivity().startActivity(i)
            } catch (e: Exception) {
                // Código para manejar la excepción
                Toast.makeText(requireContext(), "No se pudo iniciar Whatsaap $e", Toast.LENGTH_SHORT).show()
                Log.d("whatsapp", "whatSapp: error: $e")
            }


        }


    }


    //LLAMAR POR TELEFONO
    private fun call(phone: String) {

        val i = Intent(Intent.ACTION_CALL)
        i.data = Uri.parse("tel:$phone")

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        requireActivity().startActivity(i)
    }

    private fun getDriverInfo() {
        driverProvider.getDriver(booking.idDriver!!).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                textViewClientName?.text = "${driver?.name} ${driver?.lastname}"

                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(requireActivity()).load(driver?.image).into(circleImageClient!!)
                    }

                  //  if (driver?.imageVehiculo!= null){
                  //      Glide.with(requireActivity()).load(driver?.imageVehiculo).into(circleImageVehiculo!!)
                  //  }
                }
            }
        }
    }

    companion object {
        const val TAG = "ModalBottomSheetTripInfo"
    }


}