package com.umg.appapi

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.umg.appapi.ui.theme.AppApiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val api = TareaApi.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppApiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TareaScreen(api, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TareaScreen(api: TareaApi, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tareas by remember { mutableStateOf(emptyList<Tarea>()) }
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun cargarTareas() {
        scope.launch {
            isLoading = true
            try {
                tareas = api.getTareas()
                Log.d("API_SUCCESS", "Tareas cargadas: ${tareas.size}")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error al cargar tareas", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar tareas al iniciar
    LaunchedEffect(Unit) {
        cargarTareas()
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Nueva Tarea", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (titulo.isBlank()) return@Button
                scope.launch {
                    try {
                        api.createTarea(Tarea(titulo = titulo, descripcion = descripcion))
                        cargarTareas()
                        titulo = ""
                        descripcion = ""
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al crear: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
        ) {
            Text("Agregar")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mis Tareas", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }

        if (tareas.isEmpty() && !isLoading) {
            Text("No hay tareas o no se pudo conectar al servidor", modifier = Modifier.padding(top = 16.dp))
        }

        LazyColumn {
            items(tareas) { tarea ->
                TareaItem(
                    tarea = tarea,
                    onDelete = {
                        scope.launch {
                            try {
                                api.deleteTarea(tarea.id!!)
                                cargarTareas()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onToggle = {
                        scope.launch {
                            try {

                                val nuevaTarea = tarea.copy(completadaInt = if (tarea.completada) 0 else 1)
                                api.updateTarea(tarea.id!!, nuevaTarea)
                                cargarTareas()
                            } catch (e: Exception) {
                                Log.e("API_ERROR", "Error al actualizar", e)
                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TareaItem(tarea: Tarea, onDelete: () -> Unit, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = tarea.completada, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(tarea.titulo, style = MaterialTheme.typography.titleMedium)
                // Manejamos el posible nulo de descripcion
                Text(tarea.descripcion ?: "", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}
