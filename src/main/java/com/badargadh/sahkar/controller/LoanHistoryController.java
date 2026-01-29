package com.badargadh.sahkar.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.LoanHistoryDTO;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.service.LoanService;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@Component
public class LoanHistoryController extends BaseController implements Initializable {
	
    @FXML private TableView<LoanHistoryDTO> tblLoanHistory;
    @FXML private TableColumn<LoanHistoryDTO, String> colDate, colStatus;
    @FXML private TableColumn<LoanHistoryDTO, Long> colAmount;
    @FXML private TableColumn<LoanHistoryDTO, String> colType, colWitnessName, colAuthorityName;
    
    @Autowired private LoanAccountRepository accountRepository;
    @Autowired private LoanService loanService;

    public void loadHistory(Member member) {
        // 1. Setup the CellValueFactories (tells JavaFX which DTO field goes to which column)
        colDate.setCellValueFactory(new PropertyValueFactory<>("loanDate"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colType.setCellValueFactory(new PropertyValueFactory<>("collectionType"));
        colWitnessName.setCellValueFactory(new PropertyValueFactory<>("witnessName"));
        colAuthorityName.setCellValueFactory(new PropertyValueFactory<>("authorityName"));
        
        // Inside loadHistory - Add a custom cell factory for the Amount column
        colAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₹ %,d", item)); // Formats as ₹ 50,000
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
                }
            }
        });
        
        colStatus.setCellFactory(column -> new TableCell<LoanHistoryDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                	Label badge = new Label();
                    
                    if (item.toUpperCase().equals("ACTIVE")) {
                        badge.setText("ACTIVE");
                        badge.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                                     "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    } else {
                        badge.setText("PAID");
                        badge.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                                     "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // 2. Fetch and Convert Data
        List<LoanAccount> accounts = accountRepository.findByMember(member);
        
        List<LoanHistoryDTO> loanHistoryDTOs = accounts.stream()
                .map(a -> {
                    String loanDate = a.getCreatedDate() != null ? a.getCreatedDate().toString() : "N/A";
                    LoanApplication application = a.getLoanApplication();
                    LoanWitness loanWitness = loanService.getLoanWitness(application.getMember().getMemberNo());
                    String witnessName = "";
                    if(loanWitness != null) {
            			if(loanWitness.getWitnessMember() != null) {
            				witnessName = loanWitness.getWitnessMember().getGujFullname();
            			}
            			else {
            				witnessName = loanWitness.getWintessName();
                		}
            		}
                    return new LoanHistoryDTO(
                        loanDate, 
                        a.getGrantedAmount().longValue(), 
                        a.getLoanStatus().name(),
                        application.getCollectionType() != null ? application.getCollectionType().name() : "",
                        witnessName,
                        application.getAuthorityName() != null ? application.getAuthorityName() : ""
                    );
                }).toList();

        // 3. Set the data into the TableView
        tblLoanHistory.setItems(FXCollections.observableArrayList(loanHistoryDTOs));
        tblLoanHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    }

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
	}
}