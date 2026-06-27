package com.example.androidlearningpath

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    // Factory manual provisional para armar la arquitectura
                    val context = LocalContext.current
                    val database = remember { AppDatabase.getDatabase(context) }
                    val repository = remember { TareaRepository(database.tareaDao()) }
                    val viewModel = remember { TareaViewModel(repository) }

                    // Disparador inicial
                    LaunchedEffect(Unit) {
                        viewModel.cargarTareasIniciales()
                    }

                    TodoAppArquitectonica(viewModel)
                }
            }
        }
    }
}

@Composable
fun TodoAppArquitectonica(viewModel: TareaViewModel) {
    var textoTarea by remember { mutableStateOf("") }
    val cargandoApi by viewModel.cargandoApi.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Laboratorio MVVM Avanzado",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.importarDeInternet() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargandoApi
        ) {
            Text(if (cargandoApi) "Sincronizando con Servidor..." else "Importar Tareas de Internet")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = textoTarea,
                onValueChange = { textoTarea = it },
                placeholder = { Text("Escribe una tarea...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textoTarea.isNotBlank()) {
                        viewModel.agregarNuevaTarea(textoTarea)
                        textoTarea = ""
                    }
                }
            ) {
                Text("Guardar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(viewModel.listaTareas, key = { it.id }) { tarea ->
                ItemTareaPersistente(
                    tarea = tarea,
                    onCheckedChange = { nuevoEstado -> viewModel.cambiarEstadoTarea(tarea, nuevoEstado) },
                    onDeleteClick = { viewModel.borrarTarea(tarea) }
                )
            }
        }
    }
}

@Composable
fun ItemTareaPersistente(tarea: TareaEntity, onCheckedChange: (Boolean) -> Unit, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = tarea.estaCompletada, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = tarea.titulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onDeleteClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Borrar")
            }
        }
    }
}