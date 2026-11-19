-- =============================================================================
-- UAGRM - FICCT - INF513 Tecnología Web (Gestión 2026-2)
-- Grupo #11 SA: Duran Marcelo Kelly Alisson / Quispe Delgado Julio Cesar
-- Sistema de Analítica y Monitoreo Académico DEDTE (Entornos Moodle AVA)
-- Motor: PostgreSQL (Compatible con Proyecto 1 Mail y Proyecto 2 Web)
-- =============================================================================

-- Limpieza controlada (solo para inicialización limpia)
DROP TABLE IF EXISTS evaluacion_riesgo CASCADE;
DROP TABLE IF EXISTS monitoreo_academico CASCADE;
DROP TABLE IF EXISTS aulas_apertura CASCADE;
DROP TABLE IF EXISTS historicos_semestrales CASCADE;
DROP TABLE IF EXISTS entornos_moodle CASCADE;
DROP TABLE IF EXISTS parametros_sistema CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;

-- =============================================================================
-- CU1: Gestión de Administración del Sistema (Usuarios y Parámetros)
-- =============================================================================
CREATE TABLE usuarios (
    id              SERIAL PRIMARY KEY,
    ci              VARCHAR(20) NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    rol             VARCHAR(30) NOT NULL CHECK (rol IN ('ADMIN', 'DIRECTIVO', 'ANALISTA', 'DOCENTE')),
    email           VARCHAR(120) NOT NULL UNIQUE,
    telefono        VARCHAR(30),
    estado          VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parametros_sistema (
    id              SERIAL PRIMARY KEY,
    clave           VARCHAR(60) NOT NULL UNIQUE,
    valor           VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(255) NOT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- CU2: Gestión de Conexión de Entornos Moodle (Virtual, Presencial, Capacitación)
-- =============================================================================
CREATE TABLE entornos_moodle (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(20) NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('VIRTUAL', 'PRESENCIAL', 'CAPACITACION')),
    url_base        VARCHAR(200) NOT NULL,
    version_moodle  VARCHAR(20) NOT NULL DEFAULT '5.1',
    estado_conexion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado_conexion IN ('ACTIVO', 'INACTIVO', 'MANTENIMIENTO')),
    ultima_sinc     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- CU3: Gestión de Automatizar Reporte de Apertura e Infraestructura
-- =============================================================================
CREATE TABLE aulas_apertura (
    id                  SERIAL PRIMARY KEY,
    entorno_id          INT NOT NULL REFERENCES entornos_moodle(id) ON DELETE RESTRICT,
    gestion             VARCHAR(15) NOT NULL,
    facultad            VARCHAR(100) NOT NULL,
    carrera             VARCHAR(120) NOT NULL,
    sigla               VARCHAR(20) NOT NULL,
    grupo               VARCHAR(10) NOT NULL,
    materia             VARCHAR(150) NOT NULL,
    docente_codigo      VARCHAR(30) NOT NULL,
    docente_nombre      VARCHAR(150) NOT NULL,
    formato_curso       VARCHAR(40) NOT NULL DEFAULT 'MOSAICOS',
    estado_apertura     VARCHAR(25) NOT NULL DEFAULT 'APERTURADA'
                        CHECK (estado_apertura IN ('APERTURADA', 'PENDIENTE', 'OBSERVADA', 'CERRADA')),
    fecha_apertura      DATE NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (entorno_id, gestion, sigla, grupo)
);

-- =============================================================================
-- CU4: Gestión de Monitoreo Matrícula, Densidad Estudiantil y Rendimiento
-- =============================================================================
CREATE TABLE monitoreo_academico (
    id                  SERIAL PRIMARY KEY,
    aula_id             INT NOT NULL REFERENCES aulas_apertura(id) ON DELETE CASCADE,
    inscritos           INT NOT NULL CHECK (inscritos >= 0),
    activos             INT NOT NULL CHECK (activos >= 0),
    aprobados           INT NOT NULL DEFAULT 0 CHECK (aprobados >= 0),
    reprobados          INT NOT NULL DEFAULT 0 CHECK (reprobados >= 0),
    promedio_notas      NUMERIC(5,2) NOT NULL DEFAULT 0.00 CHECK (promedio_notas >= 0 AND promedio_notas <= 100),
    densidad_pct        NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    fecha_corte         DATE NOT NULL DEFAULT CURRENT_DATE
);

-- =============================================================================
-- CU5: Gestión de Evaluar Deserción y Asignar Semáforos de Riesgo Académico
-- =============================================================================
CREATE TABLE evaluacion_riesgo (
    id                  SERIAL PRIMARY KEY,
    aula_id             INT NOT NULL REFERENCES aulas_apertura(id) ON DELETE CASCADE,
    total_inscritos     INT NOT NULL CHECK (total_inscritos > 0),
    retiros             INT NOT NULL DEFAULT 0 CHECK (retiros >= 0),
    en_mora             INT NOT NULL DEFAULT 0 CHECK (en_mora >= 0),
    tasa_desercion      NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    semaforo            VARCHAR(10) NOT NULL CHECK (semaforo IN ('VERDE', 'AMARILLO', 'ROJO')),
    observacion         VARCHAR(255),
    fecha_evaluacion    DATE NOT NULL DEFAULT CURRENT_DATE
);

-- =============================================================================
-- CU7: Gestión de Consultar Históricos Semestrales
-- =============================================================================
CREATE TABLE historicos_semestrales (
    id                  SERIAL PRIMARY KEY,
    gestion             VARCHAR(15) NOT NULL,
    entorno_id          INT NOT NULL REFERENCES entornos_moodle(id) ON DELETE RESTRICT,
    facultad            VARCHAR(100) NOT NULL,
    total_aulas         INT NOT NULL CHECK (total_aulas >= 0),
    total_inscritos     INT NOT NULL CHECK (total_inscritos >= 0),
    tasa_aprobacion     NUMERIC(5,2) NOT NULL CHECK (tasa_aprobacion >= 0 AND tasa_aprobacion <= 100),
    tasa_desercion      NUMERIC(5,2) NOT NULL CHECK (tasa_desercion >= 0 AND tasa_desercion <= 100),
    promedio_general    NUMERIC(5,2) NOT NULL CHECK (promedio_general >= 0 AND promedio_general <= 100),
    UNIQUE (gestion, entorno_id, facultad)
);

-- Índices para consultas rápidas (CU6 Dashboard y CU8 Reportes)
CREATE INDEX idx_aulas_gestion ON aulas_apertura(gestion);
CREATE INDEX idx_aulas_facultad ON aulas_apertura(facultad);
CREATE INDEX idx_riesgo_semaforo ON evaluacion_riesgo(semaforo);
CREATE INDEX idx_hist_gestion ON historicos_semestrales(gestion);
