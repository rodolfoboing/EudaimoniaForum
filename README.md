# 📚 **EudaimoniaForum**

O EudaimoniaForum é um aplicativo Android desenvolvido em Java que oferece um espaço de interação, troca de mensagens e discussões em formato de fórum. Ele inclui funcionalidades de chat, postagens, denúncias, notificações e gerenciamento de perfil, com foco em criar uma comunidade saudável e moderada.

🚀 Funcionalidades Principais
Autenticação de Usuário

Login, registro e recuperação de senha.

Postagens e Fórum

Criar, visualizar e editar postagens.

Listagem de posts com adaptadores personalizados.

Chat e Conversas

Envio e recebimento de mensagens em tempo real.

Gerenciamento de conversas com ChatActivity e ConversasActivity.

Denúncias e Moderação

Sistema de denúncia de conteúdo.

Tela de moderação para administradores.

Notificações

Push notifications via Firebase (MyFirebaseMessagingService).

Exibição de notificações dentro do app.

Perfil e Configurações

Edição de perfil.

Visualização de perfis de outros usuários.

Onboarding

Fluxo inicial com telas de introdução e orientações.

Metas e Orientações

Funcionalidades relacionadas a metas pessoais e orientações de uso.

🛠️ Estrutura do Projeto
Principais pacotes e classes:

MainActivity.java → Ponto de entrada do app.

LoginActivity.java, RegistrarActivity.java, RecuperarSenhaActivity.java → Autenticação.

ForumActivity.java, Post.java, PostAdapter.java → Fórum e postagens.

ChatActivity.java, ChatMessage.java, ChatAdapter.java → Chat.

DenunciaActivity.java, Denuncia.java, DenunciaAdapter.java → Denúncias.

ModeracaoActivity.java, UsuarioModeracaoAdapter.java → Moderação.

NotificacaoActivity.java, Notificacao.java, NotificacaoAdapter.java → Notificações.

PerfilActivity.java, EditarPerfilActivity.java, VisualizarPerfilActivity.java → Perfis.

OnboardingActivity.java, OnboardingAdapter.java, OnboardingItem.java → Onboarding.

MetasWorker.java → Gerenciamento de metas.

OrientacoesActivity.java → Orientações gerais.

📲 Tecnologias Utilizadas
Java para desenvolvimento Android.

Firebase Cloud Messaging (FCM) para notificações.

Android SDK e componentes nativos.

Adapters para listas e gerenciamento de UI.

▶️ Como Executar
Clone o repositório:

bash
git clone https://github.com/rodolfoboing/EudaimoniaForum.git
Abra o projeto no Android Studio.

Configure o Firebase no projeto (adicione o google-services.json).

Compile e rode em um emulador ou dispositivo físico.

📌 Próximos Passos
Melhorar UI/UX das telas principais.

Implementar testes automatizados.

Expandir funcionalidades de moderação.

Adicionar suporte multilíngue.

👨‍💻 Autor
Desenvolvido por Rodolfo Boing.
Contribuições e sugestões são bem-vindas!
