# Roteiro de testes: uma sessão só

Versão do app: 0.9.0. Faça tudo em sequência e preencha as colunas "Resultado" (ok, falhou ou não testei) e "Anotações". No fim, me mande o preenchido (ou só as linhas que falharam) e as fotos das telas de diagnóstico.

Tempo estimado: cerca de 2 horas, contando uma hora de uso contínuo.

## Segurança
- **Não mexa no app dirigindo.** As partes A, B e D são com o carro parado. A parte C é com um passageiro anotando.
- Os testes de condução (C2) só em local seguro, sem trânsito.

## O que levar
APK 0.9.0 já instalado na central, o iPhone e o cabo, o adaptador OBDII, caneta ou o celular para fotografar as telas, e um passageiro para a parte C.

## Em qualquer falha de conexão
Abra Ajustes, **Ver registro da conexão**, e fotografe. Ele mostra cada etapa (conexão, serviços, respostas do ELM327).

---

## Parte A: carro parado, ignição ligada, motor desligado (cerca de 30 min)

### A1. Diagnóstico da central (responde de uma vez Wi-Fi, GPS, Play Store, USB...)
Ajustes, Diagnóstico da central, **Ver diagnóstico da central**. Fotografe a tela inteira (ela rola) e preencha:

| Item | Resultado | Anotações |
|---|---|---|
| Memória livre / total | ___ MB / ___ MB | |
| Bluetooth LE (BLE) | sim / não | |
| Wi-Fi no aparelho | sim / não | |
| GPS no aparelho | sim / não | |
| USB como anfitrião | sim / não | |
| Play Store instalada | sim / não | |
| Google Maps instalado | sim / não | |
| Waze instalado | sim / não | |
| Apps de espelhamento ou CarPlay encontrados | (anote os nomes) | |

### A2. USB com o iPhone no cabo
Ligue o iPhone no cabo da central. Ajustes, **Ver USB**, Atualizar.

| Item | Resultado | Anotações |
|---|---|---|
| A tela mostra um aparelho (o iPhone aparece como Apple) | sim / não | |
| Texto do resultado (copie a última frase) | | |

Lembre: espelhamento por USB do Android (Headunit Revived, scrcpy) **não vale para iPhone**. Este teste só diz se a porta é anfitriã, que o adaptador de CarPlay exigiria.

### A3. Conexão com o adaptador
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Celular desconectado do adaptador, adaptador no carro | | | |
| Ajustes, Escolher entre pareados (adaptador pareado pela lista de Bluetooth da central) | Status vira Conectando e depois Conectado | | |
| Tempo até Conectado (cronometre) | Até 15 s | ___ s | |
| Notificação do app aparece, discreta, com botão Parar | | | |

### A4. Sensores: o que o carro realmente responde
Abra Telemetria com o motor desligado e a ignição ligada, e **repita com o motor ligado** (alguns só respondem com o motor ligado). Só aparecem os sensores que o carro respondeu; sem resposta é esperado, não é erro.

| Sensor | Código | Apareceu? | Valor em marcha lenta | Faz sentido? |
|---|---|---|---|---|
| Rotação | 0C | | | |
| Velocidade | 0D | | | |
| Temperatura da água | 05 | | | |
| Temperatura de admissão | 0F | | | |
| Carga do motor | 04 | | | |
| Borboleta | 11 | | | |
| Pressão do coletor | 0B | | | |
| Fluxo de ar (MAF) | 10 | | | |
| Avanço de ignição | 0E | | | |
| Correção de combustível curta | 06 | | | |
| Correção de combustível longa | 07 | | | |
| Nível de combustível | 2F | | | |
| Temperatura ambiente | 46 | | | |
| Tensão da ECU | 42 | | | |
| Temperatura do óleo | 5C | | | |
| Oxigênio antes do catalisador | 14 | | | |
| Oxigênio depois do catalisador | 15 | | | |
| Temperatura do catalisador | 3C | | | |
| Tensão lida pelo adaptador (ATRV) | | | | |

Compare três valores com o painel do carro:

| Comparação | Painel do carro | App | Resultado |
|---|---|---|---|
| Temperatura da água | | | |
| Rotação em marcha lenta | | | |
| Velocidade (andando devagar, na parte C) | | | |

Se houver **fluxo de ar (MAF)**, o consumo (km/L) aparece; sem ele fica "--".

---

## Parte B: motor ligado, carro parado (cerca de 25 min)

### B1. Reconexão
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Desligar motor e ignição, esperar 30 s, ligar de novo | O app reconecta sozinho | | |
| Tempo para voltar a Conectado | Até 15 s | ___ s | |
| Tirar o adaptador da porta OBD por 10 s e recolocar | Mostra erro e depois reconecta | | |
| Desligar e ligar o Bluetooth da central | Mostra "Bluetooth desligado" e reconecta | | |

