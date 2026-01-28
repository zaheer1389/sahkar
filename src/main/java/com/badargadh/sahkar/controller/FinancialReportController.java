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

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberFeesRefundDTO;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.dto.LoanWitnessBookDTO;
import com.badargadh.sahkar.dto.MonthlyPaymentCollectionDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MemberService;
import com.badargadh.sahkar.service.MonthlyStatementService;
import com.badargadh.sahkar.service.RefundService;
import com.badargadh.sahkar.service.report.JasperReportService;
import com.badargadh.sahkar.service.report.JasperYearlyReportService;
import com.badargadh.sahkar.service.report.JasperYearlyReportService2;
import com.badargadh.sahkar.service.report.YearlyReportService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.FileUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;

@Controller
public class FinancialReportController implements Initializable {

	@FXML private ComboBox<Integer> cmbYear;
	@Autowired private MainController mainController;
    @Autowired private MemberService memberService;
    @Autowired private FinancialMonthService monthService;
    @Autowired private MemberRepository memberRepo;
    @Autowired private MonthlyStatementService statementService;
    @Autowired private FinancialMonthRepository monthRepo;
    @Autowired private YearlyReportService yearlyReportService;
    @Autowired private JasperReportService jasperReportService;
    @Autowired private JasperYearlyReportService jasperYearlyReportService;
    @Autowired private JasperYearlyReportService2 jasperYearlyReportService2;
    @Autowired private LoanService loanService;
    @Autowired private RefundService refundService;
    @Autowired private FeeService feeService;
    @Autowired private AppConfigService configService;
    
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
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Monthly_Collection_Report_" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
        if (!activeMonthOpt.isPresent()) {
            return;
        }
        FinancialMonth month = activeMonthOpt.get();
        List<MonthlyPaymentCollectionDTO> history = memberRepo.findReceivedPaymentsByMonthWithGroup(month.getId());
        if(history.isEmpty()) {
        	Platform.runLater(() -> DialogManager.showInfo("Empty List!!", "Month Collection list is empty"));
    		return;
    	}
    	executeReportTask(() -> {
    		try {
            	jasperReportService.generateCollectionReport(history, month.getMonthId(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				AppLogger.error("Monthly_Collection_Report_Error", e);			}
     		return targetFile;
    	});
    	  
    }
    
    @FXML private void printPendingCollection() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Monthly_Pending_Collection_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	
    	Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
        if (!activeMonthOpt.isPresent()) {
            return;
        }
        FinancialMonth month = activeMonthOpt.get(); 
    	List<PendingMonthlyCollectionDTO> pending = memberRepo.findActiveMembersPendingForMonth1(month.getId());
    	if(pending.isEmpty()) {
    		Platform.runLater(() -> DialogManager.showInfo("Empty List!!", "Pending collection list is empty"));
    		return;
    	}
    	executeReportTask(() -> {
            try {
            	List<PendingMonthlyCollectionDTO> list = pending.stream().map(c -> {
            		c.setFeesAmountDue(configService.getSettings().getMonthlyFees().doubleValue());
            		return c;
            	}).toList();            	
            	jasperReportService.generatePendingCollectionReport(list, month.getMonthId(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				AppLogger.error("Monthly__Pending_Collection_Report_Error", e);			}
     		return targetFile;
    	}); 
    }
    
    @FXML private void printPendingCollectionWithWitnessNames() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Monthly_Pending_Collection_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	
    	Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
        if (!activeMonthOpt.isPresent()) {
            return;
        }
        FinancialMonth month = activeMonthOpt.get(); 
    	List<PendingMonthlyCollectionDTO> pending = memberRepo.findActiveMembersPendingForMonth1(month.getId());
    	if(pending.isEmpty()) {
    		Platform.runLater(() -> DialogManager.showInfo("Empty List!!", "Pending collection list is empty"));
    		return;
    	}
    	
    	executeReportTask(() -> {
            try {
            	List<PendingMonthlyCollectionDTO> pendingListWithWitnessNames = pending.stream().map(c -> {
            		LoanWitness loanWitness = loanService.getLoanWitness(c.getMemberNo());
            		if(loanWitness != null) {
            			if(loanWitness.getWitnessMember() != null) {
            				c.setWitnessNameGuj(loanWitness.getWitnessMember().getGujFullname());
            			}
            			else {
                			c.setWitnessNameGuj(loanWitness.getWintessName());
                		}
            		}
            		return c;
            		
            	}).toList();
            	jasperReportService.generatePendingCollectionWithWitnessNamesReport(pendingListWithWitnessNames, month.getMonthId(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				AppLogger.error("Monthly__Pending_Collection_Report_Error", e);			}
     		return targetFile;
    	}); 
    }
    
    @FXML private void printMonthSummary() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Monthly_Statement_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return null;
            }
            FinancialMonth month = activeMonthOpt.get();
    		MonthlyStatementDTO dto = statementService.getActiveMonthStatement(month);
    		try {
    			//monthlyReportService.generateMonthlyStatementReport(dto, month.getMonthId(), targetFile.getAbsolutePath());
    			jasperReportService.generateMonthlyStatementReport(dto, month.getMonthId(), targetFile.getAbsolutePath());
    	        //NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        AppLogger.error("Monthly_Statement_Report_Generation_Error", e);
    	    }
    		
    		return targetFile;
    		
    	});
    	
    }
    
    @FXML private void printFullPayments() {
    	
    }
    
    @FXML private void printLoanWitnessBook() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Loan_Witness_Book_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return null;
            }
            FinancialMonth month = activeMonthOpt.get();
            
            List<LoanApplication> loanApplications = loanService.getAllApplicationsWithStatuses(month, 
                    Arrays.asList(LoanApplicationStatus.SELECTED_IN_DRAW, LoanApplicationStatus.DISBURSED));
            
            
            if (loanApplications.isEmpty()) {
            	Platform.runLater(() -> DialogManager.showInfo("Empty List!!", "Data not available to generate report"));
            	return null;
            }
            
    		try {
    			List<LoanWitnessBookDTO> data = loanApplications.stream()
                        .filter(d -> d.getDrawRank().startsWith("SL"))
                        .map(m -> {
                        	Member member = m.getMember();
		    				LoanWitnessBookDTO dto = new LoanWitnessBookDTO();
		    				dto.setMemberNo(member.getMemberNo());
		    				dto.setFullNameGuj(member.getGujFullname());
		    				dto.setLoanAmount(m.getAppliedAmount().longValue());
		    				dto.setLoanDate(m.getFinancialMonth().getStartDate().plusDays(9));
		    				return dto;
		    			}).toList();
    			jasperReportService.generateLoanWitnessBookReport(data, month, targetFile.getAbsolutePath());
    	        //NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        AppLogger.error("Loan_Witness_Book_Report_Generation_Error", e);
    	    }
    		
    		return targetFile;
    		
    	});
    }
    
    @FXML private void printMemberReport() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Member_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
     		try {
				//reportService.generateMemberSummaryPdf(memberService.findActiveMembersGujNames(), targetFile.getAbsolutePath());
				jasperReportService.generateMemberReport(memberService.findActiveMembersForReport(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
    	        AppLogger.error("Member_Report_Generation_Error", e);
			}
     		return targetFile;
    	});
    }
    
    @FXML private void printMemberSummaryReport() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Member_Summary_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
     		try {
				//reportService.generateMemberSummaryPdf(memberService.findActiveMembersGujNames(), targetFile.getAbsolutePath());
				jasperReportService.generateMemberSummary(memberService.findActiveMembersGujNames(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
    	        AppLogger.error("Member_Summary_Report_Generation_Error", e);
			}
     		return targetFile;
    	});
    }
    
    @FXML private void printNewJoiners() {
    	
    }
    
    @FXML private void printCancelledMembers() {
    	
    }
    
    @FXML private void printFeesRefundReport() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Fees_Refund_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return null;
            }
            FinancialMonth month = activeMonthOpt.get();
            
            List<MemberFeesRefundDTO> masterList = refundService.getMonthlyRefundReportList(month.getStartDate(), month.getEndDate())
    	    		.stream()
    	    		.map(member -> {
    	    			MemberFeesRefundDTO dto = new MemberFeesRefundDTO();
    	    			dto.setMember(member);
    	    			dto.setFeesRefundAmount(feeService.getMemberTotalFees(member));
    	    			return dto;
    	    		})
    	    		.collect(Collectors.toList());
            
            if (masterList.isEmpty()) {
            	DialogManager.showInfo("Empty List!!", "Data not available to generate report");
            	return null;
            }
            
    		try {
    			//monthlyReportService.generateMonthlyStatementReport(dto, month.getMonthId(), targetFile.getAbsolutePath());
    			//jasperReportService.generateSelectedLoansJasper(selected, month, targetFile.getAbsolutePath());
    			jasperReportService.generateFeesRefundPdf(masterList, targetFile.getAbsolutePath());
    	        //NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        AppLogger.error("Fees_Refund_Report_Generation_Error", e);
    	    }
    		
    		return targetFile;
    		
    	});
    }
    
    @FXML private void printLoanSelectionReport() {
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Loan_Selection_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return null;
            }
            FinancialMonth month = activeMonthOpt.get();
            
            List<LoanApplication> loanApplications = loanService.getAllApplicationsWithStatuses(month, 
                    Arrays.asList(LoanApplicationStatus.SELECTED_IN_DRAW, LoanApplicationStatus.DISBURSED));
            
            List<Member> selected = loanApplications.stream()
                    .filter(d -> d.getDrawRank().startsWith("SL"))
                    .map(LoanApplication::getMember).collect(Collectors.toList());
            
            if (selected.isEmpty()) {
            	Platform.runLater(() -> DialogManager.showInfo("Empty List!!", "Data not available to generate report"));
            	return null;
            }
            
    		try {
    			//monthlyReportService.generateMonthlyStatementReport(dto, month.getMonthId(), targetFile.getAbsolutePath());
    			jasperReportService.generateSelectedLoansJasper(selected, month, targetFile.getAbsolutePath());
    	        //NotificationManager.show("Report generated!!", NotificationType.INFO, Pos.CENTER);
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        AppLogger.error("Loan_Selection_Report_Generation_Error", e);
    	    }
    		
    		return targetFile;
    		
    	});
    }
    
    @FXML private void printCombinedLoanAndFeesRefundReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Loan_Selection_And_Fees_Refund_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
    	executeReportTask(() -> {
    		Optional<FinancialMonth> activeMonthOpt = monthService.getActiveMonth();
            if (!activeMonthOpt.isPresent()) {
                return null;
            }
            FinancialMonth month = activeMonthOpt.get();
            
            List<LoanApplication> loanApplications = loanService.getAllApplicationsWithStatuses(month, 
                    Arrays.asList(LoanApplicationStatus.SELECTED_IN_DRAW, LoanApplicationStatus.DISBURSED));
            
            List<Member> selected = loanApplications.stream()
                    .filter(d -> d.getDrawRank().startsWith("SL"))
                    .map(LoanApplication::getMember).collect(Collectors.toList());
            
            List<Map<String, Object>> refundRows = refundService.getMonthlyRefundReportList(month.getStartDate(), month.getEndDate())
	            .stream().map(m -> {
	                Map<String, Object> map = new HashMap<>();
	                map.put("memberNo", m.getMemberNo());
	                map.put("memberName", m.getGujFullname());
	                map.put("feesRefundAmount", feeService.getMemberTotalFees(m));
	                return map;
	            }).collect(Collectors.toList());
            
    		try {
    			jasperReportService.generateCombinedReport(selected, refundRows, month, targetFile.getAbsolutePath());
    	    } catch (Exception e) {
    	        e.printStackTrace();
    	        AppLogger.error("Loan_Selection_And_Fees_Refund_Report", e);
    	    }
    		
    		return targetFile;
    		
    	});
    }
    
    @FXML private void printYearlyReport() {
    	if(cmbYear.getValue() == null) {
    		DialogManager.showError("Error", "Please select financial year to download report!");
    		return;
    	}
    	
    	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Financial_Year_Report" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
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
				//yearlyReportService.generateYearlyReport(yearlyData, cmbYear.getValue().toString(), targetFile.getAbsolutePath());
				jasperYearlyReportService2.generateYearlyJasperReport(yearlyData, cmbYear.getValue().toString(), targetFile.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
    	        AppLogger.error("Financial_Year_Report_Generation_Error", e);
			}
    		return targetFile;
    	});
    }

    private void executeReportTask(java.util.concurrent.Callable<File> reportLogic) {
        // 1. Show loader (We are currently on the UI thread)
        loadingOverlay.setVisible(true);

        Task<File> task = new Task<File>() {
            @Override
            protected File call() throws Exception {
                // 2. Heavy work happens here (Background thread)
            	return reportLogic.call();
            }
        };

        task.setOnSucceeded(e -> {
        	Platform.runLater(() -> {
                loadingOverlay.setVisible(false);
                File generatedFile = task.getValue();
                
                if (generatedFile != null && generatedFile.exists()) {
                    try {
                        // Safety check: Open the viewer
                        mainController.showReport(generatedFile);
                        
                        // Use a delay or Platform.runLater if Dialogs conflict with the Viewer
                        DialogManager.showInfo("Report Exported", "File saved at: " + generatedFile.getAbsolutePath());
                    } catch (Exception ex) {
                    	ex.printStackTrace();
                        AppLogger.error("Error displaying report", ex);
                    }
                }
            });
        });

        task.setOnFailed(e -> {
            loadingOverlay.setVisible(false);
            task.getException().printStackTrace();
            AppLogger.error("Report Generation Task Error", task.getException());
            DialogManager.showError("Report Failed", task.getException().getMessage());
        });

        // 5. Execution
        Thread thread = new Thread(task);
        thread.setDaemon(true); 
        thread.start();
    }
    
}