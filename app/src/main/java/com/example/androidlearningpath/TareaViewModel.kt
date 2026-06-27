package com.example.androidlearningpath

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TareaViewModel(private val repository: TareaRepository) : ViewModel() {

    // Lista reactiva que la UI va a observar
    val listaTareas = mutableStateListOf<TareaEntity>()

    private val _cargandoApi = MutableStateFlow(false)
    val cargandoApi: StateFlow<Boolean> = _cargandoApi

    fun cargarTareasIniciales() {
        viewModelScope.launch {
            val locales = repository.getTareasLocales()
            listaTareas.clear()
            listaTareas.addAll(locales)
        }
    }

    fun agregarNuevaTarea(titulo: String) {
        if (titulo.isNotBlank()) {
            val nueva = TareaEntity(titulo = titulo)
            viewModelScope.launch {
                repository.guardarTarea(nueva)
                listaTareas.add(nueva)
            }
        }
    }

    fun cambiarEstadoTarea(tarea: TareaEntity, nuevoEstado: Boolean) {
        val index = listaTareas.indexOf(tarea)
        if (index != -1) {
            val actualizada = tarea.copy(estaCompletada = nuevoEstado)
            viewModelScope.launch {
                repository.guardarTarea(actualizada)
                listaTareas[index] = actualizada
            }
        }
    }

    fun borrarTarea(tarea: TareaEntity) {
        viewModelScope.launch {
            repository.eliminarTarea(tarea)
            listaTareas.remove(tarea)
        }
    }

    fun importarDeInternet() {
        viewModelScope.launch {
            _cargandoApi.value = true
            val remotas = repository.refrescarDesdeAPI()
            remotas.forEach { nuevaTarea ->
                repository.guardarTarea(nuevaTarea)
                if (listaTareas.none { it.titulo == nuevaTarea.titulo }) {
                    listaTareas.add(nuevaTarea)
                }
            }
            _cargandoApi.value = false
        }
    }
}