package com.carlosvicente.uberkotlin.api


//import com.carlosvicente.uberdriverkotlin.models.FCMBody
//import com.carlosvicente.uberdriverkotlin.models.FCMResponse
import com.carlosvicente.uberkotlin.models.FCMBody
import com.carlosvicente.uberkotlin.models.FCMResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMApi {

    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAA2wXihB0:APA91bFoI7_aBicijAri0XxnZxp5o34YXbMhxZTwpbqx5mB5w1xmL-hyzZGj6A1ESA0QKfukf2yY5wAxJRz_caKCzkYqHzZbENcao6FCmfZFkFeK7BUlJ6fx4fT8p2wVwl1wLv-wa8v0"
    )
    @POST("fcm/send")
    fun send(@Body body: FCMBody): Call<FCMResponse>


}