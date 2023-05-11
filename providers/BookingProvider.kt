package com.carlosvicente.uberkotlin.providers

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.carlosvicente.uberkotlin.models.Booking
import com.google.firebase.firestore.DocumentSnapshot

class BookingProvider {

    val db = Firebase.firestore.collection("Bookings")
    val authProvider = AuthProvider()

    fun create(booking: Booking): Task<Void> {
        return db.document(authProvider.getId()).set(booking).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }


    fun getBooking(): DocumentReference {
        return db.document(authProvider.getId())
    }
    fun getBookingSnap(): Query {
        return db.whereEqualTo("idDriver", authProvider.getId()).whereEqualTo("activo",true)
    }

    fun remove(): Task<Void> {
        return db.document(authProvider.getId()).delete().addOnFailureListener { exception ->
            Log.d("FIRESTORE", "ERROR: ${exception.message}")
        }
    }
    fun getBookingId(idDriver: String): Task<DocumentSnapshot> {
        return db.document(idDriver).get().addOnFailureListener { exception ->
            Log.d("FIREBASE", "ERROR: ${exception.toString()}")
        }
    }
    fun updateStatus(idClient: String, status: String): Task<Void> {
        return db.document(idClient).update("status", status,).addOnFailureListener { exception ->
            Log.d("FIRESTORE", "ERROR: ${exception.message}")
        }
    }
    fun updatePosicion(idClient: String, originLat: Double, originLng:Double): Task<Void> {
        return db.document(idClient).update("originLat", originLat, "originLng",originLng).addOnFailureListener { exception ->
            Log.d("FIRESTORE", "ERROR: ${exception.message}")
        }
    }



}