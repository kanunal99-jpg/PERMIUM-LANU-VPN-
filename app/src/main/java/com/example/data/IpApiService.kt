package com.example.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class IpResponse(val ip: String)

interface IpApi {
  @GET("?format=json") suspend fun getMyIp(): IpResponse
}

object IpApiService {
  private val client = OkHttpClient.Builder().build()
  private val retrofit = Retrofit.Builder()
    .baseUrl("https://api.ipify.org/")
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()
  val api: IpApi = retrofit.create(IpApi::class.java)

  suspend fun fetchCurrentIp(): String {
    val response = api.getMyIp()
    require(response.ip.isNotBlank()) { "Public IP response was empty" }
    return response.ip
  }
}
