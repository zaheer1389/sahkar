package com.badargadh.sahkar.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.dto.LoanRDSCandidateDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.AppConfigService;
import com.badargadh.sahkar.service.FeeService;
import com.badargadh.sahkar.service.FinancialMonthService;
import com.badargadh.sahkar.service.LoanService;
import com.badargadh.sahkar.service.MonthlyStatementService;
import com.badargadh.sahkar.service.report.ReportService;
import com.badargadh.sahkar.util.DialogManager;
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

@Component
public class LoanPlanningController extends BaseController implements Initializable {

    @FXML private Label lblMonthInfo, lblOpeningBal, lblExpectedFees, lblExpectedEmi;
    @FXML private Label lblFreshLoansVirtualEmi, lblRefunds, lblNetAvailable;
    @FXML private Label lblPossible50k, lblStatus, lblSelectedCount, lblWaitingCount;
    @FXML private Label lblFullPayments, lblNewMemberFeeDeductions, lblMiscCredis;
    
    @FXML private TableView<LoanRDSCandidateDTO> tblDrawResults;
    @FXML private TableColumn<LoanRDSCandidateDTO, String> colDrawMemNo, colDrawName, colLastLoanDate, colDrawStatus, colDrawRank;
    
    @FXML private Button btnRunDraw, btnConfirmSave, btnRefresh;

    @Autowired private MainController mainController; 
    @Autowired private FinancialMonthService monthService;
    @Autowired private MemberRepository memberRepo;
    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private LoanApplicationRepository loanAppRepo;
    @Autowired private FeePaymentRepository feePaymentRepository;
    @Autowired private LoanService loanService;
    @Autowired private AppConfigService appConfigService;
    @Autowired private ReportService reportService;
    @Autowired private MonthlyStatementService monthlyStatementService;
    @Autowired private FeeService feeService;
    
    private AppConfig appConfig;
    private Long loanAmount;
    private FinancialMonth month;
    private Thread drawThread;
    
    private Map<Long, LoanApplication> memberRankMap = new HashMap<>();
    private Map<Long, String> drawTypeMap = new HashMap<>();

    private Double expectedMonthlyTotalAmount;
    private Double actualMonthlyPaymentAmount;
    
    private int expectedLoanCanBeOpened, selectedCount;
    
    private List<LoanRDSCandidateDTO> candidateDTOs;
    List<LoanApplication> loanApplications;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {     
        appConfig = appConfigService.getSettings();
        loanAmount = appConfig.getLoanAmount();
        
        monthService.getActiveMonth().ifPresent(activeMonth -> {
            this.month = activeMonth;
            setupTable();
            refresh();
        });
    }

