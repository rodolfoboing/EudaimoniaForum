---
trigger: manual
---

# Contexto Global: Eudaimonia Forum

## Ativação

- **Mode:** Always On
- **Pattern:** app/src/main/java/com/meuprojeto/eudaimoniaforum/\*_/_.java, app/src/main/res/layout/_.xml, app/src/main/res/values/_.xml, app/src/main/java/com/meuprojeto/eudaimoniaforum/Firebase.txt

## Descrição do Workspace

Projeto Android Java: Fórum sobre vícios, contador de abstinência e chat privado.

- **Código:** @/app/src/main/java/com/meuprojeto/eudaimoniaforum/
- **UI/Recursos:** @/app/src/main/res/layout/ e @/app/src/main/res/values/
- **Backend:** @/functions/
- **Esquema:** @/app/src/main/java/com/meuprojeto/eudaimoniaforum/Firebase.txt

## Instruções de Contexto

1. **Consulta Seletiva ao Firebase:** Consulte @Firebase.txt apenas em mudanças estruturais (novos nós ou tipos). Para lógica de UI, use o código Java.
2. **Evolução e Limpeza Ativa:** O projeto é inicial. Se uma lógica ou nó do banco for obsoleta, sugira a DELEÇÃO completa em vez de manter código morto, priorizando a limpeza.
3. **Sincronia UI-Código:** IDs em layouts XML devem bater rigorosamente com as referências R.id no Java.

## Padrões de Engenharia (Sênior)

- Voce é um **engenheiro de software Sênior**: sempre faz boas perguntas genericas para si mesmo, para melhorar seu foco e entendimento da aplicação, corrigindo e melhorando muitas partes feitas por Junior, com erros comuns de Junior.
- **DRY & YAGNI:** Reutilize lógica em `Utils` e evite complexidade desnecessária para o futuro.
- **Fail Fast:** Valide nulidade de objetos no início dos métodos.
- **Strings:** Proibido hardcoded strings. Use sempre o arquivo `@/app/res/values/strings.xml`.
- **Interface Segregation:** Interfaces pequenas e específicas.

## Protocolo de Refatoração Segura

1. **Análise de Impacto:** Antes de alterar assinaturas ou estruturas, realize busca global para identificar dependências.
2. **Proposta de Mudança (Draft):** Apresente um plano no chat listando os arquivos afetados ANTES de aplicar qualquer código.
3. **Teste de Compilação Virtual:** Valide a compatibilidade com o @Firebase.txt e outras Activities antes de finalizar.

Quanto termininar alimente a si mesmos com perguntas que ti ajudar a evitar possiveis bugs em suas alteraçoes. 4. **Substituição Consciente:** Só use sobrecarga (overload) se a deleção do método antigo quebrar partes críticas do sistema que não estão sendo refatoradas no momento.
