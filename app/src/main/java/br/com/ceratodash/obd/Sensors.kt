package br.com.ceratodash.obd

/** Um valor mostrável da telemetria (rótulo, unidade e como ler da Telemetry). */
class Sensor(
    val key: String,
    val label: String,
    val unit: String,
    val decimals: Int,
    val read: (Telemetry) -> Float?
)

object Sensors {
    val all: List<Sensor> = listOf(
        Sensor("rpm", "Rotação", "rpm", 0) { it.rpm },
        Sensor("speed", "Velocidade", "km/h", 0) { it.speed },
        Sensor("coolant", "Água", "°C", 0) { it.coolant },
        Sensor("intake", "Admissão", "°C", 0) { it.intake },
        Sensor("load", "Carga do motor", "%", 0) { it.load },
        Sensor("throttle", "Borboleta", "%", 0) { it.throttle },
        Sensor("map", "Pressão do coletor", "kPa", 0) { it.map },
        Sensor("maf", "Fluxo de ar", "g/s", 1) { it.maf },
        Sensor("timing", "Avanço de ignição", "°", 1) { it.timing },
        Sensor("stft", "Correção curta", "%", 1) { it.stft },
        Sensor("ltft", "Correção longa", "%", 1) { it.ltft },
        Sensor("fuelLevel", "Nível de combustível", "%", 0) { it.fuelLevel },
        Sensor("ambient", "Temperatura ambiente", "°C", 0) { it.ambient },
        Sensor("voltage", "Tensão", "V", 1) { it.voltage },
        Sensor("oilTemp", "Temperatura do óleo", "°C", 0) { it.oilTemp },
        Sensor("o2b1s1", "Oxigênio antes do catalisador", "V", 2) { it.o2b1s1 },
        Sensor("o2b1s2", "Oxigênio depois do catalisador", "V", 2) { it.o2b1s2 },
        Sensor("catTemp", "Temperatura do catalisador", "°C", 0) { it.catTemp },
        Sensor("lph", "Consumo", "L/h", 1) { it.litersPerHour },
        Sensor("kml", "Consumo", "km/L", 1) { it.kmPerLiter }
    )

    fun byKey(key: String): Sensor? = all.firstOrNull { it.key == key }
}

/**
 * Histórico dos últimos `windowSec` segundos de cada sensor, amostrado a cada `periodMs`,
 * mais o pico e o mínimo da sessão. Escrito pelo serviço, lido pela tela (por isso sincronizado).
 */
class History(val windowSec: Int = 60, private val periodMs: Long = 500) {
    val size: Int = (windowSec * 1000L / periodMs).toInt()

    private class Series(size: Int) {
        val v = FloatArray(size) { Float.NaN }
        var next = 0
        var count = 0
        var peak = Float.NaN
        var min = Float.NaN
        var seen = false
    }

    private val series = HashMap<String, Series>()
    private var lastMs = Long.MIN_VALUE

    /** Registra uma amostra se já passou o período. Devolve true se registrou. */
    @Synchronized
    fun add(nowMs: Long, t: Telemetry): Boolean {
        if (lastMs != Long.MIN_VALUE && nowMs - lastMs < periodMs) return false
        lastMs = nowMs
        for (s in Sensors.all) {
            val x = s.read(t)
            val sr = series.getOrPut(s.key) { Series(size) }
            sr.v[sr.next] = x ?: Float.NaN
            sr.next = (sr.next + 1) % size
            if (sr.count < size) sr.count++
            if (x != null) {
                sr.seen = true
                if (sr.peak.isNaN() || x > sr.peak) sr.peak = x
                if (sr.min.isNaN() || x < sr.min) sr.min = x
            }
        }
        return true
    }

    /** Zera tudo (nova sessão de conexão). */
    @Synchronized
    fun reset() {
        series.clear()
        lastMs = Long.MIN_VALUE
    }

    /** O carro já respondeu esse sensor nesta sessão? */
    @Synchronized
    fun hasData(key: String): Boolean = series[key]?.seen == true

    @Synchronized
    fun peak(key: String): Float? = series[key]?.peak?.takeIf { !it.isNaN() }

    @Synchronized
    fun min(key: String): Float? = series[key]?.min?.takeIf { !it.isNaN() }

    /** Copia os valores em ordem do mais antigo ao mais novo (NaN = sem dado). Devolve quantos. */
    @Synchronized
    fun copyInto(key: String, dst: FloatArray): Int {
        val sr = series[key] ?: return 0
        val n = minOf(sr.count, dst.size)
        val start = (sr.next - n + size) % size
        for (i in 0 until n) dst[i] = sr.v[(start + i) % size]
        return n
    }
}

/** Histórico único do app, alimentado pelo serviço. */
object LiveHistory {
    val history = History()
}
