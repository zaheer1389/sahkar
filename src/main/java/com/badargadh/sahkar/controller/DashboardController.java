package com.badargadh.sahkar.controller;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.EmiCounterDTO;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.FeesRefundRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.MonthlyExpenseService;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@Component
public class DashboardController implements Initializable {

	@FXML private NumberAxis yAxis;
	
	@FXML private HBox emiCardContainer;
    @FXML private Label lblActiveMembers, lblTotalFees, lblTotalPending, lblActiveLoans, lblJamatBal; 
    @FXML private Label lblStationaryBal, lblClosingBal, lblFeesCount;
    @FXML private LineChart<String, Number> chartLoansByMonth;
    @FXML private TableView<EmiSummary> tblEmiCounts;
    @FXML private TableColumn<EmiSummary, String> colEmiAmount, colMemberCount;

    @Autowired private LoanAccountRepository loanRepo;
    @Autowired private FeePaymentRepository feeRepo;
    @Autowired private FeesRefundRepository feesRefundRepo;
    @Autowired private MemberRepository memberRepo;
    @Autowired private MonthlyExpenseService expenseService;
    
    //@Autowired private MainController mainController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        
        // Prevent the axis from showing negative numbers
        yAxis.setForceZeroInRange(true);
        
        // Disable auto-ranging if you want absolute control over the 0 point
        // but usually, just forcing zero and setting a small buffer works:
        yAxis.setLowerBound(0);
        
        // This removes the gap between the 0 tick and the X-axis line
        chartLoansByMonth.setVerticalZeroLineVisible(true);
        
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
        Double jammatBal = expenseService.getJammatOutstandingBalance();
        Double closingBal = totalFees - totalPending - jammatBal;
        Double expenseBal = expenseService.getMonthlyExpenseBalance();
        
        int totalCount = memberRepo.countByStatus(MemberStatus.ACTIVE);
        int activeCount = (int)loanRepo.countActiveLoans();
        int feeCount = totalCount - activeCount;
        
        lblActiveMembers.setText(String.valueOf(totalCount));
        lblActiveLoans.setText(String.valueOf(activeCount));
        lblFeesCount.setText(String.valueOf(feeCount));
        
        lblTotalFees.setText(String.format("₹ %,.0f", totalFees != null ? totalFees : 0.0));
        lblTotalPending.setText(String.format("₹ %,.0f", totalPending != null ? totalPending : 0.0));
        lblJamatBal.setText(String.format("₹ %,.0f", jammatBal != null ? jammatBal : 0.0));
        lblClosingBal.setText(String.format("₹ %,.0f", closingBal != null ? closingBal : 0.0));
        lblStationaryBal.setText(String.format("₹ %,.0f", expenseBal != null ? expenseBal : 0.0));

        loadChartData();
        
        loadEmiCards();

