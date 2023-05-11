package com.carlosvicente.uberkotlin.activities

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.carlosvicente.uberkotlin.R
import com.carlosvicente.uberkotlin.adapters.PagoMovilAdapter
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.protobuf.Empty
import com.carlosvicente.uberkotlin.databinding.ActivityProfileBinding
import com.carlosvicente.uberkotlin.models.Client
import com.carlosvicente.uberkotlin.models.Driver
import com.carlosvicente.uberkotlin.models.PagoMovil
import com.carlosvicente.uberkotlin.providers.AuthProvider
import com.carlosvicente.uberkotlin.providers.ClientProvider
import com.carlosvicente.uberkotlin.providers.PagoMovilProvider
import com.tommasoberlose.progressdialog.ProgressDialogFragment
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    val authProvider = AuthProvider()
    private var pagoMoviles = ArrayList<PagoMovil>()
    private var pagoMovilProvider = PagoMovilProvider()
    private val clientProvider = ClientProvider()

    private var imageFile: File? = null

    private var progressDialog = ProgressDialogFragment
    private var totalBs = 0.0
    private var totalDollar= 0.0
    private var totalSinVeriBs = 0.0
    private var totalSinVeriBsDollar = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        progressDialog.showProgressBar(this)
        getClient()

        binding.imageViewBack.setOnClickListener { finish() }
        binding.btnUpdate.setOnClickListener { updateInfo() }
        binding.circleImageProfile.setOnClickListener { selectImage() }
    }


    private fun updateInfo() {
        progressDialog.showProgressBar(this)
        val name = binding.textFieldName.text.toString()
        val lastname = binding.textFieldLastname.text.toString()
        val phone = binding.textFieldPhone.text.toString()


        val client = Client(
            id = authProvider.getId(),
            name = name,
            lastname = lastname,
            phone = phone,
        )

        if (imageFile != null) {
            clientProvider.uploadImage(authProvider.getId(), imageFile!!).addOnSuccessListener { taskSnapshot ->
                clientProvider.getImageUrl().addOnSuccessListener { url ->
                    val imageUrl = url.toString()
                    client.image = imageUrl

                    clientProvider.update(client).addOnCompleteListener {
                        if (it.isSuccessful) {
                            progressDialog.hideProgressBar(this)
                            Toast.makeText(this@ProfileActivity, "Datos actualizados correctamente", Toast.LENGTH_LONG).show()
                        }
                        else {
                            progressDialog.hideProgressBar(this)
                            Toast.makeText(this@ProfileActivity, "No se pudo actualizar la informacion", Toast.LENGTH_LONG).show()
                        }
                    }
                    Log.d("STORAGE", "$imageUrl")
                }
            }
        }
        else {
            clientProvider.update(client).addOnCompleteListener {
                if (it.isSuccessful) {
                    progressDialog.hideProgressBar(this)
                    Toast.makeText(this@ProfileActivity, "Datos actualizados correctamente", Toast.LENGTH_LONG).show()
                }
                else {
                    progressDialog.hideProgressBar(this)
                    Toast.makeText(this@ProfileActivity, "No se pudo actualizar la informacion", Toast.LENGTH_LONG).show()
                }
            }
        }


    }

    private fun getClient() {
        clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
            if (document.exists()) {
                val client = document.toObject(Client::class.java)
                binding.textViewEmail.text = client?.email
                binding.textFieldName.setText(client?.name)
                binding.textFieldLastname.setText(client?.lastname)
                binding.textFieldPhone.setText(client?.phone)

                totalizaPagos()
                if (client?.image != null) {
                    if (client.image != "") {
                        Glide.with(this).load(client.image).into(binding.circleImageProfile)
                    }
                }
            }
            progressDialog.hideProgressBar(this)
        }
    }

    private val startImageForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        val resultCode = result.resultCode
        val data = result.data

        if (resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            imageFile = File(fileUri?.path)
            binding.circleImageProfile.setImageURI(fileUri)
        }
        else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(this, "Tarea cancelada", Toast.LENGTH_LONG).show()
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
            binding.textFieldWallet.setText(totalVerdes.toString())
            progressDialog.hideProgressBar(this)
            updateBilletera(authProvider.getId(),totalVerdes)
        }

    }



    private fun selectImage() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080,1080)
            .createIntent { intent ->
                startImageForResult.launch(intent)
            }
    }

}