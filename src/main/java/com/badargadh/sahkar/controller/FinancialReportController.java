package com.badargadh.sahkar.controller;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.MonthlyStatementService;
import com.badargadh.sahkar.service.report.MonthlyReportService;
import com.badargadh.sahkar.service.report.ReportService;
import com.badargadh.sahkar.service.report.YearlyReportService;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

@Controller
public class FinancialReportController implements Initializable {

	@FXML private ComboBox<Integer> cmbYear;
	
    @Autowired private ReportService reportService;
    @Autowired private MemberService memberService;
    @Autowired private LoanService loanService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private MemberRepository memberRepo;
    @Autowired private MonthlyStatementService statementService;
    @Autowired private MonthlyReportService monthlyReportService;
    @Autowired private FinancialMonthRepository monthRepo;
    @Autowired private YearlyReportService yearlyReportService;
    
    @FXML private StackPane loadingOverlay;
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    	// TODO Auto-generated method stub
    	int currentYear = LocalDate.now().getYear();
        cmbYear.setItems(FXCollections.observableArrayList(currentYear - 1, currentYear, currentYear + 1));
        cmbYear.setValue(currentYear);
    }
    
    @FXML private void printMonthCollection() {
    	String filePath = getSavePath("Monthly_Collection_Report");
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return;
            }
            FinancialMonth month = activeMonthOpt.get();
            // 1. Fetch data from DB
            List<MemberSummaryDTO> history = memberRepo.findReceivedPaymentsByMonth(month.getId());

    	    // Define Headers
    	    List<String> headers = List.of("Date", "M. No", "Name", "EMI", "Fees", "Joining Fee");

    	    // Convert DTOs to Row Data (List of Strings)
    	    List<List<String>> dataRows = history.stream().map(dto -> Arrays.asList(
    	    	    dto.getTransactionDate() != null ? dto.getTransactionDate().format(formatter) : "",
    	    	    String.valueOf(dto.getMemberNo()),
    	    	    dto.getFullName() != null ? dto.getFullName() : "",
    	    	    dto.getEmiAmount() != null ? String.valueOf(dto.getEmiAmount()) : "0",
    	    	    dto.getTotalFees() != null ? String.valueOf(dto.getTotalFees()) : "0",
    	    	    dto.getJoiningFees() != null ? String.valueOf(dto.getJoiningFees()) : "0"
    	    	)).collect(Collectors.toList());

    	    // 2. Define Column Sizes (Must sum to 100 or be relative proportions)
    	    // Date(15%), M.No(10%), Name(40%), EMI(12%), Fees(12%), Join(11%)
    	    float[] widths = {14f, 10f, 46f, 10f, 10f, 10f};
    	    
    	    try {
    	        boolean generated = reportService.generateGenericPdf(
    	            "Monthly Collection Report",
    	            "Month: " + month.getMonthId(),
    	            headers,
    	            widths,
    	            dataRows,
    	            filePath
    	        );
    	        if(generated) {
    	        	NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	        }
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	    }
    	}, filePath);
    	  
    }
    
    @FXML private void printPendingCollection() {
    	String filePath = getSavePath("Monthly_Pending_Collection_Report");
		
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return;
            }
            FinancialMonth month = activeMonthOpt.get();
            // 1. Fetch data from DB
    		List<MemberSummaryDTO> history = memberRepo.findActiveMembersPendingForMonth(month.getId());

    	    // Define Headers
    	    List<String> headers = List.of("M. No", "Name", "EMI", "Fees");

    	    // Convert DTOs to Row Data (List of Strings)
    	    List<List<String>> dataRows = history.stream().map(dto -> Arrays.asList(
    	    	    String.valueOf(dto.getMemberNo()),
    	    	    dto.getFullName() != null ? dto.getFullName() : "",
    	    	    dto.getEmiAmount() != null ? String.valueOf(dto.getEmiAmount()) : "0",
    	    	    dto.getTotalFees() != null ? String.valueOf(dto.getTotalFees()) : "0"
    	    	)).collect(Collectors.toList());

    	    // 2. Define Column Sizes (Must sum to 100 or be relative proportions)
    	    // Date(15%), M.No(10%), Name(40%), EMI(12%), Fees(12%), Join(11%)
    	    float[] widths = {15f, 65f, 10f, 10f};
    	    
    	    try {
    	        boolean generated = reportService.generateGenericPdf(
    	            "Monthly Pending Collection Report",
    	            "Month: " + month.getMonthId(),
    	            headers,
    	            widths,
    	            dataRows,
    	            filePath
    	        );
    	        if(generated) {
    	        	//NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	        }
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	    }
    	}, null); 
    }
    
    @FXML private void printMonthSummary() {
    	String filePath = getSavePath("Monthly Statement Report");
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return;
            }
            FinancialMonth month = activeMonthOpt.get();
    		MonthlyStatementDTO dto = statementService.getActiveMonthStatement(month);
    		try {
    			monthlyReportService.generateMonthlyStatementReport(dto, month.getMonthId(), filePath);
    	        //NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	    }
    	}, filePath);
    }
    
    @FXML private void printFullPayments() {
    	
    }
    
    @FXML private void printMemberReport() {
    	String filePath = getSavePath("Member Summary Report");
    	executeReportTask(() -> {
     		try {
				reportService.generateMemberSummaryPdf(memberService.findActiveMembers(), filePath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}, filePath);
    }
    
    @FXML private void printNewJoiners() {
    	
    }
    
    @FXML private void printCancelledMembers() {
    	
    }
    
    @FXML private void printFeeRefunds() {
    	
    }
    
    @FXML private void printYearlyReport() {
    	if(cmbYear.getValue() == null) {
    		DialogManager.showError("Error", "Please select financial year to download report!");
    		return;
    	}
    	
    	String filePath = getSavePath("Financial Year Statement Report");
    	executeReportTask(() -> {
    		/*Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return;
            }*/

    		List<FinancialMonth> financialMonths = monthRepo.findAllByYearOrderByIdAsc(cmbYear.getValue());    		
    		Map<String, MonthlyStatementDTO> yearlyData =  new HashMap<String, MonthlyStatementDTO>();
    		for(FinancialMonth month : financialMonths) {
    			MonthlyStatementDTO dto = statementService.getActiveMonthStatement(month);
    			yearlyData.put(month.getMonthName(), dto);
    		}
    		
    		try {
				yearlyReportService.generateYearlyReport(yearlyData, cmbYear.getValue().toString(), filePath);        				        				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
    	}, filePath);
    }

    private String getSavePath(String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(defaultName + "_" + LocalDateTime.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);
        return (file != null) ? file.getAbsolutePath() : null;
    }

    private void executeReportTask(Runnable reportLogic, String savedPath) {
        // 1. Show loader (We are currently on the UI thread)
        loadingOverlay.setVisible(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // 2. Heavy work happens here (Background thread)
                reportLogic.run();
                return null;
            }
        };

        // 3. Task Success Handler (Automatically returns to UI thread)
        task.setOnSucceeded(e -> {
            loadingOverlay.setVisible(false);
            DialogManager.showInfo("Report Exported", "File saved at: " + savedPath);
        });

        // 4. Task Failure Handler (Automatically returns to UI thread)
        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            Throwable ex = task.getException();
            ex.printStackTrace();
            DialogManager.showError("Report Failed", ex.getMessage());
        });

        // 5. Execution
        Thread thread = new Thread(task);
        thread.setDaemon(true); 
        thread.start();
    }
}