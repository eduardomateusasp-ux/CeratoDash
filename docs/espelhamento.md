# Mapas e espelhamento do iPhone na central

Situação: central Android sem CarPlay, com iPhone. O CeratoDash **não tem espelhamento próprio**, e não terá: um app que capture a tela de um iPhone e envie para a central exigiria um app iOS, o que não dá para construir neste projeto. Os caminhos abaixo usam recursos que já existem.

Nada aqui foi testado na sua central. As fontes são páginas de projetos e lojas, não testes independentes.

## Caminho recomendado: mapa offline na própria central (sem espelhar nada)
Se o que você quer é só usar mapas na central, o mais simples é instalar nela um app de navegação **offline**, com GPS da própria central. Não precisa de espelhamento nem de Waze ou Google Maps.

- **Organic Maps** (`app.organicmaps`): aberto, leve, navegação por voz e busca 100% offline, com mapas do OpenStreetMap. Pede Android 5.0 ou mais novo. Está no [F-Droid](https://f-droid.org/packages/app.organicmaps/), que permite baixar o APK direto (instale por pendrive, como o CeratoDash). Sem Play Store, tudo bem.
- **OsmAnd**: também offline e completo, mas a versão gratuita limita a quantidade de mapas baixados (as fontes que achei divergem sobre o limite; confira no próprio app). Não conferi o pacote nem os requisitos da versão para central.

O que você precisa:
1. **GPS na central.** Sem GPS não há posição, e nenhum app de navegação funciona direito. O Diagnóstico da central (Ajustes) mostra se ela declara ter GPS. Atenção: muitas centrais só têm GPS se uma antena estiver ligada, e o Diagnóstico não consegue saber isso.
2. **Internet uma vez, para baixar os mapas** do seu estado ou região (não precisa do país todo, que é grande). Use o ponto de acesso do iPhone, se a central tiver Wi-Fi. Depois disso, o mapa funciona sem internet.
3. Instalar o APK pelo pendrive.

O CeratoDash já tem atalhos para esses apps na tela Mapas, e a faixa de rotação, velocidade e água pode aparecer sobre eles (se a central permitir sobreposição).

Não testado na sua central: peso na memória de 1 GB, qualidade do GPS e a voz em português (a voz usa o mecanismo do Android, o mesmo da voz dos alertas).

## Se a central não tiver GPS
Há soluções como um receptor GPS Bluetooth ou USB, mas elas dependem de a central aceitar localização simulada de outro app. **Eu não verifiquei isso**, e o iPhone não consegue servir de GPS para uma central Android. Veja o resultado do Diagnóstico antes de pensar nisso.

## Qual caminho escolher
Depois de rodar o **Diagnóstico da central** (Ajustes) e o **Ver USB**:

| Se o diagnóstico mostrar | Tente |
|---|---|
| GPS sim | **D0** primeiro (Organic Maps offline na central): resolve o seu objetivo sem espelhar |
| Wi-Fi sim, Play Store sim | **D1** (Maps ou Waze na central), precisa de internet sempre |
| Wi-Fi sim | **D2** (receptor AirPlay) |
| USB anfitrião sim | **D3** (adaptador de CarPlay), só depois de D1 e D2 |
| GPS não | Mapas na central dependem de uma solução de GPS (veja acima); sobra o celular no suporte, usando o próprio GPS |
| Wi-Fi não e USB anfitrião não | Nenhum dos três serve |

## D1. Maps ou Waze direto na central
- Instale o Google Maps ou o Waze na central (Play Store, ou o APK se não houver loja).
- Internet: o ponto de acesso (Wi-Fi) do iPhone, com a central conectada nele.
- Precisa de GPS na central para a posição ficar boa. O Diagnóstico mostra se ela declara ter GPS.
- O Google Maps funciona offline se você baixar as áreas antes (com internet). O Waze precisa de internet.
- O CeratoDash já tem atalhos (Mapas) e a faixa sobre o mapa.

## D2. Receptor AirPlay (espelhar a tela do iPhone)
- Existe um receptor AirPlay de código aberto para Android: [android-airplay-server](https://github.com/jqssun/android-airplay-server), também no [F-Droid](https://f-droid.org/en/packages/io.github.jqssun.airplay/). Requer Android 7 ou mais novo e suporta espelhamento. Conteúdo com DRM não aparece.
- No iPhone: Central de Controle, Espelhamento de Tela, e escolha a central.
- iPhone e central precisam estar na mesma rede. **Não verifiquei** se o iPhone enxerga o receptor quando a rede é o ponto de acesso dele mesmo.
- Só vê a tela: a central não controla o iPhone. O iPhone fica ligado, com a tela acesa.
- Cuidado: notificações do iPhone aparecem na tela da central. Use o modo Não perturbe.
- Não sei como o app se comporta na central de 1 GB.

## D3. Adaptador de CarPlay para central Android
- Existe um adaptador USB (marcas como Carlinkit) que funciona com um app na central (ZLink ou um APK do fabricante) e dá CarPlay de verdade, com toque e voz. Veja [CarTech Studio: CarPlay em central Android](https://cartechstudio.com/blogs/apple-carplay/apple-carplay-for-android-head-unit).
- Risco: as fontes dizem que depende da porta USB e do firmware da central. APK de fórum feito para outro modelo pode quebrar o app. Há relatos de instabilidade sem fio ([XDA](https://forum.xda-developers.com/t/zlink-help.4274979/post-85333975)).
- Custa dinheiro. Não compre antes de D1 e D2, e antes de conferir a compatibilidade com o vendedor.

## O que NÃO vale para iPhone
Headunit Revived, Android Auto e espelhamento por USB estilo scrcpy são só para celular Android.

## Se um dia você trocar para Android
O [Headunit Revived](https://github.com/andreknieriem/headunit-revived) transforma uma central Android com USB anfitrião num receptor de Android Auto, pelo cabo, com Maps e Waze com toque. O teste **Ver USB** já diz se a porta serve.
