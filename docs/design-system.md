# Design system do CeratoDash

Este documento descreve só o que está implementado no código. O que ainda falta aparece em "Pendências".

## Princípios
Legível a metros de distância, esportivo sem poluição, leve (1 GB de RAM) e seguro (alvos grandes, nada que exija atenção prolongada). Somente modo escuro, paisagem.

## Tokens de cor
Fonte única: `app/src/main/res/values/colors.xml`. O código lê esses valores pela classe `Theme` (`ui/Theme.kt`), iniciada em `CeratoApp.onCreate`.

| Token | Valor | Uso |
|---|---|---|
| bg | #0B0D10 | fundo |
| surface | #14181D | cartões, barra lateral |
| surface_high | #1C2229 | botões, trilho dos gauges |
| surface_pressed | #2A333D | botão pressionado |
| accent | #FF3B30 | destaque esportivo, zona vermelha |
| info | #00D1FF | dados, gráficos, seleção |
| ok | #2ED573 | estado normal |
| warn | #FFB020 | atenção |
| danger | #FF453A | perigo |
| text | #F2F5F7 | texto principal |
| text_muted | #8A94A0 | rótulos |

Contraste calculado pela fórmula WCAG (razão de luminância):

| Combinação | Razão | Meta de 7:1 |
|---|---|---|
| text sobre bg | 17,8 | sim |
| text sobre surface | 16,3 | sim |
| info sobre bg / surface | 10,7 / 9,8 | sim |
| ok sobre surface | 9,2 | sim |
| warn sobre surface | 9,7 | sim |
| danger sobre surface | 5,2 | **não** |
| text_muted sobre bg / surface | 6,3 / 5,8 | **não** (só rótulos) |
| texto escuro (bg) sobre a faixa amarela / vermelha | 10,6 / 5,7 | amarela sim, vermelha **não** |

Os números principais dos gauges usam `text` (passa). Mas o valor de um cartão em estado de perigo usa `danger` sobre `surface` (5,2:1), e o texto da faixa de perigo tem 5,7:1. Esses dois ficam abaixo dos 7:1 pedidos. Como o `danger` (#FF453A) é um token definido por você, não mudei a cor. Para atingir 7:1 seria preciso clareá-lo nesses usos.

## Tipografia
Fontes embutidas no APK (`res/font`), sem baixar da internet. Licença SIL OFL 1.1 (textos em `docs/licencas/`).

- Rajdhani SemiBold e Bold: números e rótulos do painel, títulos de tela.
- Inter Regular e Medium (v4.1): texto geral e rótulos.

Escala (em sp, definida em `Theme`): valor principal 96, secundário 40, rótulo 16, texto 18, mínimo 14. Nada abaixo de 14 sp. O valor de 96 sp dos gauges é reduzido automaticamente só se não couber dentro do arco (a resolução da central é desconhecida).

## Layout
Paisagem. Margens de 24 dp, cantos de 16 dp, espaçamentos múltiplos de 4 e 8 dp. Painel em três zonas: conta-giros à esquerda (gauges com raio de 15% da largura), carro e barra de rotação ao centro, velocímetro à direita, cartões de métrica embaixo. A foto do carro fica entre os gauges, sem sobreposição. Navegação lateral de 104 dp de largura com ícone e rótulo; cada item tem no mínimo 64 dp de altura.

## Componentes
| Componente | Onde | Observações |
|---|---|---|
| Gauge em arco de 240 graus | `DashboardView` | marcador do valor atual, zona vermelha, traços e textos pré-calculados |
| Barra de rotação em segmentos | `DashboardView` | 24 segmentos (azul, amarelo, vermelho); pisca a partir de 300 rpm abaixo do limite configurado |
| Cartão de métrica | `DashboardView` | rótulo, valor e mini gráfico dos últimos 60 s |
| Gráfico ao vivo | `ChartsView` (Telemetria) | 60 s, pico e mínimo da sessão |
| Indicador de status | `StatusIndicator` | ponto colorido mais texto |
| Faixa de alerta | `AlertBanner` | no topo de todas as telas; ícone mais texto, nunca só a cor |
| Botão grande | `bigButton` | mínimo de 56 dp; estados normal, pressionado (borda azul) e desabilitado; opção escolhida com borda azul |
| Navegação lateral | `MainActivity` | ícones vetoriais próprios (`ic_nav_*`); rola se a tela for baixa |
| Faixa de dica de condução | `CoachBanner` | abaixo da faixa de alerta; texto curto por 5 s |
| Painel de pontos do carro | `HotspotPanel` | abre ao tocar no anel de motor, bateria ou escapamento |
| Faixa sobre o mapa | `OverlayStrip` | rotação, velocidade e água sobre outros apps; opcional, sem toque |
| Foto do carro | `CarPhoto` | em cartão arredondado; escolhida em Ajustes ou a padrão do APK |

## Alertas
Regras em `data/Alerts.kt`, com testes. Visuais, sem som.

- Água: atenção a partir de 8 °C abaixo do limite crítico (padrão 100 °C), perigo a partir do crítico (padrão 108 °C, configurável de 100 a 120 °C).
- Tensão: perigo abaixo de 11,5 V ou acima de 15,2 V.
- Rotação: perigo acima do limite configurado (padrão 6500 rpm, de 4000 a 8000).
- Sensor sem resposta nunca gera alerta.

## Voz
Opcional e desligada por padrão. Usa o mecanismo de voz do Android (precisa de voz em português instalada). Alertas: uma vez por episódio; perigo repete a cada 30 s. Dicas de condução: no máximo uma a cada 8 s, sempre abaixo dos alertas em prioridade. O volume da música abaixa enquanto fala. A voz nunca substitui o alerta visual.

## Movimento
Ponteiro suave: cerca de 95% do caminho em 150 ms, independente da taxa de quadros. Transição entre telas: fade de 150 ms. Máximo de 30 quadros por segundo (`postInvalidateDelayed(33)`). Sem animação contínua fora dos gauges e da barra de rotação perto do limite.

## Pendências
- Alvos de toque: botões e itens de navegação seguem o mínimo, mas os chips de sensor da Telemetria têm 48 dp (abaixo dos 64 dp pedidos para a navegação).
- Unidades: só km/h, °C e km/L, por decisão do dono.
- A aparência foi verificada só pela compilação. Falta conferir contraste, tamanhos e legibilidade na tela real da central.
