package br.com.ceratodash

data class Telemetry(
    val rpm: Float? = null,
    val speed: Float? = null,
    val coolant: Float? = null,
    val intake: Float? = null,
    val load: Float? = null,
    val throttle: Float? = null,
    val map: Float? = null,
    val maf: Float? = null,
    val timing: Float? = null,
    val stft: Float? = null,
    val ltft: Float? = null,
    val fuelLevel: Float? = null,
    val ambient: Float? = null,
    val voltage: Float? = null,
    val oilTemp: Float? = null,
    val litersPerHour: Float? = null,
    val kmPerLiter: Float? = null
)

enum class ConnState { DESCONECTADO, CONECTANDO, INICIALIZANDO, CONECTADO, ERRO }

data class ObdStatus(
    val state: ConnState = ConnState.DESCONECTADO,
    val message: String = ""
)

/** Parâmetros para estimar consumo a partir do fluxo de ar (MAF). São valores aproximados. */
enum class Fuel(val label: String, val afr: Float, val density: Float) {
    GASOLINA("Gasolina", 13.5f, 750f),
    ETANOL("Etanol", 9.0f, 790f)
}
