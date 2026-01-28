package com.badargadh.sahkar.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

@Component
public class PdfViewerController {

	@Autowired private HostServices hostServices;
	@Autowired private MainController mainController;
	
    @FXML private WebView webView;
    @FXML private Label lblReportName;
    
    private String base64Data;
    private File currentPdfFile;
    
    public void preparePdfData(File file) throws IOException {
    	this.currentPdfFile = file;
        byte[] data = Files.readAllBytes(file.toPath());
        this.base64Data = Base64.getEncoder().encodeToString(data);
    }
    
    public void renderPdf() {
    	
    	WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        String viewerPath = getClass().getResource("/static/pdfjs/web/viewer.html").toExternalForm();
        engine.load(viewerPath);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    engine.executeScript(
                        "setTimeout(function() {" +
                        "  var pdfData = atob('" + base64Data + "');" +
                        "  var uint8Array = new Uint8Array(pdfData.length);" +
                        "  for (var i = 0; i < pdfData.length; i++) {" +
                        "    uint8Array[i] = pdfData.charCodeAt(i);" +
                        "  }" +
                        "  PDFViewerApplication.open(uint8Array);" +
                        "}, 500);"
                    );
                } catch (Exception ex) {
                    AppLogger.error("Error injecting PDF data", ex);
                }
            }
        });
    }

    public void loadPdf(File pdfFile) {
        try {
        	currentPdfFile = pdfFile;
        	
            lblReportName.setText(pdfFile.getName().toUpperCase());
            WebEngine engine = webView.getEngine();
            engine.setJavaScriptEnabled(true);

            // 1. Get the path to the viewer
            String viewerPath = getClass().getResource("/static/pdfjs/web/viewer.html").toExternalForm();

            // 2. Load the viewer first
            engine.load(viewerPath);

            // 3. Once the viewer is loaded, inject the PDF data
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        // Read PDF file to Base64
                        byte[] data = Files.readAllBytes(pdfFile.toPath());
                        String base64 = Base64.getEncoder().encodeToString(data);

                        // Call PDF.js 'open' function directly with the base64 data
                        // We use a small delay to ensure viewer.js is fully initialized
                        engine.executeScript(
                            "setTimeout(function() {" +
                            "  var pdfData = atob('" + base64 + "');" +
                            "  var uint8Array = new Uint8Array(pdfData.length);" +
                            "  for (var i = 0; i < pdfData.length; i++) {" +
                            "    uint8Array[i] = pdfData.charCodeAt(i);" +
                            "  }" +
                            "  PDFViewerApplication.open(uint8Array);" +
                            "}, 500);"
                        );
                    } catch (Exception ex) {
                        AppLogger.error("Error injecting PDF data", ex);
                    }
                }
            });

        } catch (Exception e) {
            AppLogger.error("PDF Viewer Error", e);
        }
    }
    

    @FXML 
    private void handlePrint() {
        if (currentPdfFile == null || !currentPdfFile.exists()) {
            NotificationManager.show("File no longer exists", NotificationType.WARNING, Pos.CENTER);
            return;
        }

        try {
            // Use the external form to ensure the file protocol is correctly formatted
            String url = currentPdfFile.toURI().toURL().toExternalForm();
            
            if (hostServices != null) {
                hostServices.showDocument(url);
            } else {
                // Manual fallback if hostServices wasn't injected correctly
                java.awt.Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            AppLogger.error("Failed to open document externally", e);
            NotificationManager.show("Could not open system viewer", NotificationType.ERROR, Pos.CENTER);
        }
    }

    @FXML private void handleClose() {
    	mainController.loadDashboard();
    }
}