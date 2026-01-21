package com.badargadh.sahkar.component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimePicker extends HBox {

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> hourCombo;
    @FXML private ComboBox<String> minuteCombo;

    // The core property that holds the combined LocalDateTime
    private final ObjectProperty<LocalDateTime> value = new SimpleObjectProperty<>(LocalDateTime.now());

    public DateTimePicker() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/components/DateTimePicker.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    public void initialize() {
        // Populate Hours and Minutes
        for (int i = 0; i < 24; i++) hourCombo.getItems().add(String.format("%02d", i));
        for (int i = 0; i < 60; i++) minuteCombo.getItems().add(String.format("%02d", i));

        // Set initial UI values to match the property default (Now)
        syncUIToProperty();

        // Add Listeners: Whenever UI changes, update the internal 'value' property
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> updatePropertyFromUI());
        hourCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePropertyFromUI());
        minuteCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePropertyFromUI());
        
        handleResetToNow(); // Default to current time
    }
    
    @FXML
    private void handleResetToNow() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Set Date
        datePicker.setValue(now.toLocalDate());
        
        // 2. Set Hour (padded with 0 if necessary)
        String currentHour = String.format("%02d", now.getHour());
        hourCombo.setValue(currentHour);
        
        // 3. Set Minute (padded with 0 if necessary)
        String currentMinute = String.format("%02d", now.getMinute());
        minuteCombo.setValue(currentMinute);
    }

    private void updatePropertyFromUI() {
        LocalDate date = datePicker.getValue();
        if (date != null && hourCombo.getValue() != null && minuteCombo.getValue() != null) {
            LocalTime time = LocalTime.of(Integer.parseInt(hourCombo.getValue()), 
                                          Integer.parseInt(minuteCombo.getValue()));
            value.set(LocalDateTime.of(date, time));
        } else {
            value.set(null);
        }
    }

    private void syncUIToProperty() {
        LocalDateTime val = value.get();
        if (val != null) {
            datePicker.setValue(val.toLocalDate());
            hourCombo.setValue(String.format("%02d", val.getHour()));
            minuteCombo.setValue(String.format("%02d", val.getMinute()));
        }
    }

    // Standard JavaFX Property Methods
    public ObjectProperty<LocalDateTime> valueProperty() { return value; }
    public LocalDateTime getValue() { return value.get(); }
    public void setValue(LocalDateTime dateTime) { 
        value.set(dateTime); 
        syncUIToProperty();
    }
}