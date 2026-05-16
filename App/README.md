# AppApi — Gestor de Tareas Android

Aplicacion Android nativa en **Kotlin + Jetpack Compose** que consume una API REST CRUD de tareas alojada en un servidor local.

---

## Tecnologias

| Capa | Tecnologia | Version |
|---|---|---|
| Lenguaje | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material 3 | BOM 2024.09.00 |
| HTTP Client | Retrofit2 | 2.11.0 |
| Cliente base | OkHttp3 | 4.12.0 |
| JSON | Gson (Retrofit converter) | — |
| Concurrencia | Kotlin Coroutines | — |
| Build | Gradle 8.13 (KTS) + Version Catalog | — |

---

## Estructura del Proyecto

```
AppApi/
├── app/
│   ├── build.gradle.kts                     # dependencias del modulo
│   └── src/main/
│       ├── AndroidManifest.xml              # permisos INTERNET + cleartext
│       └── java/com/umg/appapi/
│           ├── Tarea.kt                     # data class (modelo de datos)
│           ├── TareaApi.kt                  # interfaz Retrofit + factory
│           ├── MainActivity.kt             # UI Compose + logica CRUD
│           └── ui/theme/
│               ├── Color.kt
│               ├── Theme.kt                # Material 3 dinamico
│               └── Type.kt
└── gradle/
    └── libs.versions.toml                   # version catalog
```

---

## Arquitectura

```
[TareaScreen / TareaItem]  ← Jetpack Compose (UI stateless/stateful)
         |
    mutableStateOf         ← estado reactivo (tareas, isLoading, titulo...)
         |
   Kotlin Coroutines       ← scope.launch / LaunchedEffect
         |
      TareaApi             ← interfaz Retrofit (suspend fun)
         |
      OkHttpClient         ← ejecuta peticiones en hilo I/O
         |
  http://10.0.2.2:3000     ← API REST (localhost del host en emulador)
```

---

## Archivos Clave

### `Tarea.kt` — Modelo de datos

```kotlin
data class Tarea(
    val id: Int? = null,
    val titulo: String,
    val descripcion: String? = "",
    @SerializedName("completada")   // campo JSON es "completada" (Int 0/1 MySQL)
    val completadaInt: Int = 0
) {
    val completada: Boolean get() = completadaInt == 1
}
```

**Por que `Int` y no `Boolean`?**  
MySQL almacena booleanos como `TINYINT(1)`. Si la API devuelve `0`/`1` y se usa `Boolean` en Gson, puede fallar la deserializacion. El campo `completadaInt: Int` es robusto y compatible.

---

### `TareaApi.kt` — Cliente HTTP (Retrofit)

```kotlin
interface TareaApi {
    @GET("tareas")
    suspend fun getTareas(): List<Tarea>

    @GET("tareas/{id}")
    suspend fun getTarea(@Path("id") id: Int): Tarea

    @POST("tareas")
    suspend fun createTarea(@Body tarea: Tarea): Tarea

    @PUT("tareas/{id}")
    suspend fun updateTarea(@Path("id") id: Int, @Body tarea: Tarea): Tarea

    @DELETE("tareas/{id}")
    suspend fun deleteTarea(@Path("id") id: Int)

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/"  // IP del emulador

        fun create(): TareaApi {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TareaApi::class.java)
        }
    }
}
```

**Reglas importantes de `BASE_URL`:**
- Debe terminar en `/`
- NO debe incluir el path `tareas`
- Para emulador: `10.0.2.2` = `localhost` del host
- Para dispositivo fisico: usar la IP local de la PC (ej. `192.168.1.x`)

---

### `MainActivity.kt` — UI Compose

```kotlin
class MainActivity : ComponentActivity() {
    private val api = TareaApi.create()   // instancia unica

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppApiTheme {
                Scaffold { innerPadding ->
                    TareaScreen(api, Modifier.padding(innerPadding))
                }
            }
        }
    }
}
```

**`TareaScreen`** gestiona el estado y las operaciones CRUD:

