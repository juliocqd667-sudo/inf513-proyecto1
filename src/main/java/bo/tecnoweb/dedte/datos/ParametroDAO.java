package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU1: Acceso a datos para parametros_sistema (umbrales de semáforo, gestión actual, etc.).
 */
public class ParametroDAO {

    public int insertar(String clave, String valor, String descripcion) throws SQLException {
        String sql = "INSERT INTO parametros_sistema (clave, valor, descripcion) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave.toUpperCase());
            ps.setString(2, valor);
            ps.setString(3, descripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizar(String clave, String valor, String descripcion) throws SQLException {
        String sql = "UPDATE parametros_sistema SET valor = ?, descripcion = ?, updated_at = CURRENT_TIMESTAMP WHERE UPPER(clave) = UPPER(?)";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, descripcion);
            ps.setString(3, clave);
            return ps.executeUpdate() > 0;
        }
    }

    public double obtenerDecimal(String clave, double porDefecto) {
        String sql = "SELECT valor FROM parametros_sistema WHERE UPPER(clave) = UPPER(?)";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Double.parseDouble(rs.getString("valor"));
                }
            }
        } catch (Exception ignored) {
            // Usa valor por defecto si no existe o no es numérico
        }
        return porDefecto;
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String sql = todos
                ? "SELECT id, clave, valor, descripcion, updated_at FROM parametros_sistema ORDER BY id ASC"
                : "SELECT id, clave, valor, descripcion, updated_at FROM parametros_sistema WHERE UPPER(clave) LIKE UPPER(?) ORDER BY id ASC";

        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!todos) {
                ps.setString(1, "%" + filtro + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("clave"),
                            rs.getString("valor"),
                            rs.getString("descripcion"),
                            String.valueOf(rs.getTimestamp("updated_at"))
                    });
                }
            }
        }
        return filas;
    }
}
