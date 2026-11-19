package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU2: Acceso a datos para entornos_moodle (Virtual, Presencial, Capacitación).
 */
public class EntornoMoodleDAO {

    public int insertar(String codigo, String nombre, String tipo, String urlBase, String version) throws SQLException {
        String sql = "INSERT INTO entornos_moodle (codigo, nombre, tipo, url_base, version_moodle, estado_conexion) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVO') RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo.toUpperCase());
            ps.setString(2, nombre);
            ps.setString(3, tipo.toUpperCase());
            ps.setString(4, urlBase);
            ps.setString(5, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizar(int id, String nombre, String tipo, String urlBase, String version, String estado) throws SQLException {
        String sql = "UPDATE entornos_moodle SET nombre = ?, tipo = ?, url_base = ?, version_moodle = ?, "
                + "estado_conexion = ?, ultima_sinc = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, tipo.toUpperCase());
            ps.setString(3, urlBase);
            ps.setString(4, version);
            ps.setString(5, estado.toUpperCase());
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM entornos_moodle WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String sql = todos
                ? "SELECT id, codigo, nombre, tipo, url_base, version_moodle, estado_conexion FROM entornos_moodle ORDER BY id ASC"
                : "SELECT id, codigo, nombre, tipo, url_base, version_moodle, estado_conexion FROM entornos_moodle "
                  + "WHERE UPPER(tipo) = UPPER(?) OR UPPER(codigo) = UPPER(?) OR UPPER(estado_conexion) = UPPER(?) ORDER BY id ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, filtro);
                ps.setString(2, filtro);
                ps.setString(3, filtro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getString("url_base"),
                            rs.getString("version_moodle"),
                            rs.getString("estado_conexion")
                    });
                }
            }
        }
        return filas;
    }
}