```kotlin
@Composable
fun TareaScreen(api: TareaApi, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var tareas by remember { mutableStateOf(emptyList<Tarea>()) }
    var isLoading by remember { mutableStateOf(false) }

    fun cargarTareas() {
        scope.launch {
            isLoading = true
            try { tareas = api.getTareas() }
            catch (e: Exception) { /* Toast de error */ }
            finally { isLoading = false }
        }
    }

    LaunchedEffect(Unit) { cargarTareas() }  // carga al iniciar

    // Formulario + LazyColumn con TareaItem
}
```

---

### `AndroidManifest.xml` — Permisos

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application android:usesCleartextTraffic="true" ...>
```

- `INTERNET`: obligatorio para cualquier peticion de red.
- `usesCleartextTraffic="true"`: Android 9+ bloquea HTTP por defecto. Necesario porque la API corre en HTTP, no HTTPS.

---

## Endpoints de la API

| Metodo | Ruta | Funcion | Descripcion |
|---|---|---|---|
| GET | `/tareas` | `getTareas()` | Lista todas las tareas |
| GET | `/tareas/{id}` | `getTarea(id)` | Obtiene una tarea por ID |
| POST | `/tareas` | `createTarea(tarea)` | Crea una nueva tarea |
| PUT | `/tareas/{id}` | `updateTarea(id, tarea)` | Actualiza una tarea |
| DELETE | `/tareas/{id}` | `deleteTarea(id)` | Elimina una tarea |

**Ejemplo de JSON:**

```json
{
  "id": 1,
  "titulo": "Estudiar Kotlin",
  "descripcion": "Repasar coroutines",
  "completada": 0
}
```

---

## Flujo de una Operacion

**Listar tareas:**

```
LaunchedEffect(Unit)
  → cargarTareas()
    → scope.launch { }
      → isLoading = true
      → api.getTareas()          ← suspend: hilo I/O
        → GET http://10.0.2.2:3000/tareas
        ← [{"id":1,...}, ...]    ← JSON
        → Gson deserializa List<Tarea>
      → tareas = resultado       ← recomposicion automatica
      → isLoading = false
```

**Toggle completada:**

```kotlin
val actualizada = tarea.copy(completadaInt = if (tarea.completada) 0 else 1)
api.updateTarea(tarea.id!!, actualizada)
cargarTareas()
```

---

## Dependencias (libs.versions.toml)

```toml
[versions]
agp     = "8.13.2"
kotlin  = "2.0.21"
retrofit = "2.11.0"
okhttp  = "4.12.0"

[libraries]
retrofit       = { group = "com.squareup.retrofit2", name = "retrofit",          version.ref = "retrofit" }
retrofit-gson  = { group = "com.squareup.retrofit2", name = "converter-gson",   version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3",   name = "logging-interceptor", version.ref = "okhttp" }
```

---

## Como Ejecutar

1. Iniciar el servidor de la API en tu PC (puerto `3000`)
2. Abrir el proyecto en **Android Studio Hedgehog** o superior
3. Verificar que `BASE_URL = "http://10.0.2.2:3000/"` en `TareaApi.kt`
4. Correr en el emulador con `Run > Run 'app'`
5. La lista de tareas se carga automaticamente

---

## Errores Comunes

| Error | Causa | Solucion |
|---|---|---|
| `ConnectException` | Servidor no esta corriendo | Iniciar el servidor en el host |
| `CLEARTEXT not permitted` | Falta `usesCleartextTraffic` | Agregar en AndroidManifest |
| Lista vacia / no conecta | IP incorrecta | Usar `10.0.2.2`, no `localhost` |
| `JsonSyntaxException` | JSON no coincide con data class | Revisar `@SerializedName` |
| `404 Not Found` | BASE_URL mal formada | BASE_URL debe terminar en `/` y sin `"tareas"` |

---

## minSdk y compatibilidad

```
minSdk   = 24   → Android 7.0 (Nougat) en adelante
targetSdk = 36  → Android 16
```

El color dinamico (Material You) solo se activa en **Android 12+ (API 31)**. En versiones anteriores usa el esquema de colores estatico purpura.
