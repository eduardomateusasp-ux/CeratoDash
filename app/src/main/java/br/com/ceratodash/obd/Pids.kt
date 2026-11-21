package br.com.ceratodash.obd

import br.com.ceratodash.data.*
import br.com.ceratodash.obd.*
import br.com.ceratodash.service.*
import br.com.ceratodash.ui.*

/**
 * Um sensor OBD-II padrão (modo 01): código, nº de bytes da resposta, fórmula e onde guardar.
 * Fórmulas conforme SAE J1979 (tabela de PIDs do modo 01, também em ISO 15031-5).
 * `apply` recebe null quando o carro deixou de responder, para o painel mostrar "--".
 */
class Pid(
    val code: String,
    val bytes: Int,
    val decode: (IntArray) -> Float,
    val apply: (Telemetry, Float?) -> Telemetry
)

object Pids {
    val all: List<Pid> = listOf(
        Pid("0C", 2, { (it[0] * 256 + it[1]) / 4f }, { t, v -> t.copy(rpm = v) }),
        Pid("0D", 1, { it[0].toFloat() }, { t, v -> t.copy(speed = v) }),
        Pid("05", 1, { it[0] - 40f }, { t, v -> t.copy(coolant = v) }),
        Pid("0F", 1, { it[0] - 40f }, { t, v -> t.copy(intake = v) }),
        Pid("04", 1, { it[0] * 100f / 255f }, { t, v -> t.copy(load = v) }),
        Pid("11", 1, { it[0] * 100f / 255f }, { t, v -> t.copy(throttle = v) }),
        Pid("0B", 1, { it[0].toFloat() }, { t, v -> t.copy(map = v) }),
        Pid("10", 2, { (it[0] * 256 + it[1]) / 100f }, { t, v -> t.copy(maf = v) }),
        Pid("0E", 1, { it[0] / 2f - 64f }, { t, v -> t.copy(timing = v) }),
        Pid("06", 1, { it[0] * 100f / 128f - 100f }, { t, v -> t.copy(stft = v) }),
        Pid("07", 1, { it[0] * 100f / 128f - 100f }, { t, v -> t.copy(ltft = v) }),
        Pid("2F", 1, { it[0] * 100f / 255f }, { t, v -> t.copy(fuelLevel = v) }),
        Pid("46", 1, { it[0] - 40f }, { t, v -> t.copy(ambient = v) }),
        Pid("42", 2, { (it[0] * 256 + it[1]) / 1000f }, { t, v -> t.copy(voltage = v) }),
        Pid("5C", 1, { it[0] - 40f }, { t, v -> t.copy(oilTemp = v) })
    )

    fun byCode(code: String): Pid? = all.firstOrNull { it.code == code }
}
