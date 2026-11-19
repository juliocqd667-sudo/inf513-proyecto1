package bo.tecnoweb.dedte.correo;

import java.util.List;

/**
 * Genera respuestas HTML limpias y legibles con identidad institucional UAGRM - DEDTE.
 */
public final class FormateadorHTML {

    private FormateadorHTML() {}

    public static String envolver(String titulo, String contenidoHtml, boolean esError) {
        String colorCabecera = esError ? "#991B1B" : "#1E3A8A";
        String badge = esError ? "ERROR DE PROCESAMIENTO" : "OPERACIÓN PROCESADA";

        return "<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,Helvetica,sans-serif;background:#F3F4F6;margin:0;padding:20px;color:#111827;'>"
                + "<div style='max-width:920px;margin:0 auto;background:#FFFFFF;border-radius:10px;overflow:hidden;border:1px solid #E5E7EB;'>"
                + "<div style='background:" + colorCabecera + ";color:#FFFFFF;padding:18px 24px;'>"
                + "<div style='font-size:11px;letter-spacing:1px;opacity:0.85;'>UAGRM · FICCT · INF513 · GRUPO #11 SA · DEDTE ANALYTICS</div>"
                + "<h2 style='margin:6px 0 0 0;font-size:20px;'>" + escapar(titulo) + "</h2>"
                + "<span style='display:inline-block;margin-top:8px;padding:3px 10px;font-size:11px;border-radius:12px;background:rgba(255,255,255,0.18);'>" + badge + "</span>"
                + "</div>"
                + "<div style='padding:22px 24px;font-size:14px;line-height:1.55;'>"
                + contenidoHtml
                + "</div>"
                + "<div style='background:#F9FAFB;border-top:1px solid #E5E7EB;padding:12px 24px;font-size:12px;color:#6B7280;'>"
                + "Sistema Email-Driven DEDTE-UAGRM · Envíe <code>HELP[\"*\"]</code> en el asunto para consultar la guía de comandos."
                + "</div></div></body></html>";
    }

    public static String exito(String titulo, String mensaje) {
        String body = "<div style='background:#ECFDF5;border-left:4px solid #059669;padding:14px 16px;border-radius:4px;color:#065F46;'>"
                + "<strong>Éxito:</strong> " + escapar(mensaje)
                + "</div>";
        return envolver(titulo, body, false);
    }

    public static String error(String titulo, String detalle) {
        String body = "<div style='background:#FEF2F2;border-left:4px solid #DC2626;padding:14px 16px;border-radius:4px;color:#991B1B;'>"
                + "<strong>Error:</strong> " + escapar(detalle)
                + "</div>"
                + "<p style='margin-top:14px;color:#4B5563;'>Verifique la sintaxis <code>COMANDO[\"param1\",\"param2\"]</code> o envíe <code>HELP[\"*\"]</code>.</p>";
        return envolver(titulo, body, true);
    }

    public static String tabla(String titulo, String subtitulo, String[] columnas, List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        if (subtitulo != null && !subtitulo.isBlank()) {
            sb.append("<p style='margin-top:0;color:#374151;'>").append(escapar(subtitulo)).append("</p>");
        }

        if (filas == null || filas.isEmpty()) {
            sb.append("<div style='background:#FFFBEB;border-left:4px solid #D97706;padding:12px 16px;color:#92400E;'>")
              .append("No se encontraron registros para el criterio solicitado.")
              .append("</div>");
            return envolver(titulo, sb.toString(), false);
        }

        sb.append("<div style='overflow-x:auto;'>")
          .append("<table style='width:100%;border-collapse:collapse;font-size:13px;'>")
          .append("<thead><tr style='background:#1E3A8A;color:#FFFFFF;'>");
        for (String col : columnas) {
            sb.append("<th style='padding:9px 10px;text-align:left;border:1px solid #CBD5E1;'>")
              .append(escapar(col))
              .append("</th>");
        }
        sb.append("</tr></thead><tbody>");

        int idx = 0;
        for (String[] fila : filas) {
            String bg = (idx++ % 2 == 0) ? "#FFFFFF" : "#F8FAFC";
            sb.append("<tr style='background:").append(bg).append(";'>");
            for (String celda : fila) {
                sb.append("<td style='padding:8px 10px;border:1px solid #E2E8F0;'>")
                  .append(formatearCelda(celda))
                  .append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table></div>");
        sb.append("<p style='margin-top:12px;font-size:12px;color:#6B7280;'>Total de registros: <strong>")
          .append(filas.size()).append("</strong></p>");

        return envolver(titulo, sb.toString(), false);
    }

    private static String formatearCelda(String val) {
        if (val == null) return "-";
        if ("VERDE".equals(val)) {
            return "<span style='background:#DCFCE7;color:#166534;padding:2px 8px;border-radius:10px;font-weight:bold;'>VERDE</span>";
        }
        if ("AMARILLO".equals(val)) {
            return "<span style='background:#FEF9C3;color:#854D0E;padding:2px 8px;border-radius:10px;font-weight:bold;'>AMARILLO</span>";
        }
        if ("ROJO".equals(val)) {
            return "<span style='background:#FEE2E2;color:#991B1B;padding:2px 8px;border-radius:10px;font-weight:bold;'>ROJO</span>";
        }
        return escapar(val);
    }

    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