    private void setupTable() {
        colDrawMemNo.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMember().getMemberNo())));
        colDrawName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMember().getFullname()));
        colLastLoanDate.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(calculateWaitMonths(d.getValue().getMember()))));
        
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
        
        MonthlyStatementDTO statementDTO = monthlyStatementService.getActiveMonthStatement(month);
        
        loanApplications = loanService.getAllApplicationsWithStatuses(this.month, 
                Arrays.asList(LoanApplicationStatus.APPLIED, LoanApplicationStatus.SELECTED_IN_DRAW, 
                              LoanApplicationStatus.DISBURSED, LoanApplicationStatus.WAITING));
        
        Double feeDeduction = 0d;
        
        for(LoanApplication application : loanApplications) {
        	feeDeduction = feeDeduction + getNewMemberFeesDeduction(application.getMember());
        }
        
        List<MemberSummaryDTO> pending = memberRepo.findActiveMembersPendingForMonth(month.getId());
        EmiCounterDTO counterDTO = new EmiCounterDTO();
        pending.forEach(dto -> {
            if (dto.getEmiAmount() == 0) {
            	counterDTO.setFeesCount(counterDTO.getFeesCount() + 1);
            	return;
            };
            int emiAmount = dto.getPendingLoan() < dto.getEmiAmount() ? dto.getPendingLoan() : dto.getEmiAmount();
            switch (emiAmount) {
                case 100 -> counterDTO.setEmi100(counterDTO.getEmi100() + 1);
                case 200 -> counterDTO.setEmi200(counterDTO.getEmi200() + 1);
                case 300 -> counterDTO.setEmi300(counterDTO.getEmi300() + 1);
                case 400 -> counterDTO.setEmi400(counterDTO.getEmi400() + 1);
                // ignore other values safely
            }
        });
        
        double totalOfOldEmis = pending.stream()
                .mapToLong(m -> m.getEmiAmount() != null && m.getEmiAmount() < m.getPendingLoan() 
                	? m.getEmiAmount() : m.getPendingLoan())
                .sum();
        
        // 1. Calculate Expected Liquidity (Projections)
        double opening = monthService.calculateOpeningBalance(this.month);
        long activeCount = memberRepo.countByStatus(MemberStatus.ACTIVE);
        double fees = activeCount * appConfig.getMonthlyFees();
        long fresh = loanRepo.countLastMonthFreshLoans();
        double refunds = feePaymentRepository.calculateExpectedRefunds(this.month.getEndDate());
        double fullpayments = statementDTO.getFullPaymentAmount();
        double otherMiscCredits = statementDTO.getExpenseCredit();
        
        double expectedNet = (opening + fees + totalOfOldEmis + (fresh * 200.0) + fullpayments + feeDeduction + otherMiscCredits) - refunds;
        
        lblOpeningBal.setText(String.format("₹ %,.2f", opening));
        lblExpectedFees.setText(String.format("₹ %,.2f", fees));
        lblExpectedEmi.setText(String.format("₹ %,.2f", totalOfOldEmis));
        lblFreshLoansVirtualEmi.setText(String.format("₹ %,.2f", (fresh * 200.0)));
        lblRefunds.setText(String.format("- ₹ %,.2f", refunds));
        lblNetAvailable.setText(String.format("₹ %,.2f", expectedNet));
        lblFullPayments.setText(String.format("₹ %,.2f", fullpayments));
        lblNewMemberFeeDeductions.setText(String.format("₹ %,.2f", feeDeduction));
        lblMiscCredis.setText(String.format("₹ %,.2f", otherMiscCredits));
        
        // 2. Fetch Actual Payment Amount (Real-time Statement)
        actualMonthlyPaymentAmount = statementDTO.getTotalIncome();

        // 3. Logic: Use Actual if it's more than Expected, otherwise use Expected
        // This ensures that if extra money came in, we open more loan slots.
        expectedMonthlyTotalAmount = Math.max(expectedNet, actualMonthlyPaymentAmount);
        
        // Calculate slots based on the higher amount
        expectedLoanCanBeOpened = (int) (expectedMonthlyTotalAmount / loanAmount);
        
        //expectedLoanCanBeOpened = 21;

        
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
            .map(a -> new LoanRDSCandidateDTO(a.getMember(), a))
            .sorted(Comparator.comparing(dto -> dto.getApplication().getDrawRank(), Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        tblDrawResults.setItems(FXCollections.observableArrayList(candidateDTOs));
    }
    
    public EmiCounterDTO buildEmiCounter(LocalDate currentMonthStart) {

        List<Object[]> results =
            loanRepo.findRunningLoanEmiCounts(currentMonthStart);

        EmiCounterDTO dto = new EmiCounterDTO();

        for (Object[] row : results) {
            Double emiAmount = (Double) row[0];
            Long count = (Long) row[1];

            if (emiAmount == null) continue;

            switch (emiAmount.intValue()) {
                case 100 -> dto.setEmi100(count.intValue());
                case 200 -> dto.setEmi200(count.intValue());
                case 300 -> dto.setEmi300(count.intValue());
                case 400 -> dto.setEmi400(count.intValue());
                // ignore others safely
            }
        }

        return dto;
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
        
        if(DialogManager.confirm("Please confirm!", "Do you really want to run computerized selection of loan applications?")) {
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
                            results.add(new LoanRDSCandidateDTO(a.getMember(), a));
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

        
    }

    private Map<String, Integer> processPool(List<Member> pool, List<LoanRDSCandidateDTO> results, 
                                            int slStart, int wlStart, int totalSlots, String tag) {
        int sl = slStart;
        int wl = wlStart;
        Map<Integer, List<Member>> tiers = new HashMap<>();
        for (Member m : pool) tiers.computeIfAbsent(calculateWaitMonths(m), k -> new ArrayList<>()).add(m);

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
                drawTypeMap.put(m.getId(), tag);
                results.add(dto);
                memberRankMap.put(m.getId(), dto.getApplication());
            }
        }
        Map<String, Integer> next = new HashMap<>();
        next.put("sl", sl); next.put("wl", wl);
        return next;
    }

    private int calculateWaitMonths(Member m) {
        LocalDate ref = (this.month != null) ? this.month.getStartDate() : LocalDate.now();
        Optional<LoanAccount> active = loanRepo.findFirstByMemberAndLoanStatusOrderByEndDateDesc(m, LoanStatus.ACTIVE);
        if (active.isPresent() && active.get().getEmiAmount() != null) {
        	return -(int)(active.get().getPendingAmount() / active.get().getEmiAmount());
        }
        
        Optional<LoanAccount> lastPaid = loanRepo.findFirstByMemberAndLoanStatusOrderByEndDateDesc(m, LoanStatus.PAID);
        LocalDate start = lastPaid.isPresent() ? lastPaid.get().getEndDate() : LocalDate.of(2024, 12, 10);
        
        if(lastPaid.isPresent()) {
        	if(lastPaid.get().getEndDate().isAfter(month.getStartDate()) && lastPaid.get().getEndDate().isBefore(month.getEndDate()))
        		return 0;
        }
        
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(start, ref) + 1;
    }

    @FXML 
    private void confirmAndSaveDraw() { 
        if (memberRankMap.isEmpty()) return;
        if (DialogManager.confirm("Confirm Save", "Save rankings for " + memberRankMap.size() + " members?")) {
            if(showSecurityGate("Authorize Draw Results")) {
                try {
                    loanService.processDrawResults(memberRankMap, this.month);
                    NotificationManager.show("Results Saved!", NotificationType.SUCCESS, Pos.CENTER);
                    refresh();
                } catch (Exception e) { DialogManager.showError("Save Error", e.getMessage()); }
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
                .map(LoanRDSCandidateDTO::getMember).collect(Collectors.toList());
        if (selected.isEmpty()) return;
        
        String fileName = "loan_selection_report_" + LocalDateTime.now() + ".pdf";
        File file = DialogManager.showSaveDialog(tblDrawResults, "Save loan selection list", fileName);
        if (file != null) {
            try {
                reportService.generateSelectedLoansPdf(selected, month, file.getAbsolutePath());
                NotificationManager.show("PDF Generated!", NotificationType.SUCCESS, Pos.TOP_RIGHT);
            } catch (Exception e) { e.printStackTrace(); }
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
        return new LoanRDSCandidateDTO(m, loanAppRepo.findByMemberAndFinancialMonth(m, this.month).get());
    }
}