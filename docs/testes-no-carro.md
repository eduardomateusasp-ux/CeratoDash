# Roteiro de testes no carro

Este roteiro é para você fazer dentro do carro. Preencha as colunas "Resultado" (ok, falhou ou não testei) e "Anotações". Depois me mande o preenchido, ou só as linhas que falharam, para eu corrigir.

## Segurança
- **Não mexa no app dirigindo.** Faça os testes de interface com o carro parado, ou peça a um passageiro que anote.
- Os testes de condução (seção 9) só em local seguro, sem trânsito, e sem tirar a atenção da via.

## 0. Preparação
| Item | Resultado | Anotações |
|---|---|---|
| APK instalado na central (confira a versão 0.8.0 em Configurações, Apps) | | |
| Adaptador ELM327 ligado na porta OBD e pareado no Bluetooth da central | | |
| Car Scanner fechado no celular, e Bluetooth do celular desligado (o adaptador só aceita um aparelho) | | |
| Ignição ligada, motor ainda desligado | | |

## 1. Primeiro uso e escolha do adaptador
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Abrir o app | Tela do Painel, status "Desconectado: abra Ajustes e escolha o adaptador" | | |
| Adaptador clássico: Ajustes, Escolher entre pareados | Lista os aparelhos pareados | | |
| Adaptador BLE: Ajustes, Buscar adaptador (BLE), aceitar a localização | Seu adaptador aparece na lista (com nome e sinal) | | |
| Se a busca não achar: Digitar endereço, escolher Bluetooth LE | Conecta pelo endereço | | |
| Se aparecer erro de conexão, anote a mensagem inteira (código ou serviços) | | | |
| Escolher o OBDII | Status muda para "Conectando" e depois "Conectado" | | |
| Tempo até "Conectado" (cronometre, com a ignição ligada) | Até 15 s | ___ s | |
| Notificação do app aparece na central | Discreta, com botão Parar | | |

## 2. Sensores: o que o carro realmente responde
Com o motor ligado e em marcha lenta, abra Telemetria. Só aparecem os sensores que o carro respondeu. Marque o que apareceu e anote o valor.

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

Importante: se um sensor não apareceu, é porque o carro não respondeu. Isso é esperado, não é erro. Os valores do modo demonstração do Car Scanner não valem como comparação.

Compare 2 ou 3 valores (água, rotação, tensão) com o que o painel do carro mostra:

| Comparação | Painel do carro | App | Resultado |
|---|---|---|---|
| Temperatura da água | | | |
| Rotação em marcha lenta | | | |
| Velocidade (carro parado e depois andando devagar) | | | |

## 3. Consumo
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| O carro respondeu o fluxo de ar (MAF)? | Se não, o consumo mostra "--" | | |
| Combustível em Ajustes igual ao que está no tanque (gasolina ou etanol) | | | |
| Consumo (km/L) com o carro andando, comparado com a sua expectativa | Valor plausível, é uma estimativa | | |

## 4. Reconexão
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Desligar o motor e a ignição, esperar 30 s, ligar de novo | O app reconecta sozinho, sem você tocar | | |
| Tempo para voltar a "Conectado" | Até 15 s | ___ s | |
| Tirar o adaptador da porta OBD por 10 s e recolocar | Status mostra erro e depois reconecta | | |
| Desligar o Bluetooth da central e ligar de novo | Mostra "Bluetooth desligado" e reconecta | | |

## 5. Alertas e voz
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ajustes, Voz, Testar voz | Fala "Voz do CeratoDash funcionando." Se não, mostra o motivo | | |
| Ligar Voz nos alertas | | | |
| Reduzir o limite de rotação em Ajustes para um valor baixo e acelerar um pouco, parado | Faixa vermelha com texto e ícone, e a voz fala "Perigo. Rotação acima do limite" | | |
| Voltar o limite ao normal | | | |
| A voz repete a cada 30 s enquanto o alerta continua | | | |
| Volume da música abaixa enquanto a voz fala | | | |

## 6. Painel e pontos interativos
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Conta-giros e velocímetro se movem de forma suave | Sem travar | | |
| Barra de rotação acende e pisca perto do limite | | | |
| Números legíveis a distância (do banco do motorista, de dia e com sol) | | | |
| Tocar no anel do Motor | Abre o painel com os dados | | |
| Tocar na Bateria e no Escapamento | Abrem os painéis | | |
| Os anéis ficam no lugar certo sobre o carro da foto | | | |
| Fechar o painel | | | |

## 7. Diagnóstico
Anote os códigos ANTES de limpar qualquer coisa.

| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ler códigos | Mostra a luz de injeção e os códigos (ou "Nenhum código") | | |
| Códigos lidos (anote todos) | | | |
| Os códigos genéricos têm descrição, os outros dizem "sem descrição" | | | |
| Limpar códigos (só se quiser): aparece confirmação antes | | | |

## 8. Viagens
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ligar o motor e esperar 5 s | Aparece "Viagem em andamento" em Viagens | | |
| Andar um trajeto conhecido. Anote o odômetro antes e depois | | km antes ___ / depois ___ | |
| Distância mostrada pelo app | Próxima da do odômetro | ___ km | |
| Desligar o motor e esperar 30 s | A viagem entra no histórico | | |
| Duração, média, máxima, consumo e nota plausíveis | | | |
| Apagar uma viagem (com confirmação) | | | |

## 9. Condução (em local seguro)
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Acelerar de forma suave | Sem dicas, nota alta | | |
| Acelerar forte uma vez | Dica "Acelerou forte: alivie o pedal" por alguns segundos | | |
| Frear forte uma vez | Dica de freada | | |
| Ligar Voz nas dicas e repetir | A dica é falada, sem tagarelar | | |
| Estilo Econômica, Equilibrada, Esportiva: a mesma condução gera dicas diferentes | | | |
| Os limites parecem razoáveis para o seu jeito de dirigir? | Anote o que ajustar | | |

## 10. Maps, Waze e faixa sobre o mapa
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Mapas, Google Maps | Abre o Maps (ou avisa que não está instalado) | | |
| Mapas, Waze | Abre o Waze (precisa de internet) | | |
| Voltar ao CeratoDash: a leitura continuou | Dados continuam atualizando | | |
| Ajustes, Faixa sobre o mapa, ligar | Pede a permissão Sobrepor outros apps, ou avisa que a central não permite | | |
| Com a permissão, abrir o Maps | Faixa pequena com rotação, velocidade e água, que não atrapalha o toque no mapa | | |

## 11. Uma hora de uso
| Item | Resultado | Anotações |
|---|---|---|
| O app rodou 1 hora sem fechar nem travar | | |
| O painel continuou fluido (sem engasgos) | | |
| A central não ficou lenta com o Maps aberto junto | | |
| O app voltou a conectar sozinho quando necessário | | |

## 12. Início com o sistema (opcional)
| Passo | Esperado | Resultado | Anotações |
|---|---|---|---|
| Ajustes, Iniciar com o sistema, ligar | | | |
| Desligar e ligar a central | A notificação do app aparece e a conexão volta, sem abrir o app | | |
| Abrir o app em seguida | Painel já com dados | | |

## Resumo
| Pergunta | Resposta |
|---|---|
| Quais sensores o carro respondeu? | |
| O que falhou? | |
| O que incomodou na interface? | |
| Que limites da condução ou dos alertas você mudaria? | |
