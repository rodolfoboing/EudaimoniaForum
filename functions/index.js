const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Função que escuta novos registros em "notificacoes/{userId}/{notificationId}"
exports.sendNotification = functions.database.ref("/notificacoes/{userId}/{notificationId}")
    .onCreate(async (snapshot, context) => {
      const notification = snapshot.val();
      const userId = context.params.userId;

      // Log para debug no console do Firebase
      console.log("Nova notificação para o usuário:", userId);

      // Busca o token FCM do usuário no banco de dados
      const userRef = admin.database().ref(`users/${userId}/fcmToken`);
      const tokenSnapshot = await userRef.once("value");
      const fcmToken = tokenSnapshot.val();

      if (!fcmToken) {
        console.log("Usuário não possui token FCM cadastrado.");
        return null; // Encerra a execução se não houver token
      }

      // Define o título com base no tipo de notificação
      let title = "Eudaimonia Forum";
      if (notification.tipo === "chat") {
        title = "Nova Mensagem";
      } else if (notification.tipo === "comentario") {
        title = "Novo Comentário";
      }

      // Monta a mensagem para o FCM
      // Usando estrutura específica para Android para garantir comportamento correto
      const payload = {
        notification: {
          title: title,
          body: notification.mensagem,
          sound: "default",
          channel_id: "fcm_default_channel", // Importante: deve bater com o ID no Android Manifest/Service
        },
        data: {
          tipo: notification.tipo,
          idReferencia: notification.idReferencia,
          click_action: "FLUTTER_NOTIFICATION_CLICK", // Mantido por compatibilidade, mas o Android nativo usa Intent Filters
        },
      };

      // Envia a notificação
      // Usando sendToDevice (Legacy) que é amplamente suportado
      return admin.messaging().sendToDevice(fcmToken, payload)
          .then((response) => {
            console.log("Notificação enviada com sucesso para", userId);
            // Opcional: Remover notificação do banco após envio se desejar, mas aqui mantemos o histórico
            return null;
          })
          .catch((error) => {
            console.error("Erro ao enviar notificação:", error);
            return null;
          });
    });