        // 3. Table Data
        //List<Object[]> emiData = loanRepo.getEmiWiseCounts();
        //List<EmiSummary> emiSummaries = emiData.stream()
            //.map(row -> new EmiSummary((Double) row[0], (Long) row[1]))
            //.collect(Collectors.toList());
        //tblEmiCounts.setItems(FXCollections.observableArrayList(emiSummaries));
    }
    
    private void loadChartData() {
        // 1. Show your global loader
        //mainController.showLoader("Analyzing Trends...");

    	Task<List<XYChart.Series<String, Number>>> task = new Task<>() {
    	    @Override
    	    protected List<XYChart.Series<String, Number>> call() throws Exception {
    	        // 1. Fetch data from Repositories
    	        // Ensure your SQL queries use "ORDER BY m.start_date ASC" to keep these lists chronological
    	        List<Object[]> monthlyData = loanRepo.getLoanCountsByFinancialMonthNative(LocalDate.now().minusMonths(12));
    	        List<Object[]> feesRefunds = feesRefundRepo.getFeesRefundCountsByFinancialMonthNative(LocalDateTime.now().minusMonths(12));

    	        // 2. Extract and Merge unique months (The Master Timeline)
    	        List<String> loanMonths = monthlyData.stream().map(row -> (String) row[0]).toList();
    	        List<String> refundMonths = feesRefunds.stream().map(row -> (String) row[0]).toList();
    	        
    	        // 1. Force the axis to start exactly at 0
    	        yAxis.setAutoRanging(false); // Turn off auto to prevent the "buffer"
    	        yAxis.setLowerBound(0);

    	        // 2. Dynamically calculate the upper bound based on your data
    	        long maxLoans = monthlyData.stream().mapToLong(r -> ((Number)r[1]).longValue()).max().orElse(10L);
    	        long maxRefunds = feesRefunds.stream().mapToLong(r -> ((Number)r[1]).longValue()).max().orElse(10L);
    	        long absoluteMax = Math.max(maxLoans, maxRefunds);

    	        // Add 20% breathing room only at the TOP
    	        yAxis.setUpperBound(Math.ceil(absoluteMax * 1.2)); 
    	        yAxis.setTickUnit(1.0); // Ensure whole numbers
    	        
    	        // 1. Extract all unique months into a list
    	        Set<String> allMonthsSet = new HashSet<>();
    	        allMonthsSet.addAll(loanMonths);
    	        allMonthsSet.addAll(refundMonths);

    	        // 1. Build a formatter that ignores case
    	        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
    	            .parseCaseInsensitive()
    	            .appendPattern("MMMM-yyyy")
    	            .toFormatter(Locale.ENGLISH);

    	        // 2. Now use it for sorting
    	        List<String> allUniqueMonths = allMonthsSet.stream()
    	            .sorted(Comparator.comparing(s -> YearMonth.parse(s, formatter)))
    	            .toList();


    	        // 3. Convert result lists to Maps for O(1) lookup
    	        // This prevents "IndexOutOfBounds" or alignment issues if a month has loans but no refunds
    	        Map<String, Long> loanMap = monthlyData.stream()
    	            .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));

    	        Map<String, Long> refundMap = feesRefunds.stream()
    	            .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));

    	        // 4. Create Series and populate using the Master Timeline
    	        XYChart.Series<String, Number> seriesLoans = new XYChart.Series<>();
    	        seriesLoans.setName("Loans Issued");

    	        XYChart.Series<String, Number> seriesRefunds = new XYChart.Series<>();
    	        seriesRefunds.setName("Fee Refunded");

    	        for (String month : allUniqueMonths) {
    	            // If month is missing in a specific dataset, default to 0
    	            seriesLoans.getData().add(new XYChart.Data<>(month, loanMap.getOrDefault(month, 0L)));
    	            seriesRefunds.getData().add(new XYChart.Data<>(month, refundMap.getOrDefault(month, 0L)));
    	        }

    	        return List.of(seriesLoans, seriesRefunds);
    	    }
    	};

    	task.setOnSucceeded(e -> {
    	    List<XYChart.Series<String, Number>> results = task.getValue();
    	    
    	    Platform.runLater(() -> {
    	        chartLoansByMonth.getData().setAll(results);
    	        
    	        // Use a small delay or a nested runLater to ensure CSS/Nodes are rendered
    	        Platform.runLater(() -> {
    	            for (XYChart.Series<String, Number> series : chartLoansByMonth.getData()) {
    	                for (XYChart.Data<String, Number> data : series.getData()) {
    	                    Node node = data.getNode();
    	                    
    	                    if (node != null) {
    	                        Tooltip tooltip = new Tooltip(
    	                            series.getName() + "\n" +
    	                            "માસ: " + data.getXValue() + "\n" +
    	                            "સંખ્યા: " + data.getYValue()
    	                        );
    	                        
    	                        tooltip.setShowDelay(javafx.util.Duration.millis(50));
    	                        Tooltip.install(node, tooltip);
    	                        
    	                        // Professional interaction
    	                        node.setOnMouseEntered(ev -> node.setScaleX(1.4));
    	                        node.setOnMouseExited(ev -> node.setScaleX(1.0));
    	                        node.setCursor(Cursor.HAND);
    	                    }
    	                }
    	            }
    	        });
    	        
    	    });
    	});

    	task.setOnFailed(e -> {
    	    //mainController.hideLoader();
    	    Throwable ex = task.getException();
    	    ex.printStackTrace();
    	    // Show error alert to user if necessary
    	});

    	new Thread(task).start();
    }
    
    private void applyChartPolish(List<XYChart.Series<String, Number>> seriesList) {
        for (XYChart.Series<String, Number> series : seriesList) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip(series.getName() + ": " + data.getYValue());
                Tooltip.install(data.getNode(), tooltip);
                
                // Hover effect
                data.getNode().setOnMouseEntered(ev -> data.getNode().setScaleX(1.4));
                data.getNode().setOnMouseExited(ev -> data.getNode().setScaleX(1.0));
            }
        }
    }
    
    private void loadEmiCards() {
        EmiCounterDTO counterDTO = calculateExpectedCollection();
        
        // Clear container before adding
        emiCardContainer.getChildren().clear();

        // Using the unique CSS classes we defined earlier
        emiCardContainer.getChildren().add(createStyledEmiCard(400, counterDTO.getEmi400(), "stat-card-purple"));
        emiCardContainer.getChildren().add(createStyledEmiCard(300, counterDTO.getEmi300(), "stat-card-teal"));
        emiCardContainer.getChildren().add(createStyledEmiCard(200, counterDTO.getEmi200(), "stat-card-blue"));
        emiCardContainer.getChildren().add(createStyledEmiCard(100, counterDTO.getEmi100(), "stat-card-amber"));
        emiCardContainer.getChildren().add(createStyledEmiCard(0, counterDTO.getOtherEmiCount(), "stat-card-red"));
    }

    private VBox createStyledEmiCard(int amount, int count, String styleClass) {
        VBox card = new VBox(2);
        card.setPrefSize(180, 110); // Slightly adjusted for better alignment in ScrollPane
        
        // Apply the base class and the specific color class
        card.getStyleClass().addAll("stat-card", styleClass);

        // Label for the EMI Amount (Title)
        Label lblAmount = new Label(amount > 0 ? "EMI: ₹" + amount : "નવી લોન (New Loans)");
        lblAmount.getStyleClass().add("title-label");

        // Label for the Count (Value)
        Label lblCount = new Label(String.valueOf(count));
        lblCount.getStyleClass().add("value-label");
        
        // Optional: Add a FontAwesome icon for EMI
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TICKET);
        icon.setFill(javafx.scene.paint.Color.WHITE);
        icon.setOpacity(0.5);
        icon.setGlyphSize(16);

        card.getChildren().addAll(icon, lblAmount, lblCount);
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
                        if (effectiveEmi == 0) counts.setOtherEmiCount(counts.getOtherEmiCount() + 1);
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