package io.carpets.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Response<T>{

    private Map<String, Object> response = new HashMap<>();
    public static final String EXITO = "ok";
    public static final String INTERNAL_ERROR = "internal_error";
    public static final String MESSAGE_ERROR = "error";
    /*
    // Patrón para validar DNI peruano (8 dígitos exactos)
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
*/


    /**
     * En caso el flujo sea exitoso, agrega los datos y un verificador (status).
     * @param datos Son los datos que retornarás.
     */
    public void exito(T datos) {
        response.put("status", EXITO);
        response.put("Content", datos);
    }
    /**
     * En caso el flujo sea exitoso. Se usa esta sobrecarga en caso no se pase ningun dato.
     */
    public void exito() {
        response.put("status", EXITO);
    }

    /**
     * En caso el flujo tenga un error, Response conservará el error y lo imprimirá.
     * @param mensaje El mensaje a conservar/imprimir.
     */
    public void internal_error(String mensaje) {
        response.put("status", INTERNAL_ERROR);
        response.put("mensaje", mensaje);
        System.out.println(mensaje);
    }

    /**
     * Si en el flujo hay algún error, el mensaje será almacenado para ser mostrado en la aplicación.
     * @param mensaje Es el mensaje a mostrar.
     */
    public void message_error(String mensaje){
        response.put("status", MESSAGE_ERROR);
        response.put("mensaje", mensaje);
    }

    /**
     * @return El estado de la respuesta: Ok, InternalError, MessageError.
     */
    public String getStatus(){
        return response.get("status").toString();
    }

    /**
     * @return El contenido de la respuesta. Pueden ser listas, objetos, etc.
     */
    public boolean isOk(){
        return response.get("status").toString().equals(EXITO);
    }

    /**
     * @return El contenido de la respuesta. Pueden ser listas, objetos, etc.
     */
    public T getContent(){
        return (T) response.get("Content");
    }

    /**
     * @return Retorna el mapa almacenado. Usado para enviarlo por el MethodChannels.
     */
    public Map<String, Object> getMap(){
        return response;
    }

    /*
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
*/
    // Validación de RUC peruano (opcional, por si lo necesitas después)
    public static boolean validarRUC(String ruc) {
        if (ruc == null || ruc.trim().isEmpty()) {
            return false;
        }

        // RUC peruano: 11 dígitos exactos
        return Pattern.matches("^\\d{11}$", ruc.trim());
    }

    /*
    // Validación de teléfono peruano (opcional)
    public static boolean validarTelefonoPeruano(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }

        // Teléfono peruano: 9 dígitos, puede empezar con 9
        return Pattern.matches("^9\\d{8}$", telefono.trim());
    }*/
}