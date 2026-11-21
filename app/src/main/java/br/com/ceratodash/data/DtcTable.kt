package br.com.ceratodash.data

/**
 * Descrições dos códigos de falha GENÉRICOS (P0xxx).
 *
 * Fonte: definições genéricas da norma SAE J2012 (igual a ISO 15031-6). Esta é uma seleção dos
 * códigos mais comuns, não a tabela completa. O texto em português é tradução própria do título
 * em inglês da norma. Códigos fora da tabela, e todos os específicos do fabricante (P1xxx, P2xxx
 * e outros), ficam SEM descrição: o app não inventa significado.
 */
object DtcTable {
    private val table: Map<String, String> = """
P0100|Circuito do fluxo de ar (MAF) com defeito
P0101|Fluxo de ar (MAF): faixa ou desempenho
P0102|Fluxo de ar (MAF): sinal baixo
P0103|Fluxo de ar (MAF): sinal alto
P0104|Fluxo de ar (MAF): intermitente
P0105|Circuito da pressão do coletor (MAP) ou barométrica com defeito
P0106|Pressão do coletor (MAP): faixa ou desempenho
P0107|Pressão do coletor (MAP): sinal baixo
P0108|Pressão do coletor (MAP): sinal alto
P0109|Pressão do coletor (MAP): intermitente
P0110|Circuito da temperatura do ar de admissão com defeito
P0111|Temperatura do ar de admissão: faixa ou desempenho
P0112|Temperatura do ar de admissão: sinal baixo
P0113|Temperatura do ar de admissão: sinal alto
P0114|Temperatura do ar de admissão: intermitente
P0115|Circuito da temperatura do líquido de arrefecimento com defeito
P0116|Temperatura do líquido de arrefecimento: faixa ou desempenho
P0117|Temperatura do líquido de arrefecimento: sinal baixo
P0118|Temperatura do líquido de arrefecimento: sinal alto
P0119|Temperatura do líquido de arrefecimento: intermitente
P0120|Circuito A do sensor de posição da borboleta ou do pedal com defeito
P0121|Sensor A da borboleta ou do pedal: faixa ou desempenho
P0122|Sensor A da borboleta ou do pedal: sinal baixo
P0123|Sensor A da borboleta ou do pedal: sinal alto
P0124|Sensor A da borboleta ou do pedal: intermitente
P0125|Temperatura insuficiente do líquido para controle de combustível em malha fechada
P0128|Termostato: temperatura do líquido abaixo da temperatura de regulagem
P0130|Circuito do sensor de oxigênio com defeito (banco 1, sensor 1)
P0131|Sensor de oxigênio: tensão baixa (banco 1, sensor 1)
P0132|Sensor de oxigênio: tensão alta (banco 1, sensor 1)
P0133|Sensor de oxigênio: resposta lenta (banco 1, sensor 1)
P0134|Sensor de oxigênio: sem atividade detectada (banco 1, sensor 1)
P0135|Aquecedor do sensor de oxigênio com defeito (banco 1, sensor 1)
P0136|Circuito do sensor de oxigênio com defeito (banco 1, sensor 2)
P0137|Sensor de oxigênio: tensão baixa (banco 1, sensor 2)
P0138|Sensor de oxigênio: tensão alta (banco 1, sensor 2)
P0139|Sensor de oxigênio: resposta lenta (banco 1, sensor 2)
P0140|Sensor de oxigênio: sem atividade detectada (banco 1, sensor 2)
P0141|Aquecedor do sensor de oxigênio com defeito (banco 1, sensor 2)
P0171|Sistema pobre demais (banco 1)
P0172|Sistema rico demais (banco 1)
P0174|Sistema pobre demais (banco 2)
P0175|Sistema rico demais (banco 2)
P0200|Circuito do injetor com defeito
P0201|Circuito do injetor com defeito (cilindro 1)
P0202|Circuito do injetor com defeito (cilindro 2)
P0203|Circuito do injetor com defeito (cilindro 3)
P0204|Circuito do injetor com defeito (cilindro 4)
P0300|Falha de ignição aleatória ou em vários cilindros detectada
P0301|Falha de ignição detectada no cilindro 1
P0302|Falha de ignição detectada no cilindro 2
P0303|Falha de ignição detectada no cilindro 3
P0304|Falha de ignição detectada no cilindro 4
P0305|Falha de ignição detectada no cilindro 5
P0306|Falha de ignição detectada no cilindro 6
P0325|Circuito do sensor de detonação 1 com defeito
P0335|Circuito A do sensor de posição do virabrequim com defeito
P0336|Sensor A do virabrequim: faixa ou desempenho
P0337|Sensor A do virabrequim: sinal baixo
P0338|Sensor A do virabrequim: sinal alto
P0339|Sensor A do virabrequim: intermitente
P0340|Circuito do sensor de posição do comando de válvulas com defeito
P0341|Sensor do comando de válvulas: faixa ou desempenho
P0420|Eficiência do catalisador abaixo do limite (banco 1)
P0430|Eficiência do catalisador abaixo do limite (banco 2)
P0440|Sistema de controle de emissões evaporativas com defeito
P0441|Fluxo de purga incorreto
P0442|Vazamento pequeno detectado no sistema evaporativo
P0443|Circuito da válvula de purga com defeito
P0446|Circuito da ventilação do sistema evaporativo com defeito
P0455|Vazamento grande detectado no sistema evaporativo
P0456|Vazamento muito pequeno detectado no sistema evaporativo
P0500|Sensor de velocidade do veículo com defeito
P0505|Sistema de controle da marcha lenta com defeito
P0506|Rotação da marcha lenta menor que a esperada
P0507|Rotação da marcha lenta maior que a esperada
P0560|Tensão do sistema com defeito
P0562|Tensão do sistema baixa
P0563|Tensão do sistema alta
P0601|Erro de soma de verificação da memória interna do módulo de controle
P0700|Sistema de controle da transmissão com defeito
P0705|Circuito do sensor de faixa da transmissão com defeito
P0720|Circuito do sensor de velocidade de saída com defeito
P0730|Relação de marcha incorreta
""".trimIndent().lines().filter { it.contains('|') }.associate {
        val (code, text) = it.split('|', limit = 2)
        code to text
    }

    /** Descrição da tabela documentada, ou null se o código não estiver nela. */
    fun describe(code: String): String? = table[code.uppercase()]

    val size: Int get() = table.size
}
