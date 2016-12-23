package com.waicool20.kaga.controllers;

import com.waicool20.kaga.Kaga;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

import java.util.Set;

public class LbasTabController {
    @FXML private CheckBox enableButton;
    @FXML private CheckBox group1CheckBox;
    @FXML private CheckBox group2CheckBox;
    @FXML private CheckBox group3CheckBox;

    @FXML private GridPane content;

    @FXML public void initialize() {
        setValues();
        createBindings();
    }

    private void setValues() {
        updateGroupCheckBoxes(Kaga.PROFILE.getLbas().getEnabledGroups());
    }

    private void createBindings() {
        enableButton.selectedProperty().bindBidirectional(Kaga.PROFILE.getLbas().enabledProperty());
        Kaga.PROFILE.getLbas().getEnabledGroups()
            .addListener((SetChangeListener<? super Integer>) change -> {
                Set<? extends Integer> set = change.getSet();
                updateGroupCheckBoxes(set);
            });
        group1CheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Kaga.PROFILE.getLbas().getEnabledGroups().add(1);
            } else {
                Kaga.PROFILE.getLbas().getEnabledGroups().remove(1);
            }
        });
        group2CheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Kaga.PROFILE.getLbas().getEnabledGroups().add(2);
            } else {
                Kaga.PROFILE.getLbas().getEnabledGroups().remove(2);
            }
        });
        group3CheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Kaga.PROFILE.getLbas().getEnabledGroups().add(3);
            } else {
                Kaga.PROFILE.getLbas().getEnabledGroups().remove(3);
            }
        });
        content.visibleProperty().bind(enableButton.selectedProperty());
    }

    private void updateGroupCheckBoxes(Set<? extends Integer> set) {
        group1CheckBox.selectedProperty().setValue(set.contains(1));
        group2CheckBox.selectedProperty().setValue(set.contains(2));
        group3CheckBox.selectedProperty().setValue(set.contains(3));
    }

    @FXML private void onConfigureGroup1NodesButton() {

    }

    @FXML private void onConfigureGroup2NodesButton() {

    }

    @FXML private void onConfigureGroup3NodesButton() {

    }
}
