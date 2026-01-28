package com.badargadh.sahkar;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class JasperFontConfig {

    @PostConstruct
    public void init() {
    	System.out.println("🔍 Font check: " + getClass().getResource("/static/fonts/NotoSansGujarati.ttf"));
        // Get the global context
        DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
        JRPropertiesUtil propertiesUtil = JRPropertiesUtil.getInstance(context);

        // 1. Tell Jasper to ignore missing system fonts and look for extensions
        propertiesUtil.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

        // 2. Register the Extension Factory
        // This is the most critical property. It tells Jasper to use the default extension loader.
        propertiesUtil.setProperty("net.sf.jasperreports.extension.registry.factory.fonts", 
                                   "net.sf.jasperreports.extensions.DefaultExtensionsRegistryFactory");

        // 3. Point to your fonts.xml file
        // Ensure fonts.xml is located in src/main/resources/fonts.xml
        propertiesUtil.setProperty("net.sf.jasperreports.extension.fonts.families.mapping", "fonts.xml");

        System.out.println("✅ JasperReports: Font Extension Registry initialized.");
    }
}