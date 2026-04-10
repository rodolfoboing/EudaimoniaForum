const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Função que escuta novos registros criados em "notificacoes/{userId}/{notificationId}"
exports.sendNotification = functions.database
    .ref("/notificacoes/{userId}/{notificationId}")
    .onCreate(async (snapshot, context) => {
      const userId = context.params.userId;
      const notificationId = context.params.notificationId;

      const notification = snapshot.val();

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

// ============================================================================
// FUNÇÃO DE LIMPEZA DE DADOS: Evita Nicks Órfãos
// ============================================================================

// 1. Limpa o banco de dados caso uma conta seja excluída no Firebase Authentication (Console)
exports.cleanupOnAuthDelete = functions.auth.user().onDelete(async (user) => {
  const userId = user.uid;
  console.log(`[cleanupOnAuthDelete] Conta Auth excluída (UID: ${userId}). Removendo rastros do banco de dados...`);
  
  const snap = await admin.database().ref(`users/${userId}/nick`).once("value");
  const nick = snap.val();

  const updates = {};
  updates[`users/${userId}`] = null;
  updates[`banidos/${userId}`] = null;
  
  if (nick) {
    console.log(`[cleanupOnAuthDelete] Removendo nick reservado: ${nick}`);
    updates[`usernames/${nick}`] = null;
  }

  return admin.database().ref().update(updates).then(() => {
    console.log(`[cleanupOnAuthDelete] ✨ Limpeza completa para ${userId}.`);
  });
});

// 2. Limpa o nick caso você (Admin) delete a pasta do usuário diretamente dentro do Realtime Database
exports.cleanupUserLinks = functions.database
    .ref("/users/{userId}")
    .onDelete(async (snapshot, context) => {
      const deletedUser = snapshot.val();
      const userId = context.params.userId;

      if (deletedUser && deletedUser.nick) {
        console.log(`[cleanupUserLinks] Nó de usuário (UID: ${userId}) apagado. Libertando o nick: ${deletedUser.nick}`);
        return admin.database().ref(`usernames/${deletedUser.nick}`).remove();
      }
      return null;
    });

// ============================================================================
// FAXINA GERAL PROFUNDA: Sincroniza o Database com o Authentication
// ============================================================================

// Esta função você roda uma única vez via navegador para limpar os "fantasmas"
exports.manualmenteLimparNicksOrfaos = functions.https.onRequest(async (req, res) => {
  console.log("🚀 INICIANDO FAXINA GERAL: Cruzando Database vs Authentication...");
  
  try {
    const usersSnap = await admin.database().ref("users").once("value");
    const usernamesSnap = await admin.database().ref("usernames").once("value");
    const banidosSnap = await admin.database().ref("banidos").once("value");
    
    if (!usersSnap.exists() && !usernamesSnap.exists() && !banidosSnap.exists()) {
      return res.send("Banco de dados vazio. Nada para limpar.");
    }

    const usersData = usersSnap.val() || {};
    const usernamesData = usernamesSnap.val() || {};
    const banidosData = banidosSnap.val() || {};
    
    const updates = {};
    let contasApagadas = 0;

    // 1. Vasculhar todos os UIDs dentro do nó 'users'
    for (const [uid, userData] of Object.entries(usersData)) {
      try {
        // Tenta buscar no Firebase Auth
        await admin.auth().getUser(uid);
      } catch (error) {
        if (error.code === 'auth/user-not-found') {
          console.log(`[FAXINA] 👻 Fantasma detectado! UID: ${uid} (Não existe mais no Auth). Apagando...`);
          
          updates[`users/${uid}`] = null;
          updates[`banidos/${uid}`] = null;
          
          if (userData && userData.nick) {
            updates[`usernames/${userData.nick}`] = null;
          }
          
          contasApagadas++;
        }
      }
    }

    // 2. Vasculhar 'banidos' órfãos (UIDs que estão só nos banidos, sem Auth)
    for (const uid of Object.keys(banidosData)) {
      try {
        await admin.auth().getUser(uid);
      } catch (error) {
        if (error.code === 'auth/user-not-found') {
          updates[`banidos/${uid}`] = null;
        }
      }
    }

    // 3. Vasculhar nicks órfãos puros (UID que pode não estar no 'users', mas está no 'usernames')
    for (const [nick, uid] of Object.entries(usernamesData)) {
      try {
        await admin.auth().getUser(uid);
      } catch (error) {
        if (error.code === 'auth/user-not-found') {
          updates[`usernames/${nick}`] = null;
        }
      }
    }

    if (Object.keys(updates).length > 0) {
      await admin.database().ref().update(updates);
      console.log(`[FAXINA] SUCESSO! Removidos rastros de ${contasApagadas} contas.`);
      return res.send(`Faxina Profunda Concluída! Foram dizimados do banco de dados <b>${contasApagadas} usuários antigos</b> que já não existem no painel de Authentication.`);
    } else {
      return res.send("Parabéns! O seu Realtime DB está perfeitamente espelhado com o Authentication. Nenhuma sujeira antiga encontrada.");
    }

  } catch (error) {
    console.error("Erro na faxina profunda:", error);
    return res.status(500).send("Erro ao processar a faxina: " + error.message);
  }
});

// ============================================================================
// RESTAURADOR (AUTO-HEAL): Reconstrói nicks perdidos do nó "usernames"
// ============================================================================
exports.restaurarNicksAtivos = functions.https.onRequest(async (req, res) => {
  console.log("🛠️ INICIANDO RESTAURAÇÃO: Repovoando a pasta 'usernames'...");
  
  try {
    const usersSnap = await admin.database().ref("users").once("value");
    
    if (!usersSnap.exists()) {
      return res.send("Nenhum usuário encontrado na pasta 'users'. Nada a reconstruir.");
    }

    const usersData = usersSnap.val();
    const updates = {};
    let nicksRestaurados = 0;

    for (const [uid, userData] of Object.entries(usersData)) {
      if (userData && userData.nick) {
        updates[`usernames/${userData.nick}`] = uid;
        nicksRestaurados++;
      }
    }

    if (Object.keys(updates).length > 0) {
      await admin.database().ref().update(updates);
      console.log(`[RESTAURAÇÃO] SUCESSO! ${nicksRestaurados} nicks foram cravados de volta no sistema.`);
      return res.send(`<h2>Restauração Completa!</h2>Foram reconstruídos e amarrados com sucesso <b>${nicksRestaurados} usernames</b>! O Firebase Database agora tem a pasta de volta e o PRESIDENTE está protegido.`);
    } else {
      return res.send("Nenhum usuário da pasta 'users' possuía um nick válido.");
    }

  } catch (error) {
    console.error("Erro na restauração:", error);
    return res.status(500).send("Erro ao restaurar nicks: " + error.message);
  }
});
