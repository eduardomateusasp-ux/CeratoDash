# CeratoDash

Painel de telemetria OBDII para a central multimídia Android do Kia Cerato 2010/2011 SX2 1.6 automático. Lê o carro por um adaptador ELM327 Bluetooth e mostra rotação, velocidade, temperaturas, bateria, carga, borboleta e consumo estimado, com gráficos, diagnóstico, viagens e um treinador de condução. Funciona 100% sem internet.

**Estado: versão 0.7.0, ainda não testada no carro.** Os testes automáticos (103) rodam com um adaptador simulado. Veja `docs/PRD.md` para o que está feito e o que falta, e `docs/testes-no-carro.md` para o roteiro de testes que você vai fazer.

## Telas
Painel, Telemetria, Condução, Diagnóstico, Viagens, Mapas e Ajustes, numa barra lateral. Alertas visuais no topo (com voz opcional), dicas de condução e foto do carro com pontos interativos (motor, bateria e escapamento).

## Como gerar o APK

### Opção A: pelo GitHub (não precisa instalar nada)
1. Envie esta pasta para um repositório no GitHub (no VSCode: Source Control, Publish to GitHub).
2. Na aba Actions, abra a execução mais recente de "Gerar APK" (ele roda sozinho a cada envio) ou clique em Run workflow.
3. Quando terminar, role até Artifacts, baixe `CeratoDash-apk` e extraia o `app-release.apk` do zip.
4. A etapa de testes roda antes. Se ela falhar, o APK ainda é gerado, e os erros aparecem como anotações na página da execução.

### Opção B: no seu computador
Requisitos: JDK 17, Android SDK (variável `ANDROID_HOME`) e Gradle 8.9 ou mais novo.

    gradle assembleRelease

O APK sai em `app/build/outputs/apk/release/app-release.apk`. Para rodar os testes: `gradle testDebugUnitTest`.

### Assinatura
Sem configuração, o APK sai assinado com a chave de debug, o que basta para instalar por pendrive. Para assinar com a sua própria keystore (guardada fora do repositório e lida dos GitHub Secrets), siga `docs/assinatura-do-apk.md`. Atenção: ao trocar da chave de debug para a sua, desinstale a versão antiga na central antes (uma vez só).

## Como instalar na central
1. Copie o `.apk` para um pendrive e ligue na entrada USB da central.
2. No gerenciador de arquivos da central, toque no APK e instale. Se pedir, permita instalar de fontes desconhecidas.
3. Se já havia uma versão instalada com outra assinatura, desinstale antes.

## Como parear e escolher o adaptador
1. No Bluetooth da central, pareie o adaptador OBDII (normalmente aparece como "OBDII" ou "OBD2"; a senha costuma ser 1234 ou 0000).
2. O adaptador só aceita um aparelho por vez. Feche o Car Scanner e desligue o Bluetooth do celular.
3. Abra o CeratoDash, vá em Ajustes, Escolher adaptador, e toque no adaptador.
4. Com a ignição ligada (ou o motor ligado), o status muda para Conectado e o painel começa a mostrar os dados. Se o carro não responder a um sensor, o painel mostra "--".

## Como trocar a foto do carro
- **Pelo app (sem gerar APK):** Ajustes, Foto do carro, Escolher foto. Use uma imagem em vista lateral. A foto fica só no aparelho. Se a frente do carro na foto apontar para a direita, troque a opção "Frente do carro na foto" para que os pontos fiquem no lugar certo. "Voltar à foto padrão" desfaz.
- **Pelo projeto:** troque `app/src/main/res/drawable-nodpi/car_photo.jpg` (ou use `car_photo.png`, apagando o `.jpg`) e gere o APK de novo.
- A foto padrão é um Kia Cerato 1.6 SX 2010 do Wikimedia Commons, licença CC BY-SA 2.0. Créditos em `docs/licencas/foto-do-carro.md`.
- A foto não precisa de fundo transparente: ela aparece num cartão de cantos arredondados.

## Voz nos alertas
Em Ajustes, Voz: ligue a voz nos alertas e, separadamente, nas dicas de condução. Use Testar voz. Precisa de uma voz em português instalada na central (o app avisa se não houver). Desligada por padrão.

## Faixa sobre o Maps e o Waze
Em Ajustes, Mapas: ligue a faixa para ver rotação, velocidade e água sobre o mapa. Ela precisa da permissão Sobrepor outros apps, que a central pode não oferecer. Se não oferecer, o app só abre o mapa e continua lendo em segundo plano. Desligada por padrão.

## Limitações conhecidas
- **Nada foi testado no carro.** Bluetooth real, reconexão, serviço em primeiro plano, BLE, voz, faixa sobre o mapa e uso de uma hora são incógnitas.
- Só sensores OBD-II padrão. O app pergunta ao carro quais ele responde e mostra apenas esses. Sem resposta, "--".
- O consumo (km/L) só aparece se o carro informar o fluxo de ar (MAF), e é uma estimativa.
- A distância das viagens vem da velocidade do OBD (sem GPS), então tem pequena imprecisão.
- A descrição dos códigos de falha existe só para cerca de 85 códigos genéricos P0xxx (SAE J2012, tradução própria). Códigos do fabricante aparecem sem descrição.
- Os limites do treinador de condução são valores gerais de direção econômica, não uma especificação da Kia, e precisam de calibração. Sem GPS, o limite de velocidade é o que você configura.
- BLE só funciona com adaptador já pareado (não há varredura).
- O app usa só km/h, °C e km/L, de propósito.
- O Google Maps funciona offline se você baixar as áreas antes. O Waze precisa de internet. O app não embute mapa (veja `docs/avaliacao-mapa-offline.md`).
- O vermelho de perigo tem contraste de 5,2:1, abaixo da meta de 7:1 (`docs/design-system.md`).

## Estrutura do projeto
- `app/src/main/java/br/com/ceratodash/obd`: conexão, protocolo ELM327, sensores (PIDs), gerenciador.
- `.../data`: preferências, viagens (SQLite), alertas, condução, voz, tabela de códigos.
- `.../service`: serviço em primeiro plano, voz, faixa sobre o mapa, início com o sistema.
- `.../ui`: telas e componentes (Views e Canvas, sem Compose).
- `app/src/test`: testes unitários com ELM327 simulado.
- `docs/`: PRD, design system, roteiro de testes no carro, assinatura, avaliação do mapa offline, licenças.

## Créditos e licenças
Fontes Rajdhani e Inter (licença OFL, em `docs/licencas/`) e a foto padrão (CC BY-SA 2.0, `docs/licencas/foto-do-carro.md`). Sem telemetria, sem análise de uso e sem envio de dados para fora do aparelho.
