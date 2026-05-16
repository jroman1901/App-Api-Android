package com.umg.appapi

import com.google.gson.annotations.SerializedName

data class Tarea(
    val id: Int? = null,
    val titulo: String,
    val descripcion: String? = "",
    // Usamos Int para que sea compatible con el 0/1 de MySQL
    @SerializedName("completada")
    val completadaInt: Int = 0
) {
    // Propiedad auxiliar para usarla fácilmente como Boolean en la UI
    val completada: Boolean get() = completadaInt == 1
}
