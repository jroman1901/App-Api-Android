package com.umg.appapi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface TareaApi {
    @GET("tareas")
    suspend fun getTareas(): List<Tarea>

    @GET("tareas/{id}")
    suspend fun getTarea(@Path("id") id: Int): Tarea

    @POST("tareas")
    suspend fun createTarea(@Body tarea: Tarea): Tarea

    @PUT("tareas/{id}")
    suspend fun updateTarea(@Path("id") id: Int, @Body tarea: Tarea): Tarea

    @DELETE("tareas/{id}")
    suspend fun deleteTarea(@Path("id") id: Int)

    companion object {
        // CORRECCIÓN:
        // 1. Debe terminar en "/"
        // 2. NO debe incluir "tareas"
        // 3. 10.0.2.2 es la IP para el EMULADOR (accede al localhost de tu PC)
        private const val BASE_URL = "http://10.0.2.2:3000/"

        fun create(): TareaApi {
            val logger = HttpLoggingInterceptor().apply { 
                level = HttpLoggingInterceptor.Level.BODY 
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TareaApi::class.java)
        }
    }
}
