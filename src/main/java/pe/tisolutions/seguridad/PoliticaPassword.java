package pe.tisolutions.seguridad;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;

/** Reglas de contraseñas. Devuelve el problema encontrado o null si la contraseña es aceptable. */
public final class PoliticaPassword {

    /** BCrypt solo usa los primeros 72 bytes; limitamos para que no haya sorpresas. */
    public static final int MAXIMO = 64;
    public static final int MINIMO_CLIENTE = 8;
    public static final int MINIMO_PERSONAL = 10;

    private static final Set<String> COMUNES = Set.of(
            "12345678", "123456789", "1234567890", "password", "password1", "contraseña", "contrasena",
            "qwerty123", "abc12345", "11111111", "00000000", "iloveyou", "admin123", "administrador",
            "tisolutions", "tisolutions1", "peru1234", "lima1234");

    private static final String LETRAS_TEMP = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    private PoliticaPassword() {
    }

    public static String problemaCliente(String password, String email) {
        String comun = problemaComun(password, email, MINIMO_CLIENTE);
        if (comun != null) return comun;
        if (!password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit)) {
            return "La contraseña debe tener letras y números.";
        }
        return null;
    }

    public static String problemaPersonal(String password, String email) {
        String comun = problemaComun(password, email, MINIMO_PERSONAL);
        if (comun != null) return comun;
        if (!password.chars().anyMatch(Character::isUpperCase) || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)) {
            return "La contraseña debe tener mayúsculas, minúsculas y números.";
        }
        return null;
    }

    /** Contraseña temporal aleatoria para el personal (la cambiarán al entrar). */
    public static String generarTemporal() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(LETRAS_TEMP.charAt(AZAR.nextInt(LETRAS_TEMP.length())));
            }
            String p = sb.toString();
            if (problemaPersonal(p, "") == null) return p;
        }
    }

    private static String problemaComun(String password, String email, int minimo) {
        if (password == null || password.length() < minimo) {
            return "La contraseña debe tener al menos " + minimo + " caracteres.";
        }
        if (password.length() > MAXIMO) {
            return "La contraseña no puede tener más de " + MAXIMO + " caracteres.";
        }
        String minus = password.toLowerCase(Locale.ROOT);
        if (COMUNES.contains(minus)) {
            return "Esa contraseña es demasiado común. Elige otra.";
        }
        String usuarioCorreo = email == null ? "" : email.split("@")[0].toLowerCase(Locale.ROOT);
        if (usuarioCorreo.length() >= 4 && minus.contains(usuarioCorreo)) {
            return "La contraseña no debe contener tu correo.";
        }
        return null;
    }
}
