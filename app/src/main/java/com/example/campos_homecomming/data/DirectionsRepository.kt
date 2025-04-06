package com.example.campos_homecomming.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//******Clases para parsear la respuesta de OpenRouteService******
data class RouteResponse(
    //Lista de features que contiene la información de la ruta
    val features: List<Feature>
)

data class Feature(
    //La geometria de la ruta que contiene las coordenadas
    val geometry: Geometry
)

data class Geometry(
    //Lista de coordenadas; cada coordenada es [longitud, latitud]
    val coordinates: List<List<Double>>
)

//Interfaz Retrofit para la API de OpenRouteService
interface OpenRouteServiceApi {
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,  //API key para autenticar la peticion
        @Query("start") start: String,     //Ubicacion de inicio en formato "longitud,latitud"
        @Query("end") end: String          //Ubicacion final en el mismo formato de "longitud,latitud"
    ): RouteResponse  //Devuelve la respuesta parseada en un objeto RouteResponse
}

object DirectionsRepository {
    private const val BASE_URL = "https://api.openrouteservice.org/" //URL base de la API
    //API key de OpenRouteService
    private const val API_KEY = "5b3ce3597851110001cf6248beb8289b828149d0a3f25e868be927f2"

    //Se inicializa la API usando Retrofit y un cliente OkHttp con interceptor de logging
    private val api: OpenRouteServiceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)//Establece la URL base de la API
            .addConverterFactory(GsonConverterFactory.create())//Se utiliza para convertir el JSON en objetos Kotlin
            .build()
            .create(OpenRouteServiceApi::class.java)//Crea la instancia de la interfaz que define los endpoints
    }


    //Funcion que obtiene la ruta mas corta entre la ubicacion actual y el destino, o sea la casita
    //Los parametros son la latitud y longitud actuales y el destino como GeoPoint
    //y devuelve la lista de GeoPoints que forman la ruta por medio del callback o null en caso de que no charche
    fun getRoute(
        currentLat: Double,
        currentLon: Double,
        destination: GeoPoint,
        callback: (List<GeoPoint>?) -> Unit
    ) {
        //Se lanza una corrutina en el Dispatcher.IO para hacer la peticion en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //Se formatean la ubicacion de inicio y el destino en el formato "lon,lat"
                val start = "$currentLon,$currentLat"
                val end = "${destination.longitude},${destination.latitude}"
                //Se realiza la petición a la API
                val response = api.getRoute(API_KEY, start, end)
                if (response.features.isNotEmpty()) {
                    //Se extraen las coordenadas del primer feature de la respuesta
                    val coords = response.features[0].geometry.coordinates
                    //Se convierten las coordenadas [lon, lat] a objetos GeoPoint (lat, lon)
                    val points = coords.map { coord ->
                        GeoPoint(coord[1], coord[0])
                    }
                    callback(points) //Se devuelve la lista de GeoPoint por medio del callback
                } else {
                    callback(null) //Si no hay features se devuelve un nullsote
                }
            } catch (e: Exception) {
                //Se registra el error en el log y se devuelve null
                Log.e("DirectionsRepository", "Error al obtener la ruta: ${e.localizedMessage}")
                callback(null)
            }
        }
    }
}
