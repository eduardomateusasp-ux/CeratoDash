# CeratoDash: requisitos do produto (PRD)

Versão do app: 0.9.1. Atualizado em 22/11/2026.

**Regra de leitura:** "Feito" quer dizer implementado e, quando possível, coberto por testes automáticos com um ELM327 simulado. **Só um teste no carro foi feito (relato do dono, sem detalhes):** o app conectou por Bluetooth clássico, respondeu bem, ficou leve, sem travar, e mostrou informações corretas. Todo o resto que depende do carro ou da central está marcado como não testado.

## 1. Produto
Painel de telemetria OBDII, esportivo e interativo, para a central multimídia Android do Kia Cerato 2010/2011 SX2 1.6 automático.

- Central: Android 12, MediaTek MT8163, 1 GB de RAM, paisagem, resolução desconhecida (o app usa dp e sp). Entrada USB.
- Adaptador: ELM327 Bluetooth, um aparelho por vez.
- Instalação: APK por pendrive. Sem loja de apps.
- Stack: Kotlin nativo, Views e Canvas, coroutines, SQLite local, sem backend, sem internet.
- Princípios: leve (30 fps no máximo, sem bibliotecas pesadas), 100% offline, nunca inventar dados (sem resposta do carro vira "--"), interface em português do Brasil.

## 2. Requisitos funcionais

### 2.1 Conexão OBDII
| Requisito | Estado | Observação |
|---|---|---|
| Escolher o adaptador (entre os pareados, por busca BLE ou por endereço) e guardar | Feito | Testado no carro: conectou (adaptador clássico pareado pela central) |
| Inicializar o ELM327 (ATZ, ATE0, ATL0, ATS0, ATH0, ATSP0) e descobrir os PIDs | Feito | Testado só com adaptador simulado |
| Reconexão automática, status sempre visível, mensagens claras | Feito | Tenta de novo a cada 3 s. Não testado no carro |
| Tolerar quedas; tratar NO DATA, UNABLE TO CONNECT, SEARCHING, BUS INIT e várias ECUs | Feito | Também CAN ERROR, BUS BUSY, LV RESET e STOPPED. Testado só em simulação |
| Bluetooth clássico (SPP) e BLE | Feito | BLE com busca dentro do app, ou endereço digitado, sem exigir pareamento. Nunca testado em adaptador real. Depende de a central ter Bluetooth LE |
| Serviço em primeiro plano, com notificação discreta | Feito | Em uso no primeiro teste; a notificação não foi conferida em detalhe |
| Iniciar com o sistema (BOOT_COMPLETED), opção desligada por padrão | Feito | Só o serviço sobe; a tela precisa ser aberta. Não testado |

### 2.2 Dados em tempo real (somente OBD-II padrão, só o que o carro responder)
| Requisito | Estado | Observação |
|---|---|---|
| Rotação, velocidade, água, admissão, carga, borboleta, coletor, MAF, avanço, correções curta e longa, combustível, ambiente, tensão da ECU, óleo, tensão do adaptador | Feito | Fórmulas SAE J1979, testadas. No carro, o dono relata informações corretas (quais sensores o carro responde ainda não foi listado) |
| Sensores extras: oxigênio antes e depois do catalisador (14, 15) e temperatura do catalisador (3C) | Feito | Para o ponto do escapamento. SAE J1979 |
| Consumo estimado pelo MAF (L/h e km/L), gasolina ou etanol | Feito | Sem MAF mostra "--" |
| Rotação e velocidade em todo ciclo; demais em rodízio | Feito | Dois sensores lentos por ciclo (um se o adaptador for lento) |
| Atualizar de 2 a 5 vezes por segundo | Previsto | Não medido |

### 2.3 Telas
| Tela | Estado | Observação |
|---|---|---|
| Painel: conta-giros (0 a 8000, vermelho a partir de 6500), velocímetro (0 a 240), carro, indicadores, status | Feito | Barra de rotação em segmentos que pisca perto do limite; cartões com mini gráfico |
| Telemetria: gráficos dos últimos 60 s, pico e mínimo | Feito | Até 6 sensores; só os que o carro respondeu |
| Diagnóstico: ler códigos (modo 03), luz de injeção, limpar (modo 04) com confirmação | Feito | Descrição só para cerca de 85 códigos genéricos P0xxx (SAE J2012, tradução própria). Não testado no carro |
| Viagens: início e fim automáticos, distância, tempo, média, máxima, consumo, histórico, apagar | Feito | Distância pela velocidade do OBD (sem GPS) |
| Mapas: atalhos para Google Maps e Waze, faixa reduzida sobre o app | Feito | Faixa opcional, depende da permissão de sobreposição. Não testada |
| Ajustes: adaptador, combustível, limites de alerta, iniciar com o sistema, foto do carro | Feito | Unidades: só km/h, °C e km/L, por decisão do dono (sem mph, °F ou L/100 km) |

