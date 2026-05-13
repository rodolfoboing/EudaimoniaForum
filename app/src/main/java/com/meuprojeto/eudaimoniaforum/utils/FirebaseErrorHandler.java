package com.meuprojeto.eudaimoniaforum.utils;

import com.google.firebase.database.DatabaseError;

public class FirebaseErrorHandler {
    
    public static String getFriendlyMessage(android.content.Context context, DatabaseError error) {
        if (error == null) return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unknown);
        
        int code = error.getCode();
        String message = error.getMessage().toLowerCase();
        
        if (code == DatabaseError.NETWORK_ERROR || message.contains("offline") || message.contains("disconnected")) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_no_internet);
        } else if (code == DatabaseError.PERMISSION_DENIED) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_no_permission);
        } else if (code == DatabaseError.EXPIRED_TOKEN || code == DatabaseError.INVALID_TOKEN) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_auth_expired);
        } else if (code == DatabaseError.MAX_RETRIES) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_timeout);
        } else if(code == DatabaseError.UNAVAILABLE) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_service_unavailable);
        }
        
        return "Firebase Error: " + error.getMessage();
    }
    
    public static String getFriendlyMessage(android.content.Context context, Exception exception) {
        if (exception == null) return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unknown);
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (message.contains("offline") || message.contains("network") || message.contains("timeout")) {
            return context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_no_internet);
        }
        return "Internal Error: " + exception.getMessage();
    }
}
