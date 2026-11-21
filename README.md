# CeratoDash

Painel de telemetria OBDII para a central multimídia Android do Kia Cerato.
Lê o carro por um adaptador ELM327 Bluetooth e mostra rotação, velocidade,
temperatura, bateria, carga, borboleta e consumo estimado. Abre o Google Maps
e o Waze por botões.

Status: versão 0.1, ainda não testada no carro.

## Como gerar o APK

### Opção A: pelo GitHub (sem instalar nada)
1. Crie um repositório no GitHub e envie esta pasta (VSCode: Source Control, Publish to GitHub).
2. Abra a aba Actions, escolha "Gerar APK" e clique em Run workflow.
3. Quando terminar, baixe o arquivo `CeratoDash-apk` e extraia o `app-release.apk`.

### Opção B: no seu computador
Requisitos: JDK 17, Android SDK (variável ANDROID_HOME) e Gradle 8.9 ou mais novo.

    gradle assembleRelease

O APK sai em `app/build/outputs/apk/release/app-release.apk`.
Para criar o wrapper do Gradle uma vez: `gradle wrapper --gradle-version 8.9`.

## Como instalar na central
1. Copie o `.apk` para um pendrive e ligue na entrada USB da central.
2. Abra o gerenciador de arquivos da central, toque no APK e instale.
   Se pedir, permita instalar de fontes desconhecidas.
3. Pareie o adaptador OBDII nos ajustes de Bluetooth da central.
   O adaptador só aceita um aparelho por vez: desconecte do celular antes.
4. Abra o CeratoDash, toque em Ajustes, Escolher adaptador OBDII e selecione o adaptador.
5. Com a ignição ligada (ou o motor ligado), o painel começa a mostrar os dados.

## Foto do carro
Coloque um PNG com fundo transparente, vista lateral, em
`app/src/main/res/drawable-nodpi/car_photo.png` e gere o APK de novo.
Sem o arquivo, o app desenha uma silhueta.

## Limitações conhecidas da 0.1
- Só sensores OBD-II padrão. O app pergunta ao carro quais ele suporta.
- Consumo (km/L) só aparece se o carro informar o fluxo de ar (MAF); é uma estimativa.
- Google Maps funciona offline se você baixar as áreas antes. O Waze precisa de internet.
- Ao abrir o Maps ou o Waze, a leitura continua em segundo plano, mas o Android da central
  pode encerrar o app se faltar memória. Um serviço em primeiro plano fica para a próxima versão.
