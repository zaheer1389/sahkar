package com.badargadh.sahkar.service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.enums.CancellationReason;
import com.badargadh.sahkar.enums.CollectionLocation;
import com.badargadh.sahkar.enums.LoanApplicationStatus;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.MemberStatus;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.EmiPaymentRepository;
import com.badargadh.sahkar.repository.FeePaymentRepository;
import com.badargadh.sahkar.repository.FinancialMonthRepository;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.util.UserSession;

import jakarta.annotation.PostConstruct;

@Service
public class MonthlyLoadDataService {
	
	@Autowired private AppConfigService appConfigService;
	@Autowired private UserService userService;
	@Autowired private FeeService feeService;
	@Autowired private EmiPaymentService emiPaymentService;
	@Autowired private LoanService loanService;
	@Autowired private LoanDisbursementService loanDisbursementService;
	@Autowired private MemberRepository memberRepository;
	@Autowired private FeePaymentRepository feePaymentRepository;
	@Autowired private EmiPaymentRepository emiPaymentRepository;
	@Autowired private FinancialMonthRepository financialMonthRepository;
	@Autowired private LoanAccountRepository loanAccountRepository;
	@Autowired private FinancialMonthService financialMonthService;
	@Autowired private PaymentCollectionService collectionService;
	@Autowired private LoanApplicationRepository loanAppRepo;
	
	private static String MONTH = "Dec-25";
	//private LocalDate emiDateOfMonth = LocalDate.of(2025, 10, 10);
	//private LocalDate businessMonthStartDate = LocalDate.of(2025, 10, 01);
	private AppUser appUser;
	private AppConfig appConfig;
	private FinancialMonth financialMonth;
	
	Map<Long, Long> newLoanEMIAmountMap = new HashMap<Long, Long>();
	List<Long> fullPaymentList = new ArrayList<Long>();
	List<Long> newLoanList = new ArrayList<Long>();
	List<Long> cancelledMemberList = new ArrayList<Long>();
	
