package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberRepository;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@Component
public class DashboardController implements Initializable {

	@FXML private HBox emiCardContainer;
    @FXML private Label lblActiveMembers, lblTotalFees, lblTotalPending, lblActiveLoans;
    @FXML private BarChart<String, Number> chartLoansByMonth;
    @FXML private TableView<EmiSummary> tblEmiCounts;
    @FXML private TableColumn<EmiSummary, String> colEmiAmount, colMemberCount;

    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private MemberRepository memberRepo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //setupTable();
        loadStatistics();
    }

    private void setupTable() {
        colEmiAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmiAmount()));
        colMemberCount.setCellValueFactory(new PropertyValueFactory<>("count"));
    }

    private void loadStatistics() {
        // 1. Text Summaries
        Double totalFees = feeRepo.sumAllFees();
        Double totalPending = loanRepo.sumTotalPending();
        long activeCount = loanRepo.countActiveLoans();

        lblActiveMembers.setText(String.valueOf(memberRepo.countByStatus(MemberStatus.ACTIVE)));
        lblTotalFees.setText(String.format("₹ %,.0f", totalFees != null ? totalFees : 0.0));
        lblTotalPending.setText(String.format("₹ %,.0f", totalPending != null ? totalPending : 0.0));
        lblActiveLoans.setText(String.valueOf(activeCount));

        // 2. Bar Chart Data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Loans Issued");
        
        List<Object[]> monthlyData = loanRepo.getLoansGrantedByMonth(LocalDate.now().minusMonths(12));
        for (Object[] row : monthlyData) {
            series.getData().add(new XYChart.Data<>((String) row[0], (Long) row[1]));
        }
        chartLoansByMonth.getData().add(series);
        
        loadEmiCards();

        // 3. Table Data
        //List<Object[]> emiData = loanRepo.getEmiWiseCounts();
        //List<EmiSummary> emiSummaries = emiData.stream()
            //.map(row -> new EmiSummary((Double) row[0], (Long) row[1]))
            //.collect(Collectors.toList());
        //tblEmiCounts.setItems(FXCollections.observableArrayList(emiSummaries));
    }
    
    private void loadEmiCards() {
        emiCardContainer.getChildren().clear();
        EmiCounterDTO counterDTO = calculateExpectedCollection();
        emiCardContainer.getChildren().add(createStyledEmiCard(400, counterDTO.getEmi400(), "#674EA7"));
        emiCardContainer.getChildren().add(createStyledEmiCard(300, counterDTO.getEmi300(), "#28a745"));
        emiCardContainer.getChildren().add(createStyledEmiCard(200, counterDTO.getEmi200(), "#17a2b8"));
        emiCardContainer.getChildren().add(createStyledEmiCard(100, counterDTO.getEmi100(), "#dc3545"));
    }

    private VBox createStyledEmiCard(int amount, int count, String color) {
        VBox card = new VBox(2);
        card.setPrefSize(220, 100);
        card.setPadding(new Insets(15));
        
        // Matches the "Card Box" style from your top bar
        card.setStyle("-fx-background-color: "+color+"; " +
                      "-fx-background-radius: 10; " +                      
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        //Label lblTier = new Label(tier);
        //lblTier.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10; -fx-font-weight: bold; -fx-text-transform: uppercase;");

        Label lblAmount = new Label(amount > 0 ? "" + amount : "New Loans");
        lblAmount.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblCount = new Label(count + "");
        lblCount.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: white;");

        card.getChildren().addAll(lblAmount, lblCount);
        return card;
    }

    // Inner DTO for Table
    public static class EmiSummary {
        private final Double emiAmount;
        private final Long count;
        public EmiSummary(Double emi, Long count) { this.emiAmount = emi; this.count = count; }
        public String getEmiAmount() { return emiAmount != null ? emiAmount.intValue()+"" : "Last Month Loans"; }
        public String getCount() { return count+""; }
    }
    
    public EmiCounterDTO calculateExpectedCollection() {
        // 1. Get all active members
        List<Member> activeMembers = memberRepo.findAllByStatus(MemberStatus.ACTIVE);
        
        // 2. Get all active loan accounts
        List<LoanAccount> activeLoans = loanRepo.findAllByLoanStatus(LoanStatus.ACTIVE);
        
        // Map for quick lookup: MemberID -> LoanAccount
        Map<Long, LoanAccount> loanMap = activeLoans.stream()
                .collect(Collectors.toMap(l -> l.getMember().getId(), l -> l));

        EmiCounterDTO counts = new EmiCounterDTO();
        int feeOnlyCount = 0;

        for (Member m : activeMembers) {
            LoanAccount loan = loanMap.get(m.getId());

            if (loan != null && loan.getPendingAmount() > 0) {
                // Member has a loan - determine effective EMI
                double standardEmi = loan.getEmiAmount() != null ? loan.getEmiAmount() : 0.0;
                double pending = loan.getPendingAmount();
                
                // Effective EMI is the smaller of the two
                int effectiveEmi = (int) Math.min(standardEmi, pending);

                switch (effectiveEmi) {
                    case 100 -> counts.setEmi100(counts.getEmi100() + 1);
                    case 200 -> counts.setEmi200(counts.getEmi200() + 1);
                    case 300 -> counts.setEmi300(counts.getEmi300() + 1);
                    case 400 -> counts.setEmi400(counts.getEmi400() + 1);
                    default -> {
                        // Handle edge cases (e.g., final payment of 50 or 150)
                        if (effectiveEmi > 0) counts.setOtherEmiCount(counts.getOtherEmiCount() + 1);
                    }
                }
            } else {
                // No active loan or pending amount is zero
                feeOnlyCount++;
            }
        }
        
        counts.setFeesCount(feeOnlyCount);
        return counts;
    }
}