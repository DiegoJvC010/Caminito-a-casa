package com.example.campos_homecomming.data
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationManager(private val context: Context) {

    //Se crea una instancia de FusedLocationProviderClient para acceder a la ubicacion del dispositivo
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Location) -> Unit) {
        //Se crea una solicitud de ubicacion (LocationRequest) utilizando el Builder
        //se solicita PRIORITY_HIGH_ACCURACY para obtener la mayor precision que se puede
        //y se establece un intervalo de 10 segundos (10_000L) entre cada actualizacion
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L
        )
            //intervalo minimo de 8 segundos entre actualizaciones
            .setMinUpdateIntervalMillis(8_000L)
            .build()//Se construye el objeto LocationRequest

        //Se define un callback (LocationCallback) para recibir las actualizaciones de ubicacion
        //este callback va aser llamado cada vez que se obtenga una nueva ubicacion
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                //Se obtiene la ultima ubicacion recibida
                val lastLoc = locationResult.lastLocation
                if (lastLoc != null) {
                    //Si se obtiene una ubicacion valida se ejecuta el callback pasado como parametro
                    callback(lastLoc)
                }
            }
        }

        //Se solicitan actualizaciones de ubicacion usando el fusedLocationClient
        //Los parametros que tiene son:
        //**locationRequest: la configuracion de la solicitud de ubicacion
        //**locationCallback: el callback definido para manejar la actualizacion
        //**Looper.getMainLooper(): para ejecutar el callback en el hilo principal
        //lo cual es importante para actualizar la parte visual
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}