	//"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
    //"JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"

	
	public void readExcelFile(String handle) {
		try {
			
			appUser = userService.getAdminUser();
			appConfig = appConfigService.getSettings();
			
			financialMonth = financialMonthService.getMonthFromMonthAndYear("DECEMBER", 2025).get();
			
			readFullPaymentList();
			readNewLoanEMIAmountList();
			readNewLoanList();
			readCancellationList();
			
			System.err.println("New Loan EMI Amount Size :: "+newLoanEMIAmountMap.size());
			System.err.println("New Loan List Size :: "+newLoanList.size());
			
			if(handle.equals("data")) {
				processData();
			}
			else if(handle.equals("loans")) {
				processLoanApplications();
			}
			else if(handle.equals("cancellations")) {
				processCancellation();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readNewLoanEMIAmountList() {
		newLoanEMIAmountMap.clear();
		try {
			String fileName = "D:\\data\\sahkar\\"+MONTH+"\\Members_NewLoan_EMI.xlsx";
			FileInputStream fis = new FileInputStream(new File(fileName));
			XSSFWorkbook workbook = new XSSFWorkbook(fis); 
			XSSFSheet sheet = workbook.getSheetAt(0); 
			for (Row row : sheet) // iteration over row using for each loop
			{
				Cell cell = row.getCell(0);
				Cell cell2 = row.getCell(1);
				
				String retValue = returnStringValue(cell);
				String retValue2 = returnStringValue(cell2);
				
				newLoanEMIAmountMap.put(Long.parseLong(retValue), Long.parseLong(retValue2));
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void readNewLoanList() {
		newLoanList.clear();
		try {
			String fileName = "D:\\data\\sahkar\\"+MONTH+"\\Members_NewLoan_List.xlsx";
			FileInputStream fis = new FileInputStream(new File(fileName));
			XSSFWorkbook workbook = new XSSFWorkbook(fis); 
			XSSFSheet sheet = workbook.getSheetAt(0); 
			for (Row row : sheet) // iteration over row using for each loop
			{
				Cell cell = row.getCell(0);
				String retValue = returnStringValue(cell);
				newLoanList.add(Long.parseLong(retValue));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readFullPaymentList() {
		fullPaymentList.clear();
		try {
			String fileName = "D:\\data\\sahkar\\"+MONTH+"\\Members_Full_Payment.xlsx";
			FileInputStream fis = new FileInputStream(new File(fileName));
			XSSFWorkbook workbook = new XSSFWorkbook(fis); 
			XSSFSheet sheet = workbook.getSheetAt(0); 
			for (Row row : sheet) // iteration over row using for each loop
			{
				Cell cell = row.getCell(0);
				String retValue = returnStringValue(cell);
				fullPaymentList.add(Long.parseLong(retValue));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readCancellationList() {
		cancelledMemberList.clear();
		try {
			String fileName = "D:\\data\\sahkar\\"+MONTH+"\\Members_Cancellation.xlsx";
			FileInputStream fis = new FileInputStream(new File(fileName));
			XSSFWorkbook workbook = new XSSFWorkbook(fis); 
			XSSFSheet sheet = workbook.getSheetAt(0); 
			for (Row row : sheet) // iteration over row using for each loop
			{
				Cell cell = row.getCell(0);
				String retValue = returnStringValue(cell);
				cancelledMemberList.add(Long.parseLong(retValue));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String returnStringValue(Cell cell) {
	    CellType cellType = cell.getCellType();
	    
	    if(cellType == CellType.NUMERIC) {
	    	double doubleVal = cell.getNumericCellValue();
            if (doubleVal == (int) doubleVal) {
                int value = Double.valueOf(doubleVal).intValue();
                return String.valueOf(value);
            } else {
                return String.valueOf(doubleVal);
            }
	    }
	    else if(cellType == CellType.STRING) {
	    	return cell.getStringCellValue();
	    }
	    else if(cellType == CellType.ERROR) {
	    	return String.valueOf(cell.getErrorCellValue());
	    }
	    else if(cellType == CellType.BLANK) {
	    	return "";
	    }
	    else if(cellType == CellType.FORMULA) {
	    	return cell.getCellFormula();
	    }
	    else if(cellType == CellType.BOOLEAN) {
	    	return String.valueOf(cell.getBooleanCellValue());
	    }
	   
	    return "error decoding string value of the cell";

	}
	

	public void processData() {
		
		recordFullPayment();
		
		List<Member> members = memberRepository.findAllActiveMembers();	
		for(Member member : members) {
			if(!collectionService.isMonthlyMemberEmiorFeesPaid(member, financialMonth)) {
				Optional<LoanAccount> optional = loanAccountRepository.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
				if(optional.isPresent()) {
					recordEmi(member, optional.get(), false);
				}
				else {
					recordFees(member);
				}
			}
		}
	}
	
	private void recordFees(Member member) {
		try {
			LocalDateTime dateTime = LocalDateTime.of(financialMonth.getStartDate().plusDays(10), LocalTime.of(13, 30));
			feeService.recordMonthlyFee(member, appConfig.getMonthlyFees(), dateTime, financialMonth, member, CollectionLocation.MUMBAI, null);
		}
		catch(Exception e) {
			System.err.println("error while adding fees for member - "+member.getMemberNo());
			System.err.println(e.getMessage());
		}
	}
	
	private void recordEmi(Member member, LoanAccount loanAccount, boolean fullpayment) {
	    try {
	        recordFees(member);
	        
	        LocalDateTime dateTime = LocalDateTime.of(financialMonth.getStartDate().plusDays(10), LocalTime.of(13, 30));
	        
	        if (loanAccount != null) {
	            double currentPending = loanAccount.getPendingAmount();
	            double emiAmountToRecord = 0.0;
	            double extraPrincipalAmount = 0.0;

	            // 1. Calculate Standard EMI portion
	            double standardEmi = loanAccount.getEmiAmount() != null ? loanAccount.getEmiAmount().doubleValue() : 0.0;
	            
	            // Handle first-time EMI locking
	            if (!loanAccount.isEmiLocked()) {
	                standardEmi = newLoanEMIAmountMap.get(member.getMemberNo().longValue()).doubleValue();
	                loanAccount.setEmiAmount(standardEmi);
	                loanAccount.setStartDate(financialMonth.getStartDate().plusDays(10));
	            }
	            
	            if(standardEmi <= 0) {
	            	return;
	            }

	            if (fullpayment) {
	                // If Full Payment: The standard EMI is taken first, 
	                // and the rest is recorded as Full Payment (extra principal)
	                emiAmountToRecord = Math.min(standardEmi, currentPending);
	                extraPrincipalAmount = currentPending - emiAmountToRecord;
	            } else {
	                // Standard EMI only: Cap it at current pending
	                emiAmountToRecord = Math.min(standardEmi, currentPending);
	                extraPrincipalAmount = 0.0;
	            }

	            // 2. Prepare EMI Payment Record
	            EmiPayment emi = new EmiPayment();
	            emi.setMember(member);
	            emi.setLoanAccount(loanAccount);
	            emi.setFinancialMonth(financialMonth);
	            
	            // Store separate values
	            emi.setAmountPaid(emiAmountToRecord);       // The regular EMI part
	            emi.setFullPayment(fullpayment);
	            emi.setFullPaymentAmount(extraPrincipalAmount); // The closure/extra part
	            
	            emi.setPaymentDateTime(dateTime);
	            emi.setDipositedBy(member);
	            emi.setAddedBy(UserSession.getLoggedInUser());
	            
	            emiPaymentService.recordEmiPayment(emi);

	            // 3. Update Loan Balance
	            // Total deduction = EMI Amount + Full Payment Amount
	            double totalDeduction = emiAmountToRecord + extraPrincipalAmount;
	            double newPending = currentPending - totalDeduction;
	            
	            loanAccount.setPendingAmount(Math.max(0, newPending));

	            // 4. Handle Loan Closure
	            if (loanAccount.getPendingAmount() <= 0) {
	                loanAccount.setLoanStatus(LoanStatus.PAID);
	                loanAccount.setEndDate(financialMonth.getStartDate().plusDays(10));
	                loanAccount.setPendingAmount(0.0);
	            }
	            
	            loanAccountRepository.save(loanAccount);
	        }
	    } catch(Exception e) {
	        System.err.println("Error while adding fees for member - " + member.getMemberNo());
	        e.printStackTrace();
	    }
	}
	
	private void recordFullPayment() {
		
		for(Long no : fullPaymentList) {
			Member member = memberRepository.findByMemberNo(no.intValue()).get();
			Optional<LoanAccount> optional = loanAccountRepository.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
			if(optional.isPresent()) {
				recordEmi(member, optional.get(), true);
			}
		}
	}
	
	private void processLoanApplications() {
		for(Long no : newLoanList) {
			Member member = memberRepository.findByMemberNo(no.intValue()).get();
			
			LocalDateTime dateTime = LocalDateTime.of(financialMonth.getStartDate().plusDays(10), LocalTime.of(13, 30));
			
			LoanApplication loanApplication = new LoanApplication();
			loanApplication.setMember(member);
			loanApplication.setApplicationNumber(UUID.randomUUID().toString().substring(1, 5));
			loanApplication.setFinancialMonth(financialMonth);
			loanApplication.setAppliedAmount(appConfig.getLoanAmount().doubleValue());
			loanApplication.setStatus(LoanApplicationStatus.APPLIED);
			loanApplication.setApplicationDateTime(dateTime);
			loanApplication = loanAppRepo.save(loanApplication);
	        
	        LoanAccount account = new LoanAccount();
	        account.setLoanApplication(loanApplication);
	        account.setFinancialMonth(financialMonth);
	        account.setGrantedAmount(appConfig.getLoanAmount().doubleValue());
	        account.setMember(member);
	        account.setPendingAmount(appConfig.getLoanAmount().doubleValue());
	        account.setLoanStatus(LoanStatus.ACTIVE);
	        account = loanAccountRepository.save(account);
	        
	        loanApplication.setStatus(LoanApplicationStatus.DISBURSED);
	        loanApplication.setCollectionRemarks("System generated :: loan disbursment");
	        loanApplication = loanAppRepo.save(loanApplication);
	        
	        Double feesDeduction = feeService.getMemberFeeDeductionOnFirstLoan(member);
	        
	        if(feesDeduction > 0) {
	        	feeService.recordLoanDeductionFee(member, feesDeduction.longValue(), dateTime, financialMonth, member);
	        }
		}
	}

	private void processCancellation() {
		for(Long no : cancelledMemberList) {
			Member member = memberRepository.findByMemberNo(no.intValue()).get();
			if(member.getStatus() == null) {
				Optional<LoanAccount> optional = loanAccountRepository.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
				if(!optional.isPresent()) {
					member.setStatus(MemberStatus.CANCELLED);
				    member.setCancellationDateTime(LocalDateTime.of(financialMonth.getStartDate().plusDays(15), LocalTime.of(16, 45)));
				    member.setCancellationReason(CancellationReason.SELF_CANCELLATION);
				    memberRepository.save(member);
				}
			}
		}
	}
}
