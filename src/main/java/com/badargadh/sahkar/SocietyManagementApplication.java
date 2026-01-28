package com.badargadh.sahkar;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.badargadh.sahkar.service.MySqlBackupService;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.fonts.FontFamily;

@SpringBootApplication
public class SocietyManagementApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Start Spring
        this.springContext = new SpringApplicationBuilder()
                .sources(SocietyManagementApplication.class)
                .run();
    }

    @Override
    public void start(Stage primaryStage) {
        // Publish the event to the Initializer
        springContext.publishEvent(new StageReadyEvent(primaryStage));
    }

    @Override
    public void stop() {
    	MySqlBackupService.performBackup();
        this.springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        Application.launch(SocietyManagementApplication.class, args);
        
        List<FontFamily> families = DefaultJasperReportsContext.getInstance().getExtensions(FontFamily.class);
    	for (FontFamily family : families) {
    	    System.out.println("Available Font: " + family.getName());
    	}
    }
    
    @Bean
    public HostServices getHostServicesBean() {
        return getHostServices();
    }
}