### B2. Painel e pontos interativos
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Conta-giros e velocímetro se movem de forma suave | Sem travar | | |
| Barra de rotação acende e pisca perto do limite | | | |
| Números legíveis do banco do motorista (de dia, com sol) | | | |
| Tocar nos anéis de Motor, Bateria e Escapamento | Abrem os painéis com dados | | |
| Os anéis ficam no lugar certo sobre o carro da foto | | | |

### B3. Alertas e voz
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ajustes, Voz, Testar voz | Fala "Voz do CeratoDash funcionando." (ou mostra o motivo) | | |
| Ligar Voz nos alertas. Baixar o limite de rotação em Ajustes e acelerar, parado | Faixa vermelha com texto e ícone, e a voz fala | | |
| A voz repete a cada 30 s enquanto o alerta continua | | | |
| O volume da música abaixa enquanto a voz fala | | | |
| Voltar o limite ao normal | | | |

### B4. Diagnóstico
Anote os códigos ANTES de limpar qualquer coisa.

| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ler códigos | Mostra a luz de injeção e os códigos (ou "Nenhum código") | | |
| Códigos lidos (anote todos) | | | |
| Genéricos têm descrição, os outros dizem "sem descrição" | | | |
| Limpar códigos (só se quiser): aparece confirmação antes | | | |

### B5. Faixa sobre o mapa
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ajustes, Faixa sobre o mapa, ligar | Pede Sobrepor outros apps, ou avisa que a central não permite | | |
| Com a permissão, abrir o Google Maps (se estiver instalado) | Faixa pequena com rotação, velocidade e água, sem atrapalhar o toque | | |

---

## Parte C: rodando, com um passageiro anotando (cerca de 30 min)

### C1. Viagem
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ligar o motor e esperar 5 s | Aparece "Viagem em andamento" em Viagens | | |
| Anotar o odômetro antes e depois de um trajeto conhecido | | km antes ___ / depois ___ | |
| Distância mostrada pelo app | Próxima da do odômetro | ___ km | |
| Desligar o motor e esperar 30 s | A viagem entra no histórico | | |
| Duração, média, máxima, consumo e nota plausíveis | | | |
| Apagar uma viagem (com confirmação) | | | |

### C2. Condução (só em local seguro)
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Acelerar de forma suave | Sem dicas, nota alta | | |
| Acelerar forte uma vez | Dica "Acelerou forte: alivie o pedal" | | |
| Frear forte uma vez | Dica de freada | | |
| Ligar Voz nas dicas e repetir | A dica é falada, sem tagarelar | | |
| Estilo Econômica, Equilibrada, Esportiva dão dicas diferentes | | | |
| Os limites parecem razoáveis para o seu jeito de dirigir? | Anote o que ajustar | | |

---

## Parte D: mapas e espelhamento com iPhone (cerca de 20 min)
Depende do que apareceu em A1 e A2. Leia `docs/espelhamento.md`. Faça só o que se aplica.

| Opção | Só vale se (de A1 e A2) | Resultado | Anotações |
|---|---|---|---|
| D0. Organic Maps na central (mapa offline), APK do F-Droid por pendrive; baixar o seu estado com internet uma vez; depois testar sem internet e fazer uma rota | GPS sim | | |
| D1. Maps ou Waze direto na central, com internet do ponto de acesso do iPhone | Wi-Fi sim, Play Store sim ou APK, e de preferência GPS sim | | |
| D2. Receptor AirPlay na central (espelhar a tela do iPhone) | Wi-Fi sim | | |
| D3. Adaptador de CarPlay (só pesquisar, não comprar ainda) | USB anfitrião sim | | |

Para o D2: a central e o iPhone precisam estar na mesma rede (o ponto de acesso do iPhone); no iPhone, Central de Controle, Espelhamento de Tela, e escolha a central. Anote se o iPhone enxergou a central, se a imagem apareceu e se tem atraso.

---

## Parte E: uma hora de uso contínuo
| Item | Resultado | Anotações |
|---|---|---|
| O app rodou 1 hora sem fechar nem travar | | |
| O painel continuou fluido | | |
| A central não ficou lenta com o Maps aberto junto | | |
| O app reconectou sozinho quando precisou | | |

## Parte F: início com o sistema (opcional)
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ajustes, Iniciar com o sistema, ligar | | | |
| Desligar e ligar a central | A notificação do app aparece e a conexão volta, sem abrir o app | | |

---

## Resumo (preencha por último)
| Pergunta | Resposta |
|---|---|
| Quais sensores o carro respondeu? | |
| O que falhou? | |
| O que incomodou na interface? | |
| Que limites de condução ou de alertas você mudaria? | |
| Qual opção de mapa ou espelhamento funcionou? | |
