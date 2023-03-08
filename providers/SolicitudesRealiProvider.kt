package com.carlosvicente.uberkotlin.providers

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.carlosvicente.uberkotlin.models.Booking
import com.carlosvicente.uberkotlin.models.SolicitudesRealizadas
import com.google.firebase.firestore.DocumentSnapshot

class SolicitudesRealiProvider {

    val db = Firebase.firestore.collection("Solicitudes")
    val authProvider = AuthProvider()

//    fun create(solicitudes: SolicitudesRealizadas): Task<Void> {
//        return db.document(authProvider.getId()).set(solicitudes).addOnFailureListener {
//            Log.d("FIRESTORE", "ERROR: ${it.message}")
//        }
//    }
    fun create(solicitudes: SolicitudesRealizadas): Task<DocumentReference> {
        return db.add(solicitudes).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }



    fun getSolicitudes(): DocumentReference {
        return db.document(authProvider.getId())
    }


    fun remove(): Task<Void> {
        return db.document(authProvider.getId()).delete().addOnFailureListener { exception ->
            Log.d("FIRESTORE", "ERROR: ${exception.message}")
        }
    }
    fun getBookingId(idClient: String): Task<DocumentSnapshot> {
        return db.document(idClient).get().addOnFailureListener { exception ->
            Log.d("FIREBASE", "ERROR: ${exception.toString()}")
        }
    }



}