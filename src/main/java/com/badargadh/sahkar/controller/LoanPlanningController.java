package com.badargadh.sahkar.controller;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.LoanRDSCandidateDTO;
import com.badargadh.sahkar.dto.MonthlyFundSummaryDTO;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MonthlyLoanFundsCalculationService;
import com.badargadh.sahkar.service.report.JasperReportService;
import com.badargadh.sahkar.util.AppLogger;
import com.badargadh.sahkar.util.DialogManager;
import com.badargadh.sahkar.util.FileUtil;
import com.badargadh.sahkar.util.GujaratiNumericUtils;
import com.badargadh.sahkar.util.NotificationManager;
import com.badargadh.sahkar.util.NotificationManager.NotificationType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

@Component
public class LoanPlanningController extends BaseController implements Initializable {

    @FXML private Label lblMonthInfo, lblOpeningBal, lblExpectedFees, lblExpectedEmi;
    @FXML private Label lblFreshLoansVirtualEmi, lblRefunds, lblNetAvailable;
    @FXML private Label lblPossible50k, lblStatus, lblSelectedCount, lblWaitingCount;
    @FXML private Label lblFullPayments, lblNewMemberFeeDeductions, lblMiscCredis;
    @FXML private TextField txtTotalLoanCanBeOpen;
    @FXML private TableView<LoanRDSCandidateDTO> tblDrawResults;
    @FXML private TableColumn<LoanRDSCandidateDTO, String> colDrawMemNo, colDrawName, colLastLoanDate, colDrawStatus, colDrawRank;
    
    @FXML private Button btnRunDraw, btnConfirmSave, btnRefresh;

    @Autowired private MainController mainController; 
    @Autowired private FinancialMonthService monthService;
    @Autowired private LoanApplicationRepository loanAppRepo;
    @Autowired private LoanService loanService;
    @Autowired private AppConfigService appConfigService;
    @Autowired private JasperReportService jasperReportService;
    @Autowired private FeeService feeService;
    @Autowired private MonthlyLoanFundsCalculationService calculationService;
    
    private AppConfig appConfig;
    private Long loanAmount;
    private FinancialMonth month;
    private Thread drawThread;
    
    private Map<Long, LoanApplication> memberRankMap = new HashMap<>();
    private Map<Long, String> drawTypeMap = new HashMap<>();

    private Double expectedMonthlyTotalAmount;
    
    private int expectedLoanCanBeOpened, selectedCount;
    
    private List<LoanRDSCandidateDTO> candidateDTOs;
    private List<LoanApplication> loanApplications;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {     
        appConfig = appConfigService.getSettings();
        loanAmount = appConfig.getLoanAmount();
        
        btnRunDraw.setDisable(true);
        
        monthService.getActiveMonth().ifPresent(activeMonth -> {
            this.month = activeMonth;
            if(activeMonth.getStartDate().isAfter(LocalDate.of(2025, Month.DECEMBER, 31))) {
            	btnRunDraw.setDisable(false);
            	
            	setupTable();
                refresh();
                applyNumericFilter(txtTotalLoanCanBeOpen);
            }
        });
    }

