package com.example.androidlearningpath

import io.ktor.client.request.*
import io.ktor.client.call.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TareaRepository(private val tareaDao: TareaDao) {

    // Obtener datos locales
    suspend fun getTareasLocales(): List<TareaEntity> = withContext(Dispatchers.IO) {
        tareaDao.getAllTareas()
    }

    // Guardar o Actualizar local
    suspend fun guardarTarea(tarea: TareaEntity) = withContext(Dispatchers.IO) {
        tareaDao.insertTarea(tarea)
    }

    // Borrar local
    suspend fun eliminarTarea(tarea: TareaEntity) = withContext(Dispatchers.IO) {
        tareaDao.deleteTarea(tarea)
    }

    // Descargar de Internet y mapear directamente a nuestro modelo local
    suspend fun refrescarDesdeAPI(): List<TareaEntity> = withContext(Dispatchers.IO) {
        try {
            val deRed: List<TareaRemoteDto> = httpClient.get("https://jsonplaceholder.typicode.com/todos?_limit=5").body()
            deRed.map { dto ->
                TareaEntity(
                    titulo = "[API] ${dto.title}",
                    estaCompletada = dto.completed
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}