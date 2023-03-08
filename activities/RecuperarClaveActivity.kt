package com.carlosvicente.uberkotlin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.carlosvicente.uberkotlin.R

import com.carlosvicente.uberkotlin.databinding.ActivityRecuperarClaveBinding
import com.google.firebase.auth.FirebaseAuth
import com.tommasoberlose.progressdialog.ProgressDialogFragment

private lateinit var binding: ActivityRecuperarClaveBinding
private var email=""

private lateinit var auth: FirebaseAuth
private var progressDialog = ProgressDialogFragment


class RecuperarClaveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecuperarClaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRecuperarLogin.setOnClickListener{
            email = binding.txtEmailRecuperar.text.toString()
            if (email!=null){
                progressDialog.showProgressBar(this)
                recuperarContrasena()
            }else{
                Toast.makeText(this, "Tiene que ingresar el email", Toast.LENGTH_SHORT).show()
            }
             }

    }

    private fun recuperarContrasena() {
        auth.setLanguageCode("es")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // La solicitud de restablecimiento de contraseña se ha enviado correctamente.
                    progressDialog.hideProgressBar(this)
                    Toast.makeText(this, "Se ha enviado un correo electrónico para restablecer la contraseña", Toast.LENGTH_LONG).show()

                } else {
                    progressDialog.hideProgressBar(this)
                    // Ocurrió un error al enviar la solicitud de restablecimiento de contraseña.
                    Toast.makeText(this, "No se pudo enviar la solicitud de restablecimiento de contraseña. Inténtalo de nuevo más tarde.", Toast.LENGTH_LONG).show()
                }
            }
    }
    private fun goToMap() {
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }
}