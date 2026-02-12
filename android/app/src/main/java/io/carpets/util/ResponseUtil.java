package io.carpets.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ResponseUtil {

    // Patrón para validar DNI peruano (8 dígitos exactos)
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");

    // Retorna un mensaje de éxito con datos opcionales
    public static Map<String, Object> exito(String mensaje, Object datos) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("mensaje", mensaje);
        response.put("datos", datos);
        return response;
    }

    // Retorna un mensaje de error
    public static Map<String, Object> error(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("mensaje", mensaje);
        return response;
    }

    // Validación simple de campos (no nulos)
    public static boolean validarNoNulo(Object campo) {
        return campo != null;
    }

    // Validación de cadenas no vacías
    public static boolean validarString(String campo) {
        return campo != null && !campo.trim().isEmpty();
    }

    // Validación numérica positiva
    public static boolean validarPositivo(double valor) {
        return valor > 0;
    }

    // Validación de DNI peruano (8 dígitos exactos, solo números)
    public static boolean validarDNI(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            return false;
        }

        // Validar formato: exactamente 8 dígitos
        boolean formatoValido = DNI_PATTERN.matcher(dni.trim()).matches();

        if (!formatoValido) {
            return false;
        }

        // Validar que no sea una secuencia de ceros
        if (dni.trim().equals("00000000")) {
            return false;
        }

        return true;
    }

    // Validación de DNI con mensaje de error detallado
    public static Map<String, Object> validarDNIConMensaje(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            return error("El DNI no puede estar vacío");
        }

        // Validar formato
        if (!DNI_PATTERN.matcher(dni.trim()).matches()) {
            return error("El DNI debe tener exactamente 8 dígitos numéricos");
        }

        // Validar que no sea una secuencia de ceros
        if (dni.trim().equals("00000000")) {
            return error("El DNI no puede ser 00000000");
        }

        return exito("DNI válido", dni.trim());
    }

    // Validación de RUC peruano (opcional, por si lo necesitas después)
    public static boolean validarRUC(String ruc) {
        if (ruc == null || ruc.trim().isEmpty()) {
            return false;
        }

        // RUC peruano: 11 dígitos exactos
        return Pattern.matches("^\\d{11}$", ruc.trim());
    }

    // Validación de teléfono peruano (opcional)
    public static boolean validarTelefonoPeruano(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }

        // Teléfono peruano: 9 dígitos, puede empezar con 9
        return Pattern.matches("^9\\d{8}$", telefono.trim());
    }
}