### 2.4 Alertas
| Requisito | Estado |
|---|---|
| Água: atenção 8 °C abaixo do limite crítico (padrão 100 °C), perigo no crítico (padrão 108 °C, ajustável) | Feito |
| Tensão abaixo de 11,5 V ou acima de 15,2 V | Feito |
| Rotação acima do limite configurado | Feito |
| Visuais, com ícone e texto, sem som obrigatório | Feito |
| Voz opcional nos alertas (desligada por padrão) | Feito. Precisa de voz em português na central; não testado |

### 2.5 Mapas e GPS
| Requisito | Estado | Observação |
|---|---|---|
| Atalhos (Organic Maps, OsmAnd, Google Maps, Waze); aviso se o app não estiver instalado | Feito | Organic Maps e OsmAnd navegam offline, mas precisam de GPS na central. Não testado |
| Google Maps offline com áreas baixadas; Waze precisa de internet | Informado na tela de Mapas | |
| Mapa embutido com OpenStreetMap offline (MapLibre) | Avaliado, não implementado | Ver `docs/avaliacao-mapa-offline.md`: não recomendado agora |

### 2.6 Foto do carro
| Requisito | Estado | Observação |
|---|---|---|
| Usar a foto do projeto quando existir; senão, a silhueta | Feito | Foto padrão: Cerato 1.6 SX 2010 (Wikimedia Commons, CC BY-SA 2.0) |
| Escolher outra foto nos Ajustes | Feito | Fica só no aparelho. Depende de a central ter um seletor de arquivos |
| Pontos interativos (motor, bateria, escapamento) que abrem o dado | Feito | Posição aproximada; não testada na foto real |

### 2.7 Extras além do pedido inicial
| Recurso | Estado |
|---|---|
| Treinador de condução em tempo real (8 itens, 3 estilos, nota da viagem) | Feito. Limites são valores gerais de direção econômica, não da Kia; precisam de calibração |
| Voz nas dicas de condução (opcional) | Feito |
| Diagnóstico da central (Wi-Fi, GPS, BLE, USB, apps), Ver USB e registro da conexão | Feito. Servem para decidir mapas e espelhamento e para diagnosticar falhas. Não testados |
| Espelhar o celular na central | Não feito. O celular é iPhone, então não há app próprio possível. Caminhos em `docs/espelhamento.md` (Maps ou Waze na central, receptor AirPlay, adaptador de CarPlay) |

## 3. Requisitos não funcionais
| Requisito | Estado | Observação |
|---|---|---|
| Atualizar sensores de 2 a 5 vezes por segundo | Não medido | |
| Conectar em até 15 s e reconectar sozinho | Não medido | |
| Tela sempre ligada com o painel aberto, paisagem, barra de navegação visível | Feito | |
| Rodar 1 hora sem travar nem fechar | Não testado | |
| Dados de viagem só no aparelho; sem telemetria nem envio para fora | Feito | |
| App leve e sem travar | Relato do dono no primeiro teste | Sem medição de memória nem de quadros |
| Leve: 30 fps no máximo, sem imagens grandes | Feito | Foto reduzida a 1200 px de largura; fontes embutidas (cerca de 1,6 MB) |

## 4. Design system
Ver `docs/design-system.md`. Pendências: o vermelho de perigo tem contraste de 5,2:1 (meta de 7:1); os botões de escolha de sensor têm 48 dp (meta de 64 dp).

## 5. Arquitetura
Pacotes: `obd` (conexão, protocolo ELM327, PIDs, gerenciador), `data` (preferências, viagens, alertas, condução, voz, banco), `service` (serviço em primeiro plano, voz, faixa sobre o mapa, início com o sistema), `ui` (telas e componentes).
Camadas: Bluetooth, protocolo ELM327 e PIDs, dados ao vivo (StateFlow), interface. O serviço em primeiro plano é dono da conexão; a Activity só observa. Banco com `SQLiteOpenHelper` (Room não foi usado, para manter o app leve).

## 6. Qualidade
103 testes unitários (rodam no GitHub Actions a cada envio). Eles usam um ELM327 simulado: provam a lógica, não o carro. O roteiro de testes no carro está em `docs/testes-no-carro.md`.

## 7. Distribuição e assinatura
APK gerado pelo GitHub Actions (`gradle assembleRelease`). Assinatura com keystore própria preparada pelos Secrets (ver `docs/assinatura-do-apk.md`); sem os Secrets, o APK sai com a chave de debug. O caminho com keystore própria ainda não foi exercitado.
