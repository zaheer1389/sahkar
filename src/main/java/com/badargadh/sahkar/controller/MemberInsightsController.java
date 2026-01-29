package com.badargadh.sahkar.controller;

import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.service.MemberService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemberInsightsController extends BaseController implements Initializable {

    @FXML private TabPane tabPane;
    @FXML private Tab tabLoanHistory;
    @FXML private Tab tabRemarks;
    @FXML private Label lblMemberHeader; // Label to show Member Name at the top

    @Autowired private MemberService memberService;
    
    public void initData(MemberSummaryDTO dto) {
        if (dto == null || dto.getMemberNo() == null) return;

        Member member = memberService.findByMemberNumber(dto.getMemberNo());
        
        if(member == null) return;
        
        // Set the header text in the popup
        lblMemberHeader.setText("Member: " + member.getMemberNo() + " - " + member.getGujFullname());

        try {
            // 1. Load Loan History into its Tab
            FXMLLoader loanLoader = new FXMLLoader(getClass().getResource("/fxml/MemberLoanHistory.fxml"));
            loanLoader.setControllerFactory(springContext::getBean);
            AnchorPane loanContent = loanLoader.load();
            
            // Get the child controller and pass the ID
            LoanHistoryController loanCtrl = loanLoader.getController();
            loanCtrl.loadHistory(member); 
            
            tabLoanHistory.setContent(loanContent);

            // 2. Load Remarks into its Tab
            FXMLLoader remarksLoader = new FXMLLoader(getClass().getResource("/fxml/MemberRemarks.fxml"));
            remarksLoader.setControllerFactory(springContext::getBean);
            AnchorPane remarksContent = remarksLoader.load();
            
            // Get the child controller and pass the full DTO
            MemberRemarkController remarksCtrl = remarksLoader.getController();
            remarksCtrl.loadMemberRemarks(member); 
            
            tabRemarks.setContent(remarksContent);

        } catch (IOException e) {
            System.err.println("Error loading Tab FXMLs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        tabPane.getScene().getWindow().hide();
    }

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
	}
}