package com.example.androidlearningpath

import android.content.Context
import androidx.room.*
import kotlinx.serialization.Serializable
import java.util.UUID

// 1. Entidad de la Tabla SQL
@Entity(tableName = "tabla_tareas")
data class TareaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "esta_completada") val estaCompletada: Boolean = false
)

// 2. DTO de Red (API)
@Serializable
data class TareaRemoteDto(
    val id: Int,
    val title: String,
    val completed: Boolean
)

// 3. Interfaz de Consultas SQL (DAO)
@Dao
interface TareaDao {
    @Query("SELECT * FROM tabla_tareas")
    fun getAllTareas(): List<TareaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTarea(tarea: TareaEntity)

    @Delete
    fun deleteTarea(tarea: TareaEntity)
}

// 4. El Administrador de la Base de Datos (Trasladado aquí para resolver el error)
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