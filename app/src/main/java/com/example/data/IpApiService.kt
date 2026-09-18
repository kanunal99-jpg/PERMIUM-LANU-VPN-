package com.example.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class IpResponse(val ip: String)

interface IpApi {
  @GET("/") fun getMyIp(): retrofit2.Call<IpResponse>
}

object IpApiService {
  private val client = OkHttpClient.Builder().build()

  private val retrofit =
    Retrofit.Builder()
      .baseUrl("https://api.ipify.org?format=json/")
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create())
      .build()

  val api: IpApi = retrofit.create(IpApi::class.java)

  fun fetchCurrentIp(): String {
    return try {
      val response = api.getMyIp().execute()
      response.body()?.ip ?: "Unknown IP"
    } catch (e: Exception) {
      "192.168.1.100" // Fallback local/simulated if offline
    }
  }
}
