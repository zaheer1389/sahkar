package com.badargadh.sahkar.service.report;

import java.io.InputStream;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.dto.YearlyRowDTO;
import com.badargadh.sahkar.util.GujaratiNumericUtils;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class JasperYearlyReportService {

    public void exportReport(Map<String, MonthlyStatementDTO> yearlyData, String yearRange, String outputPath) throws Exception {
        List<YearlyRowDTO> reportRows = new ArrayList<>();

     // Define the month sequence to match your extractor
        String[] months = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
                           "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

        
        // 1. Opening Balance Row
        reportRows.add(createRow("શરૂઆતની બાકી (Opening Balance)", yearlyData, months, MonthlyStatementDTO::getOpeningBal, true));

        // 2. Inflow Section
        reportRows.add(createSectionHeader("આવક (CASH INFLOW)"));
        reportRows.add(createRow("નવી મેમ્બર ફી", yearlyData, months, MonthlyStatementDTO::getNewMemberFee, false));
        reportRows.add(createRow("માસિક ફી આવક", yearlyData, months, MonthlyStatementDTO::getMonthlyFee, false));
        reportRows.add(createRow("હપ્તા વસૂલાત (EMI)", yearlyData, months, MonthlyStatementDTO::getTotalEmi, false));
        reportRows.add(createRow("લોન કપાત", yearlyData, months, MonthlyStatementDTO::getLoanDeduction, false));
        reportRows.add(createRow("કુલ આવક (TOTAL INFLOW)", yearlyData, months, MonthlyStatementDTO::getTotalIncome, true));

        // 3. Outflow Section
        reportRows.add(createSectionHeader("જાવક (CASH OUTFLOW)"));
        reportRows.add(createRow("આપેલ લોન", yearlyData, months, MonthlyStatementDTO::getTotalLoanGranted, false));
        reportRows.add(createRow("ફી રિફંડ", yearlyData, months, MonthlyStatementDTO::getTotalFeeRefund, false));
        reportRows.add(createRow("કુલ જાવક (TOTAL OUTFLOW)", yearlyData, months, MonthlyStatementDTO::getTotalOutgoing, true));

        // 4. Closing Balance
        reportRows.add(createRow("આખરની બાકી (NET CLOSING)", yearlyData, months, MonthlyStatementDTO::getClosingBalance, true));

        // Jasper Parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("yearRange", yearRange);

        // Load JRXML
        InputStream stream = getClass().getResourceAsStream("/jrxml/YearlyConsolidatedReport.jrxml");
        JasperReport report = JasperCompileManager.compileReport(stream);
        
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportRows);
        JasperPrint print = JasperFillManager.fillReport(report, parameters, dataSource);
        
        JasperExportManager.exportReportToPdfFile(print, outputPath);
    }

    private YearlyRowDTO createSectionHeader(String title) {
        YearlyRowDTO row = new YearlyRowDTO();
        row.setCategory(title);
        row.setIsSectionHeader(true);
        // Set all months to 0 for headers
        return row;
    }

    private YearlyRowDTO createRow(String label, Map<String, MonthlyStatementDTO> data,  String[] months,
                                   Function<MonthlyStatementDTO, Double> extractor, boolean isTotal) {
    	YearlyRowDTO row = new YearlyRowDTO();
        row.setCategory(label);
        row.setIsTotalRow(isTotal);

        Double[] vals = new Double[12];
        double horizontalSum = 0;

        for (int i = 0; i < months.length; i++) {
            MonthlyStatementDTO dto = data.get(months[i]);
            double v = (dto != null && extractor.apply(dto) != null) ? extractor.apply(dto) : 0.0;
            vals[i] = v;
            horizontalSum += v;
        }

        // Convert values to Gujarati Strings before setting them in the DTO
        row.setJan(toGujarati(vals[0])); row.setFeb(toGujarati(vals[1])); 
        row.setMar(toGujarati(vals[2])); row.setApr(toGujarati(vals[3]));
        row.setMay(toGujarati(vals[4])); row.setJun(toGujarati(vals[5])); 
        row.setJul(toGujarati(vals[6])); row.setAug(toGujarati(vals[7]));
        row.setSep(toGujarati(vals[8])); row.setOct(toGujarati(vals[9])); 
        row.setNov(toGujarati(vals[10])); row.setDec(toGujarati(vals[11]));

        // Horizontal Total Logic
        double total;
        if (label.contains("Opening")) {
            total = vals[0];
        } else if (label.contains("Closing")) {
            total = vals[11];
        } else {
            total = horizontalSum;
        }
        row.setYearlyTotal(toGujarati(total));

        return row;
    }

    private String toGujarati(Object input) {
        return GujaratiNumericUtils.toGujarati(input);
    }
    
    private Double getValue(MonthlyStatementDTO dto, Function<MonthlyStatementDTO, Double> ex) {
        return (dto != null && ex.apply(dto) != null) ? ex.apply(dto) : 0.0;
    }
}