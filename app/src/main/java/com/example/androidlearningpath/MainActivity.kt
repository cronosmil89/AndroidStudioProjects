package com.example.androidlearningpath

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
import androidx.compose.ui.unit.dp
import java.util.UUID

// 1. El Modelo de Datos se mantiene intacto
data class TareaModel(
    val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val estaCompletada: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoAppEvolucionada()
                }
            }
        }
    }
}

@Composable
fun TodoAppEvolucionada() {
    var textoTarea by remember { mutableStateOf("") }
    val listaTareas = remember { mutableStateListOf<TareaModel>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Laboratorio de Tareas v2",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textoTarea,
                onValueChange = { textoTarea = it },
                placeholder = { Text("Escribe una tarea con estado...") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textoTarea.isNotBlank()) {
                        listaTareas.add(TareaModel(titulo = textoTarea))
                        textoTarea = ""
                    }
                }
            ) {
                Text("Añadir")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(listaTareas, key = { it.id }) { tarea ->
                ItemTareaInteractiva(
                    tarea = tarea,
                    onCheckedChange = { nuevoEstado ->
                        val index = listaTareas.indexOf(tarea)
                        if (index != -1) {
                            listaTareas[index] = tarea.copy(estaCompletada = nuevoEstado)
                        }
                    },
                    onDeleteClick = {
                        listaTareas.remove(tarea)
                    }
                )
            }
        }
    }
}

@Composable
fun ItemTareaInteractiva(
    tarea: TareaModel,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = tarea.estaCompletada,
                onCheckedChange = onCheckedChange
            )

            // Corregido el '=' sobrante que causaba error de sintaxis
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = tarea.titulo,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            // Cambiado el Icono por un Text Button rojo para evitar dependencias faltantes
            TextButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Borrar")
            }
        }
    }
}