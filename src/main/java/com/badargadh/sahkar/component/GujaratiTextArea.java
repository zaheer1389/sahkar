package com.badargadh.sahkar.component;

import com.badargadh.sahkar.util.FullBarakhadiEngine;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Set;

public class GujaratiTextArea extends VBox {

    private final TextArea textArea;
    private final ContextMenu suggestionMenu;
    private String currentWord = "";

    public GujaratiTextArea() {
        this.textArea = new TextArea();
        this.suggestionMenu = new ContextMenu();

        setupUI();
        setupLogic();
    }

    private void setupUI() {
        textArea.getStyleClass().add("gujarati-text-area");
        textArea.setWrapText(true);
        textArea.setPrefHeight(100);

        Label hint = new Label("Type English + Space for Gujarati suggestions");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #005461; -fx-font-weight: bold;");

        getChildren().addAll(hint, textArea);
    }

    private void setupLogic() {
        textArea.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                handleTransliterationTrigger();
            } else {
                suggestionMenu.hide();
            }
        });
    }

    private void handleTransliterationTrigger() {
        String text = textArea.getText();
        int caretPos = textArea.getCaretPosition();

        // Identify the word just typed before the space
        String textBeforeCaret = text.substring(0, caretPos).trim();
        int lastSpace = textBeforeCaret.lastIndexOf(" ");
        int wordStart = (lastSpace == -1) ? 0 : lastSpace + 1;
        currentWord = textBeforeCaret.substring(wordStart);

        if (!currentWord.isEmpty() && currentWord.matches("[a-zA-Z]+")) {
            fetchAndShowSuggestions(wordStart, caretPos);
        }
    }

    private void fetchAndShowSuggestions(int start, int end) {
        Set<String> suggestions = FullBarakhadiEngine.getSuggestions(currentWord);

        if (suggestions.isEmpty()) return;

        suggestionMenu.getItems().clear();
        for (String sug : suggestions) {
            MenuItem item = new MenuItem(sug);
            item.setOnAction(e -> {
                textArea.replaceText(start, end, sug + " ");
                textArea.requestFocus();
            });
            suggestionMenu.getItems().add(item);
        }

        // Calculate Position for the Menu
        // lookup(".content") gets the internal text container for accurate bounds
        Bounds bounds = textArea.lookup(".content").localToScreen(textArea.lookup(".content").getBoundsInLocal());

        // Approximate X/Y position relative to caret
        double x = bounds.getMinX() + (textArea.getCaretPosition() * 7);
        double y = bounds.getMinY() + 30;

        suggestionMenu.show(textArea, x, y);
    }

    public String getText() { return textArea.getText(); }
    public void setText(String t) { textArea.setText(t); }
}