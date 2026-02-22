package io.carpets.autenticacion;

import android.content.Context;
import android.content.SharedPreferences;

/*
    Clase para gestionar la autenticación del usuario mediante SharedPreferences.
    Permite guardar, recuperar y eliminar la sesión del usuario.
*/

public class AuthManager {
    private static final String PREFS = "COM.EXAMPLE.NELLYSHOP";
    private final SharedPreferences prefs;

    public AuthManager(Context ctx) {
        this.prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Guarda la sesión del usuario
    public void saveSession(String email, String token, String role) {
        prefs.e
        dit().putString("email", email).putString("token", token).putString("role", role).apply();
    }

    // Guarda solo el token de autenticación
    public void saveToken(String token) {
        prefs.edit().putString("token", token).apply();
    }

    public String getToken() { return prefs.getString("token", null); }
    public String getEmail() { return prefs.getString("email", null); }
    public String getRole() { return prefs.getString("role", null); }

    public boolean isLoggedIn() { return getToken() != null; }

    public void logout() { prefs.edit().remove("token").remove("email").remove("role").apply(); }
}
