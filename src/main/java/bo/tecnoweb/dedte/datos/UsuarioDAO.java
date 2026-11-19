package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU1: Acceso a datos para la tabla usuarios (Prepared Statements).
 */
public class UsuarioDAO {

    public int insertar(String ci, String nombre, String apellido, String rol, String email, String telefono) throws SQLException {
        String sql = "INSERT INTO usuarios (ci, nombre, apellido, rol, email, telefono, estado) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO') RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ci);
            ps.setString(2, nombre);
            ps.setString(3, apellido);
            ps.setString(4, rol);
            ps.setString(5, email);
            ps.setString(6, telefono);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizar(int id, String nombre, String apellido, String rol, String email, String telefono, String estado) throws SQLException {
        String sql = "UPDATE usuarios SET nombre = ?, apellido = ?, rol = ?, email = ?, telefono = ?, estado = ? WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, rol);
            ps.setString(4, email);
            ps.setString(5, telefono);
            ps.setString(6, estado);
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM usuarios WHERE id = ?";
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
                ? "SELECT id, ci, nombre, apellido, rol, email, telefono, estado FROM usuarios ORDER BY id ASC"
                : "SELECT id, ci, nombre, apellido, rol, email, telefono, estado FROM usuarios "
                  + "WHERE UPPER(rol) = UPPER(?) OR ci = ? OR LOWER(nombre || ' ' || apellido) LIKE LOWER(?) ORDER BY id ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, filtro);
                ps.setString(2, filtro);
                ps.setString(3, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("ci"),
                            rs.getString("nombre") + " " + rs.getString("apellido"),
                            rs.getString("rol"),
                            rs.getString("email"),
                            rs.getString("telefono"),
                            rs.getString("estado")
                    });
                }
            }
        }
        return filas;
    }
}
