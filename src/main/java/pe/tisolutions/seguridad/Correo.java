package pe.tisolutions.seguridad;

import java.util.Locale;

/** Los correos se guardan y comparan siempre en minúsculas y sin espacios. */
public final class Correo {

    private Correo() {
    }

    public static String normalizar(String email) {
        if (email == null) return "";
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.length() > 120 ? e.substring(0, 120) : e;
    }
}
