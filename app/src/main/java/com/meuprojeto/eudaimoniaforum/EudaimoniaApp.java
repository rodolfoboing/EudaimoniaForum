package com.meuprojeto.eudaimoniaforum;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class EudaimoniaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Habilitar a persistência de dados offline do Firebase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        // As Referências importantes (ex: forum, chat e profile)
        // podem chamar keepSynced(true) posteriormente em seus respectivos Managers 
        // para forçar a atualização dos caches, mas apenas essa linha já 
        // garante o armazenamento local básico das queries realizadas.
    }
}
