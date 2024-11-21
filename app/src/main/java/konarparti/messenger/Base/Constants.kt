package konarparti.messenger.Base

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konarparti.messenger.Web.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object Constants {

    const val BASE_URL = "https://faerytea.name:8008/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val mapper = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client  = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val serverApi =  Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(mapper))
        .build()
        .create(ApiService::class.java)

    fun getServerAPI(): ApiService = serverApi
    fun getMapper(): Moshi = mapper
}