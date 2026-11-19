package bo.tecnoweb.dedte.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CU5: Acceso a datos para evaluacion_riesgo (Deserción y Semáforos de Riesgo).
 */
public class EvaluacionRiesgoDAO {

    public int insertar(int aulaId, int totalInscritos, int retiros, int enMora,
                        double tasaDesercion, String semaforo, String observacion) throws SQLException {
        String sql = "INSERT INTO evaluacion_riesgo "
                + "(aula_id, total_inscritos, retiros, en_mora, tasa_desercion, semaforo, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, aulaId);
            ps.setInt(2, totalInscritos);
            ps.setInt(3, retiros);
            ps.setInt(4, enMora);
            ps.setDouble(5, tasaDesercion);
            ps.setString(6, semaforo);
            ps.setString(7, observacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean actualizar(int id, int totalInscritos, int retiros, int enMora,
                              double tasaDesercion, String semaforo, String observacion) throws SQLException {
        String sql = "UPDATE evaluacion_riesgo SET total_inscritos = ?, retiros = ?, en_mora = ?, "
                + "tasa_desercion = ?, semaforo = ?, observacion = ?, fecha_evaluacion = CURRENT_DATE WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalInscritos);
            ps.setInt(2, retiros);
            ps.setInt(3, enMora);
            ps.setDouble(4, tasaDesercion);
            ps.setString(5, semaforo);
            ps.setString(6, observacion);
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM evaluacion_riesgo WHERE id = ?";
        try (Connection conn = ConexionDB.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<String[]> listar(String filtro) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        boolean todos = (filtro == null || filtro.equals("*") || filtro.isBlank());
        String base = "SELECT r.id, a.sigla || '-' || a.grupo AS curso, a.facultad, r.total_inscritos, "
                + "r.retiros, r.en_mora, r.tasa_desercion, r.semaforo, r.observacion "
                + "FROM evaluacion_riesgo r JOIN aulas_apertura a ON a.id = r.aula_id ";
        String sql = todos
                ? base + "ORDER BY r.tasa_desercion DESC, r.id ASC"
                : base + "WHERE UPPER(r.semaforo) = UPPER(?) OR UPPER(a.facultad) = UPPER(?) OR UPPER(a.sigla) = UPPER(?) "
                  + "ORDER BY r.tasa_desercion DESC, r.id ASC";

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
                            rs.getString("curso"),
                            rs.getString("facultad"),
                            String.valueOf(rs.getInt("total_inscritos")),
                            String.valueOf(rs.getInt("retiros")),
                            String.valueOf(rs.getInt("en_mora")),
                            String.format("%.2f%%", rs.getDouble("tasa_desercion")),
                            rs.getString("semaforo"),
                            rs.getString("observacion")
                    });
                }
            }
        }
        return filas;
    }
}