    private void setupTable() {
        colDrawMemNo.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMember().getMemberNo())));
        colDrawName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMember().getFullname()));
        colLastLoanDate.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(loanService.calculateMemberLoanPriority(d.getValue().getMember(), month))));
        
        colDrawStatus.setCellValueFactory(d -> {
            LoanApplication app = d.getValue().getApplication();
            String status = app.getStatus() != null ? app.getStatus().name() : "N/A";
            String tag = drawTypeMap.get(d.getValue().getMember().getId());
            if (tag != null && status.equals("SELECTED_IN_DRAW")) {
                return new SimpleStringProperty("SELECTED (" + tag.replace("_", " ") + ")");
            }
            return new SimpleStringProperty(status);
        });
        
        colDrawRank.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getApplication().getDrawRank()));

        colDrawStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String style = "-fx-padding: 2 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white;";
                    if (item.contains("PHASE 1")) badge.setStyle(style + "-fx-background-color: #27ae60;");
                    else if (item.contains("PHASE 2")) badge.setStyle(style + "-fx-background-color: #f39c12;");
                    else if (item.equals("DISBURSED")) badge.setStyle(style + "-fx-background-color: #8e44ad;");
                    else badge.setStyle(style + "-fx-background-color: #34495e;");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colDrawRank.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String color = item.startsWith("SL") ? "#2c3e50" : "#7f8c8d";
                    badge.setStyle("-fx-padding: 2 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " + color + ";");
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    public void refresh() {
        if (this.month == null) return;
        
        loanApplications = loanService.getAllApplicationsWithStatuses(this.month, 
                Arrays.asList(LoanApplicationStatus.APPLIED, LoanApplicationStatus.SELECTED_IN_DRAW, 
                              LoanApplicationStatus.DISBURSED, LoanApplicationStatus.WAITING));
        
        MonthlyFundSummaryDTO fundSummary = calculationService.getMonthlyFundSummaryDTO(loanApplications, month);
        fundSummary.setDefaultEmiamount(200);
        
        double feeDeduction = fundSummary.getTotalFeesDeductions();      
        double totalOfOldEmis = fundSummary.getTotalTargetEmi();
        double totalOfFreshLoans = fundSummary.getTotalFreshLoansAmount();
        double opening = fundSummary.getOpeningBal();
        double fees = fundSummary.getExpectedMonthlyFees();
        double refunds = fundSummary.getTotalRefundLiability();
        double fullpayments = fundSummary.getTotalFullPayment();
        double otherMiscCredits = fundSummary.getOtherMiscCredit();
        
        double expectedNet = fundSummary.getExpectedNet();
        
        lblOpeningBal.setText(String.format("₹ %,.2f", opening));
        lblExpectedFees.setText(String.format("₹ %,.2f", fees));
        lblExpectedEmi.setText(String.format("₹ %,.2f", totalOfOldEmis));
        lblFreshLoansVirtualEmi.setText(String.format("₹ %,.2f", totalOfFreshLoans));
        lblRefunds.setText(String.format("- ₹ %,.2f", (refunds)));
        lblNetAvailable.setText(String.format("₹ %,.2f", expectedNet));
        lblFullPayments.setText(String.format("₹ %,.2f", fullpayments));
        lblNewMemberFeeDeductions.setText(String.format("₹ %,.2f", feeDeduction));
        lblMiscCredis.setText(String.format("₹ %,.2f", otherMiscCredits));

        // 3. Logic: Use Actual if it's more than Expected, otherwise use Expected
        // This ensures that if extra money came in, we open more loan slots.
        expectedMonthlyTotalAmount = expectedNet;
        
        // Calculate slots based on the higher amount
        expectedLoanCanBeOpened = (int) (expectedMonthlyTotalAmount / loanAmount);
        
        //expectedLoanCanBeOpened = 16;
        
        txtTotalLoanCanBeOpen.setText(String.valueOf(expectedLoanCanBeOpened));

        
        // 4. Update UI Labels
        lblOpeningBal.setText(String.format("₹ %,.0f", opening));
        
        // Suggestion: Display both to the admin so they see the difference
        //lblNetAvailable.setText(String.format("Exp: ₹ %,.0f | Act: ₹ %,.0f", expectedNet, actualMonthlyPaymentAmount));
        lblNetAvailable.setText(String.format("Exp: ₹ %,.0f", expectedNet));
        
        // The "Allocated" fund display (The higher value)
        // lblStatus.setText(String.format("Allocating from: ₹ %,.0f", expectedMonthlyTotalAmount));
        
        lblPossible50k.setText(String.valueOf(expectedLoanCanBeOpened));
        
        // 5. Sync Table and Counters
        

        selectedCount = (int) loanApplications.stream()
            .filter(a -> a.getDrawRank() != null && a.getDrawRank().startsWith("SL"))
            .count();
        lblSelectedCount.setText(String.valueOf(selectedCount));

        candidateDTOs = loanApplications.stream()
            .map(a -> new LoanRDSCandidateDTO(a.getMember(), a, LocalDateTime.now()))
            .sorted(Comparator.comparing(dto -> dto.getApplication().getDrawRank(), Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        tblDrawResults.setItems(FXCollections.observableArrayList(candidateDTOs));
    }
    
    

    @FXML
    private void executeComputerizedDraw() {
    	
    	if(loanApplications != null && loanApplications.size() == 0) {
    		DialogManager.showError("Zero Applications", "No loan applications present to run draw");
            return;
    	}
    	
        if (expectedLoanCanBeOpened <= selectedCount) {
            DialogManager.showError("Funds Full", "All available slots are already filled.");
            return;
        }
        
        int loans = 0;
        if(txtTotalLoanCanBeOpen.getText() != null && !txtTotalLoanCanBeOpen.getText().isEmpty()) {
        	loans = Integer.parseInt(txtTotalLoanCanBeOpen.getText());
        }
        
        if(DialogManager.confirm("Please confirm!", "Do you really want to run computerized selection of loan applications?")) {
        	if(loans > expectedLoanCanBeOpened) {
        		String msg = "You are going to run draw for "+loans+" loans as per budget only "+expectedLoanCanBeOpened+" loans can be open."
        				+ "Are you sure you want to go ahead with "+loans+" loans draw.";
        		 if(DialogManager.confirm("Please confirm!", msg)) {
        			 expectedLoanCanBeOpened = loans;
        			 runDraw();
        		 }
        	}
        	else {
        		runDraw();
        	}
        }

        
    }
    
    private void runDraw() {
    	mainController.showBlocker("PREPARING TWO-PHASE DRAW...");

        Task<List<LoanRDSCandidateDTO>> drawTask = new Task<>() {
            @Override
            protected List<LoanRDSCandidateDTO> call() throws Exception {
                List<LoanRDSCandidateDTO> results = new ArrayList<>();
                memberRankMap.clear();
                drawTypeMap.clear();

                // 1. Check for saved Phase 1 results
                List<LoanApplication> allApps = loanService.getAllApplicationsWithStatuses(month, 
                        Arrays.asList(LoanApplicationStatus.APPLIED, LoanApplicationStatus.SELECTED_IN_DRAW, 
                                      LoanApplicationStatus.DISBURSED, LoanApplicationStatus.WAITING));

                List<LoanApplication> phase1Saved = allApps.stream()
                        .filter(a -> !a.isSurplusNoticeApp() && a.getDrawRank() != null)
                        .collect(Collectors.toList());

                int startSl = 1, startWl = 1;

                if (!phase1Saved.isEmpty()) {
                    updateMessage("RESTORING PHASE 1 RESULTS...");
                    for (LoanApplication a : phase1Saved) {
                        results.add(new LoanRDSCandidateDTO(a.getMember(), a, LocalDateTime.now()));
                    }
                    startSl = phase1Saved.stream().filter(a -> a.getDrawRank().startsWith("SL"))
                                .mapToInt(a -> Integer.parseInt(a.getDrawRank().split("-")[1])).max().orElse(0) + 1;
                    startWl = phase1Saved.stream().filter(a -> a.getDrawRank().startsWith("WL"))
                                .mapToInt(a -> Integer.parseInt(a.getDrawRank().split("-")[1])).max().orElse(0) + 1;
                } else {
                    updateMessage("PHASE 1: REGULAR DRAW...");
                    List<Member> pool1 = allApps.stream().filter(a -> !a.isSurplusNoticeApp() && a.getDrawRank() == null)
                                        .map(LoanApplication::getMember).collect(Collectors.toList());
                    Map<String, Integer> p1 = processPool(pool1, results, 1, 1, expectedLoanCanBeOpened, "PHASE_1");
                    startSl = p1.get("sl"); startWl = p1.get("wl");
                }

                // 2. Process Phase 2 (Surplus Notice)
                updateMessage("PHASE 2: SURPLUS NOTICE DRAW...");
                List<Member> pool2 = allApps.stream().filter(a -> a.isSurplusNoticeApp() && a.getDrawRank() == null)
                                    .map(LoanApplication::getMember).collect(Collectors.toList());
                
                processPool(pool2, results, startSl, startWl, expectedLoanCanBeOpened, "PHASE_2");

                results.sort(Comparator.comparing(dto -> dto.getApplication().getDrawRank(), Comparator.nullsLast(Comparator.naturalOrder())));
                return results;
            }
        };

        drawTask.messageProperty().addListener((obs, old, msg) -> mainController.showBlocker(msg));
        drawTask.setOnSucceeded(e -> {
            tblDrawResults.setItems(FXCollections.observableArrayList(drawTask.getValue()));
            mainController.hideBlocker();
            NotificationManager.show("Draw Complete!", NotificationType.SUCCESS, Pos.CENTER);
        });
        drawTask.setOnFailed(e -> { mainController.hideBlocker(); drawTask.getException().printStackTrace(); });

        drawThread = new Thread(drawTask);
        drawThread.setDaemon(true);
        drawThread.start();
    }

    private Map<String, Integer> processPool(List<Member> pool, List<LoanRDSCandidateDTO> results, 
                                            int slStart, int wlStart, int totalSlots, String tag) {
        int sl = slStart;
        int wl = wlStart;
        Map<Integer, List<Member>> tiers = new HashMap<>();
        for (Member m : pool) {
        	tiers.computeIfAbsent(loanService.calculateMemberLoanPriority(m, month), k -> new ArrayList<>()).add(m);
        }

        List<Integer> sortedTiers = new ArrayList<>(tiers.keySet());
        sortedTiers.sort(Comparator.reverseOrder());

        for (Integer tier : sortedTiers) {
            List<Member> tierList = tiers.get(tier);
            Collections.shuffle(tierList);
            for (Member m : tierList) {
                String rank;
                LoanApplicationStatus status;
                if (sl <= totalSlots) {
                    rank = String.format("SL-%02d", sl++);
                    status = LoanApplicationStatus.SELECTED_IN_DRAW;
                } else {
                    rank = String.format("WL-%02d", wl++);
                    status = LoanApplicationStatus.WAITING;
                }
                LoanRDSCandidateDTO dto = getLoanDrawDTO(m);
                dto.getApplication().setDrawRank(rank);
                dto.getApplication().setStatus(status);
                dto.getApplication().setSelectionDateTime(LocalDateTime.now());
                drawTypeMap.put(m.getId(), tag);
                results.add(dto);
                memberRankMap.put(m.getId(), dto.getApplication());
            }
        }
        Map<String, Integer> next = new HashMap<>();
        next.put("sl", sl); next.put("wl", wl);
        return next;
    }

    @FXML 
    private void confirmAndSaveDraw() { 
    	if(tblDrawResults.getItems().size() == 0) {
    		DialogManager.warning("Saving List", "No applications found to save");
    		return;
    	}
    	
        if (memberRankMap.isEmpty()) return;
        
        if (DialogManager.confirm("Confirm Save", "Save rankings for " + memberRankMap.size() + " members?")) {
            if(showSecurityGate("Authorize Draw Results")) {
                try {
                    loanService.processDrawResults(memberRankMap, this.month);
                    NotificationManager.show("Results Saved!", NotificationType.SUCCESS, Pos.CENTER);
                    refresh();
                } catch (Exception e) { 
                	DialogManager.showError("Save Error", e.getMessage()); 
                	AppLogger.error("Loan_Draw_Save_Result_Error", e);
                }
            }
        }
    }

    @FXML
    private void handleExportPDF() {
    	
    	if(tblDrawResults.getItems().size() == 0) {
    		DialogManager.warning("PDF Export", "No applications found to print");
    		return;
    	}
    	
        List<Member> selected = tblDrawResults.getItems().stream()
                .filter(d -> d.getApplication().getDrawRank().startsWith("SL"))
                .map(dto -> {
                	Member member = dto.getMember();
                	member.setMemberNoGuj(GujaratiNumericUtils.toGujarati(member.getMemberNo()));
                	return member;
                })
                .collect(Collectors.toList());
        if (selected.isEmpty()) return;
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    	String fileName = "Loan_Application_Selection_List_Report_" + timestamp + ".pdf";
    	File targetFile = FileUtil.getReportOutputFile(fileName);
        if (targetFile != null) {
            try {
                //reportService.generateSelectedLoansPdf(selected, month, targetFile.getAbsolutePath());
            	jasperReportService.generateSelectedLoansJasper(selected, month, targetFile.getAbsolutePath());
                mainController.showReport(targetFile);
                NotificationManager.show("PDF Generated!", NotificationType.SUCCESS, Pos.TOP_RIGHT);
            } catch (Exception e) { 
            	e.printStackTrace();
            	AppLogger.error("Loan_Draw_List_Export_Error", e);
            }
        }
    }
    
    private Double getNewMemberFeesDeduction(Member member) {
    	Double feesDeduction = feeService.getMemberFeeDeductionOnFirstLoan(member);
    	return feesDeduction;
    }

    @FXML
    private void handleCancelDraw() {
        if (drawThread != null && drawThread.isAlive()) {
            drawThread.interrupt();
            mainController.hideBlocker();
            refresh();
        }
    }

    @FXML
    private void handleResetDraw() {
        memberRankMap.clear();
        drawTypeMap.clear();
        refresh();
    }

    private LoanRDSCandidateDTO getLoanDrawDTO(Member m) {
        return new LoanRDSCandidateDTO(m, loanAppRepo.findByMemberAndFinancialMonth(m, this.month).get(), LocalDateTime.now());
    }
    
    private void applyNumericFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

}