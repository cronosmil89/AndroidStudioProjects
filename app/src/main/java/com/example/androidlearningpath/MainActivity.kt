package com.example.androidlearningpath


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import kotlinx.coroutines.launch
import java.util.UUID

// ==========================================
// 1. CAPA DE DATOS: ROOM (SQLite)
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
    fun getAllTareas(): List<TareaEntity> // 🟢 Quitamos suspend

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTarea(tarea: TareaEntity) // 🟢 Quitamos suspend

    @Delete
    fun deleteTarea(tarea: TareaEntity) // 🟢 Quitamos suspend
}

@Database(entities = [TareaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tareas_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 2. CAPA DE VISTA (UI con Compose)
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoAppPersistente()
                }
            }
        }
    }
}

@Composable
fun TodoAppPersistente() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val tareaDao = db.tareaDao()

    var textoTarea by remember { mutableStateOf("") }
    val listaTareas = remember { mutableStateListOf<TareaEntity>() }
    val coroutineScope = rememberCoroutineScope()

    // Cargar las tareas asincrónicamente al arrancar
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val tareasDeDb = withContext(Dispatchers.IO) {
                tareaDao.getAllTareas() // Ejecuta en hilo de fondo
            }
            listaTareas.addAll(tareasDeDb) // Vuelve al hilo de UI
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Laboratorio SQL Persistente",
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
                placeholder = { Text("Escribe una tarea para guardar en SQL...") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textoTarea.isNotBlank()) {
                        val nuevaTarea = TareaEntity(titulo = textoTarea)
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                tareaDao.insertTarea(nuevaTarea) // Ejecuta en segundo plano
                            }
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

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(listaTareas, key = { it.id }) { tarea ->
                ItemTareaPersistente(
                    tarea = tarea,
                    onCheckedChange = { nuevoEstado ->
                        val index = listaTareas.indexOf(tarea)
                        if (index != -1) {
                            val tareaActualizada = tarea.copy(estaCompletada = nuevoEstado)
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    tareaDao.insertTarea(tareaActualizada) // Hace REPLACE de fondo
                                }
                                listaTareas[index] = tareaActualizada
                            }
                        }
                    },
                    onDeleteClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                tareaDao.deleteTarea(tarea) // Elimina de fondo
                            }
                            listaTareas.remove(tarea)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ItemTareaPersistente(
    tarea: TareaEntity,
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

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = tarea.titulo,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            TextButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Borrar")
            }
        }
    }
}