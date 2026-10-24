package pe.tisolutions.web.pagina;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos usados en las plantillas: ${@fmt.soles(monto)}, ${@fmt.fecha(f)}. */
@Component("fmt")
public class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String soles(BigDecimal monto) {
        if (monto == null) {
            return "—";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "S/ " + df.format(monto);
    }

    public String numero(BigDecimal n) {
        return n == null ? "—" : n.stripTrailingZeros().toPlainString();
    }

    public String fecha(LocalDateTime f) {
        return f == null ? "—" : f.format(FECHA);
    }

    public String fechaHora(LocalDateTime f) {
        return f == null ? "—" : f.format(FECHA_HORA);
    }

    /** Clase CSS del badge según el estado. */
    public String claseEstado(String estado) {
        return "badge badge-" + (estado == null ? "" : estado.toLowerCase().replace('_', '-'));
    }
}
