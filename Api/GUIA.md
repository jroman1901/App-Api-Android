# Crear una API REST con Node.js y MySQL

Guia paso a paso para construir una API de gestion de tareas.

---

## Endpoints que vamos a construir

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| GET | /tareas | Obtener todas las tareas |
| GET | /tareas/:id | Obtener una tarea por ID |
| POST | /tareas | Crear una nueva tarea |
| PUT | /tareas/:id | Actualizar una tarea |
| DELETE | /tareas/:id | Eliminar una tarea |

---

## Paso 1 — Requisitos previos

Antes de comenzar, asegurate de tener instalado:

- **Node.js** v18 o superior — nodejs.org
- **MySQL** v8 — puedes usar XAMPP o MySQL Workbench
- **Visual Studio Code**

Verifica tu instalacion en la terminal:

```bash
node --version
npm --version
```

---

## Paso 2 — Crear la base de datos en MySQL

Abre MySQL Workbench y ejecuta el siguiente script SQL:

```sql
CREATE DATABASE db_tareas;

USE db_tareas;

CREATE TABLE tareas (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  titulo      VARCHAR(100) NOT NULL,
  descripcion TEXT,
  completada  BOOLEAN DEFAULT false
);
```

> La columna `id` se incrementa automaticamente. `completada` empieza en `false` por defecto.

---

## Paso 3 — Inicializar el proyecto Node.js

Crea una carpeta y abrela en la terminal:

```bash
mkdir api-tareas
cd api-tareas
```

Inicializa el proyecto:

```bash
npm init -y
```

Instala las dependencias:

```bash
npm install express mysql2 dotenv
```

| Paquete | Para que sirve |
|---------|----------------|
| express | Framework para crear el servidor HTTP |
| mysql2 | Driver para conectarse a MySQL |
| dotenv | Leer variables de entorno desde `.env` |

---

## Paso 4 — Crear el archivo .env

Crea un archivo llamado `.env` en la raiz del proyecto:

```
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=db_tareas
```

> **Importante:** nunca subas el archivo `.env` a GitHub. Agregalo a tu `.gitignore`.

---

## Paso 5 — Crear la conexion a MySQL (db.js)

Crea el archivo `db.js` en la raiz del proyecto:

```js
const mysql = require('mysql2');
require('dotenv').config();

const connection = mysql.createConnection({
  host:     process.env.DB_HOST     || 'localhost',
  user:     process.env.DB_USER     || 'root',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME     || 'db_tareas'
});

connection.connect((err) => {
  if (err) {
    console.error('Error conectando a MySQL:', err);
    return;
  }
  console.log('Conectado a MySQL correctamente');
});

module.exports = connection;
```

> `require('dotenv').config()` carga las variables del archivo `.env` en `process.env`.

---

## Paso 6 — Crear el servidor y las rutas (index.js)

Crea el archivo `index.js`. Este es el archivo principal de la API.

### 6.1 Configuracion inicial

```js
const express = require('express');
const db      = require('./db');

const app = express();
app.use(express.json());
```

> `express.json()` permite que el servidor entienda el cuerpo de las peticiones en formato JSON.

### 6.2 GET — Obtener todas las tareas

```js
// GET - Obtener todas las tareas
app.get('/tareas', (req, res) => {
  db.query('SELECT * FROM tareas', (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
});
```

### 6.3 GET — Obtener una tarea por ID

```js
// GET - Obtener una tarea por ID
app.get('/tareas/:id', (req, res) => {
  db.query(
    'SELECT * FROM tareas WHERE id = ?',
    [req.params.id],
    (err, results) => {
      if (err) return res.status(500).json({ error: err.message });
      if (results.length === 0)
        return res.status(404).json({ mensaje: 'Tarea no encontrada' });
      res.json(results[0]);
    }
  );
});
```

> El `?` en la query SQL es un prepared statement. Evita inyecciones SQL.

### 6.4 POST — Crear una tarea

```js
// POST - Crear una tarea
app.post('/tareas', (req, res) => {
  const { titulo, descripcion } = req.body;
  db.query(
    'INSERT INTO tareas (titulo, descripcion) VALUES (?, ?)',
    [titulo, descripcion],
    (err, result) => {
      if (err) return res.status(500).json({ error: err.message });
      res.status(201).json({ mensaje: 'Tarea creada', id: result.insertId });
    }
  );
});
```

### 6.5 PUT — Actualizar una tarea

```js
// PUT - Actualizar una tarea
app.put('/tareas/:id', (req, res) => {
  const { titulo, descripcion, completada } = req.body;
  db.query(
    'UPDATE tareas SET titulo = ?, descripcion = ?, completada = ? WHERE id = ?',
    [titulo, descripcion, completada, req.params.id],
    (err, result) => {
      if (err) return res.status(500).json({ error: err.message });
      if (result.affectedRows === 0)
        return res.status(404).json({ mensaje: 'Tarea no encontrada' });
      res.json({ mensaje: 'Tarea actualizada' });
    }
  );
});
```

### 6.6 DELETE — Eliminar una tarea

```js
// DELETE - Eliminar una tarea
app.delete('/tareas/:id', (req, res) => {
  db.query(
    'DELETE FROM tareas WHERE id = ?',
    [req.params.id],
    (err, result) => {
      if (err) return res.status(500).json({ error: err.message });
      if (result.affectedRows === 0)
        return res.status(404).json({ mensaje: 'Tarea no encontrada' });
      res.json({ mensaje: 'Tarea eliminada' });
    }
  );
});
```

### 6.7 Iniciar el servidor

```js
const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Servidor corriendo en http://localhost:${PORT}`);
});
```

---

## Paso 7 — Estructura final del proyecto

```
api-tareas/
├── node_modules/
├── .env
├── db.js
├── index.js
└── package.json
```

---

## Paso 8 — Correr el servidor y probarlo

Inicia el servidor:

```bash
npm start
```

Salida esperada en consola:

```
Conectado a MySQL correctamente
Servidor corriendo en http://localhost:3000
```

### Probar con curl

Crear una tarea:

```bash
curl -X POST http://localhost:3000/tareas \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Mi primera tarea", "descripcion": "Descripcion de prueba"}'
```

Obtener todas las tareas:

```bash
curl http://localhost:3000/tareas
```

Obtener una tarea por ID:

```bash
curl http://localhost:3000/tareas/1
```

Actualizar una tarea:

```bash
curl -X PUT http://localhost:3000/tareas/1 \
  -H "Content-Type: application/json" \
  -d '{"titulo": "Tarea actualizada", "descripcion": "Nueva descripcion", "completada": true}'
```

Eliminar una tarea:

```bash
curl -X DELETE http://localhost:3000/tareas/1
```

> Tambien puedes usar **Postman** o la extension **Thunder Client** de VS Code para probar los endpoints de forma visual.
