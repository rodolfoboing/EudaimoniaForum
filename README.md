# 📚 **EudaimoniaForum**

O EudaimoniaForum é um aplicativo Android desenvolvido em Java que oferece um espaço seguro de interação, troca de mensagens e suporte mútuo focado na superação de vícios. O projeto segue princípios de **Clean Architecture**, com uma estrutura modular baseada em Managers para garantir escalabilidade, segurança e fácil manutenção.

# 🚀 Funcionalidades Principais

### 🔐 Autenticação e Segurança
*   Login, registro e recuperação de senha via Firebase Auth.
*   **Anti-Spam Nativo:** Travas de cooldown para postagens (60s), comentários (30s) e chat privado (2s).
*   **Blindagem de Backend:** Cloud Functions otimizadas para evitar notificações duplicadas.

### 🏛️ Arquitetura Modular (Manager Pattern)
O app foi totalmente refatorado para separar a lógica de negócio da interface (UI):
*   **Managers:** Centralizam todas as chamadas ao Firebase, transações de banco e lógica de validação.
*   **Activities:** Focam exclusivamente na experiência do usuário e exibição de dados.

### 💬 Comunidade e Interação
*   **Fórum:** Criação e moderação de postagens com sistema de comentários em tempo real.
*   **Chat Privado:** Conversas diretas entre usuários com criptografia lógica e sistema de bloqueio.
*   **Notificacoes:** Push notifications multicanal (💬 Chat / 💬 Comentários) via Firebase Cloud Messaging.

### 🛡️ Moderação e Administração
*   **AppLogger:** Sistema proprietário de logs para administradores capturarem spams, erros de banco e abusos em tempo real via Firebase.
*   **Painel Administrativo:** Interface para revisão de denúncias e banimento de usuários nocivos.

### 📈 Recuperação e Evolução
*   **Contador de Abstinência:** Monitoramento preciso de tempo de sobriedade.
*   **Sistema de Conquistas:** Medalhas e Star-Rating baseados no compromisso diário do usuário.

# 🛠️ Estrutura Técnica do Projeto

### Camada de Lógica (Managers)
*   `AuthManager.java` → Gerenciamento de sessão e tokens.
*   `ForumManager.java` & `ComentarioManager.java` → Fluxo de dados do fórum.
*   `ChatManager.java` & `ConversasManager.java` → Mecanismo de mensagens.
*   `ProfileManager.java` → Estatísticas, conquistas e dados de perfil.
*   `ModeracaoManager.java` → Denúncias e controle de acesso.
*   `NotificacaoManager.java` → Ciclo de vida de notificações.

### Utilitários e Backend
*   `AppLogger.java` → Monitoramento de saúde do app e segurança.
*   `functions/index.js` → Lógica de servidor para Push Notifications.
*   `ConquistasWorker.java` → Processamento em segundo plano.

# 📲 Tecnologias Utilizadas
*   **Java** (Android Nativo).
*   **Firebase Realtime Database** (Dados em tempo real).
*   **Firebase Cloud Functions** (Node.js backend).
*   **Firebase Cloud Messaging** (Push Notifications).
*   **WorkManager** (Tarefas agendadas).

# ▶️ Como Executar
1. Clone o repositório: `git clone https://github.com/rodolfoboing/EudaimoniaForum.git`
2. Abra no Android Studio (Ladybug ou superior).
3. Adicione o seu arquivo `google-services.json` na pasta `/app`.
4. Instale as dependências via Gradle e execute.

# 👨‍💻 Autor
Desenvolvido por **Rodolfo Boing**.  
*Transformando tecnologia em ferramenta de superação.*
