package pe.tisolutions.servicio;

/** Validación de DNI y RUC peruanos. */
public final class ValidadorDocumento {

    private static final int[] FACTORES_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private ValidadorDocumento() {
    }

    /** DNI: exactamente 8 dígitos. */
    public static boolean dniValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    /**
     * RUC: 11 dígitos, empieza en 10 (persona con negocio), 15, 17 o 20 (empresa),
     * y el último dígito es el verificador de SUNAT (módulo 11).
     */
    public static boolean rucValido(String ruc) {
        if (ruc == null || !ruc.matches("(10|15|17|20)\\d{9}")) return false;
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * FACTORES_RUC[i];
        }
        int digito = 11 - (suma % 11);
        if (digito == 10) digito = 0;
        if (digito == 11) digito = 1;
        return digito == ruc.charAt(10) - '0';
    }
}
