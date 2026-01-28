package com.badargadh.sahkar.service.report;

import java.io.InputStream;
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
public class JasperYearlyReportService2 {

    public void generateYearlyJasperReport(Map<String, MonthlyStatementDTO> yearlyData, String yearRange, String outputPath) throws Exception {
        List<YearlyRowDTO> reportRows = new ArrayList<>();
        
        // Define the month sequence to match your extractor
        String[] months = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
                           "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

        // 1. Opening Balance
        reportRows.add(mapToRow("ગત મહીના ની બી પુરાંત (Opening Balance)", yearlyData, months, MonthlyStatementDTO::getOpeningBal, true));

        // 2. Inflow Section
        reportRows.add(createHeaderRow("આવક (CASH INFLOW)"));
        reportRows.add(mapToRow("નવીન મેમ્બર ટોટલ ફીસ", yearlyData, months, MonthlyStatementDTO::getNewMemberFee, false));
        reportRows.add(mapToRow("મહીના ની કુલ ફીસ આવક", yearlyData, months, MonthlyStatementDTO::getMonthlyFee, false));
        reportRows.add(mapToRow("માસીક હફ્તા ની કુલ આવક", yearlyData, months, MonthlyStatementDTO::getTotalEmi, false));
        reportRows.add(mapToRow("નવીન મેમ્બર ફીસ કપાત", yearlyData, months, MonthlyStatementDTO::getLoanDeduction, false));
        reportRows.add(mapToRow("બીજી આવક (જમા)", yearlyData, months, MonthlyStatementDTO::getExpenseCredit, false));
        reportRows.add(mapToRow("કુલ આવક (TOTAL INFLOW)", yearlyData, months, MonthlyStatementDTO::getTotalIncome, true));

        // 3. Outflow Section
        reportRows.add(createHeaderRow("જાવક (CASH OUTFLOW)"));
        reportRows.add(mapToRow("લોન ની જાવક", yearlyData, months, MonthlyStatementDTO::getTotalLoanGranted, false));
        reportRows.add(mapToRow("કુલ ફીસ રીફંડ", yearlyData, months, MonthlyStatementDTO::getTotalFeeRefund, false));
        reportRows.add(mapToRow("બીજી જાવક (ઉધાર)", yearlyData, months, MonthlyStatementDTO::getExpenseDebit, false));
        reportRows.add(mapToRow("કુલ જાવક (TOTAL OUTFLOW)", yearlyData, months, MonthlyStatementDTO::getTotalOutgoing, true));

        // 4. Closing Balance
        reportRows.add(mapToRow("આખરની બી પુરાંત (NET CLOSING)", yearlyData, months, MonthlyStatementDTO::getClosingBalance, true));

        // Jasper Parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("yearRange", yearRange);

        // Compile and Fill
        InputStream inputStream = getClass().getResourceAsStream("/jrxml/YearlyConsolidatedReport.jrxml");
        JasperReport report = JasperCompileManager.compileReport(inputStream);
        JasperPrint print = JasperFillManager.fillReport(report, parameters, new JRBeanCollectionDataSource(reportRows));
        
        JasperExportManager.exportReportToPdfFile(print, outputPath);
    }

    private YearlyRowDTO createHeaderRow(String label) {
        YearlyRowDTO row = new YearlyRowDTO();
        row.setCategory(label);
        row.setIsSectionHeader(true);
        return row;
    }

    private YearlyRowDTO mapToRow(String label, Map<String, MonthlyStatementDTO> data, String[] months, Function<MonthlyStatementDTO, Double> extractor, boolean isTotal) {
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

        row.setJan(vals[0]); row.setFeb(vals[1]); row.setMar(vals[2]); row.setApr(vals[3]);
        row.setMay(vals[4]); row.setJun(vals[5]); row.setJul(vals[6]); row.setAug(vals[7]);
        row.setSep(vals[8]); row.setOct(vals[9]); row.setNov(vals[10]); row.setDec(vals[11]);

        // Logic for Yearly Total (Horizontal)
        if (label.contains("Opening")) {
            row.setYearlyTotal(vals[0]); // Yearly opening is the first month's opening
        } else if (label.contains("Closing")) {
            row.setYearlyTotal(vals[11]); // Yearly closing is the last month's closing
        } else {
            row.setYearlyTotal(horizontalSum);
        }

        return row;
    }
}