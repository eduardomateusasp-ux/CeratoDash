# Assinatura do APK com keystore própria

Hoje o APK sai assinado com a chave de debug, que serve para instalar por pendrive. Para ter uma assinatura sua, siga os passos abaixo. A keystore e as senhas ficam **fora do repositório**: o projeto só lê a keystore dos GitHub Secrets (ou de um arquivo local ignorado pelo git).

Nunca cole a senha nem a keystore em chat, e-mail ou issue.

## Antes de começar: avisos importantes
- **Guarde a keystore e a senha em lugar seguro, com cópia.** Se perder, não dá para atualizar o app instalado na central: será preciso desinstalar e instalar de novo, perdendo ajustes e viagens.
- **A primeira instalação com a chave nova não entra por cima de uma versão assinada com a chave de debug.** O Android recusa. Desinstale a versão antiga na central antes (os ajustes e as viagens são apagados).

## 1. Instalar o JDK 17 no seu computador
Você precisa do `keytool`, que vem no JDK. No PowerShell:

    winget install EclipseAdoptium.Temurin.17.JDK

Feche e abra o terminal depois.

## 2. Gerar a keystore
Escolha uma pasta fora do projeto (por exemplo, `C:\Chaves`):

    keytool -genkeypair -v -keystore C:\Chaves\ceratodash-release.jks -alias ceratodash -keyalg RSA -keysize 2048 -validity 10000

O `keytool` pergunta uma senha (a mesma vale para a chave, se você aceitar o padrão) e alguns dados (nome, cidade etc.). Anote o alias (`ceratodash`) e a senha num gerenciador de senhas.

## 3. Converter a keystore para texto (base64)
O GitHub Secrets guarda texto, não arquivo. No PowerShell:

    [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Chaves\ceratodash-release.jks")) | Set-Clipboard

O texto fica na área de transferência.

## 4. Criar os quatro Secrets no GitHub
No repositório: Settings, Secrets and variables, Actions, New repository secret. Crie:

| Nome | Valor |
|---|---|
| `KEYSTORE_BASE64` | o texto do passo 3 (cole com Ctrl+V) |
| `KEYSTORE_PASSWORD` | a senha da keystore |
| `KEY_ALIAS` | `ceratodash` (ou o alias que você usou) |
| `KEY_PASSWORD` | a senha da chave (a mesma da keystore, se você aceitou o padrão) |

## 5. Gerar o APK e conferir
Rode o workflow "Gerar APK" (aba Actions, Run workflow). Na etapa "Preparar assinatura" deve aparecer **"Assinatura: keystore própria."** Se aparecer "chave de debug", algum Secret está faltando ou com o nome errado.

Baixe o artefato `CeratoDash-apk` e instale (lembrando de desinstalar a versão antiga antes, uma única vez).

## Gerar assinado no próprio computador (opcional)
Crie o arquivo `keystore.properties` na raiz do projeto (ele está no `.gitignore` e nunca vai para o GitHub):

    storeFile=C:/Chaves/ceratodash-release.jks
    storePassword=SUA_SENHA
    keyAlias=ceratodash
    keyPassword=SUA_SENHA

Depois: `gradle assembleRelease`.

## Como o projeto decide qual chave usar
No `app/build.gradle.kts`: se existir `KEYSTORE_FILE` (variável de ambiente, definida pelo workflow) ou `keystore.properties`, assina com a keystore própria; senão, usa a chave de debug. Se a keystore existir mas faltar senha, o build falha de propósito.

## Estado
Preparado e **não testado**: eu não tenho a sua keystore, então o caminho "keystore própria" nunca rodou. Só o caminho com chave de debug foi confirmado no GitHub Actions.
