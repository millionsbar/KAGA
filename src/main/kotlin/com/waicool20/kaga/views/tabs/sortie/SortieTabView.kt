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

package com.waicool20.kaga.views.tabs.sortie

import com.waicool20.kaga.Kaga
import com.waicool20.kaga.config.KancolleAutoProfile.*
import com.waicool20.kaga.util.NoneSelectableCellFactory
import com.waicool20.kaga.util.asTimeSpinner
import com.waicool20.kaga.util.bind
import javafx.beans.binding.Bindings
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.GridPane
import javafx.util.StringConverter
import tornadofx.*
import java.util.concurrent.TimeUnit


class SortieTabView {
    @FXML private lateinit var enableButton: CheckBox
    @FXML private lateinit var engineComboBox: ComboBox<Engine>
    @FXML private lateinit var mapComboBox: ComboBox<String>
    @FXML private lateinit var nodesSpinner: Spinner<Int>
    @FXML private lateinit var fleetModeComboBox: ComboBox<FleetMode>
    @FXML private lateinit var retreatLimitComboBox: ComboBox<DamageLevel>
    @FXML private lateinit var repairLimitComboBox: ComboBox<DamageLevel>
    @FXML private lateinit var repairTimeHourSpinner: Spinner<Int>
    @FXML private lateinit var repairTimeMinSpinner: Spinner<Int>
    @FXML private lateinit var reserveDocksCheckBox: CheckBox
    @FXML private lateinit var checkFatigueCheckBox: CheckBox
    @FXML private lateinit var checkPortCheckBox: CheckBox
    @FXML private lateinit var medalStopCheckBox: CheckBox

    @FXML private lateinit var content: GridPane

    private val maps = setOf(
            "-- World 1 --",
            "1-1", "1-2", "1-3", "1-4", "1-5", "1-6",
            "-- World 2 --",
            "2-1", "2-2", "2-3", "2-4", "2-5",
            "-- World 3 --",
            "3-1", "3-2", "3-3", "3-4", "3-5",
            "-- World 4 --",
            "4-1", "4-2", "4-3", "4-4", "4-5",
            "-- World 5 --",
            "5-1", "5-2", "5-3", "5-4", "5-5",
            "-- World 6 --",
            "6-1", "6-2", "6-3", "6-4", "6-5",
            "-- Event --",
            "E-1", "E-2", "E-3", "E-4",
            "E-5", "E-6", "E-7", "E-8"
    )

    @FXML
    fun initialize() {
        setValues()
        createBindings()
    }

    private fun setValues() {
        val engineConverter = object : StringConverter<Engine>() {
            override fun toString(engine: Engine) = engine.prettyString
            override fun fromString(string: String) = Engine.fromPrettyString(string)
        }
        engineComboBox.converter = engineConverter
        engineComboBox.items.setAll(Engine.values().toList())

        mapComboBox.cellFactory = NoneSelectableCellFactory(Regex("--.+?--"))
        mapComboBox.items.setAll(maps)

        nodesSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12)

        val fleetModeConverter = object : StringConverter<FleetMode>() {
            override fun toString(fleetMode: FleetMode) = fleetMode.prettyString
            override fun fromString(string: String) = FleetMode.fromPrettyString(string)
        }
        fleetModeComboBox.converter = fleetModeConverter
        fleetModeComboBox.items.setAll(FleetMode.values().toList())

        val damageConverter = object : StringConverter<DamageLevel>() {
            override fun toString(level: DamageLevel) = level.prettyString
            override fun fromString(string: String) = DamageLevel.fromPrettyString(string)
        }
        retreatLimitComboBox.items.setAll(DamageLevel.values().toList())
        repairLimitComboBox.items.setAll(DamageLevel.values().toList())
        retreatLimitComboBox.converter = damageConverter
        repairLimitComboBox.converter = damageConverter

        repairTimeHourSpinner.asTimeSpinner(TimeUnit.HOURS)
        repairTimeMinSpinner.asTimeSpinner(TimeUnit.MINUTES)
        with(String.format("%04d", Kaga.PROFILE.sortie.repairTimeLimit.toInt())) {
            repairTimeHourSpinner.valueFactory.value = substring(0, 2).toInt()
            repairTimeMinSpinner.valueFactory.value = substring(2, 4).toInt()
        }

        with(Kaga.PROFILE.sortie.miscOptions) {
            reserveDocksCheckBox.isSelected = contains(SortieOptions.RESERVE_DOCKS)
            checkFatigueCheckBox.isSelected = contains(SortieOptions.CHECK_FATIGUE)
            checkPortCheckBox.isSelected = contains(SortieOptions.PORT_CHECK)
            medalStopCheckBox.isSelected = contains(SortieOptions.MEDAL_STOP)
        }
    }

    private fun createBindings() {
        with(Kaga.PROFILE.sortie) {
            enableButton.bind(enabledProperty)
            engineComboBox.bind(engineProperty)
            mapComboBox.bind(mapProperty)
            nodesSpinner.bind(nodesProperty)
            fleetModeComboBox.bind(fleetModeProperty)

            retreatLimitComboBox.bind(retreatLimitProperty)
            repairLimitComboBox.bind(repairLimitProperty)
            val binding = Bindings.concat(repairTimeHourSpinner.valueProperty().asString("%02d"),
                    repairTimeMinSpinner.valueProperty().asString("%02d"))
            repairTimeLimitProperty.bind(binding)
        }

        with(Kaga.PROFILE.sortie.miscOptions) {
            reserveDocksCheckBox.selectedProperty().addListener { _, _, newVal ->
                if (newVal) add(SortieOptions.RESERVE_DOCKS) else remove(SortieOptions.RESERVE_DOCKS)
            }
            checkFatigueCheckBox.selectedProperty().addListener { _, _, newVal ->
                if (newVal) add(SortieOptions.CHECK_FATIGUE) else remove(SortieOptions.CHECK_FATIGUE)
            }
            checkPortCheckBox.selectedProperty().addListener { _, _, newVal ->
                if (newVal) add(SortieOptions.PORT_CHECK) else remove(SortieOptions.PORT_CHECK)
            }
            medalStopCheckBox.selectedProperty().addListener { _, _, newVal ->
                if (newVal) add(SortieOptions.MEDAL_STOP) else remove(SortieOptions.MEDAL_STOP)
            }
        }

        content.disableProperty().bind(Bindings.not(enableButton.selectedProperty()))
    }

    @FXML
    private fun onConfigureNodeSelectsButton() =
            find(NodeSelectsChooserView::class).openModal(owner = Kaga.ROOT_STAGE.owner)

    @FXML
    private fun onConfigureFormationsButton() =
            find(FormationChooserView::class).openModal(owner = Kaga.ROOT_STAGE.owner)

    @FXML
    private fun onConfigureNightBattlesButton() =
            find(NightBattlesChooserView::class).openModal(owner = Kaga.ROOT_STAGE.owner)
}
