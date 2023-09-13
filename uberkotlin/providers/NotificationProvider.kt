package com.carlosvicente.uberkotlin.providers

import com.carlosvicente.uberkotlin.api.IFCMApi
import com.carlosvicente.uberkotlin.api.RetrofitClient
import com.carlosvicente.uberkotlin.models.FCMBody
import com.carlosvicente.uberkotlin.models.FCMResponse
import retrofit2.Call

class NotificationProvider {

    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClient.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}