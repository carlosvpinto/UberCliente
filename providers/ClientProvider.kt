package com.carlosvicente.uberkotlin.providers

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.carlosvicente.uberkotlin.models.Client
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File

class ClientProvider {

    val db = Firebase.firestore.collection("Clients")
    var storage = FirebaseStorage.getInstance().getReference().child("profile")

    fun create(client: Client): Task<Void> {
        return db.document(client.id!!).set(client)
    }

    fun getClientById(id: String): Task<DocumentSnapshot> {
        return db.document(id).get()
    }

    fun uploadImage(id: String, file: File): StorageTask<UploadTask.TaskSnapshot> {
        var fromFile = Uri.fromFile(file)
        val ref = storage.child("$id.jpg")
        storage = ref
        val uploadTask = ref.putFile(fromFile)

        return uploadTask.addOnFailureListener {
            Log.d("STORAGE", "ERROR: ${it.message}")
        }
    }

    fun getImageUrl(): Task<Uri> {
        return storage.downloadUrl
    }

    fun createToken(idClient: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                val token = it.result // TOKEN DE NOTIFICACIONES
                updateToken(idClient, token)
            }
        }
    }

    fun updateToken(idClient: String, token: String): Task<Void> {
        val map: MutableMap<String, Any> = HashMap()
        map["token"] = token
        return db.document(idClient).update(map)
    }
    fun updateBilleteraClient(id: String, monto: Double): Task<Void> {
        return db.document(id).update("billetera", monto).addOnFailureListener { exception ->
        }
    }

    fun update(client: Client): Task<Void> {
        val map: MutableMap<String, Any> = HashMap()
        map["name"] = client?.name!!
        map["lastname"] = client?.lastname!!
        map["phone"] = client?.phone!!
        if (client?.image != null){
            map["image"] = client?.image!!
        }
        return db.document(client?.id!!).update(map)
    }

    fun editarDirecFrecuente1(idClient: String, direFrecuen1: String, latFrecuente1: Double, lngFrecuente1:Double): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "direcionFrecu1" to direFrecuen1,
            "latFrecuente1" to latFrecuente1,
            "lngFrecuente1" to lngFrecuente1,
        )

        return db.document(idClient).update(updates)
            .addOnFailureListener { exception ->
                Log.d("FIRESTORE", "ERROR: ${exception.message}")
            }
    }

    fun editarDirecFrecuente2(idClient: String, direFrecuen2: String, latFrecuente2: Double, lngFrecuente2:Double): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "direcionFrecu2" to direFrecuen2,
            "latFrecuente2" to latFrecuente2,
            "lngFrecuente2" to lngFrecuente2,
        )

        return db.document(idClient).update(updates)
            .addOnFailureListener { exception ->
                Log.d("FIRESTORE", "ERROR: ${exception.message}")
            }
    }



}