package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU3: Acceso a datos para aulas_apertura (Reporte de Apertura e Infraestructura).
 */
public class AulaAperturaDAO {

    public int insertar(int entornoId, String gestion, String facultad, String carrera, String sigla,
                        String grupo, String materia, String docenteCodigo, String docenteNombre,
                        String formatoCurso, String estadoApertura) throws SQLException {
        String sql = "INSERT INTO aulas_apertura "
                + "(entorno_id, gestion, facultad, carrera, sigla, grupo, materia, docente_codigo, docente_nombre, formato_curso, estado_apertura) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entornoId);
            ps.setString(2, gestion);
            ps.setString(3, facultad.toUpperCase());
            ps.setString(4, carrera);
            ps.setString(5, sigla.toUpperCase());
            ps.setString(6, grupo.toUpperCase());
            ps.setString(7, materia);
            ps.setString(8, docenteCodigo.toUpperCase());
            ps.setString(9, docenteNombre);
            ps.setString(10, formatoCurso.toUpperCase());
            ps.setString(11, estadoApertura.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizarEstado(int id, String formatoCurso, String estadoApertura, String docenteNombre) throws SQLException {
        String sql = "UPDATE aulas_apertura SET formato_curso = ?, estado_apertura = ?, docente_nombre = ? WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, formatoCurso.toUpperCase());
            ps.setString(2, estadoApertura.toUpperCase());
            ps.setString(3, docenteNombre);
            ps.setInt(4, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM aulas_apertura WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String base = "SELECT a.id, e.codigo AS entorno, a.gestion, a.facultad, a.sigla || '-' || a.grupo AS curso, "
                + "a.materia, a.docente_nombre, a.formato_curso, a.estado_apertura "
                + "FROM aulas_apertura a JOIN entornos_moodle e ON e.id = a.entorno_id ";
        String sql = todos
                ? base + "ORDER BY a.id ASC"
                : base + "WHERE UPPER(a.facultad) = UPPER(?) OR UPPER(a.gestion) = UPPER(?) "
                  + "OR UPPER(a.sigla) = UPPER(?) OR UPPER(a.estado_apertura) = UPPER(?) ORDER BY a.id ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, filtro);
                ps.setString(2, filtro);
                ps.setString(3, filtro);
                ps.setString(4, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("entorno"),
                            rs.getString("gestion"),
                            rs.getString("facultad"),
                            rs.getString("curso"),
                            rs.getString("materia"),
                            rs.getString("docente_nombre"),
                            rs.getString("formato_curso"),
                            rs.getString("estado_apertura")
                    });
                }
            }
        }
        return filas;
    }
}
