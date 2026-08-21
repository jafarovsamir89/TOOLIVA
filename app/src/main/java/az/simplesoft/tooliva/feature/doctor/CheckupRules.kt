package az.simplesoft.tooliva.feature.doctor

import android.os.PowerManager
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot

internal fun checkupAttentionItems(snapshot: PhoneDoctorSnapshot, failedHardwareTests: Int): List<String> = buildList {
    if (snapshot.memory?.lowMemory == true) add("Memory pressure is high")
    if (snapshot.thermal.status != null && snapshot.thermal.status != PowerManager.THERMAL_STATUS_NONE) {
        add("Thermal status: ${snapshot.thermal.label}")
    }
    if (snapshot.battery.health in setOf("Overheat", "Dead", "Over voltage", "Unspecified failure")) {
        add("Android reports a battery health warning")
    }
    if (failedHardwareTests > 0) {
        add("$failedHardwareTests hardware test${if (failedHardwareTests == 1) "" else "s"} reported a problem")
    }
}
