/*
 * GPLv3 License
 *
 *  Copyright (c) KAGA by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.waicool20.kaga.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.waicool20.kaga.Kaga
import com.waicool20.kaga.util.IniConfig
import com.waicool20.kaga.util.fromObject
import com.waicool20.kaga.util.toObject
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleSetProperty
import javafx.collections.FXCollections
import org.ini4j.Wini
import org.slf4j.LoggerFactory
import tornadofx.*
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.concurrent.thread

data class KancolleAutoProfile(
        val general: General = General(),
        val scheduledSleep: ScheduledSleep = ScheduledSleep(),
        /* TODO Disabled temporarily till kcauto-kai is finalized
        val scheduledStop: ScheduledStop = ScheduledStop(),*/
        val expeditions: Expeditions = Expeditions(),
        val pvp: Pvp = Pvp(),
        val sortie: Sortie = Sortie(),
        val shipSwitcher: ShipSwitcher = ShipSwitcher(),
        val quests: Quests = Quests()
) {
    constructor(
            name: String,
            general: General = General(),
            scheduledSleep: ScheduledSleep = ScheduledSleep(),
            expeditions: Expeditions = Expeditions(),
            pvp: Pvp = Pvp(),
            sortie: Sortie = Sortie(),
            shipSwitcher: ShipSwitcher = ShipSwitcher(),
            quests: Quests = Quests()
    ) : this(general, scheduledSleep, expeditions, pvp, sortie, shipSwitcher, quests) {
        this.name = name
    }

    init {
        general.pauseProperty.addListener { _, _, newVal ->
            thread {
                val text = path().toFile().readText().replace(Regex("Pause.+"), "Pause = ${newVal.toString().capitalize()}").toByteArray()
                Files.write(path(), text, StandardOpenOption.TRUNCATE_EXISTING)
            }
        }
    }

    private val logger = LoggerFactory.getLogger(KancolleAutoProfile::class.java)
    @JsonIgnore var nameProperty = KancolleAutoProfile.Loader.DEFAULT_NAME.toProperty()
    @get:JsonProperty var name by nameProperty

    fun path(): Path = Kaga.CONFIG_DIR.resolve("$name-config.ini")

    fun save(path: Path = path()) {
        logger.info("Saving KancolleAuto profile")
        logger.debug("Saving to $path")
        if (Files.notExists(path)) {
            logger.debug("Profile not found, creating file $path")
            Files.createDirectories(path.parent)
            Files.createFile(path)
        }
        val config = asIniString().replace("true", "True").replace("false", "False")
        val header = "# Configuration automatically generated by KAGA\n"
        Files.write(path, (header + config).toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        logger.info("Saving KancolleAuto profile was successful")
        logger.debug("Saved $this to $path")
    }

    fun asIniString() = StringWriter().also { getIni().store(it) }.toString()

    private fun getIni() = Wini().apply {
        add("General").fromObject(general)
        add("ScheduledSleep").fromObject(scheduledSleep)
        /* TODO Disabled temporarily till kcauto-kai is finalized
        add("ScheduledStop").fromObject(scheduledStop)*/
        add("Expeditions").fromObject(expeditions)
        add("PvP").fromObject(pvp)
        add("Combat").fromObject(sortie)
        add("ShipSwitcher").fromObject(shipSwitcher)
        /* TODO Disabled temporarily till kcauto-kai is finalized
        quests.quests.setAll(quests.quests.map(String::toLowerCase))*/
        add("Quests").fromObject(quests)
    }

    fun delete(): Boolean {
        with(path()) {
            return if (Files.exists(this)) {
                Files.delete(this)
                logger.info("Deleted profile")
                logger.debug("Deleted ${this@KancolleAutoProfile} from $this")
                true
            } else {
                logger.warn("File doesn't exist, can't delete!")
                logger.debug("Couldn't delete $this")
                false
            }
        }
    }

    companion object Loader {
        private val loaderLogger = LoggerFactory.getLogger(KancolleAutoProfile.Loader::class.java)
        const val DEFAULT_NAME = "[Current Profile]"
        val VALID_NODES = (1..12).map { it.toString() }
                .plus("ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("").filter { it.isNotEmpty() })
                .plus(listOf("Z1", "Z2", "Z3", "Z4", "Z5", "Z6", "Z7", "Z8", "Z9", "ZZ1", "ZZ2", "ZZ3"))
                .let { FXCollections.observableList(it) }

        fun load(path: Path = Kaga.CONFIG.kcaKaiRootDirPath.resolve("config.ini")): KancolleAutoProfile {
            if (Files.exists(path)) {
                loaderLogger.info("Attempting to load KancolleAuto Profile")
                loaderLogger.debug("Loading KancolleAuto Profile from $path")
                val name = Regex("(.+?)-config\\.ini").matchEntire("${path.fileName}")?.groupValues?.get(1)
                        ?: run {
                            val backupPath = (0..999).asSequence()
                                    .map { "config.ini.bak$it" }
                                    .map { path.resolveSibling(it) }
                                    .first { Files.notExists(it) }
                            loaderLogger.info("Copied backup of existing configuration to $backupPath")
                            Files.copy(path, backupPath)
                            DEFAULT_NAME
                        }
                val ini = Wini(path.toFile())

                return KancolleAutoProfile(
                        name,
                        general = loadSection(ini),
                        scheduledSleep = loadSection(ini),
                        /* TODO Disabled temporarily till kcauto-kai is finalized
                        scheduledStop,*/
                        expeditions = loadSection(ini),
                        pvp = loadSection(ini, "PvP"),
                        sortie = loadSection(ini, "Combat"),
                        shipSwitcher = loadSection(ini),
                        quests = loadSection(ini)
                ).apply {
                    loaderLogger.info("Loading KancolleAuto profile was successful")
                    loaderLogger.debug("Loaded $this")
                }
            } else {
                loaderLogger.debug("Config at $path not found, falling back to config.ini in kancolle-auto root")
                check(Kaga.CONFIG.isValid())
                return load()
            }
        }

        private inline fun <reified T> loadSection(ini: Wini, section: String? = null): T {
            val name = T::class.simpleName
            return ini[section ?: name]?.toObject() ?: run {
                error("Could not parse $name section! Using defaults for it!")
            }.let { T::class.java.newInstance() }
        }
    }

    enum class RecoveryMethod { BROWSER, KC3, KCV, KCT, EO, NONE }

    enum class ScheduledStopMode { TIME, EXPEDITION, SORTIE, PVP }

    enum class CombatFormation(val prettyString: String) {
        LINE_AHEAD("Line Ahead"), DOUBLE_LINE("Double Line"), DIAMOND("Diamond"),
        ECHELON("Echelon"), LINE_ABREAST("Line Abreast"), VANGUARD("Vanguard"),
        COMBINEDFLEET_1("Cruising Formation 1 (Anti-Sub)"), COMBINEDFLEET_2("Cruising Formation 2 (Forward)"),
        COMBINEDFLEET_3("Cruising Formation 3 (Ring)"), COMBINEDFLEET_4("Cruising Formation 4 (Battle)");

        companion object {
            fun fromPrettyString(string: String) = values().first { it.prettyString.equals(string, true) }
        }

        override fun toString(): String = name.toLowerCase()
    }

    enum class Engine(val prettyString: String) {
        LEGACY("Legacy"), LIVE("Live / Dynamic");

        companion object {
            fun fromPrettyString(string: String) = values().first { it.prettyString.equals(string, true) }
        }

        override fun toString() = name.toLowerCase()
    }

    enum class FleetMode(val prettyString: String, val value: String) {
        STANDARD("Standard", ""),
        CTF("Carrier Task Force", "ctf"),
        STF("Strike Task Force", "stf"),
        TRANSPORT("Transport Escort", "transport"),
        STRIKING("Striking Fleet", "striking");

        companion object {
            fun fromPrettyString(string: String) = values().first { it.prettyString.equals(string, true) }
        }

        override fun toString() = value
    }

    enum class DamageLevel(val prettyString: String, val value: String) {
        LIGHT("Light Damage", "minor"),
        MODERATE("Moderate Damage", "moderate"),
        CRITICAL("Critical Damage", "heavy");

        companion object {
            fun fromPrettyString(string: String) = values().first { it.prettyString.equals(string, true) }
        }

        override fun toString() = value
    }

    enum class SortieOptions(val value: String) {
        CHECK_FATIGUE("CheckFatigue"),
        RESERVE_DOCKS("ReserveDocks"),
        PORT_CHECK("PortCheck"),
        CLEAR_STOP("ClearStop");

        override fun toString() = value
    }

    enum class SwitchCriteria(val prettyString: String, val value: String) {
        FATIGUE("Fatigue", "fatigue"),
        DAMAGE("Damage", "damage"),
        SPARKLE("Sparkle", "sparkle");

        companion object {
            fun fromPrettyString(string: String) = values().first { it.prettyString.equals(string, true) }
        }

        override fun toString() = value
    }

    class General(
            program: String = "Chrome",
            pause: Boolean = false
            /* TODO Disabled temporarily till kcauto-kai is finalized
            recoveryMethod: RecoveryMethod = RecoveryMethod.KC3,
            basicRecovery: Boolean = true,
            sleepCycle: Int = 20,
            paranoia: Int = 1,
            sleepModifier: Int = 0*/
    ) {
        @JsonIgnore @IniConfig(key = "Program")
        val programProperty = program.toProperty()
        @JsonIgnore @IniConfig(key = "JSTOffset", shouldRead = false)
        val jstOffsetProperty = ((TimeZone.getDefault().rawOffset - TimeZone.getTimeZone("Japan").rawOffset) / 3600000).toProperty()
        @JsonIgnore @IniConfig(key = "Pause")
        val pauseProperty = pause.toProperty()
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @JsonIgnore @IniConfig(key = "RecoveryMethod") val recoveryMethodProperty = SimpleObjectProperty<RecoveryMethod>(recoveryMethod)
        @JsonIgnore @IniConfig(key = "BasicRecovery") val basicRecoveryProperty: BooleanProperty = basicRecovery.toProperty()
        @JsonIgnore @IniConfig(key = "SleepCycle") val sleepCycleProperty: IntegerProperty = sleepCycle.toProperty()
        @JsonIgnore @IniConfig(key = "Paranoia") val paranoiaProperty: IntegerProperty = paranoia.toProperty()
        @JsonIgnore @IniConfig(key = "SleepModifier") val sleepModifierProperty: IntegerProperty = sleepModifier.toProperty()*/

        @get:JsonProperty var program by programProperty
        @get:JsonProperty var jstOffset by jstOffsetProperty
        @get:JsonProperty var pause by pauseProperty
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @get:JsonProperty var recoveryMethod by recoveryMethodProperty
        @get:JsonProperty var basicRecovery by basicRecoveryProperty
        @get:JsonProperty var sleepCycle by sleepCycleProperty
        @get:JsonProperty var paranoia by paranoiaProperty
        @get:JsonProperty var sleepModifier by sleepModifierProperty*/
    }

    class ScheduledSleep(
            scriptSleepEnabled: Boolean = false,
            scriptSleepStartTime: String = "0030",
            scriptSleepLength: Double = 3.5,
            expSleepEnabled: Boolean = false,
            expSleepStartTime: String = "0030",
            expSleepLength: Double = 3.5,
            sortieSleepEnabled: Boolean = false,
            sortieSleepStartTime: String = "0030",
            sortieSleepLength: Double = 3.5
    ) {
        @JsonIgnore @IniConfig(key = "ScriptSleepEnabled")
        val scriptSleepEnabledProperty = scriptSleepEnabled.toProperty()
        @JsonIgnore @IniConfig(key = "ScriptSleepStartTime")
        val scriptSleepStartTimeProperty = scriptSleepStartTime.toProperty()
        @JsonIgnore @IniConfig(key = "ScriptSleepLength")
        val scriptSleepLengthProperty = scriptSleepLength.toProperty()

        @JsonIgnore @IniConfig(key = "ExpeditionSleepEnabled")
        val expSleepEnabledProperty = expSleepEnabled.toProperty()
        @JsonIgnore @IniConfig(key = "ExpeditionSleepStartTime")
        val expSleepStartTimeProperty = expSleepStartTime.toProperty()
        @JsonIgnore @IniConfig(key = "ExpeditionSleepLength")
        val expSleepLengthProperty = expSleepLength.toProperty()

        @JsonIgnore @IniConfig(key = "CombatSleepEnabled")
        val sortieSleepEnabledProperty = sortieSleepEnabled.toProperty()
        @JsonIgnore @IniConfig(key = "CombatSleepStartTime")
        val sortieSleepStartTimeProperty = sortieSleepStartTime.toProperty()
        @JsonIgnore @IniConfig(key = "CombatSleepLength")
        val sortieSleepLengthProperty = sortieSleepLength.toProperty()

        @get:JsonProperty var scriptSleepEnabled by scriptSleepEnabledProperty
        @get:JsonProperty var scriptSleepStartTime by scriptSleepStartTimeProperty
        @get:JsonProperty var scriptSleepLength by scriptSleepLengthProperty

        @get:JsonProperty var expSleepEnabled by expSleepEnabledProperty
        @get:JsonProperty var expSleepStartTime by expSleepStartTimeProperty
        @get:JsonProperty var expSleepLength by expSleepLengthProperty

        @get:JsonProperty var sortieSleepEnabled by sortieSleepEnabledProperty
        @get:JsonProperty var sortieSleepStartTime by sortieSleepStartTimeProperty
        @get:JsonProperty var sortieSleepLength by sortieSleepLengthProperty
    }

    /* TODO Disabled temporarily till kcauto-kai is finalized
    class ScheduledStop(
            enabled: Boolean = false,
            mode: ScheduledStopMode = ScheduledStopMode.TIME,
            count: Int = 5
    ) {
        @JsonIgnore @IniConfig(key = "Enabled") val enabledProperty = enabled.toProperty()
        @JsonIgnore @IniConfig(key = "Mode") val modeProperty = mode.toProperty()
        @JsonIgnore @IniConfig(key = "Count") val countProperty = count.toProperty()

        @get:JsonProperty var enabled by enabledProperty
        @get:JsonProperty var mode by modeProperty
        @get:JsonProperty var count by countProperty
    }*/

    class Expeditions(
            enabled: Boolean = true,
            fleet2: List<String> = mutableListOf("2"),
            fleet3: List<String> = mutableListOf("5"),
            fleet4: List<String> = mutableListOf("21")
    ) {
        @JsonIgnore @IniConfig(key = "Enabled")
        val enabledProperty = enabled.toProperty()
        @JsonIgnore @IniConfig(key = "Fleet2")
        val fleet2Property = SimpleListProperty(FXCollections.observableList(fleet2))
        @JsonIgnore @IniConfig(key = "Fleet3")
        val fleet3Property = SimpleListProperty(FXCollections.observableList(fleet3))
        @JsonIgnore @IniConfig(key = "Fleet4")
        val fleet4Property = SimpleListProperty(FXCollections.observableList(fleet4))

        @get:JsonProperty var enabled by enabledProperty
        @get:JsonProperty var fleet2 by fleet2Property
        @get:JsonProperty var fleet3 by fleet3Property
        @get:JsonProperty var fleet4 by fleet4Property
    }

    class Pvp(
            enabled: Boolean = false
            /* TODO Disabled temporarily till kcauto-kai is finalized
            fleetComp: Int = 1*/
    ) {
        @JsonIgnore @IniConfig(key = "Enabled")
        val enabledProperty = enabled.toProperty()
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @JsonIgnore @IniConfig(key = "FleetComp") val fleetCompProperty = fleetComp.toProperty()*/

        @get:JsonProperty var enabled by enabledProperty
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @get:JsonProperty var fleetComp by fleetCompProperty*/
    }

    class Sortie(
            enabled: Boolean = false,
            engine: Engine = Engine.LEGACY,
            map: String = "1-1",
            nodes: Int = 5,
            fleetMode: FleetMode = FleetMode.STANDARD,
            nodeSelects: List<String> = mutableListOf(),
            formations: List<String> = mutableListOf(),
            nightBattles: List<String> = mutableListOf(),
            retreatLimit: DamageLevel = DamageLevel.CRITICAL,
            repairLimit: DamageLevel = DamageLevel.MODERATE,
            repairTimeLimit: String = "0030",
            lbasGroups: Set<String> = mutableSetOf(),
            lbasGroup1Nodes: List<String> = mutableListOf(),
            lbasGroup2Nodes: List<String> = mutableListOf(),
            lbasGroup3Nodes: List<String> = mutableListOf(),
            miscOptions: Set<SortieOptions> = mutableSetOf()
    ) {
        @JsonIgnore @IniConfig(key = "Enabled")
        val enabledProperty = enabled.toProperty()
        @JsonIgnore @IniConfig(key = "Engine")
        val engineProperty = engine.toProperty()
        @JsonIgnore @IniConfig(key = "Map")
        val mapProperty = map.toProperty()
        @JsonIgnore @IniConfig(key = "CombatNodes")
        val nodesProperty = nodes.toProperty()
        @JsonIgnore @IniConfig(key = "FleetMode")
        val fleetModeProperty = fleetMode.toProperty()
        @JsonIgnore @IniConfig(key = "NodeSelects")
        val nodeSelectsProperty = SimpleListProperty(FXCollections.observableList(nodeSelects))
        @JsonIgnore @IniConfig(key = "Formations")
        val formationsProperty = SimpleListProperty(FXCollections.observableList(formations))
        @JsonIgnore @IniConfig(key = "NightBattles")
        val nightBattlesProperty = SimpleListProperty(FXCollections.observableList(nightBattles))
        @JsonIgnore @IniConfig(key = "RetreatLimit")
        val retreatLimitProperty = retreatLimit.toProperty()
        @JsonIgnore @IniConfig(key = "RepairLimit")
        val repairLimitProperty = repairLimit.toProperty()
        @JsonIgnore @IniConfig(key = "RepairTimeLimit")
        val repairTimeLimitProperty = repairTimeLimit.toProperty()
        @JsonIgnore @IniConfig(key = "LBASGroups")
        val lbasGroupsProperty = SimpleSetProperty(FXCollections.observableSet(lbasGroups))
        @JsonIgnore @IniConfig(key = "LBASGroup1Nodes")
        val lbasGroup1NodesProperty = SimpleListProperty(FXCollections.observableList(lbasGroup1Nodes))
        @JsonIgnore @IniConfig(key = "LBASGroup2Nodes")
        val lbasGroup2NodesProperty = SimpleListProperty(FXCollections.observableList(lbasGroup2Nodes))
        @JsonIgnore @IniConfig(key = "LBASGroup3Nodes")
        val lbasGroup3NodesProperty = SimpleListProperty(FXCollections.observableList(lbasGroup3Nodes))
        @JsonIgnore @IniConfig(key = "MiscOptions")
        val miscOptionsProperty = SimpleSetProperty(FXCollections.observableSet(miscOptions))

        @get:JsonProperty var enabled by enabledProperty
        @get:JsonProperty var engine by engineProperty
        @get:JsonProperty var map by mapProperty
        @get:JsonProperty var nodes by nodesProperty
        @get:JsonProperty var fleetMode by fleetModeProperty
        @get:JsonProperty var nodeSelects by nodeSelectsProperty
        @get:JsonProperty var formations by formationsProperty
        @get:JsonProperty var nightBattles by nightBattlesProperty
        @get:JsonProperty var retreatLimit by retreatLimitProperty
        @get:JsonProperty var repairLimit by repairLimitProperty
        @get:JsonProperty var repairTimeLimit by repairTimeLimitProperty
        @get:JsonProperty var lbasGroups by lbasGroupsProperty
        @get:JsonProperty var lbasGroup1Nodes by lbasGroup1NodesProperty
        @get:JsonProperty var lbasGroup2Nodes by lbasGroup2NodesProperty
        @get:JsonProperty var lbasGroup3Nodes by lbasGroup3NodesProperty
        @get:JsonProperty var miscOptions by miscOptionsProperty
    }

    class ShipSwitcher(
            enabled: Boolean = true,
            slot1Criteria: List<SwitchCriteria> = mutableListOf(),
            slot1Ships: List<String> = mutableListOf(),
            slot2Criteria: List<SwitchCriteria> = mutableListOf(),
            slot2Ships: List<String> = mutableListOf(),
            slot3Criteria: List<SwitchCriteria> = mutableListOf(),
            slot3Ships: List<String> = mutableListOf(),
            slot4Criteria: List<SwitchCriteria> = mutableListOf(),
            slot4Ships: List<String> = mutableListOf(),
            slot5Criteria: List<SwitchCriteria> = mutableListOf(),
            slot5Ships: List<String> = mutableListOf(),
            slot6Criteria: List<SwitchCriteria> = mutableListOf(),
            slot6Ships: List<String> = mutableListOf()
    ) {
        @JsonIgnore @IniConfig("Enabled")
        val enabledProperty = enabled.toProperty()
        @JsonIgnore @IniConfig("Slot1Criteria")
        val slot1CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot1Criteria))
        @JsonIgnore @IniConfig("Slot1Ships")
        val slot1ShipsProperty = SimpleListProperty(FXCollections.observableList(slot1Ships))
        @JsonIgnore @IniConfig("Slot2Criteria")
        val slot2CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot2Criteria))
        @JsonIgnore @IniConfig("Slot2Ships")
        val slot2ShipsProperty = SimpleListProperty(FXCollections.observableList(slot2Ships))
        @JsonIgnore @IniConfig("Slot3Criteria")
        val slot3CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot3Criteria))
        @JsonIgnore @IniConfig("Slot3Ships")
        val slot3ShipsProperty = SimpleListProperty(FXCollections.observableList(slot3Ships))
        @JsonIgnore @IniConfig("Slot4Criteria")
        val slot4CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot4Criteria))
        @JsonIgnore @IniConfig("Slot4Ships")
        val slot4ShipsProperty = SimpleListProperty(FXCollections.observableList(slot4Ships))
        @JsonIgnore @IniConfig("Slot5Criteria")
        val slot5CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot5Criteria))
        @JsonIgnore @IniConfig("Slot5Ships")
        val slot5ShipsProperty = SimpleListProperty(FXCollections.observableList(slot5Ships))
        @JsonIgnore @IniConfig("Slot6Criteria")
        val slot6CriteriaProperty = SimpleListProperty(FXCollections.observableList(slot6Criteria))
        @JsonIgnore @IniConfig("Slot6Ships")
        val slot6ShipsProperty = SimpleListProperty(FXCollections.observableList(slot6Ships))

        @get:JsonProperty var enabled by enabledProperty
        @get:JsonProperty var slot1Criteria by slot1CriteriaProperty
        @get:JsonProperty var slot1Ships by slot1ShipsProperty
        @get:JsonProperty var slot2Criteria by slot2CriteriaProperty
        @get:JsonProperty var slot2Ships by slot2ShipsProperty
        @get:JsonProperty var slot3Criteria by slot3CriteriaProperty
        @get:JsonProperty var slot3Ships by slot3ShipsProperty
        @get:JsonProperty var slot4Criteria by slot4CriteriaProperty
        @get:JsonProperty var slot4Ships by slot4ShipsProperty
        @get:JsonProperty var slot5Criteria by slot5CriteriaProperty
        @get:JsonProperty var slot5Ships by slot5ShipsProperty
        @get:JsonProperty var slot6Criteria by slot6CriteriaProperty
        @get:JsonProperty var slot6Ships by slot6ShipsProperty
    }

    class Quests(
            enabled: Boolean = true
            /* TODO Disabled temporarily till kcauto-kai is finalized
            quests: List<String> = listOf("bd1", "bd2", "bd3", "bd4", "bd5", "bd6", "bd7", "bd8", "bw1", "bw2", "bw3", "bw4", "bw5", "bw6", "bw7", "bw8", "bw9", "bw10", "c2", "c3", "c4", "c8", "d2", "d3", "d4", "d9", "d11", "e3", "e4"),
            checkSchedule: Int = 5*/
    ) {
        @JsonIgnore
        @IniConfig(key = "Enabled")
        val enabledProperty = enabled.toProperty()
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @JsonIgnore @IniConfig(key = "Quests") val questsProperty = SimpleListProperty(FXCollections.observableArrayList(quests))
        @JsonIgnore @IniConfig(key = "CheckSchedule") val checkScheduleProperty = checkSchedule.toProperty()*/

        @get:JsonProperty
        var enabled by enabledProperty
        /* TODO Disabled temporarily till kcauto-kai is finalized
        @get:JsonProperty var quests by questsProperty
        @get:JsonProperty var checkSchedule by checkScheduleProperty*/
    }

    override fun toString(): String = jacksonObjectMapper().writeValueAsString(this)
}
