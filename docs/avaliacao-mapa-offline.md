# Avaliação: mapa offline embutido (MapLibre com OpenStreetMap)

Pedido do PRD: avaliar antes e dizer se cabe na memória da central. Esta avaliação é por escrito. Nada foi implementado nem medido na central.

## Conclusão
**Não recomendo embutir o mapa agora.** Ele traria só um mapa, sem rota, e o Google Maps offline já resolve a navegação. O custo (peso do app, memória, tiles) é alto para um ganho pequeno.

## Motivos
1. **Só mapa, sem navegação.** MapLibre desenha o mapa. Rota, busca de endereço e avisos de curva são outros componentes que o app não tem. O Google Maps já faz tudo isso, e funciona offline se as áreas forem baixadas antes.
2. **Fonte dos tiles.** O servidor público de tiles do OpenStreetMap tem uma política de uso que desencoraja ou proíbe o download em massa para uso offline (leia a política atual antes de decidir). Para ter mapa offline seria preciso outra fonte de tiles vetoriais, com licença que permita baixar, e uma forma de guardar a região (provavelmente algumas centenas de MB).
3. **Peso e memória (não medidos).** O MapLibre Native Android traz bibliotecas nativas e desenha com OpenGL. Isso aumenta o APK e o uso de RAM. A central tem 1 GB, e o Android mais o serviço de leitura já ocupam boa parte. Sem medir na central, não dá para afirmar que cabe.
4. **GPU (não verificado).** O MT8163 usa uma GPU Mali. Eu não verifiquei qual versão de OpenGL ES ela entrega nem se atende ao que o MapLibre exige.
5. **Localização (não verificado).** Não sei se a central tem GPS. O app hoje não pede permissão de localização, e sem GPS o mapa não mostraria a posição do carro.

## O que seria preciso medir se quiser avançar
| Item | Como medir |
|---|---|
| RAM livre com o CeratoDash rodando | Configurações, Aplicativos, Memória (ou `adb shell dumpsys meminfo`) |
| Versão de OpenGL ES da central | Um app de informações do aparelho |
| Existência de GPS | Um app de teste de GPS |
| Espaço livre para os tiles | Armazenamento da central |
| Fonte de tiles com licença adequada | Decisão sua, antes de qualquer código |

## O que está pronto no lugar
- Atalhos para o Google Maps e o Waze (aviso se não estiverem instalados).
- Faixa reduzida (rotação, velocidade e água) sobre o mapa, quando a central permitir a sobreposição. Desligada por padrão; não testada na central.
- Leitura contínua em segundo plano pelo serviço em primeiro plano.

Reavaliar depois dos testes no carro, se sobrar memória e se a central tiver GPS.
