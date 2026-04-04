package com.meuprojeto.eudaimoniaforum.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppLogger {

    private static final DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("admin_logs");

    public static void logSpam(String userId, String module) {
        enviarLog("SECURITY_VIOLATION", "Spam detectado no módulo: " + module + " pelo usuário UID: " + userId);
    }

    public static void logDbError(String context, String errorMessage) {
        enviarLog("DB_UPDATE_FAILED", "Falha de BD em [" + context + "]: " + errorMessage);
    }

    public static void logModAlert(String context, String message) {
        enviarLog("MOD_ALERT", "[" + context + "] " + message);
    }

    private static void enviarLog(String type, String message) {
        try {
            String dataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            Map<String, Object> logNode = new HashMap<>();
            logNode.put("dataHora", dataHora);
            logNode.put("timestamp", System.currentTimeMillis());
            logNode.put("tipo", type);
            logNode.put("mensagem", message);

            logsRef.push().setValue(logNode);
        } catch (Exception e) {
            // Ignora silenciosamente. O logger não deve quebrar o app se falhar.
        }
    }
}
