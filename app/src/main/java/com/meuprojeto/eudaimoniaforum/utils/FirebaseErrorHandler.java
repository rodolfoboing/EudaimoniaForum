package com.meuprojeto.eudaimoniaforum.utils;

import com.google.firebase.database.DatabaseError;

public class FirebaseErrorHandler {
    
    public static String getFriendlyMessage(DatabaseError error) {
        if (error == null) return "Erro desconhecido.";
        
        int code = error.getCode();
        String message = error.getMessage().toLowerCase();
        
        if (code == DatabaseError.NETWORK_ERROR || message.contains("offline") || message.contains("disconnected")) {
            return "Sem conexão com a internet. Verifique sua rede e tente novamente.";
        } else if (code == DatabaseError.PERMISSION_DENIED) {
            return "Você não tem permissão para realizar esta operação.";
        } else if (code == DatabaseError.EXPIRED_TOKEN || code == DatabaseError.INVALID_TOKEN) {
            return "Sua autenticação expirou. Por favor, deslogue e faça login novamente.";
        } else if (code == DatabaseError.MAX_RETRIES) {
            return "Tempo de requisição esgotado. Verifique sua conexão.";
        } else if(code == DatabaseError.UNAVAILABLE) {
            return "O serviço está temporariamente indisponível.";
        }
        
        return "Erro interno do servidor: " + error.getMessage();
    }
    
    public static String getFriendlyMessage(Exception exception) {
        if (exception == null) return "Erro desconhecido.";
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (message.contains("offline") || message.contains("network") || message.contains("timeout")) {
            return "Sem conexão com a internet. Verifique sua rede e tente novamente.";
        }
        return "Erro de processamento: " + exception.getMessage();
    }
}
