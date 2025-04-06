package com.example.campos_homecomming.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.campos_homecomming.R
import com.example.campos_homecomming.data.DirectionsRepository
import com.example.campos_homecomming.data.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        //Se obtiene la instancia global de la configuracion de osmdroid
        //y se carga la configuración almacenada en SharedPreferences
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        //Se establece el valor del User Agent usando el nombre del paquete de la aplicacion
        //esto sirve pa que los servidores de mapas reconozcan y permitan el acceso a las peticiones
        //y asi evitar bloqueos por parte del servidor
        Configuration.getInstance().userAgentValue = context.packageName
    }


    //Se solicita el permiso de ubicacion
    var hasLocationPermission by remember { mutableStateOf(false) }//Por si se concede
    var permissionChecked by remember { mutableStateOf(false) }//Cuando se responde a la solicitud
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        //Actualiza el estado segun se conceda el permiso
        hasLocationPermission = isGranted
        //Marca que ya se respondio
        permissionChecked = true
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) // Lanza la solicitud de permiso
    }

    //Los estados para la logica del mapa
    var mapView by remember { mutableStateOf<MapView?>(null) }//Referencia al MapView
    var currentLocation by remember { mutableStateOf<Location?>(null) }//Almacena la ubicacion actual
    var houseLocation by remember { mutableStateOf<GeoPoint?>(null) }//Almacena la ubicación del chante
    var routePoints by remember { mutableStateOf<List<GeoPoint>?>(null) }//Lista de puntos que forman la ruta
    var errorMessage by remember { mutableStateOf<String?>(null) }//Mensaje de error en caso de fallo
    var selectingHouse by remember { mutableStateOf(false) }//Indica si se esta en modo de seleccion de casa
    var initialCentered by remember { mutableStateOf(false) }//Indica si el mapa ya esntaba en el centro al abrir la app

    //Se carga la ubicacion de la casa desde SharedPreferences si fue guardada antes
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lat = prefs.getString("house_lat", null)?.toDoubleOrNull()//Obtener latitud guardada
        val lon = prefs.getString("house_lon", null)?.toDoubleOrNull()//Obtener longitud guardada
        if (lat != null && lon != null) {
            houseLocation = GeoPoint(lat, lon)//Actualizar el estado de la casa si existen datos
        }
    }

    //Si hay permiso se obtiene la ubicacion actual usando el LocationManager
    if (hasLocationPermission) {
        val locationManager = LocationManager(context)
        LaunchedEffect(hasLocationPermission) {
            //Se solicita la ubicacion actual a travs de LocationManager
            locationManager.getCurrentLocation { location ->
                currentLocation = location//Se actualiza el estado con la ubicacion actual obtenida
                errorMessage = null//Se limpia cualquier mensaje de error que hubiera
                //Si ya hay casa se solicita la ruta entre la ubicacion actual y la casa
                houseLocation?.let { house ->
                    requestRoute(location, house) { points, error ->
                        //Si ocurre un error al obtener la ruta se actualiza el mensaje de error y
                        //se limpian los puntos de la ruta
                        if (error != null) {
                            errorMessage = error
                            routePoints = emptyList()
                        } else {
                            //Si la ruta se obtiene correctamente se actualiza el estado con los
                            //puntos de la ruta
                            routePoints = points
                        }
                    }
                }
            }
        }
    }

    //La interfaz con el mapa y los controles
    Box(modifier = Modifier.fillMaxSize()) {

        //Se uiliza AndroidView para integrar el MapView de osmdroid
        AndroidView(
            factory = { ctx ->
                MapView(ctx).also { map ->
                    mapView = map //Se guardar la referencia del MapView
                    //Se Habilitan los gestos multi-touch como hacer zoom
                    map.setMultiTouchControls(true)
                    map.setTileSource(TileSourceFactory.MAPNIK)//Establecer la fuente de mapas
                    //Se escogio es por que se ve bien y es la que mas rapido nos carga

                    map.setBuiltInZoomControls(false)//Se desactivan los controles de zoom
                    //integrados por que se usa los gestos tactiles

                    map.minZoomLevel = 2.0//Se establece el zoom minimo
                    map.maxZoomLevel = 20.0//Y aquie el maximo zoom

                    //Se define el centro inicial del mapa
                    map.controller.setCenter(GeoPoint(0.0, 0.0))
                    //Este es para el zoom inicial
                    map.controller.setZoom(5.0)

                    //Qui es donde se detecta la pulsacion para seleccionar la casa
                    val eventsReceiver = object : MapEventsReceiver {
                        //No pasa nada con tap simples
                        override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            //Si esta en modo de seleccion y se presiona un punto valido
                            if (selectingHouse && p != null) {
                                val prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putString("house_lat", p.latitude.toString())//Guardar latitud
                                    putString("house_lon", p.longitude.toString())//Guardar longitud
                                    apply()
                                }
                                //Se actualiza la ubicacion de la casa en el estado
                                houseLocation = p
                                Toast.makeText(
                                    ctx,
                                    "Casa guardada en: ${p.latitude}, ${p.longitude}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //Si ya se tiene la ubicacion actual se solicita la ruta
                                currentLocation?.let { loc ->
                                    requestRoute(loc, p) { points, error ->
                                        if (error != null) {
                                            errorMessage = error
                                            routePoints = emptyList()
                                        } else {
                                            routePoints = points
                                        }
                                    }
                                }
                                //Se desactiva el modo de seleccion
                                selectingHouse = false
                            }
                            return true
                        }
                    }
                    //Se agrega el overlay de eventos al mapa
                    map.overlays.add(MapEventsOverlay(eventsReceiver))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        //N¿Botones de seleccion y eliminacion de la casa
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row {
                //Boton para activar el modo de selección de casa
                Button(
                    onClick = {
                        selectingHouse = true
                        Toast.makeText(
                            context,
                            "Mantén pulsado en el mapa para seleccionar tu casa",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Seleccionar Casa")
                }
                Spacer(modifier = Modifier.width(8.dp))

                //Boton para eliminar la casa
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        //Eliminar datos guardados
                        prefs.edit().remove("house_lat").remove("house_lon").apply()
                        houseLocation = null
                        routePoints = null
                        Toast.makeText(context, "Casa eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Eliminar Casa")
                }
            }
        }

        //Para poder mostrar los errores y la ubicacion actual en la parte de arriba
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
            //Si no se concedio el permiso mostrar mensaje de error
            if (permissionChecked && !hasLocationPermission) {
                Text(text = "Permiso de ubicación no concedido.", color = MaterialTheme.colorScheme.error)
            }
            //Mostrar la ubicación actual si esta disponible
            currentLocation?.let { loc ->
                Text(text = "Ubicación actual: ${loc.latitude}, ${loc.longitude}")
            }
            //Mostrar cualquier mensaje de error relacionado con la ruta
            errorMessage?.let { msg ->
                Text(text = msg, color = MaterialTheme.colorScheme.error)
            }
        }
    }



    //Actualizar los overlays del mapa (o sea los marcadores y la ruta)
    LaunchedEffect(currentLocation, houseLocation, routePoints) {
        mapView?.post {
            //Eliminar overlays existentes de tipo Marker o Polyline para evitar que se dupliquen
            mapView?.overlays?.removeAll { it is Marker || it is Polyline }

            //Dibujar la ruta si existen puntos
            if (!routePoints.isNullOrEmpty()) {
                val polyline = Polyline().apply {
                    setPoints(routePoints)
                    color = android.graphics.Color.GREEN
                    width = 10f
                }
                //Se agrega la ruta al mapa
                mapView?.overlays?.add(polyline)
            }

            //Aqui se agrega el marcador de la ubicacion actual
            currentLocation?.let { loc ->
                val point = GeoPoint(loc.latitude, loc.longitude)
                val marker = Marker(mapView)
                marker.position = point
                //Anclar el marcador para que el centro horizontal y la parte inferior
                //coincidan con la coordenada
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Tu ubicación"
                val originalDrawableCurrent = ContextCompat.getDrawable(
                    context, R.drawable.pointer_pin_svgrepo_25)
                marker.icon = originalDrawableCurrent
                mapView?.overlays?.add(marker)

                //Si no hay casa definida y el mapa aun no se ha centrado se centra el mapa
                if (!initialCentered) {
                    mapView?.controller?.setZoom(17.5)
                    mapView?.controller?.animateTo(point)
                    initialCentered = true
                }
            }

            //Aca se agrega el marcador de la casa si hay uno
            houseLocation?.let { house ->
                val marker = Marker(mapView)
                marker.position = house
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Casa"

                val originalDrawable = ContextCompat.getDrawable(
                    context, R.drawable.house_svgrepo_25)
                marker.icon = originalDrawable
                mapView?.overlays?.add(marker)
            }

            //Refrescar el mapa para mostrar los nuevos overlays
            mapView?.invalidate()
        }
    }

}

//Funcion para solicitar la ruta usando DirectionsRepository y actualizar la parte visual
//en el hilo principal
private fun requestRoute(
    currentLoc: Location,//Ubicacion actual
    house: GeoPoint,//Ubicacion de la mansion

    //Callback para devolver la lista de puntos o un mensaje de error
    callback: (points: List<GeoPoint>?, error: String?) -> Unit
) {
    //Se llama a la función getRoute del DirectionsRepository pasando la latitud y longitud actuales
    //y el destino
    DirectionsRepository.getRoute(
        currentLat = currentLoc.latitude,
        currentLon = currentLoc.longitude,
        destination = house
    ) { points ->
        //Se lanza una corrutina en el hilo principal para actualizar la interfaz grafica
        CoroutineScope(Dispatchers.Main).launch {
            //Si no se obtuvieron puntos se llama al callback con un mensaje de error
            if (points == null) {
                callback(
                    null,
                    "No se pudo obtener la ruta (puede que la distancia exceda el límite).")
            } else {
                //Si se obtuvieron los puntos se llama al callback con la lista de puntos y sin error
                callback(points, null)
            }
        }
    }
}

