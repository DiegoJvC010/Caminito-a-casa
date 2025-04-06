package com.example.campos_homecomming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.campos_homecomming.ui.MapScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(
                topBar = { TopAppBar() }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    MapScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar() {
    //Barra de aplicaciones centrada usando CenterAlignedTopAppBar
    CenterAlignedTopAppBar(
        title = {
            //Define el tÃtulo de la barra superior
            Text(
                "Caminito a la casa", //Texto del titulo
                color = Color.White, //Color del texto
                //Estilo del texto con negrita
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Black //Color de fondo de la topBar
        )
    )
}
