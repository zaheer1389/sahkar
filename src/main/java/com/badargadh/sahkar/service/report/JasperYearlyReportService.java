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

        // 1. Opening Balance Row
        reportRows.add(createRow("શરૂઆતની બાકી (Opening Balance)", yearlyData, MonthlyStatementDTO::getOpeningBal, true));

        // 2. Inflow Section
        reportRows.add(createSectionHeader("આવક (CASH INFLOW)"));
        reportRows.add(createRow("નવી મેમ્બર ફી", yearlyData, MonthlyStatementDTO::getNewMemberFee, false));
        reportRows.add(createRow("માસિક ફી આવક", yearlyData, MonthlyStatementDTO::getMonthlyFee, false));
        reportRows.add(createRow("હપ્તા વસૂલાત (EMI)", yearlyData, MonthlyStatementDTO::getTotalEmi, false));
        reportRows.add(createRow("લોન કપાત", yearlyData, MonthlyStatementDTO::getLoanDeduction, false));
        reportRows.add(createRow("કુલ આવક (TOTAL INFLOW)", yearlyData, MonthlyStatementDTO::getTotalIncome, true));

        // 3. Outflow Section
        reportRows.add(createSectionHeader("જાવક (CASH OUTFLOW)"));
        reportRows.add(createRow("આપેલ લોન", yearlyData, MonthlyStatementDTO::getTotalLoanGranted, false));
        reportRows.add(createRow("ફી રિફંડ", yearlyData, MonthlyStatementDTO::getTotalFeeRefund, false));
        reportRows.add(createRow("કુલ જાવક (TOTAL OUTFLOW)", yearlyData, MonthlyStatementDTO::getTotalOutgoing, true));

        // 4. Closing Balance
        reportRows.add(createRow("આખરની બાકી (NET CLOSING)", yearlyData, MonthlyStatementDTO::getClosingBalance, true));

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

    private YearlyRowDTO createRow(String label, Map<String, MonthlyStatementDTO> data, 
                                   Function<MonthlyStatementDTO, Double> extractor, boolean isTotal) {
        YearlyRowDTO row = new YearlyRowDTO();
        row.setCategory(label);
        row.setIsTotalRow(isTotal);
        
        // Map months - Logic follows your original function
        row.setJan(getValue(data.get(Month.JANUARY.name()), extractor));
        row.setFeb(getValue(data.get(Month.FEBRUARY.name()), extractor));
        row.setMar(getValue(data.get(Month.MARCH.name()), extractor));
        row.setApr(getValue(data.get(Month.APRIL.name()), extractor));
        row.setMay(getValue(data.get(Month.MAY.name()), extractor));
        row.setJun(getValue(data.get(Month.JUNE.name()), extractor));
        row.setJul(getValue(data.get(Month.JULY.name()), extractor));
        row.setAug(getValue(data.get(Month.AUGUST.name()), extractor));
        row.setSep(getValue(data.get(Month.SEPTEMBER.name()), extractor));
        row.setOct(getValue(data.get(Month.OCTOBER.name()), extractor));
        row.setNov(getValue(data.get(Month.NOVEMBER.name()), extractor));
        row.setDec(getValue(data.get(Month.DECEMBER.name()), extractor));

        // Calculate Horizontal Total
        double sum = 0;
        // Special logic for Opening/Closing (taking start/end instead of sum)
        if (label.contains("Opening")) {
             sum = row.getJan(); // Or first non-null
        } else if (label.contains("Closing")) {
             sum = row.getDec(); // Or last non-null
        } else {
             sum = row.getJan() + row.getFeb() + row.getMar() + row.getApr() + row.getMay() + row.getJun() +
            		 row.getJul() + row.getAug() + row.getSep() + row.getOct() + row.getNov() + row.getDec() ;
        }
        row.setYearlyTotal(sum);
        return row;
    }

    private Double getValue(MonthlyStatementDTO dto, Function<MonthlyStatementDTO, Double> ex) {
        return (dto != null && ex.apply(dto) != null) ? ex.apply(dto) : 0.0;
    }
}