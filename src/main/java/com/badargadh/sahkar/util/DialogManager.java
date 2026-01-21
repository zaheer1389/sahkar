package com.badargadh.sahkar.util;

import java.io.File;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Industry-standard utility for managing system-wide dialogs and user feedback.
 */
public class DialogManager {

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("System Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("Confirmation Required");
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    public static void warning(String title, String message) {
    	Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        //alert.setHeaderText("Confirmation Required");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static <T> T showChoiceDialog(String title, String content, List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        // Use the first item as the default selection
        ChoiceDialog<T> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        // Styling the dialog to match the application's clean look
        dialog.getDialogPane().getStylesheets().add(
            DialogManager.class.getResource("/static/css/style.css").toExternalForm()
        );

        Optional<T> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    public static Optional<ButtonType> showCustomDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        // Set the button types (OK and Cancel)
        ButtonType okButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Style the OK button (optional)
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        dialog.getDialogPane().setContent(content);

        // Request focus on the content (e.g., the ComboBox)
        Platform.runLater(content::requestFocus);

        return dialog.showAndWait();
    }
    
    public static <T> ChoiceWithRemark<T> showChoiceDialogWithRemark(String title, String content, List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        // 1. Initialize a custom Dialog
        Dialog<ChoiceWithRemark<T>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        // 2. Set Button Types
        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // 3. Create UI Components
        Label contentLabel = new Label(content);
        
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(choices);
        comboBox.getSelectionModel().selectFirst();
        comboBox.setMaxWidth(Double.MAX_VALUE);

        Label remarkLabel = new Label("Remarks / Note:");
        TextArea remarksArea = new TextArea();
        remarksArea.setPromptText("Enter details here...");
        remarksArea.setPrefRowCount(3);
        remarksArea.setWrapText(true);

        // 4. Layout Container
        VBox container = new VBox(10);
        container.setPadding(new Insets(20, 150, 10, 10)); // Top, Right, Bottom, Left
        container.setPrefWidth(400);
        container.getChildren().addAll(contentLabel, comboBox, remarkLabel, remarksArea);

        dialog.getDialogPane().setContent(container);

        // 5. Apply Styles
        try {
            String css = DialogManager.class.getResource("/static/css/style.css").toExternalForm();
            dialog.getDialogPane().getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load dialog stylesheet.");
        }

        // 6. Convert the Result when OK is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new ChoiceWithRemark<>(
                    comboBox.getSelectionModel().getSelectedItem(),
                    remarksArea.getText()
                );
            }
            return null;
        });

        // 7. Show and Wait
        Optional<ChoiceWithRemark<T>> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    public static File showSaveDialog(javafx.scene.Node node, String title, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(node.getScene().getWindow());
    }
    
    public static class ChoiceWithRemark<T> {
        public final T selection;
        public final String remark;

        public ChoiceWithRemark(T selection, String remark) {
            this.selection = selection;
            this.remark = remark;
        }

		public T getSelection() {
			return selection;
		}

		public String getRemark() {
			return remark;
		}
        
        
    }
}