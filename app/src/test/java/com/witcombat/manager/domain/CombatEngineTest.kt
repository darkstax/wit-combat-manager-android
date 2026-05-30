package com.witcombat.manager.domain

import org.junit.Test
import org.junit.Assert.*

class CombatEngineTest {

    @Test
    fun `team initiative - players first when higher score`() {
        val players = listOf(
            GameUnit(name = "P1", unitType = "player", speed = 15),
            GameUnit(name = "P2", unitType = "player", speed = 10)
        )
        val monsters = listOf(
            GameUnit(name = "M1", unitType = "monster", speed = 10),
            GameUnit(name = "M2", unitType = "monster", speed = 5)
        )
        val state = teamInitiative(players, monsters)
        assertTrue(state.active)
        assertEquals("team", state.initiativeMode)
        assertEquals("player", state.firstTeam)
        assertTrue(state.turnOrder.isNotEmpty())
        assertEquals(players[0].unitId, state.turnOrder[0])
        assertEquals(players[1].unitId, state.turnOrder[1])
    }

    @Test
    fun `team initiative - monsters first when higher score`() {
        val players = listOf(GameUnit(name = "P1", unitType = "player", speed = 5))
        val monsters = listOf(
            GameUnit(name = "M1", unitType = "monster", speed = 20),
            GameUnit(name = "M2", unitType = "monster", speed = 15)
        )
        val state = teamInitiative(players, monsters)
        assertEquals("monster", state.firstTeam)
        assertEquals(monsters[0].unitId, state.turnOrder[0])
    }

    @Test
    fun `traditional initiative generates rolls`() {
        val units = listOf(
            GameUnit(name = "A", speed = 10),
            GameUnit(name = "B", speed = 12),
            GameUnit(name = "C", speed = 8)
        )
        val state = traditionalInitiative(units, diceFaces = 20)
        assertEquals(3, state.initiativeRolls.size)
        assertEquals(3, state.turnOrder.size)
    }

    @Test
    fun `apply physical damage with resist`() {
        val unit = GameUnit(name = "Test", currentHp = 50, maxHp = 50, physicalResist = 10)
        val msg = applyDamage(unit, 30, dmgType = "物理", isAttack = false)
        assertEquals(30, unit.currentHp)
        assertTrue(msg.contains("20"))
    }

    @Test
    fun `shield blocks attack`() {
        val unit = GameUnit(name = "Shielded", currentHp = 50, maxHp = 50)
        unit.addStatus("护盾", 1)
        val msg = applyDamage(unit, 30, dmgType = "法术")
        assertEquals(50, unit.currentHp)
        assertFalse(unit.hasStatus("护盾"))
        assertTrue(msg.contains("护盾抵消"))
    }

    @Test
    fun `true damage bypasses shield but not barrier`() {
        val unit = GameUnit(name = "Test", currentHp = 50, maxHp = 50, tempHp = 10)
        unit.addStatus("屏障", 1)
        val msg = applyDamage(unit, 30, dmgType = "真实")
        assertEquals(30, unit.currentHp)
        assertEquals(0, unit.tempHp)
    }

    @Test
    fun `healing restores HP`() {
        val unit = GameUnit(name = "Test", currentHp = 30, maxHp = 50)
        val msg = applyHealing(unit, 15)
        assertEquals(45, unit.currentHp)
        assertTrue(msg.contains("15"))
    }

    @Test
    fun `healing blocked by 禁疗`() {
        val unit = GameUnit(name = "Test", currentHp = 30, maxHp = 50)
        unit.addStatus("禁疗")
        val msg = applyHealing(unit, 15)
        assertEquals(30, unit.currentHp)
        assertTrue(msg.contains("禁疗"))
    }

    @Test
    fun `status upgrade chain`() {
        val unit = GameUnit(name = "Test")
        val msg1 = applyStatus(unit, "麻痹")
        assertTrue(unit.hasStatus("麻痹"))
        assertTrue(msg1.contains("获得了"))
        val msg2 = applyStatus(unit, "麻痹")
        assertFalse(unit.hasStatus("麻痹"))
        assertTrue(unit.hasStatus("眩晕"))
        assertTrue(msg2.contains("升级"))
    }

    @Test
    fun `immunity blocks status`() {
        val unit = GameUnit(name = "Immune")
        unit.addStatus("免疫")
        val msg = applyStatus(unit, "眩晕")
        assertTrue(msg.contains("免疫"))
        assertFalse(unit.hasStatus("眩晕"))
    }

    @Test
    fun `resist consumes one charge`() {
        val unit = GameUnit(name = "Test")
        unit.addStatus("抵抗", 1)
        val msg = applyStatus(unit, "眩晕")
        assertFalse(unit.hasStatus("抵抗"))
        assertTrue(msg.contains("抵抗"))
        assertFalse(unit.hasStatus("眩晕"))
    }

    @Test
    fun `elemental damage during burst does 3x true damage`() {
        val unit = GameUnit(name = "Test", currentHp = 100, maxHp = 100,
            elementalTenacityCurrent = 0, elementalBurst = "凋亡损伤", elementalBurstRemaining = 2)
        val msg = applyElementalDamage(unit, 5, "凋亡损伤")
        assertEquals(85, unit.currentHp)
        assertTrue(msg.contains("15"))
        assertTrue(msg.contains("爆发期间"))
    }

    @Test
    fun `advance turn increments turn number`() {
        val units = listOf(GameUnit(name = "A", speed = 10), GameUnit(name = "B", speed = 8))
        val state = traditionalInitiative(units)
        val (newState, msgs) = advanceTurn(state, units)
        assertEquals(1, newState.turn)
        assertEquals(0, newState.nowIndex)
        assertTrue(msgs.any { it.contains("第 1 回合开始") })
    }

    @Test
    fun `next actor advances through turn order`() {
        val units = listOf(GameUnit(name = "A", speed = 10), GameUnit(name = "B", speed = 8))
        val state = traditionalInitiative(units)
        assertEquals(2, state.turnOrder.size)
        val (state1, _) = nextActor(state, units)
        assertEquals(1, state1.nowIndex)
        val (state2, _) = nextActor(state1, units)
        assertEquals(1, state2.turn)
        assertEquals(0, state2.nowIndex)
    }

    @Test
    fun `vulnerability increases damage`() {
        val unit = GameUnit(name = "Test", currentHp = 50, maxHp = 50)
        unit.addStatus("脆弱", 5)
        val msg = applyDamage(unit, 20, dmgType = "物理", isAttack = false)
        assertEquals(25, unit.currentHp)
        assertTrue(msg.contains("25"))
    }
}
