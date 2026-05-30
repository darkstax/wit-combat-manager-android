package com.witcombat.manager.domain

import java.util.UUID

data class StatusEffect(
    val name: String,
    var stacks: Int = 0
)

data class GameUnit(
    val name: String = "",
    val unitType: String = "player",
    var currentHp: Int = 10,
    val maxHp: Int = 10,
    val speed: Int = 10,
    val physicalResist: Int = 0,
    val magicResist: Int = 0,
    val armorType: String = "轻甲",
    val statusEffects: MutableList<StatusEffect> = mutableListOf(),
    val unitId: String = UUID.randomUUID().toString().take(8),
    var tempHp: Int = 0,
    val weight: Int = 0,
    val eliteStage: Int = 0,
    var elementalTenacityCurrent: Int = 6,
    val elementalTenacityMax: Int = ELITE_TENACITY[eliteStage] ?: 6,
    var elementalBurst: String = "",
    var elementalBurstRemaining: Int = 0
) {
    fun hasStatus(name: String): Boolean =
        statusEffects.any { it.name == name }

    fun getStatus(name: String): StatusEffect? =
        statusEffects.firstOrNull { it.name == name }

    fun hasAnyStatus(names: Set<String>): Boolean =
        statusEffects.any { it.name in names }

    fun addStatus(name: String, stacks: Int = 0) {
        if (!hasStatus(name)) {
            statusEffects.add(StatusEffect(name, stacks))
        }
    }

    fun removeStatus(name: String): Boolean {
        val iter = statusEffects.iterator()
        while (iter.hasNext()) {
            if (iter.next().name == name) {
                iter.remove()
                return true
            }
        }
        return false
    }

    fun statusNames(): List<String> = statusEffects.map { it.name }

    fun reduceTenacity(amount: Int): Int {
        val actual = minOf(amount, elementalTenacityCurrent)
        elementalTenacityCurrent -= actual
        return amount - actual
    }

    fun recoverTenacity() {
        elementalTenacityCurrent = elementalTenacityMax
        elementalBurst = ""
        elementalBurstRemaining = 0
    }

    fun isInBurst(): Boolean =
        elementalBurst.isNotEmpty() && elementalBurstRemaining > 0

    fun effectiveHp(): Int = currentHp + tempHp

    fun toMap(): Map<String, Any?> = mapOf(
        "unit_id" to unitId,
        "name" to name,
        "unit_type" to unitType,
        "current_hp" to currentHp,
        "max_hp" to maxHp,
        "speed" to speed,
        "physical_resist" to physicalResist,
        "magic_resist" to magicResist,
        "armor_type" to armorType,
        "status_effects" to statusEffects.map { mapOf("name" to it.name, "stacks" to it.stacks) },
        "temp_hp" to tempHp,
        "weight" to weight,
        "elite_stage" to eliteStage,
        "elemental_tenacity_current" to elementalTenacityCurrent,
        "elemental_tenacity_max" to elementalTenacityMax,
        "elemental_burst" to elementalBurst,
        "elemental_burst_remaining" to elementalBurstRemaining
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): GameUnit {
            val rawStatus = map["status_effects"] as? List<*> ?: emptyList<Any>()
            val normalizedStatus = rawStatus.mapNotNull { item ->
                when (item) {
                    is String -> StatusEffect(item, 0)
                    is Map<*, *> -> StatusEffect(
                        name = item["name"]?.toString() ?: "",
                        stacks = (item["stacks"] as? Number)?.toInt() ?: 0
                    )
                    else -> null
                }
            }.toMutableList()

            val eliteStage = (map["elite_stage"] as? Number)?.toInt() ?: 0
            val tenacityMax = ELITE_TENACITY[eliteStage] ?: 6

            return GameUnit(
                unitId = map["unit_id"]?.toString() ?: UUID.randomUUID().toString().take(8),
                name = map["name"]?.toString() ?: "",
                unitType = map["unit_type"]?.toString() ?: "player",
                currentHp = (map["current_hp"] as? Number)?.toInt() ?: 10,
                maxHp = (map["max_hp"] as? Number)?.toInt() ?: 10,
                speed = (map["speed"] as? Number)?.toInt() ?: 10,
                physicalResist = (map["physical_resist"] as? Number)?.toInt() ?: 0,
                magicResist = (map["magic_resist"] as? Number)?.toInt() ?: 0,
                armorType = map["armor_type"]?.toString() ?: "轻甲",
                statusEffects = normalizedStatus,
                tempHp = (map["temp_hp"] as? Number)?.toInt() ?: 0,
                weight = (map["weight"] as? Number)?.toInt() ?: 0,
                eliteStage = eliteStage,
                elementalTenacityCurrent = (map["elemental_tenacity_current"] as? Number)?.toInt() ?: tenacityMax,
                elementalTenacityMax = tenacityMax,
                elementalBurst = map["elemental_burst"]?.toString() ?: "",
                elementalBurstRemaining = (map["elemental_burst_remaining"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
