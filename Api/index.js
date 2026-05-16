const express = require('express');
const db = require('./db');

const app = express();
app.use(express.json());

// GET - Obtener todas las tareas
app.get('/tareas', (req, res) => {
  db.query('SELECT * FROM tareas', (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    res.json(results);
  });
});

// GET - Obtener una tarea por ID
app.get('/tareas/:id', (req, res) => {
  db.query('SELECT * FROM tareas WHERE id = ?', [req.params.id], (err, results) => {
    if (err) return res.status(500).json({ error: err.message });
    if (results.length === 0) return res.status(404).json({ mensaje: 'Tarea no encontrada' });
    res.json(results[0]);
  });
});

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

// PUT - Actualizar una tarea
app.put('/tareas/:id', (req, res) => {
  const { titulo, descripcion, completada } = req.body;
  db.query(
    'UPDATE tareas SET titulo = ?, descripcion = ?, completada = ? WHERE id = ?',
    [titulo, descripcion, completada, req.params.id],
    (err, result) => {
      if (err) return res.status(500).json({ error: err.message });
      if (result.affectedRows === 0) return res.status(404).json({ mensaje: 'Tarea no encontrada' });
      res.json({ mensaje: 'Tarea actualizada' });
    }
  );
});

// DELETE - Eliminar una tarea
app.delete('/tareas/:id', (req, res) => {
  db.query('DELETE FROM tareas WHERE id = ?', [req.params.id], (err, result) => {
    if (err) return res.status(500).json({ error: err.message });
    if (result.affectedRows === 0) return res.status(404).json({ mensaje: 'Tarea no encontrada' });
    res.json({ mensaje: 'Tarea eliminada' });
  });
});

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`Servidor corriendo en http://localhost:${PORT}`);
});
