package bo.tecnoweb.dedte;

import bo.tecnoweb.dedte.lexico.AnalizadorLexico;
import bo.tecnoweb.dedte.lexico.Comando;
import org.junit.Test;

import static org.junit.Assert.*;

public class AnalizadorLexicoTest {

    private final AnalizadorLexico lex = new AnalizadorLexico();

    @Test
    public void testComandoSimpleConEspacios() {
        Comando cmd = lex.analizar("   LISUSU [ \"*\" ]   ");
        assertTrue(cmd.esValido());
        assertEquals("LISUSU", cmd.getNombre());
        assertEquals(1, cmd.cantidadParametros());
        assertEquals("*", cmd.getParam(0));
    }

    @Test
    public void testComillasEscapadasYUtf8() {
        Comando cmd = lex.analizar("INSRIE[\"1\",\"58\",\"4\",\"2\",\"Observación con \\\"comillas\\\" y ñandú\"]");
        assertTrue(cmd.esValido());
        assertEquals("INSRIE", cmd.getNombre());
        assertEquals(5, cmd.cantidadParametros());
        assertEquals("Observación con \"comillas\" y ñandú", cmd.getParam(4));
    }

    @Test
    public void testPrefijoReYHelpSinCorchetes() {
        Comando cmd1 = lex.analizar("Re: Fwd: DASHBOARD[\"2026-2\"]");
        assertTrue(cmd1.esValido());
        assertEquals("DASHBOARD", cmd1.getNombre());
        assertEquals("2026-2", cmd1.getParam(0));

        Comando cmd2 = lex.analizar("HELP");
        assertTrue(cmd2.esValido());
        assertEquals("HELP", cmd2.getNombre());
    }

    @Test
    public void testErroresDeSintaxis() {
        assertFalse(lex.analizar("").esValido());
        assertFalse(lex.analizar("LISUSU *").esValido());
        assertFalse(lex.analizar("LISUSU[\"*\"").esValido());
        assertFalse(lex.analizar("INSUSU[\"a\",b]").esValido());
        assertFalse(lex.analizar("LISUSU[\"*\"] basura").esValido());
    }
}
