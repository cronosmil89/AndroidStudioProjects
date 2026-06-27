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
import androidx.room.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

// ==========================================
// 1. CAPA DE DATOS LOCALES: ROOM (SQLite)
// ==========================================
@Entity(tableName = "tabla_tareas")
data class TareaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "esta_completada") val estaCompletada: Boolean = false
)

@Dao
interface TareaDao {
    @Query("SELECT * FROM tabla_tareas")
    fun getAllTareas(): List<TareaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTarea(tarea: TareaEntity)

    @Delete
    fun deleteTarea(tarea: TareaEntity)
}

@Database(entities = [TareaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tareaDao(): TareaDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "tareas_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 2. CAPA DE RED: KTOR & DTO (Data Transfer Object)
// ==========================================
// @Serializable le dice a Kotlin que esta clase puede mapear un JSON externo de forma automática
@Serializable
data class TareaRemoteDto(
    val id: Int,
    val title: String,
    val completed: Boolean
)

// Cliente HTTP global configurado para entender JSON
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Si la API manda datos de más, los ignora sin romper la app
        })
    }
}

// ==========================================
// 3. CAPA DE VISTA (UI con Compose)
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodoAppAPIYPersistente()
                }
            }
        }
    }
}

@Composable
fun TodoAppAPIYPersistente() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val tareaDao = db.tareaDao()

    var textoTarea by remember { mutableStateOf("") }
    val listaTareas = remember { mutableStateListOf<TareaEntity>() }
    val coroutineScope = rememberCoroutineScope()
    var cargandoApi by remember { mutableStateOf(false) }

    // Al arrancar, cargamos primero lo que esté guardado en la base de datos local
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val tareasDeDb = withContext(Dispatchers.IO) { tareaDao.getAllTareas() }
            listaTareas.addAll(tareasDeDb)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Laboratorio Híbrido: SQL + API",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // BOTÓN ESTRATÉGICO: Descargar desde la API de internet
        Button(
            onClick = {
                cargandoApi = true
                coroutineScope.launch {
                    try {
                        // 1. Hacemos la petición HTTP en el hilo de red (IO)
                        val tareasRemotas: List<TareaRemoteDto> = withContext(Dispatchers.IO) {
                            httpClient.get("https://jsonplaceholder.typicode.com/todos?_limit=5").body()
                        }

                        // 2. Mapeamos las tareas de la API externa a nuestro formato interno de Room y UI
                        tareasRemotas.forEach { dto ->
                            val nuevaTarea = TareaEntity(titulo = "[API] ${dto.title}", estaCompletada = dto.completed)

                            // Guardamos en la base de datos local para que persistan
                            withContext(Dispatchers.IO) { tareaDao.insertTarea(nuevaTarea) }

                            // Agregamos a la lista de la pantalla si no existe ya
                            if (listaTareas.none { it.titulo == nuevaTarea.titulo }) {
                                listaTareas.add(nuevaTarea)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Log del error si falla la red (ej. sin internet)
                    } finally {
                        cargandoApi = false
                    }
                }
            },
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
                placeholder = { Text("Escribe una tarea local...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textoTarea.isNotBlank()) {
                        val nuevaTarea = TareaEntity(titulo = textoTarea)
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { tareaDao.insertTarea(nuevaTarea) }
                            listaTareas.add(nuevaTarea)
                            textoTarea = ""
                        }
                    }
                }
            ) {
                Text("Guardar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(listaTareas, key = { it.id }) { tarea ->
                ItemTareaPersistente(
                    tarea = tarea,
                    onCheckedChange = { nuevoEstado ->
                        val index = listaTareas.indexOf(tarea)
                        if (index != -1) {
                            val tareaActualizada = tarea.copy(estaCompletada = nuevoEstado)
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) { tareaDao.insertTarea(tareaActualizada) }
                                listaTareas[index] = tareaActualizada
                            }
                        }
                    },
                    onDeleteClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) { tareaDao.deleteTarea(tarea) }
                            listaTareas.remove(tarea)
                        }
                    }
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