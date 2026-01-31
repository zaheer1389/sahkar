package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.FeeBookDTO;
import com.badargadh.sahkar.dto.LoanBookDTO;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberRepository;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

@Component
public class AccountBookController implements Initializable {

    @FXML private ComboBox<Integer> comboYear;
    @FXML private TableView<LoanBookDTO> tblLoanBook;
    @FXML private TableView<FeeBookDTO> tblFeeBook;
    @FXML private VBox loaderPane;
    @FXML private Button btnRefresh;
    @FXML private Label lblStatus;

    @FXML private TableColumn<LoanBookDTO, Integer> colLoanMNo;
    @FXML private TableColumn<LoanBookDTO, String> colLoanName;
    @FXML private TableColumn<LoanBookDTO, Double> colOpeningBal;
    @FXML private TableColumn<FeeBookDTO, Integer> colFeeMNo;
    @FXML private TableColumn<FeeBookDTO, String> colFeeName;
    @FXML private TableColumn<FeeBookDTO, Double> colTotalFees;

    @Autowired private MemberRepository memberRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private EmiPaymentRepository paymentRepository; 
    @Autowired private FeePaymentRepository feeRepository;

    private final String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStaticColumns();
        setupDynamicMonthColumns();
        
        int currentYear = LocalDate.now().getYear();
        for (int i = 0; i <= 2; i++) comboYear.getItems().add(currentYear - i);
        comboYear.setValue(currentYear);
        
