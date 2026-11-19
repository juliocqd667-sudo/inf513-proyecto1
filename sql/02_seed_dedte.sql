-- =============================================================================
-- Datos Semilla (Seed) - Sistema de Analítica y Monitoreo DEDTE - UAGRM
-- =============================================================================

-- CU1: Usuarios iniciales
INSERT INTO usuarios (ci, nombre, apellido, rol, email, telefono, estado) VALUES
('219198322', 'Julio Cesar', 'Quispe Delgado', 'ADMIN', 'julio.quispe@uagrm.edu.bo', '70011223', 'ACTIVO'),
('221044434', 'Kelly Alisson', 'Duran Marcelo', 'ADMIN', 'kelly.duran@uagrm.edu.bo', '70044556', 'ACTIVO'),
('4715292', 'Evans', 'Balcazar Veizaga', 'DIRECTIVO', 'evans.balcazar@uagrm.edu.bo', '71055123', 'ACTIVO'),
('5823411', 'Carla', 'Mendoza Rojas', 'ANALISTA', 'carla.mendoza@uagrm.edu.bo', '72133445', 'ACTIVO');

-- CU1: Parámetros del sistema (umbrales de semáforo de deserción y gestión activa)
INSERT INTO parametros_sistema (clave, valor, descripcion) VALUES
('GESTION_ACTUAL', '2026-2', 'Gestión académica vigente para monitoreo DEDTE'),
('UMBRAL_RIESGO_AMARILLO', '15.0', 'Porcentaje mínimo de deserción para asignar semáforo AMARILLO'),
('UMBRAL_RIESGO_ROJO', '30.0', 'Porcentaje mínimo de deserción para asignar semáforo ROJO'),
('CAPACIDAD_ESTANDAR_AULA', '50', 'Cupo referencial de estudiantes por aula virtual para cálculo de densidad');

-- CU2: Entornos Moodle reales de la UAGRM
INSERT INTO entornos_moodle (codigo, nombre, tipo, url_base, version_moodle, estado_conexion) VALUES
('AVA-VIRT', 'Moodle Virtual UAGRM', 'VIRTUAL', 'https://ava.uagrm.edu.bo/virtual', '5.1', 'ACTIVO'),
('AVA-PRES', 'Moodle Apoyo Presencial UAGRM', 'PRESENCIAL', 'https://ava.uagrm.edu.bo/presencial', '5.1', 'ACTIVO'),
('AVA-CAP',  'Moodle Capacitación Docente DEDTE', 'CAPACITACION', 'https://ava.uagrm.edu.bo/capacitacion', '5.1', 'ACTIVO');

-- CU3: Aulas aperturadas (Gestión 2026-2)
INSERT INTO aulas_apertura (entorno_id, gestion, facultad, carrera, sigla, grupo, materia, docente_codigo, docente_nombre, formato_curso, estado_apertura) VALUES
(1, '2026-2', 'FICCT', 'Ingeniería en Informática', 'INF513', 'SA', 'Tecnología Web', 'DOC-101', 'Ing. Evans Balcazar Veizaga', 'MOSAICOS', 'APERTURADA'),
(1, '2026-2', 'FICCT', 'Ingeniería en Sistemas', 'INF312', 'SB', 'Base de Datos I', 'DOC-102', 'Ing. Maria Luisa Perez', 'MOSAICOS', 'APERTURADA'),
(2, '2026-2', 'FICCT', 'Ingeniería en Redes', 'RDS410', 'SC', 'Seguridad en Redes', 'DOC-103', 'Ing. Carlos Alberto Vega', 'TEMAS', 'OBSERVADA'),
(1, '2026-2', 'FCE', 'Ingeniería Comercial', 'ECO210', 'Z1', 'Estadística Aplicada', 'DOC-201', 'Lic. Roberto Justiniano', 'MOSAICOS', 'APERTURADA'),
(3, '2026-2', 'DEDTE', 'Capacitación Docente', 'GUIA-DV', 'G1', 'Diseño de Aulas Virtuales Moodle 5.1', 'DOC-301', 'MSc. Elena Suarez', 'MOSAICOS', 'APERTURADA');

-- CU4: Monitoreo de Matrícula, Densidad y Rendimiento Académico
INSERT INTO monitoreo_academico (aula_id, inscritos, activos, aprobados, reprobados, promedio_notas, densidad_pct) VALUES
(1, 58, 54, 48, 6, 74.50, 116.00),
(2, 45, 39, 30, 9, 66.20, 90.00),
(3, 40, 26, 18, 8, 55.80, 80.00),
(4, 62, 50, 41, 9, 71.10, 124.00),
(5, 35, 34, 33, 1, 88.40, 70.00);

-- CU5: Evaluación de Deserción y Semáforos de Riesgo
INSERT INTO evaluacion_riesgo (aula_id, total_inscritos, retiros, en_mora, tasa_desercion, semaforo, observacion) VALUES
(1, 58, 4, 2, 6.90,  'VERDE',    'Retención óptima y alta participación en plataforma'),
(2, 45, 8, 3, 17.78, 'AMARILLO', 'Incremento moderado de inactividad tras primer parcial'),
(3, 40, 14, 5, 35.00, 'ROJO',    'Deserción crítica; requiere intervención académica y revisión de aula'),
(4, 62, 12, 4, 19.35, 'AMARILLO', 'Alta densidad estudiantil con retiros en semanas 4-6'),
(5, 35, 1, 0, 2.86,  'VERDE',    'Curso de capacitación docente con excelente seguimiento');

-- CU7: Históricos Semestrales
INSERT INTO historicos_semestrales (gestion, entorno_id, facultad, total_aulas, total_inscritos, tasa_aprobacion, tasa_desercion, promedio_general) VALUES
('2025-2', 1, 'FICCT', 42, 1850, 72.40, 18.20, 68.50),
('2026-1', 1, 'FICCT', 46, 2010, 75.80, 15.60, 70.90),
('2026-2', 1, 'FICCT', 50, 2180, 78.10, 13.40, 72.30),
('2025-2', 1, 'FCE',   30, 1420, 69.00, 21.50, 65.20),
('2026-1', 1, 'FCE',   33, 1510, 71.30, 19.80, 67.40),
('2026-2', 2, 'FICCT', 28, 1120, 66.50, 24.10, 63.80);
