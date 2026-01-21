package com.badargadh.sahkar.service;

import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.LoanWitness;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.PaymentRemark;
import com.badargadh.sahkar.dto.LoanWitnessRemarkDTO;
import com.badargadh.sahkar.enums.LoanStatus;
import com.badargadh.sahkar.enums.RemarkType;
import com.badargadh.sahkar.repository.LoanAccountRepository;
import com.badargadh.sahkar.repository.LoanApplicationRepository;
import com.badargadh.sahkar.repository.LoanWitnessRepository;
import com.badargadh.sahkar.repository.MemberRepository;
import com.badargadh.sahkar.repository.PaymentRemarkRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class LoanWitnessRemarkImportService {

	@Autowired MemberRepository memberRepository;
	@Autowired LoanAccountRepository loanAccountRepository;
	@Autowired PaymentRemarkRepository paymentRemarkRepository;
	@Autowired LoanWitnessRepository loanWitnessRepository;
	@Autowired LoanApplicationRepository loanApplicationRepository;
	
    public void processLoanExcel(File file) throws Exception {
        List<LoanWitnessRemarkDTO> resultList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Assuming first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row if exists
            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Skip empty rows
                if (isRowEmpty(row)) continue;

                LoanWitnessRemarkDTO dto = new LoanWitnessRemarkDTO();

                // Col 2: Member No (Index 1)
                dto.setMemberNo(getNumericValue(row.getCell(1)));

                // Col 4: Type (Index 3) - Self/Authority
                dto.setType(getStringValue(row.getCell(3)));

                // Col 5: Witness No (Index 4)
                dto.setWitnessNo(getNumericValue(row.getCell(4)));

                // Col 6, 7, 8: Remarks/Dates (Index 5, 6, 7)
                List<LocalDate> dateRemarks = new ArrayList<>();
                for (int i = 5; i <= 7; i++) {
                    LocalDate date = getDateValue(row.getCell(i));
                    if (date != null) {
                        dateRemarks.add(date);
                    }
                }
                dto.setRemarks(dateRemarks);

                resultList.add(dto);
            }
        }
        
        for(LoanWitnessRemarkDTO dto : resultList) {
        	Optional<Member> optional = memberRepository.findByMemberNo(dto.getMemberNo());
        	if(optional.isPresent()) {
        		Member member = optional.get();
        		/*Optional<LoanAccount> loOptional = loanAccountRepository.findByMemberAndLoanStatus(member, LoanStatus.ACTIVE);
        		if(loOptional.isPresent()) {
        			LoanAccount loanAccount = loOptional.get();
        			LoanApplication loanApplication = loanAccount.getLoanApplication();
        			
        		}*/
        		
        		for(LocalDate date : dto.getRemarks()) {
        			PaymentRemark remark = new PaymentRemark();
        			remark.setMember(member);
        			remark.setFinancialMonth(null);
        			remark.setIssuedDate(date);
        			remark.setRemarkType(RemarkType.LATE_FEE);
        			paymentRemarkRepository.save(remark);
        		}
        	}
        }
        
    }
    
    public void processGujMemberNameExcel(File file) throws Exception {
        List<Member> resultList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Assuming first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row if exists
            //if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Skip empty rows
                if (isRowEmpty(row)) continue;

                Member dto = new Member();
                
                // Col 2: Member No (Index 1)
                dto.setMemberNo(getNumericValue(row.getCell(3)));

                // Col 4: Type (Index 3) - Self/Authority
                dto.setFirstNameGuj((getStringValue(row.getCell(8))));
                dto.setMiddleNameGuj((getStringValue(row.getCell(9))));
                dto.setLastNameGuj((getStringValue(row.getCell(10))));
                dto.setBranchNameGuj((getStringValue(row.getCell(11))));

                resultList.add(dto);
            }
        }
        
        List<Member> members = new ArrayList<Member>();
        for(Member dto : resultList) {
        	Optional<Member> optional = memberRepository.findByMemberNo(dto.getMemberNo());
        	if(optional.isPresent()) {
        		Member member = optional.get();
        		member.setFirstNameGuj(dto.getFirstNameGuj());
        		member.setLastNameGuj(dto.getLastNameGuj());
        		member.setMiddleNameGuj(dto.getMiddleNameGuj());
        		member.setBranchNameGuj(dto.getBranchNameGuj()); 
        		members.add(member);
        	}
        }
        
        memberRepository.saveAll(members);
    }
    
    public void processGujMemberNameExcel2(File file) throws Exception {
        List<Member> resultList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Assuming first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row if exists
            //if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Skip empty rows
                if (isRowEmpty(row)) continue;

                Member dto = new Member();
                
                // Col 2: Member No (Index 1)
                dto.setMemberNo(getNumericValue(row.getCell(3)));

                // Col 4: Type (Index 3) - Self/Authority
                dto.setFirstNameGuj((getStringValue(row.getCell(8))));
                dto.setMiddleNameGuj((getStringValue(row.getCell(9))));
                dto.setLastNameGuj((getStringValue(row.getCell(10))));
                dto.setBranchNameGuj((getStringValue(row.getCell(11))));

                resultList.add(dto);
            }
        }
        
        List<Member> members = new ArrayList<Member>();
        for(Member dto : resultList) {
        	Optional<Member> optional = memberRepository.findById(dto.getMemberNo().longValue());
        	if(optional.isPresent()) {
        		Member member = optional.get();
        		member.setFirstNameGuj(dto.getFirstNameGuj());
        		member.setLastNameGuj(dto.getLastNameGuj());
        		member.setMiddleNameGuj(dto.getMiddleNameGuj());
        		member.setBranchNameGuj(dto.getBranchNameGuj()); 
        		members.add(member);
        	}
        }
        
        memberRepository.saveAll(members);
    }

    private int getNumericValue(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Integer.parseInt(cell.getStringCellValue().trim()); } catch (Exception e) { return 0; }
        }
        return 0;
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return String.valueOf(cell);
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) return null;
        
        // Handle if Excel stored it as a Date Type
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        
        // Handle if Excel stored it as a String Type (e.g., "15-05-2024")
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) return null;
            try {
                // Adjust format "dd-MM-yyyy" or "yyyy-MM-dd" as per your file
                return LocalDate.parse(val, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        Cell firstCell = row.getCell(1);
        return firstCell == null || firstCell.getCellType() == CellType.BLANK;
    }
}