        tblLoanBook.setFixedCellSize(-1); // Allow dynamic height
    }

    private void setupStaticColumns() {
        colLoanMNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colLoanName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colOpeningBal.setCellValueFactory(new PropertyValueFactory<>("openingBalance"));
        applyAmountStyling(colOpeningBal);
        
        colFeeMNo.setCellValueFactory(new PropertyValueFactory<>("memberNo"));
        colFeeName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTotalFees.setCellValueFactory(new PropertyValueFactory<>("openingRunningBalance"));
    }

    private void setupDynamicMonthColumns() {
        for (int i = 0; i < 12; i++) {
            final int monthIndex = i;
            TableColumn<LoanBookDTO, LoanBookDTO> loanCol = new TableColumn<>(monthNames[i]);
            loanCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue()));
            applyDualAmountStyling(loanCol, monthIndex);
            tblLoanBook.getColumns().add(3 + i, loanCol);

            TableColumn<FeeBookDTO, Double> feeCol = new TableColumn<>(monthNames[i]);
            feeCol.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getMonthlyFees()[monthIndex]));
            applyAmountStyling(feeCol);
            tblFeeBook.getColumns().add(3 + i, feeCol);
        }
        
        // Loan Balance Column
        TableColumn<LoanBookDTO, Double> colLoanBalance = new TableColumn<>("Balance Amount");
        colLoanBalance.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getClosingBalance()));
        applyAmountStyling(colLoanBalance);
        colLoanBalance.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT;");
        tblLoanBook.getColumns().add(colLoanBalance);
        
        // Fee Grand Total Column
        TableColumn<FeeBookDTO, Double> colClosing = new TableColumn<>("Grand Total");
        colClosing.setCellValueFactory(new PropertyValueFactory<>("closingRunningBalance"));
        applyAmountStyling(colClosing);
        colClosing.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a5276; -fx-alignment: CENTER-RIGHT;");
        tblFeeBook.getColumns().add(colClosing);
    }

    @FXML
    public void handleRefresh() {
        Integer year = comboYear.getValue();
        if (year == null) return;

        loaderPane.setVisible(true);
        btnRefresh.setDisable(true);
        lblStatus.setText("Calculating balances for " + year + "...");

        Task<Void> task = new Task<>() {
            List<LoanBookDTO> loanRows;
            List<FeeBookDTO> feeRows;

            @Override
            protected Void call() throws Exception {
                loanRows = processLoanData(year);
                feeRows = processFeeData(year);
                return null;
            }

            @Override
            protected void succeeded() {
                tblLoanBook.setItems(FXCollections.observableArrayList(loanRows));
                tblFeeBook.setItems(FXCollections.observableArrayList(feeRows));
                finalizeUI();
            }

            @Override
            protected void failed() {
                finalizeUI();
                getException().printStackTrace();
            }

            private void finalizeUI() {
                loaderPane.setVisible(false);
                btnRefresh.setDisable(false);
                lblStatus.setText("Data loaded for " + year);
            }
        };

        new Thread(task).start();
    }
    
    public List<LoanBookDTO> processLoanData(int year) {
        List<Member> members = memberRepository.findAllActiveMembers();
        List<LoanAccount> allLoans = loanAccountRepository.findAll();
        List<EmiPayment> allEmis = paymentRepository.findAll();

        Map<Long, List<LoanAccount>> loansByMember = allLoans.stream()
                .collect(Collectors.groupingBy(l -> l.getMember().getId()));
        Map<Long, List<EmiPayment>> emisByMember = allEmis.stream()
                .collect(Collectors.groupingBy(e -> e.getMember().getId()));

        List<LoanBookDTO> rows = new ArrayList<>();

        for (Member m : members) {
            List<LoanAccount> mLoans = loansByMember.getOrDefault(m.getId(), Collections.emptyList());
            List<EmiPayment> mEmis = emisByMember.getOrDefault(m.getId(), Collections.emptyList());

            // Find the pivot loan for this year
            LoanAccount newLoanThisYear = mLoans.stream()
                    .filter(l -> l.getCreatedDate().getYear() == year)
                    .findFirst().orElse(null);

            LocalDate loanIssueDate = (newLoanThisYear != null) ? newLoanThisYear.getCreatedDate() : null;

            LoanBookDTO dto = new LoanBookDTO();
            dto.setMemberNo(m.getMemberNo());
            dto.setName(m.getFullname());

            // 1. Calculate Opening Balance
            double openingBal = calculateOpeningForYear(m, mLoans, mEmis, year);
            dto.setOpeningBalance(openingBal);

            // 2. Fill Monthly Cells (DR for Loans, CR for Payments)
            for (int month = 1; month <= 12; month++) {
                if (newLoanThisYear != null && newLoanThisYear.getCreatedDate().getMonthValue() == month) {
                    dto.getMonthlyPaymentsDr()[month - 1] = newLoanThisYear.getGrantedAmount();
                } 
                
                dto.getMonthlyPayments()[month - 1] = sumEmisForMonth(mEmis, year, month);
            }

            // 3. Calculate Closing Balance
            dto.setClosingBalance(calculateClosingForYear(m, mLoans, mEmis, year));

            // Set sorting date
            //dto.setFirstLoanDate(mLoans.stream().map(LoanAccount::getCreatedDate).min(LocalDate::compareTo).orElse(LocalDate.MAX));

            if (dto.getOpeningBalance() > 0 || dto.getClosingBalance() > 0) {
                rows.add(dto);
            }
        }

        rows.sort(Comparator.comparing(LoanBookDTO::getMemberNo));
        
        return rows;
    }

    private double calculateOpeningForYear(Member m, List<LoanAccount> mLoans, List<EmiPayment> mEmis, int year) {
        LoanAccount newLoanThisYear = mLoans.stream()
                .filter(l -> l.getCreatedDate().getYear() == year).findFirst().orElse(null);
        LocalDate loanIssueDate = (newLoanThisYear != null) ? newLoanThisYear.getCreatedDate() : null;

        if (year == 2025) {
            // Rule 2025: Sum of all EMIs paid until new loan issue (Historical)
        	Optional<LoanAccount> optional = mLoans.stream()
        			.filter(loan -> loan.getLoanStatus() == LoanStatus.ACTIVE && loan.getCreatedDate().getYear() < year)
        			.findFirst();
        	double pendingBal = optional.isPresent() ? optional.get().getPendingAmount() : 0;
            return sumEmis(mEmis, null, loanIssueDate, year, true) + pendingBal;
        } else {
            // Rule 2026+: Last year closing - EMI paid till new loan issued this year
            double lastYearClosing = calculateClosingForYear(m, mLoans, mEmis, year - 1);
            double emisUntilNewLoan = sumEmis(mEmis, null, loanIssueDate, year, false);
            return Math.max(0, lastYearClosing);
            //return Math.max(0, lastYearClosing - emisUntilNewLoan);
        }
    }

    private double calculateClosingForYear(Member m, List<LoanAccount> mLoans, List<EmiPayment> mEmis, int year) {
        LoanAccount newLoanThisYear = mLoans.stream()
                .filter(l -> l.getCreatedDate().getYear() == year).findFirst().orElse(null);
        LocalDate loanIssueDate = (newLoanThisYear != null) ? newLoanThisYear.getCreatedDate() : null;

        if (newLoanThisYear != null) {
            // Rule A: New loan issued -> Granted Amount - EMI paid in year AFTER loan started
            double emisAfterLoan = sumEmis(mEmis, loanIssueDate, null, year, false);
            return Math.max(0, newLoanThisYear.getGrantedAmount() - emisAfterLoan);
        } else {
            double opening = calculateOpeningForYear(m, mLoans, mEmis, year);
            double yearEmis = sumEmis(mEmis, null, null, year, false);
            return Math.max(0, opening - yearEmis);
            /*if (year == 2025) {
                return Math.max(0, opening - yearEmis); // Rule 2025 (B)
            } else {
                return Math.max(0, opening + yearEmis); // Rule 2026 (B)
            }*/
        }
    }

    private double sumEmis(List<EmiPayment> emis, LocalDate start, LocalDate end, int year, boolean includeAllHistory) {
        return emis.stream()
            .filter(e -> includeAllHistory || e.getPaymentDateTime().getYear() == year)
            .filter(e -> {
                LocalDate pDate = e.getPaymentDateTime().toLocalDate();
                if (start != null && !pDate.isAfter(start)) return false;
                if (end != null && !pDate.isBefore(end)) return false;
                return true;
            })
            .mapToDouble(e -> getEmiAmount(e))
            .sum();
    }

    private double sumEmisForMonth(List<EmiPayment> emis, int year, int month) {
        return emis.stream()
            .filter(e -> e.getPaymentDateTime().getYear() == year && e.getPaymentDateTime().getMonthValue() == month)
            .mapToDouble(e -> getEmiAmount(e))
            .sum();
    }
    
    private Double getEmiAmount(EmiPayment e) {
    	double fullpayment = e.isFullPayment() ? e.getFullPaymentAmount() : 0.0;
    	return e.getAmountPaid() != null ? e.getAmountPaid() + fullpayment : 0.0;
    }

    private List<FeeBookDTO> processFeeData(int year) {
        List<Member> members = memberRepository.findAllActiveMembers();
        List<FeePayment> allHistoricalFees = feeRepository.findAll();
        Map<Long, List<FeePayment>> feesByMember = allHistoricalFees.stream()
                .collect(Collectors.groupingBy(f -> f.getMember().getId()));

        List<FeeBookDTO> rows = new ArrayList<>();
        for (Member m : members) {
            FeeBookDTO dto = new FeeBookDTO();
            dto.setMemberNo(m.getMemberNo());
            dto.setName(m.getFullname());
            List<FeePayment> mFees = feesByMember.getOrDefault(m.getId(), Collections.emptyList());

            double totalPaidBeforeYear = mFees.stream()
                    .filter(f -> f.getTransactionDateTime().getYear() < year)
                    .mapToDouble(f -> f.getAmount() != null ? f.getAmount() : 0.0).sum();
            
            dto.setOpeningRunningBalance(totalPaidBeforeYear);
            double paidThisYear = 0.0;
            for (FeePayment f : mFees) {
                if (f.getTransactionDateTime().getYear() == year) {
                    int mIdx = f.getTransactionDateTime().getMonthValue() - 1;
                    double amt = f.getAmount() != null ? f.getAmount() : 0.0;
                    dto.getMonthlyFees()[mIdx] += amt;
                    paidThisYear += amt;
                }
            }
            dto.setClosingRunningBalance(totalPaidBeforeYear + paidThisYear);
            rows.add(dto);
        }
        return rows;
    }

    private <T> void applyAmountStyling(TableColumn<T, Double> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText("");
                } else {
                    setText(String.format("%.0f", item));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 5 0 0;");
                }
            }
        });
    }
    
    private void applyDualAmountStyling(TableColumn<LoanBookDTO, LoanBookDTO> column, int monthIdx) {
        column.setCellFactory(tc -> new TableCell<LoanBookDTO, LoanBookDTO>() {
            @Override
            protected void updateItem(LoanBookDTO dto, boolean empty) {
                super.updateItem(dto, empty);

                if (empty || dto == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Double crAmount = dto.getMonthlyPayments()[monthIdx];   // EMI
                    Double drAmount = dto.getMonthlyPaymentsDr()[monthIdx]; // Loan

                    // Only create layout if there is data
                    if ((crAmount == null || crAmount == 0) && (drAmount == null || drAmount == 0)) {
                        setGraphic(null);
                        return;
                    }

                    VBox container = new VBox(2);
                    container.setAlignment(Pos.CENTER_RIGHT);

                    if (crAmount > 0 && drAmount > 0) {
                        // Scenario: Paid EMI AND took a New Loan
                        Label lblCr = new Label(String.format("%.0f", crAmount));
                        lblCr.setStyle("-fx-text-fill: #011536; -fx-font-weight: bold;"); // Green for EMI
                        
                        Separator sep = new Separator(Orientation.HORIZONTAL);
                        sep.setMaxWidth(40);
                        
                        Label lblDr = new Label(String.format("%.0f", drAmount));
                        lblDr.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Red for Loan
                        
                        container.getChildren().addAll(lblCr, sep, lblDr);
                    } else if (drAmount > 0) {
                        // Scenario: ONLY New Loan
                        Label lblDr = new Label(String.format("%.0f", drAmount));
                        lblDr.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        container.getChildren().add(lblDr);
                    } else {
                        // Scenario: ONLY EMI Payment
                        Label lblCr = new Label(String.format("%.0f", crAmount));
                        lblCr.setStyle("-fx-text-fill: #011536; -fx-font-weight: bold;");
                        container.getChildren().add(lblCr);
                    }

                    setGraphic(container);
                }
            }
        });
    }

    @FXML public void handlePrint() { /* Logic for PDF */ }
}