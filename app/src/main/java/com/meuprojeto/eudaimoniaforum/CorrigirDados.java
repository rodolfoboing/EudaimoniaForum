package com.meuprojeto.eudaimoniaforum;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CorrigirDados {

    public void padronizarDataEntrada() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (userSnapshot.hasChild("dataEntrada")) {
                        Object dataEntradaObj = userSnapshot.child("dataEntrada").getValue();

                        if (dataEntradaObj instanceof String) {
                            try {
                                // Converter String para Long
                                Long dataEntradaTimestamp = Long.parseLong((String) dataEntradaObj);
                                userSnapshot.getRef().child("dataEntrada").setValue(dataEntradaTimestamp);
                            } catch (NumberFormatException e) {
                                // Log de erro se a String não for um número válido
                                System.err.println("Erro ao converter dataEntrada para Long: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.err.println("Erro ao padronizar dataEntrada: " + error.getMessage());
            }
        });
    }
}