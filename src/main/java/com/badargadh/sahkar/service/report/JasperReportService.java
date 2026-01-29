package com.badargadh.sahkar.service.report;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberFeesRefundDTO;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.dto.LoanWitnessBookDTO;
import com.badargadh.sahkar.dto.MemberDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class JasperReportService {

	private static final Map<String, String[]> SURNAME_SCHEMES = new LinkedHashMap<>();
	
	static {
	    // Format: { Light Background (Name), Slightly Darker Accent (MemberNo) }
	    SURNAME_SCHEMES.put("KHORAJIYA", new String[]{"#FFEBEE", "#FFCDD2"}); // Soft Pink/Red
	    SURNAME_SCHEMES.put("NONSOLA",   new String[]{"#E8F5E9", "#C8E6C9"}); // Pale Green
	    SURNAME_SCHEMES.put("VARALIYA",  new String[]{"#E3F2FD", "#BBDEFB"}); // Ice Blue
	    SURNAME_SCHEMES.put("KADIVAL",   new String[]{"#FFFDE7", "#FFF9C4"}); // Cream/Yellow
	    SURNAME_SCHEMES.put("CHAUDHARI", new String[]{"#F3E5F5", "#E1BEE7"}); // Light Lavender
	    SURNAME_SCHEMES.put("MALPARA",   new String[]{"#E0F7FA", "#B2EBF2"}); // Light Cyan
	    SURNAME_SCHEMES.put("NODOLIYA",  new String[]{"#EFEBE9", "#D7CCC8"}); // Light Taupe/Brown
	    SURNAME_SCHEMES.put("BHORANIYA", new String[]{"#F1F8E9", "#DCEDC8"}); // Light Lime
	    SURNAME_SCHEMES.put("AGLODIYA",  new String[]{"#F9FBE7", "#F0F4C3"}); // Pale Olive
	    SURNAME_SCHEMES.put("MANASIYA",  new String[]{"#E8EAF6", "#C5CAE9"}); // Light Indigo
	    SURNAME_SCHEMES.put("MAREDIYA",  new String[]{"#FFF3E0", "#FFE0B2"}); // Light Orange/Peach
	    SURNAME_SCHEMES.put("SHERU",     new String[]{"#ECEFF1", "#CFD8DC"}); // Blue Grey
	    SURNAME_SCHEMES.put("SUNASARA",  new String[]{"#FAFAFA", "#F5F5F5"}); // Soft Grey
	}
	
	public void forceLoadGujaratiFonts() {
	    DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
	    JRPropertiesUtil propertiesUtil = JRPropertiesUtil.getInstance(context);

	    // 1. Tell Jasper to ignore missing fonts in the system (AWT) 
	    // and use our extension instead.
	    propertiesUtil.setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");

	    // 2. Force register the fonts.xml file path from your resources.
	    // This maps the Logical Name to the TTF files.
	    propertiesUtil.setProperty("net.sf.jasperreports.extension.registry.factory.fonts", 
	                               "net.sf.jasperreports.extensions.DefaultExtensionsRegistryFactory");
	    propertiesUtil.setProperty("net.sf.jasperreports.extension.fonts.families.mapping", 
	                               "fonts.xml");
	}

    public void generateMemberSummary(List<MemberSummaryDTO> data, String filePath) throws Exception {
        // Pre-process: Find color based on 'contains' match
        for (MemberSummaryDTO dto : data) {
            String last = (dto.getLastName() != null) ? dto.getLastName().toUpperCase().trim() : "";
            String [] matchedColor = {}; // Default White
            
            for (Map.Entry<String, String[]> entry : SURNAME_SCHEMES.entrySet()) {
                if (last.contains(entry.getKey())) {
                    matchedColor = entry.getValue();
                    break;
                }
            }
            dto.setRowColor(matchedColor[0]);    // Light color for the Name column
            dto.setAccentColor(matchedColor[1]); // Darker color for the M.No column
        }
        
        for(int i = 0 ; i < 137; i++) {
        	data.add(new MemberSummaryDTO(null, "", "", "", "", ""));
        }
        
        List<Map<String, String>> legendData = new ArrayList<>();
        
        SURNAME_SCHEMES.forEach((name, colors) -> {
            Map<String, String> row = new HashMap<>();
            row.put("surname", name);
            row.put("color", colors[0]); // The light background
            legendData.add(row);
        });
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LegendDataSource", new JRBeanCollectionDataSource(legendData));
        parameters.put("LegendSubreport", JasperCompileManager.compileReport(getClass().getResourceAsStream("/jrxml/SurnameIndex.jrxml")));

        InputStream stream = getClass().getResourceAsStream("/jrxml/MemberSummary.jrxml");
        JasperReport report = JasperCompileManager.compileReport(stream);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
        
        JasperPrint print = JasperFillManager.fillReport(report, parameters, ds);
        JasperExportManager.exportReportToPdfFile(print, filePath);
    }
    
    public void generateMemberReport(List<MemberDTO> data, String filePath) throws Exception {
        InputStream stream = getClass().getResourceAsStream("/jrxml/MemberReport.jrxml");
        JasperReport report = JasperCompileManager.compileReport(stream);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
        
        JasperPrint print = JasperFillManager.fillReport(report, new HashMap<>(), ds);
        JasperExportManager.exportReportToPdfFile(print, filePath);
    }
    
    public void generateLoanWitnessBookReport(List<LoanWitnessBookDTO> data, FinancialMonth month, String filePath) throws Exception {
    	
    	DateTimeFormatter dtfGujMonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("gu", "IN"));
    	Map<String, Object> parameters = new HashMap<>();
    	parameters.put("monthYear", dtfGujMonthYear.format(month.getStartDate()));
    	
        InputStream stream = getClass().getResourceAsStream("/jrxml/LoanWitnessBook.jrxml");
        JasperReport report = JasperCompileManager.compileReport(stream);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);
        
        JasperPrint print = JasperFillManager.fillReport(report, parameters, ds);
        JasperExportManager.exportReportToPdfFile(print, filePath);
    }
    
    public void generateCollectionReport(List<MonthlyPaymentCollectionDTO> history, String monthInfo, String destPath) {
        try {
            // 1. Load the JRXML file from resources
            InputStream reportStream = getClass().getResourceAsStream("/jrxml/MonthlyCollection.jrxml");
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // 2. Set Parameters (Headers/Global Info)
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("monthInfo", monthInfo);

            // 3. Convert List to Jasper Data Source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(history);

            // 4. Fill and Export
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, destPath);

        } catch (Exception e) {
            throw new RuntimeException("Error generating monthly collection report", e);
        }
    }
    
    public void generatePendingCollectionReport(List<PendingMonthlyCollectionDTO> pendingList, String monthInfo, String destPath) {
        try {
            InputStream reportStream = getClass().getResourceAsStream("/jrxml/PendingCollection.jrxml");
            
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("monthInfo", monthInfo);

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(pendingList);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            
            JasperExportManager.exportReportToPdfFile(jasperPrint, destPath);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Pending collection Report", e);
        }
    }
    
    public void generatePendingCollectionWithWitnessNamesReport(List<PendingMonthlyCollectionDTO> pendingList, String monthInfo, String destPath) {
        try {
            InputStream reportStream = getClass().getResourceAsStream("/jrxml/PendingCollectionWithWitnessName.jrxml");
            
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("monthInfo", monthInfo);

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(pendingList);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            
            JasperExportManager.exportReportToPdfFile(jasperPrint, destPath);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Pending collection Report", e);
        }
    }
    
    public void generateMonthlyStatementReport(MonthlyStatementDTO data, String monthName, String filePath) throws Exception {
        
        // 1. Load and Compile the Subreport first
        InputStream subStream = getClass().getResourceAsStream("/jrxml/FlexibleTableSubreport.jrxml");
        JasperReport compiledSubreport = JasperCompileManager.compileReport(subStream);

        // 2. Load and Compile Main Report
        InputStream mainStream = getClass().getResourceAsStream("/jrxml/MonthlyStatement.jrxml");
        JasperReport compiledMainReport = JasperCompileManager.compileReport(mainStream);

        // 3. Setup Parameters
        Map<String, Object> params = new HashMap<>();
        params.put("monthName", monthName);
        params.put("openingBal", data.getOpeningBal().intValue());
        params.put("closingBal", data.getClosingBalance().intValue());
        params.put("totalInflow", data.getTotalIncome().intValue());
        params.put("totalOutflow", data.getTotalOutgoing().intValue());
        
        // Pass the Subreport Object itself
        params.put("TableSubreportObject", compiledSubreport);
        
     // Inflow Breakdown
        params.put("newMemberFee", data.getNewMemberFee().intValue());
        params.put("monthlyFee", data.getMonthlyFee().intValue());
        params.put("loanDeduction", data.getLoanDeduction().intValue());
        params.put("totalEmi", data.getTotalEmi().intValue());
        params.put("fullPaymentAmount", data.getFullPaymentAmount().intValue());
        params.put("expenseCredit", data.getExpenseCredit().intValue());

        // Outflow Breakdown
        params.put("totalLoanGranted", data.getTotalLoanGranted().intValue());
        params.put("totalFeeRefund", data.getTotalFeeRefund().intValue());
        params.put("expenseDebit", data.getExpenseDebit().intValue());

        // Logic to convert the Map breakdown to a single string for the report
        StringBuilder emiStr = new StringBuilder();
        if (data.getEmiBreakdown() != null) {
            data.getEmiBreakdown().forEach((amt, count) -> {
                emiStr.append(amt.intValue()).append(" x ").append(count)
                      .append(" = ").append((int)(amt * count)).append("\n"); // Use \n for new line
            });
        }
        params.put("emiBreakdownString", emiStr.toString().trim());
        
        // Pass the raw data lists for the subreports to use
        params.put("NewMembersData", data.getNewMembersData() != null ? data.getNewMembersData() : new ArrayList<>());
        params.put("RefundsData", data.getRefundMembersData() != null ? data.getRefundMembersData() : new ArrayList<>());
        params.put("FullPaymentsData", data.getFullpaymentsData() != null ? data.getFullpaymentsData() : new ArrayList<>());
        params.put("PrevMonthLoansData", data.getPrevMonthNewLoansEMIAmtData() != null ? data.getPrevMonthNewLoansEMIAmtData() : new ArrayList<>());
        params.put("NewLoansData", data.getNewLoanListMembersData() != null ? data.getNewLoanListMembersData() : new ArrayList<>());
        params.put("CancelledMembersData", data.getCancelledMembersData() != null ? data.getCancelledMembersData() : new ArrayList<>());

        // 4. Fill and Export
        JasperPrint print = JasperFillManager.fillReport(compiledMainReport, params, new JREmptyDataSource());
        JasperExportManager.exportReportToPdfFile(print, filePath);
    }
    
    public void generateSelectedLoansJasper(List<Member> data, FinancialMonth month, String filePath) throws Exception {
        // 1. Prepare Data Source
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

        // 2. Prepare Parameters
        DateTimeFormatter dtfGuj = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("gu", "IN"));
        
        DateTimeFormatter dtfGujMonthYear = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("gu", "IN"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("reportDate", dtfGuj.format(LocalDate.now()));
        parameters.put("monthLabel", dtfGujMonthYear.format(month.getStartDate().plusDays(9)));
        parameters.put("totalCount", data.size());

        // 3. Get Jasper Context (using your manually loaded font config)
        DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
        
        // 4. Compile and Fill
        try (InputStream reportStream = getClass().getResourceAsStream("/jrxml/LoanSelectionListReport.jrxml")) {
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            
            JasperPrint jasperPrint = JasperFillManager.getInstance(context)
                    .fill(jasperReport, parameters, dataSource);

            // 5. Export to PDF
            JasperExportManager.getInstance(context).exportToPdfFile(jasperPrint, filePath);
        }
    }
    
    public void generateFeesRefundPdf(List<MemberFeesRefundDTO> masterList, String filePath) throws Exception {
        
        // 1. Flatten the data for Jasper
        List<Map<String, Object>> reportData = masterList.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberNo", item.getMember().getMemberNo());
            map.put("memberName", item.getMember().getGujFullname());
            map.put("feesRefundAmount", item.getFeesRefundAmount());
            return map;
        }).collect(Collectors.toList());

        // 2. Calculate Total for Parameter
        double totalAmount = masterList.stream()
                .mapToDouble(MemberFeesRefundDTO::getFeesRefundAmount)
                .sum();

        // 3. Parameters
        DateTimeFormatter dtfGuj = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("gu", "IN"));
        Map<String, Object> params = new HashMap<>();
        params.put("reportDate", dtfGuj.format(LocalDate.now()));
        params.put("totalRefundAmount", totalAmount);

        // 4. Jasper Context & Generation
        DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
        
        try (InputStream is = getClass().getResourceAsStream("/jrxml/FeesRefundReport.jrxml")) {
            JasperReport jr = JasperCompileManager.compileReport(is);
            JasperPrint jp = JasperFillManager.getInstance(context)
                    .fill(jr, params, new JRBeanCollectionDataSource(reportData));
            
            JasperExportManager.getInstance(context).exportToPdfFile(jp, filePath);
        }
    }
    
    public void generateCombinedReport(List<Member> selectedLoans, List<Map<String, Object>> refundRows, FinancialMonth month, String filePath) throws Exception {
        DefaultJasperReportsContext context = DefaultJasperReportsContext.getInstance();
        DateTimeFormatter dtfGuj = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("gu", "IN"));

        // 1. Prepare Loan Data Source (Flattened if needed)
        JRBeanCollectionDataSource loanDS = new JRBeanCollectionDataSource(selectedLoans);
        
        double refundTotal = refundRows.stream().mapToDouble(r -> (Double) r.get("feesRefundAmount")).sum();
        JRBeanCollectionDataSource refundDS = new JRBeanCollectionDataSource(refundRows);

        // 3. Compile Subreports and Master
        JasperReport masterReport = JasperCompileManager.compileReport(getClass().getResourceAsStream("/jrxml/CombinedMonthlyReport.jrxml"));
        JasperReport loanSub = JasperCompileManager.compileReport(getClass().getResourceAsStream("/jrxml/LoanSelectionSubReport.jrxml"));
        JasperReport refundSub = JasperCompileManager.compileReport(getClass().getResourceAsStream("/jrxml/FeesRefundSubReport.jrxml"));

        // 4. Setup Master Parameters
        Map<String, Object> params = new HashMap<>();
        params.put("reportDate", dtfGuj.format(LocalDate.now()));
        params.put("monthLabel", dtfGuj.format(month.getStartDate().plusDays(9)));
        
        params.put("LoanSubreportObject", loanSub);
        params.put("LoanDataSource", loanDS);
        params.put("LoanCount", selectedLoans.size());
        
        params.put("RefundSubreportObject", refundSub);
        params.put("RefundDataSource", refundDS);
        params.put("RefundTotalAmount", refundTotal);

        // 5. Fill and Export
        JasperPrint print = JasperFillManager.getInstance(context).fill(masterReport, params, new JREmptyDataSource());
        JasperExportManager.getInstance(context).exportToPdfFile(print, filePath);
    }
}