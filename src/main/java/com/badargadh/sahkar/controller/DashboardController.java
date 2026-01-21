package com.badargadh.sahkar.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        List<Object[]> emiData = loanRepo.getEmiWiseCounts();

        for (Object[] row : emiData) {
            Integer amount = row[0] != null ? ((Double) row[0]).intValue() : 0;
            Long count = (Long) row[1];
            
            // Define Legend Colors based on EMI amount
            String color = "#27ae60";
            
            if(amount == 400) {
            	color = "#674EA7";
            }
            else if(amount == 300) {
            	color = "#28a745";
            }
            else if(amount == 200) {
            	color = "#17a2b8";
            }
            else if(amount == 100) {
            	color = "#dc3545";
            }
            else {
            	color = "#007bff";
            }

            emiCardContainer.getChildren().add(createStyledEmiCard(amount, count, color));
        }
    }

    private VBox createStyledEmiCard(Integer amount, Long count, String color) {
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
    
    
}