package bo.tecnoweb.dedte.negocio;

import bo.tecnoweb.dedte.config.RegistroLog;
import bo.tecnoweb.dedte.correo.FormateadorHTML;
import bo.tecnoweb.dedte.datos.*;
import bo.tecnoweb.dedte.lexico.AnalizadorLexico;
import bo.tecnoweb.dedte.lexico.Comando;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Capa de Negocio: valida tipos, reglas de dominio y despacha cada comando
 * hacia los DAOs correspondientes para los 8 Casos de Uso aprobados (Grupo #11).
 */
public class DespachadorComandos {

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "DIRECTIVO", "ANALISTA", "DOCENTE");
    private static final Set<String> ESTADOS_USR = Set.of("ACTIVO", "INACTIVO");
    private static final Set<String> TIPOS_ENTORNO = Set.of("VIRTUAL", "PRESENCIAL", "CAPACITACION");
    private static final Set<String> ESTADOS_ENTORNO = Set.of("ACTIVO", "INACTIVO", "MANTENIMIENTO");
    private static final Set<String> ESTADOS_APERTURA = Set.of("APERTURADA", "PENDIENTE", "OBSERVADA", "CERRADA");

    private final AnalizadorLexico analizador = new AnalizadorLexico();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ParametroDAO parametroDAO = new ParametroDAO();
    private final EntornoMoodleDAO entornoDAO = new EntornoMoodleDAO();
    private final AulaAperturaDAO aulaDAO = new AulaAperturaDAO();
    private final MonitoreoDAO monitoreoDAO = new MonitoreoDAO();
    private final EvaluacionRiesgoDAO riesgoDAO = new EvaluacionRiesgoDAO();
    private final HistoricoDAO historicoDAO = new HistoricoDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    public static class Resultado {
        public final boolean exito;
        public final String asuntoRespuesta;
        public final String cuerpoHtml;
        public final String resumenLog;

        public Resultado(boolean exito, String asuntoRespuesta, String cuerpoHtml, String resumenLog) {
            this.exito = exito;
            this.asuntoRespuesta = asuntoRespuesta;
            this.cuerpoHtml = cuerpoHtml;
            this.resumenLog = resumenLog;
        }
    }

    public Resultado procesar(String remitente, String subject) {
        Comando cmd = analizador.analizar(subject);
        if (!cmd.esValido()) {
            return new Resultado(
                    false,
                    "ERROR Sintaxis - DEDTE Mail System",
                    FormateadorHTML.error("Error de Sintaxis en Comando", cmd.getError()),
                    "SINTAXIS_INVALIDA: " + cmd.getError()
            );
        }

        try {
            return ejecutar(cmd);
        } catch (IllegalArgumentException e) {
            return new Resultado(
                    false,
                    "ERROR Validación [" + cmd.getNombre() + "]",
                    FormateadorHTML.error("Error de Validación en " + cmd.getNombre(), e.getMessage()),
                    "VALIDACION: " + e.getMessage()
            );
        } catch (SQLException e) {
            RegistroLog.error("DespachadorComandos", "Error SQL ejecutando " + cmd.getNombre() + " de " + remitente, e);
            return new Resultado(
                    false,
                    "ERROR Base de Datos [" + cmd.getNombre() + "]",
                    FormateadorHTML.error("Error en Base de Datos (" + cmd.getNombre() + ")", traducirErrorSql(e)),
                    "SQL_ERROR: " + e.getSQLState() + " - " + e.getMessage()
            );
        } catch (Exception e) {
            RegistroLog.error("DespachadorComandos", "Error interno ejecutando " + cmd.getNombre(), e);
            return new Resultado(
                    false,
                    "ERROR Interno [" + cmd.getNombre() + "]",
                    FormateadorHTML.error("Error Interno del Servidor", e.getMessage()),
                    "INTERNAL_ERROR: " + e.getMessage()
            );
        }
    }

    private Resultado ejecutar(Comando cmd) throws SQLException {
        String c = cmd.getNombre();
        switch (c) {
            case "HELP":
            case "AYUDA":
                return responderHelp();

            // ===================== CU1: USUARIOS =====================
            case "LISUSU": {
                validarCantidad(cmd, 1, "LISUSU[\"*\"] o LISUSU[\"ADMIN\"]");
                List<String[]> filas = usuarioDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU1 · Listado de Usuarios",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "CI", "Nombre Completo", "Rol", "Email", "Teléfono", "Estado"}, filas);
                return ok("LISUSU", html, "Listados " + filas.size() + " usuarios");
            }
            case "INSUSU": {
                validarCantidad(cmd, 6, "INSUSU[\"ci\",\"nombre\",\"apellido\",\"rol\",\"email\",\"telefono\"]");
                String ci = noVacio(cmd.getParam(0), "CI");
                String nombre = noVacio(cmd.getParam(1), "Nombre");
                String apellido = noVacio(cmd.getParam(2), "Apellido");
                String rol = validarEnum(cmd.getParam(3), ROLES_VALIDOS, "Rol");
                String email = validarEmail(cmd.getParam(4));
                String telefono = cmd.getParam(5);
                int id = usuarioDAO.insertar(ci, nombre, apellido, rol, email, telefono);
                return ok("INSUSU", FormateadorHTML.exito("CU1 · Usuario Registrado",
                        "Usuario '" + nombre + " " + apellido + "' (" + rol + ") insertado correctamente con ID #" + id + "."),
                        "Usuario creado ID=" + id);
            }
            case "MODUSU": {
                validarCantidad(cmd, 7, "MODUSU[\"id\",\"nombre\",\"apellido\",\"rol\",\"email\",\"telefono\",\"estado\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de usuario");
                String nombre = noVacio(cmd.getParam(1), "Nombre");
                String apellido = noVacio(cmd.getParam(2), "Apellido");
                String rol = validarEnum(cmd.getParam(3), ROLES_VALIDOS, "Rol");
                String email = validarEmail(cmd.getParam(4));
                String telefono = cmd.getParam(5);
                String estado = validarEnum(cmd.getParam(6), ESTADOS_USR, "Estado");
                boolean ok = usuarioDAO.actualizar(id, nombre, apellido, rol, email, telefono, estado);
                if (!ok) throw new IllegalArgumentException("No existe un usuario con ID #" + id + ".");
                return ok("MODUSU", FormateadorHTML.exito("CU1 · Usuario Actualizado",
                        "Registro de usuario ID #" + id + " actualizado correctamente."), "Usuario actualizado ID=" + id);
            }
            case "DELUSU": {
                validarCantidad(cmd, 1, "DELUSU[\"id\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de usuario");
                boolean ok = usuarioDAO.eliminar(id);
                if (!ok) throw new IllegalArgumentException("No se encontró el usuario con ID #" + id + ".");
                return ok("DELUSU", FormateadorHTML.exito("CU1 · Usuario Eliminado",
                        "Usuario ID #" + id + " eliminado correctamente."), "Usuario eliminado ID=" + id);
            }

            // ===================== CU1: PARÁMETROS =====================
            case "LISPAR": {
                validarCantidad(cmd, 1, "LISPAR[\"*\"]");
                List<String[]> filas = parametroDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU1 · Parámetros del Sistema",
                        "Umbrales de riesgo y configuración general DEDTE",
                        new String[]{"ID", "Clave", "Valor", "Descripción", "Actualizado"}, filas);
                return ok("LISPAR", html, "Listados " + filas.size() + " parámetros");
            }
            case "INSPAR": {
                validarCantidad(cmd, 3, "INSPAR[\"clave\",\"valor\",\"descripcion\"]");
                int id = parametroDAO.insertar(noVacio(cmd.getParam(0), "Clave"),
                        noVacio(cmd.getParam(1), "Valor"), noVacio(cmd.getParam(2), "Descripción"));
                return ok("INSPAR", FormateadorHTML.exito("CU1 · Parámetro Creado",
                        "Parámetro '" + cmd.getParam(0).toUpperCase() + "' registrado con ID #" + id + "."), "Param creado ID=" + id);
            }
            case "MODPAR": {
                validarCantidad(cmd, 3, "MODPAR[\"clave\",\"valor\",\"descripcion\"]");
                boolean ok = parametroDAO.actualizar(noVacio(cmd.getParam(0), "Clave"),
                        noVacio(cmd.getParam(1), "Valor"), noVacio(cmd.getParam(2), "Descripción"));
                if (!ok) throw new IllegalArgumentException("No existe el parámetro con clave '" + cmd.getParam(0) + "'.");
                return ok("MODPAR", FormateadorHTML.exito("CU1 · Parámetro Actualizado",
                        "Parámetro '" + cmd.getParam(0).toUpperCase() + "' actualizado a '" + cmd.getParam(1) + "'."),
                        "Param actualizado " + cmd.getParam(0));
            }

            // ===================== CU2: ENTORNOS MOODLE =====================
            case "LISENT": {
                validarCantidad(cmd, 1, "LISENT[\"*\"] o LISENT[\"VIRTUAL\"]");
                List<String[]> filas = entornoDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU2 · Entornos Moodle Conectados",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "Código", "Nombre", "Tipo", "URL Base", "Versión", "Estado"}, filas);
                return ok("LISENT", html, "Listados " + filas.size() + " entornos");
            }
            case "INSENT": {
                validarCantidad(cmd, 5, "INSENT[\"codigo\",\"nombre\",\"tipo\",\"url_base\",\"version\"]");
                String tipo = validarEnum(cmd.getParam(2), TIPOS_ENTORNO, "Tipo de entorno");
                int id = entornoDAO.insertar(noVacio(cmd.getParam(0), "Código"),
                        noVacio(cmd.getParam(1), "Nombre"), tipo,
                        noVacio(cmd.getParam(3), "URL base"), noVacio(cmd.getParam(4), "Versión"));
                return ok("INSENT", FormateadorHTML.exito("CU2 · Entorno Moodle Registrado",
                        "Entorno '" + cmd.getParam(1) + "' (" + tipo + ") creado con ID #" + id + "."), "Entorno creado ID=" + id);
            }
            case "MODENT": {
                validarCantidad(cmd, 6, "MODENT[\"id\",\"nombre\",\"tipo\",\"url_base\",\"version\",\"estado\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de entorno");
                String tipo = validarEnum(cmd.getParam(2), TIPOS_ENTORNO, "Tipo de entorno");
                String estado = validarEnum(cmd.getParam(5), ESTADOS_ENTORNO, "Estado de conexión");
                boolean ok = entornoDAO.actualizar(id, noVacio(cmd.getParam(1), "Nombre"), tipo,
                        noVacio(cmd.getParam(3), "URL base"), noVacio(cmd.getParam(4), "Versión"), estado);
                if (!ok) throw new IllegalArgumentException("No existe un entorno Moodle con ID #" + id + ".");
                return ok("MODENT", FormateadorHTML.exito("CU2 · Entorno Moodle Actualizado",
                        "Entorno ID #" + id + " actualizado al estado " + estado + "."), "Entorno actualizado ID=" + id);
            }
            case "DELENT": {
                validarCantidad(cmd, 1, "DELENT[\"id\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de entorno");
                boolean ok = entornoDAO.eliminar(id);
                if (!ok) throw new IllegalArgumentException("No existe un entorno Moodle con ID #" + id + ".");
                return ok("DELENT", FormateadorHTML.exito("CU2 · Entorno Eliminado",
                        "Entorno Moodle ID #" + id + " eliminado correctamente."), "Entorno eliminado ID=" + id);
            }

            // ===================== CU3: APERTURA E INFRAESTRUCTURA =====================
            case "LISAPE": {
                validarCantidad(cmd, 1, "LISAPE[\"*\"] o LISAPE[\"FICCT\"] o LISAPE[\"2026-2\"]");
                List<String[]> filas = aulaDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU3 · Reporte de Apertura e Infraestructura de Aulas",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "Entorno", "Gestión", "Facultad", "Curso", "Materia", "Docente", "Formato", "Estado"}, filas);
                return ok("LISAPE", html, "Listadas " + filas.size() + " aulas");
            }
            case "INSAPE": {
                validarCantidad(cmd, 11, "INSAPE[\"entorno_id\",\"gestion\",\"facultad\",\"carrera\",\"sigla\",\"grupo\",\"materia\",\"doc_cod\",\"doc_nombre\",\"formato\",\"estado\"]");
                int entornoId = enteroPositivo(cmd.getParam(0), "entorno_id");
                String estado = validarEnum(cmd.getParam(10), ESTADOS_APERTURA, "Estado de apertura");
                int id = aulaDAO.insertar(entornoId, noVacio(cmd.getParam(1), "Gestión"),
                        noVacio(cmd.getParam(2), "Facultad"), noVacio(cmd.getParam(3), "Carrera"),
                        noVacio(cmd.getParam(4), "Sigla"), noVacio(cmd.getParam(5), "Grupo"),
                        noVacio(cmd.getParam(6), "Materia"), noVacio(cmd.getParam(7), "Código docente"),
                        noVacio(cmd.getParam(8), "Nombre docente"), noVacio(cmd.getParam(9), "Formato curso"), estado);
                return ok("INSAPE", FormateadorHTML.exito("CU3 · Aula Aperturada Registrada",
                        "Aula " + cmd.getParam(4).toUpperCase() + "-" + cmd.getParam(5).toUpperCase()
                                + " registrada correctamente con ID #" + id + "."), "Aula creada ID=" + id);
            }
            case "MODAPE": {
                validarCantidad(cmd, 4, "MODAPE[\"id\",\"formato\",\"estado\",\"docente_nombre\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de aula");
                String estado = validarEnum(cmd.getParam(2), ESTADOS_APERTURA, "Estado de apertura");
                boolean ok = aulaDAO.actualizarEstado(id, noVacio(cmd.getParam(1), "Formato"), estado,
                        noVacio(cmd.getParam(3), "Nombre docente"));
                if (!ok) throw new IllegalArgumentException("No existe un aula con ID #" + id + ".");
                return ok("MODAPE", FormateadorHTML.exito("CU3 · Aula Actualizada",
                        "Aula ID #" + id + " actualizada a estado " + estado + "."), "Aula actualizada ID=" + id);
            }
            case "DELAPE": {
                validarCantidad(cmd, 1, "DELAPE[\"id\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de aula");
                boolean ok = aulaDAO.eliminar(id);
                if (!ok) throw new IllegalArgumentException("No existe un aula con ID #" + id + ".");
                return ok("DELAPE", FormateadorHTML.exito("CU3 · Aula Eliminada",
                        "Aula ID #" + id + " eliminada correctamente."), "Aula eliminada ID=" + id);
            }

            // ===================== CU4: MONITOREO ACADÉMICO =====================
            case "LISMON": {
                validarCantidad(cmd, 1, "LISMON[\"*\"] o LISMON[\"FICCT\"]");
                List<String[]> filas = monitoreoDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU4 · Monitoreo de Matrícula, Densidad y Rendimiento",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "Curso", "Facultad", "Inscritos", "Activos", "Aprob.", "Reprob.", "Promedio", "Densidad", "Corte"}, filas);
                return ok("LISMON", html, "Listados " + filas.size() + " monitoreos");
            }
            case "INSMON": {
                validarCantidad(cmd, 6, "INSMON[\"aula_id\",\"inscritos\",\"activos\",\"aprobados\",\"reprobados\",\"promedio\"]");
                int aulaId = enteroPositivo(cmd.getParam(0), "aula_id");
                int inscritos = enteroNoNegativo(cmd.getParam(1), "inscritos");
                int activos = enteroNoNegativo(cmd.getParam(2), "activos");
                int aprobados = enteroNoNegativo(cmd.getParam(3), "aprobados");
                int reprobados = enteroNoNegativo(cmd.getParam(4), "reprobados");
                double promedio = decimalRango(cmd.getParam(5), 0.0, 100.0, "promedio");
                if (activos > inscritos) {
                    throw new IllegalArgumentException("Los estudiantes activos (" + activos + ") no pueden superar a los inscritos (" + inscritos + ").");
                }
                double capacidad = parametroDAO.obtenerDecimal("CAPACIDAD_ESTANDAR_AULA", 50.0);
                double densidad = capacidad > 0 ? Math.round((inscritos * 10000.0) / capacidad) / 100.0 : 0.0;
                int id = monitoreoDAO.insertar(aulaId, inscritos, activos, aprobados, reprobados, promedio, densidad);
                return ok("INSMON", FormateadorHTML.exito("CU4 · Monitoreo Registrado",
                        "Monitoreo del aula #" + aulaId + " guardado con ID #" + id
                                + " (Densidad calculada: " + String.format("%.1f%%", densidad) + ")."),
                        "Monitoreo creado ID=" + id);
            }
            case "MODMON": {
                validarCantidad(cmd, 6, "MODMON[\"id\",\"inscritos\",\"activos\",\"aprobados\",\"reprobados\",\"promedio\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de monitoreo");
                int inscritos = enteroNoNegativo(cmd.getParam(1), "inscritos");
                int activos = enteroNoNegativo(cmd.getParam(2), "activos");
                int aprobados = enteroNoNegativo(cmd.getParam(3), "aprobados");
                int reprobados = enteroNoNegativo(cmd.getParam(4), "reprobados");
                double promedio = decimalRango(cmd.getParam(5), 0.0, 100.0, "promedio");
                double capacidad = parametroDAO.obtenerDecimal("CAPACIDAD_ESTANDAR_AULA", 50.0);
                double densidad = capacidad > 0 ? Math.round((inscritos * 10000.0) / capacidad) / 100.0 : 0.0;
                boolean ok = monitoreoDAO.actualizar(id, inscritos, activos, aprobados, reprobados, promedio, densidad);
                if (!ok) throw new IllegalArgumentException("No existe un registro de monitoreo con ID #" + id + ".");
                return ok("MODMON", FormateadorHTML.exito("CU4 · Monitoreo Actualizado",
                        "Monitoreo ID #" + id + " actualizado correctamente."), "Monitoreo actualizado ID=" + id);
            }
            case "DELMON": {
                validarCantidad(cmd, 1, "DELMON[\"id\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de monitoreo");
                boolean ok = monitoreoDAO.eliminar(id);
                if (!ok) throw new IllegalArgumentException("No existe un monitoreo con ID #" + id + ".");
                return ok("DELMON", FormateadorHTML.exito("CU4 · Monitoreo Eliminado",
                        "Registro de monitoreo ID #" + id + " eliminado."), "Monitoreo eliminado ID=" + id);
            }

            // ===================== CU5: DESERCIÓN Y SEMÁFOROS =====================
            case "LISRIE": {
                validarCantidad(cmd, 1, "LISRIE[\"*\"] o LISRIE[\"ROJO\"] o LISRIE[\"FICCT\"]");
                List<String[]> filas = riesgoDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU5 · Evaluación de Deserción y Semáforos de Riesgo",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "Curso", "Facultad", "Inscritos", "Retiros", "En Mora", "Deserción", "Semáforo", "Observación"}, filas);
                return ok("LISRIE", html, "Listados " + filas.size() + " riesgos");
            }
            case "INSRIE": {
                validarCantidad(cmd, 5, "INSRIE[\"aula_id\",\"total_inscritos\",\"retiros\",\"en_mora\",\"observacion\"]");
                int aulaId = enteroPositivo(cmd.getParam(0), "aula_id");
                int total = enteroPositivo(cmd.getParam(1), "total_inscritos");
                int retiros = enteroNoNegativo(cmd.getParam(2), "retiros");
                int mora = enteroNoNegativo(cmd.getParam(3), "en_mora");
                if (retiros > total) {
                    throw new IllegalArgumentException("Los retiros (" + retiros + ") no pueden superar el total de inscritos (" + total + ").");
                }
                double tasa = Math.round((retiros * 10000.0) / total) / 100.0;
                String semaforo = calcularSemaforo(tasa);
                int id = riesgoDAO.insertar(aulaId, total, retiros, mora, tasa, semaforo, cmd.getParam(4));
                return ok("INSRIE", FormateadorHTML.exito("CU5 · Riesgo Evaluado Automáticamente",
                        "Evaluación ID #" + id + " registrada para Aula #" + aulaId
                                + " | Tasa de deserción: " + String.format("%.2f%%", tasa)
                                + " | Semáforo asignado: " + semaforo + "."),
                        "Riesgo creado ID=" + id + " SEM=" + semaforo);
            }
            case "MODRIE": {
                validarCantidad(cmd, 5, "MODRIE[\"id\",\"total_inscritos\",\"retiros\",\"en_mora\",\"observacion\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de evaluación");
                int total = enteroPositivo(cmd.getParam(1), "total_inscritos");
                int retiros = enteroNoNegativo(cmd.getParam(2), "retiros");
                int mora = enteroNoNegativo(cmd.getParam(3), "en_mora");
                double tasa = Math.round((retiros * 10000.0) / total) / 100.0;
                String semaforo = calcularSemaforo(tasa);
                boolean ok = riesgoDAO.actualizar(id, total, retiros, mora, tasa, semaforo, cmd.getParam(4));
                if (!ok) throw new IllegalArgumentException("No existe una evaluación de riesgo con ID #" + id + ".");
                return ok("MODRIE", FormateadorHTML.exito("CU5 · Evaluación Actualizada",
                        "Evaluación ID #" + id + " actualizada | Nueva tasa: " + String.format("%.2f%%", tasa)
                                + " | Semáforo: " + semaforo + "."), "Riesgo actualizado ID=" + id);
            }
            case "DELRIE": {
                validarCantidad(cmd, 1, "DELRIE[\"id\"]");
                int id = enteroPositivo(cmd.getParam(0), "ID de evaluación");
                boolean ok = riesgoDAO.eliminar(id);
                if (!ok) throw new IllegalArgumentException("No existe una evaluación de riesgo con ID #" + id + ".");
                return ok("DELRIE", FormateadorHTML.exito("CU5 · Evaluación Eliminada",
                        "Evaluación de riesgo ID #" + id + " eliminada."), "Riesgo eliminado ID=" + id);
            }

            // ===================== CU6: DASHBOARD DIRECTIVO =====================
            case "DASHBOARD": {
                validarCantidad(cmd, 1, "DASHBOARD[\"*\"] o DASHBOARD[\"2026-2\"]");
                List<String[]> filas = reporteDAO.dashboardDirectivo(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU6 · Dashboard Directivo en Tiempo Real (DEDTE-UAGRM)",
                        "Alcance de gestión: " + cmd.getParam(0),
                        new String[]{"Indicador Clave (KPI)", "Valor Actual"}, filas);
                return ok("DASHBOARD", html, "Dashboard generado");
            }

            // ===================== CU7: HISTÓRICOS SEMESTRALES =====================
            case "LISHIS": {
                validarCantidad(cmd, 1, "LISHIS[\"*\"] o LISHIS[\"2026-1\"] o LISHIS[\"FICCT\"]");
                List<String[]> filas = historicoDAO.listar(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU7 · Consulta de Históricos Semestrales",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"ID", "Gestión", "Entorno", "Facultad", "Aulas", "Inscritos", "Aprobación", "Deserción", "Promedio"}, filas);
                return ok("LISHIS", html, "Listados " + filas.size() + " históricos");
            }
            case "INSHIS": {
                validarCantidad(cmd, 8, "INSHIS[\"gestion\",\"entorno_id\",\"facultad\",\"aulas\",\"inscritos\",\"aprob_pct\",\"deserc_pct\",\"promedio\"]");
                int id = historicoDAO.insertar(
                        noVacio(cmd.getParam(0), "Gestión"),
                        enteroPositivo(cmd.getParam(1), "entorno_id"),
                        noVacio(cmd.getParam(2), "Facultad"),
                        enteroNoNegativo(cmd.getParam(3), "total_aulas"),
                        enteroNoNegativo(cmd.getParam(4), "total_inscritos"),
                        decimalRango(cmd.getParam(5), 0, 100, "tasa_aprobacion"),
                        decimalRango(cmd.getParam(6), 0, 100, "tasa_desercion"),
                        decimalRango(cmd.getParam(7), 0, 100, "promedio_general"));
                return ok("INSHIS", FormateadorHTML.exito("CU7 · Histórico Semestral Registrado",
                        "Registro histórico creado con ID #" + id + "."), "Historico creado ID=" + id);
            }
            case "COMPHIS": {
                validarCantidad(cmd, 2, "COMPHIS[\"2025-2\",\"2026-2\"]");
                List<String[]> filas = historicoDAO.compararGestiones(cmd.getParam(0), cmd.getParam(1));
                String html = FormateadorHTML.tabla("CU7 · Comparativa entre Gestiones Semestrales",
                        "Comparando gestión " + cmd.getParam(0) + " vs " + cmd.getParam(1),
                        new String[]{"Gestión", "Entorno", "Facultad", "Aulas", "Inscritos", "Aprobación", "Deserción", "Promedio"}, filas);
                return ok("COMPHIS", html, "Comparativa generada");
            }

            // ===================== CU8: REPORTES Y ESTADÍSTICAS =====================
            case "REPFAC": {
                validarCantidad(cmd, 1, "REPFAC[\"*\"] o REPFAC[\"FICCT\"]");
                List<String[]> filas = reporteDAO.reportePorFacultad(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU8 · Reporte Estadístico por Facultad",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"Facultad", "Aulas", "Inscritos", "Promedio", "Deserción Media", "Aulas en ROJO"}, filas);
                return ok("REPFAC", html, "Reporte facultad generado");
            }
            case "REPENT": {
                validarCantidad(cmd, 1, "REPENT[\"*\"] o REPENT[\"VIRTUAL\"]");
                List<String[]> filas = reporteDAO.reportePorEntorno(cmd.getParam(0));
                String html = FormateadorHTML.tabla("CU8 · Reporte Estadístico por Entorno Moodle",
                        "Filtro aplicado: " + cmd.getParam(0),
                        new String[]{"Código", "Nombre Entorno", "Tipo", "Estado", "Aulas", "Inscritos", "Promedio"}, filas);
                return ok("REPENT", html, "Reporte entorno generado");
            }

            default:
                throw new IllegalArgumentException("El comando '" + c + "' no existe. Envíe HELP[\"*\"] para ver la lista completa de comandos.");
        }
    }

    private String calcularSemaforo(double tasaDesercion) {
        double umbralRojo = parametroDAO.obtenerDecimal("UMBRAL_RIESGO_ROJO", 30.0);
        double umbralAmarillo = parametroDAO.obtenerDecimal("UMBRAL_RIESGO_AMARILLO", 15.0);
        if (tasaDesercion >= umbralRojo) {
            return "ROJO";
        }
        if (tasaDesercion >= umbralAmarillo) {
            return "AMARILLO";
        }
        return "VERDE";
    }

    private Resultado responderHelp() {
        List<String[]> comandos = List.of(
                new String[]{"CU1", "LISUSU[\"*\"]", "Lista usuarios (o filtra por rol/CI/nombre)"},
                new String[]{"CU1", "INSUSU[\"ci\",\"nombre\",\"apellido\",\"rol\",\"email\",\"tel\"]", "Registra usuario (ADMIN|DIRECTIVO|ANALISTA|DOCENTE)"},
                new String[]{"CU1", "MODUSU[\"id\",\"nombre\",\"apellido\",\"rol\",\"email\",\"tel\",\"estado\"]", "Actualiza usuario (ACTIVO|INACTIVO)"},
                new String[]{"CU1", "DELUSU[\"id\"]", "Elimina usuario por ID"},
                new String[]{"CU1", "LISPAR[\"*\"] / INSPAR[...] / MODPAR[\"clave\",\"valor\",\"desc\"]", "Consulta o configura umbrales del sistema"},
                new String[]{"CU2", "LISENT[\"*\"] / INSENT[\"cod\",\"nom\",\"tipo\",\"url\",\"ver\"]", "Gestiona entornos Moodle (VIRTUAL|PRESENCIAL|CAPACITACION)"},
                new String[]{"CU2", "MODENT[\"id\",\"nom\",\"tipo\",\"url\",\"ver\",\"estado\"] / DELENT[\"id\"]", "Actualiza o elimina entorno Moodle"},
                new String[]{"CU3", "LISAPE[\"*\"] / INSAPE[\"ent_id\",\"gest\",\"fac\",\"carr\",\"sig\",\"grp\",\"mat\",\"dcod\",\"dnom\",\"fmt\",\"est\"]", "Reporta y registra apertura de aulas"},
                new String[]{"CU3", "MODAPE[\"id\",\"fmt\",\"est\",\"dnom\"] / DELAPE[\"id\"]", "Modifica o elimina registro de apertura"},
                new String[]{"CU4", "LISMON[\"*\"] / INSMON[\"aula_id\",\"insc\",\"act\",\"aprob\",\"reprob\",\"prom\"]", "Monitorea matrícula, densidad y rendimiento"},
                new String[]{"CU4", "MODMON[\"id\",\"insc\",\"act\",\"aprob\",\"reprob\",\"prom\"] / DELMON[\"id\"]", "Actualiza o elimina monitoreo académico"},
                new String[]{"CU5", "LISRIE[\"*\"] / INSRIE[\"aula_id\",\"insc\",\"retiros\",\"mora\",\"obs\"]", "Evalúa deserción y asigna semáforo automático"},
                new String[]{"CU5", "MODRIE[\"id\",\"insc\",\"retiros\",\"mora\",\"obs\"] / DELRIE[\"id\"]", "Recalcula riesgo y semáforo"},
                new String[]{"CU6", "DASHBOARD[\"*\"] o DASHBOARD[\"2026-2\"]", "KPIs directivos consolidados en tiempo real"},
                new String[]{"CU7", "LISHIS[\"*\"] / INSHIS[...] / COMPHIS[\"2025-2\",\"2026-2\"]", "Consulta y compara históricos semestrales"},
                new String[]{"CU8", "REPFAC[\"*\"] / REPENT[\"*\"]", "Reportes estadísticos por facultad y por entorno Moodle"}
        );

        String html = FormateadorHTML.tabla(
                "Guía Oficial de Comandos · DEDTE Mail System (Grupo #11 SA)",
                "Envíe cualquiera de los siguientes comandos en el Asunto (Subject) del correo respetando las comillas dobles:",
                new String[]{"CU", "Sintaxis del Comando", "Descripción"},
                comandos
        );
        return ok("HELP", html, "Guía HELP enviada");
    }

    // ===================== HELPERS DE VALIDACIÓN =====================

    private Resultado ok(String cmd, String html, String log) {
        return new Resultado(true, "OK [" + cmd + "] - DEDTE Analytics UAGRM", html, log);
    }

    private void validarCantidad(Comando cmd, int esperada, String ejemplo) {
        if (cmd.cantidadParametros() != esperada) {
            throw new IllegalArgumentException("Número de parámetros incorrecto para " + cmd.getNombre()
                    + ": se esperaban " + esperada + " y se recibieron " + cmd.cantidadParametros()
                    + ". Uso: " + ejemplo);
        }
    }

    private String noVacio(String val, String campo) {
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio y no puede estar vacío.");
        }
        return val.trim();
    }

    private String validarEnum(String val, Set<String> permitidos, String campo) {
        String up = noVacio(val, campo).toUpperCase();
        if (!permitidos.contains(up)) {
            throw new IllegalArgumentException("Valor inválido para '" + campo + "': '" + val
                    + "'. Valores permitidos: " + String.join(", ", permitidos) + ".");
        }
        return up;
    }

    private String validarEmail(String val) {
        String v = noVacio(val, "Email");
        if (!EMAIL_REGEX.matcher(v).matches()) {
            throw new IllegalArgumentException("El correo electrónico '" + v + "' no tiene un formato válido.");
        }
        return v;
    }

    private int enteroPositivo(String val, String campo) {
        try {
            int n = Integer.parseInt(noVacio(val, campo));
            if (n <= 0) throw new IllegalArgumentException("El campo '" + campo + "' debe ser un entero mayor a 0.");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número entero válido (recibido: '" + val + "').");
        }
    }

    private int enteroNoNegativo(String val, String campo) {
        try {
            int n = Integer.parseInt(noVacio(val, campo));
            if (n < 0) throw new IllegalArgumentException("El campo '" + campo + "' no puede ser negativo.");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser un número entero válido (recibido: '" + val + "').");
        }
    }

    private double decimalRango(String val, double min, double max, String campo) {
        try {
            double d = Double.parseDouble(noVacio(val, campo).replace(',', '.'));
            if (d < min || d > max) {
                throw new IllegalArgumentException("El campo '" + campo + "' debe estar entre " + min + " y " + max + ".");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser numérico (recibido: '" + val + "').");
        }
    }

    private String traducirErrorSql(SQLException e) {
        String state = e.getSQLState();
        if ("23505".equals(state)) {
            return "Registro duplicado: ya existe un elemento con la misma clave única en la base de datos.";
        }
        if ("23503".equals(state)) {
            return "Violación de clave foránea: el ID referenciado no existe o está en uso por otros registros.";
        }
        if ("23514".equals(state)) {
            return "Violación de restricción de integridad (CHECK) en la base de datos.";
        }
        return "Error de base de datos [" + state + "]: " + e.getMessage();
    }
}
