const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Função que escuta novos registros ou atualizações em "notificacoes/{userId}/{notificationId}"
exports.sendNotification = functions.database
    .ref("/notificacoes/{userId}/{notificationId}")
    .onWrite(async (change, context) => {
      const userId = context.params.userId;
      const notificationId = context.params.notificationId;

      // Se a notificação foi apagada do banco, ignoramos
      if (!change.after.exists()) {
        console.log(`[sendNotification] ℹ️ Notificação (${notificationId}) DELETADA para o usuário ${userId}. Nenhuma ação necessária.`);
        return null;
      }

      const notification = change.after.val();

      console.log(`[sendNotification] 🚀 INÍCIO - Preparando notificação (${notificationId}) para o usuário: ${userId}`);
      console.log(`[sendNotification] 📦 Payload do banco de dados: ${JSON.stringify(notification)}`);

      console.log(`[sendNotification] 🔍 Buscando token FCM para o usuário ${userId}...`);
      const tokenSnapshot = await admin.database().ref(`users/${userId}/fcmToken`).once("value");
      const fcmToken = tokenSnapshot.val();

      if (!fcmToken) {
        console.warn(`[sendNotification] ⚠️ ALERTA: O usuário ${userId} NÃO possui um token FCM cadastrado. O Push não será enviado.`);
        return null;
      }

      console.log(`[sendNotification] ✅ Token FCM obtido com sucesso para ${userId}.`);

      // Define o título com base no tipo de notificação
      let title = "Eudaimonia Forum";
      if (notification.tipo === "chat") {
        title = "💬 Nova Mensagem";
      } else if (notification.tipo === "comentario") {
        title = "💬 Novo Comentário";
      }

      // Monta a mensagem usando apenas o bloco "data" (Data-Only Message)
      // Isso força o Android a sempre acordar o app e executar o "onMessageReceived"
      // mesmo quando o aplicativo está fechado/arrastado para o lado!
      const message = {
        token: fcmToken,
        android: {
          priority: "high", // Prioridade alta para acordar o app em Doze mode
        },
        data: {
          title: title,
          body: notification.mensagem || "Você tem uma nova notificação.",
          tipo: notification.tipo || "",
          idReferencia: String(notification.idReferencia || ""),
        },
      };

      return admin.messaging().send(message)
          .then((response) => {
            console.log(`[sendNotification] 🎉 SUCESSO! A notificação push foi entregue ao servidor do Google. Message ID: ${response}`);
            return null;
          })
          .catch(async (error) => {
            console.error(`[sendNotification] ❌ ERRO CRÍTICO ao tentar enviar Push Notification para o usuário ${userId}.`);
            console.error(`[sendNotification] ❌ Detalhes do erro: Código=[${error.code}] | Mensagem=[${error.message}]`);

            // Se o token for inválido/expirado, limpa do banco para não poluir
            if (error.code === "messaging/registration-token-not-registered" ||
                error.code === "messaging/invalid-registration-token") {
              console.warn(`[sendNotification] 🧹 LIMPANDO BANCO: O token do usuário ${userId} expirou ou é inválido. Removendo nó 'fcmToken'...`);
              await admin.database().ref(`users/${userId}/fcmToken`).remove();
              console.log(`[sendNotification] ✨ Limpeza de token concluída para o usuário ${userId}.`);
            }
            return null;
          });
    });
