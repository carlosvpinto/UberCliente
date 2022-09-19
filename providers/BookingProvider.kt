package com.carlosvicente.uberkotlin.providers

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.carlosvicente.uberkotlin.models.Booking

class BookingProvider {

    val db = Firebase.firestore.collection("Bookings")
    val authProvider = AuthProvider()

    fun create(booking: Booking): Task<Void> {
        return db.document(authProvider.getId()).set(booking).addOnFailureListener {
            Log.d("FIRESTORE", "ERROR: ${it.message}")
        }
    }

    fun TraerMotos(): DocumentReference{
        return db.document()
    }

    fun getBooking(): DocumentReference {
        return db.document(authProvider.getId())
    }

    fun remove(): Task<Void> {
        return db.document(authProvider.getId()).delete().addOnFailureListener { exception ->
            Log.d("FIRESTORE", "ERROR: ${exception.message}")
        }
    }



}