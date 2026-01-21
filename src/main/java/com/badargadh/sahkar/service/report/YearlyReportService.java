package com.badargadh.sahkar.service.report;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class YearlyReportService {

    // Fonts for different sections
    private final Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private final Font categoryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
    private final Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC);
    private final Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Font.ITALIC);

    private final String[] financialMonths = {
    		"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", 
    	    "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    };

    public void generateYearlyReport(Map<String, MonthlyStatementDTO> yearlyData, String yearRange, String filePath) throws Exception {
        // 1. Create Landscape Document
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        
        // Add Footer Event for Page Numbers and Timestamp
        writer.setPageEvent(new PdfPageEventHelper() {
            public void onEndPage(PdfWriter writer, Document document) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
                Phrase p = new Phrase("Yearly Report " + yearRange + " | Generated: " + timestamp + " | Page: " + writer.getPageNumber(), 
                                    FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC));
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, p, document.right(), 20, 0);
            }
        });

        document.open();

        // 2. Title
        Paragraph title = new Paragraph("YEARLY CONSOLIDATED FINANCIAL STATEMENT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Phrase subTitle = new Phrase("Financial Period: " + yearRange, FontFactory.getFont(FontFactory.HELVETICA, 12));
        Paragraph pSub = new Paragraph(subTitle);
        pSub.setAlignment(Element.ALIGN_CENTER);
        pSub.setSpacingAfter(20);
        document.add(pSub);

        // 3. Table Setup: 14 Columns (Category + 12 Months + Yearly Total)
        float[] columnWidths = {4.5f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        // Table Header Row
        String[] headers = {"Category", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "YRLY TOTAL"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(44, 62, 80));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
        
        addRow(table, "MONTH OPENING BALANCE", yearlyData, MonthlyStatementDTO::getOpeningBal, true, new Color(224, 236, 248));

        // 4. Data Rows - CASH INFLOW
        addSectionHeader(table, "CASH INFLOW / INCOME");
        addRow(table, "New Member Fees", yearlyData, MonthlyStatementDTO::getNewMemberFee, false,null);
        addRow(table, "Monthly Fees", yearlyData, MonthlyStatementDTO::getMonthlyFee, false,null);
        addRow(table, "EMI Collection", yearlyData, MonthlyStatementDTO::getTotalEmi, false,null);
        addRow(table, "Loan Deductions", yearlyData, MonthlyStatementDTO::getLoanDeduction, false,null);
        addRow(table, "Misc Credits", yearlyData, MonthlyStatementDTO::getExpenseCredit, false,null);
        addRow(table, "TOTAL INFLOW", yearlyData, MonthlyStatementDTO::getTotalIncome, true, new Color(236, 240, 241));

        // Spacer Row
        table.addCell(new PdfPCell(new Phrase(" ")) {{ setColspan(14); setBorder(Rectangle.NO_BORDER); setFixedHeight(10); }});

        // 5. Data Rows - CASH OUTFLOW
        addSectionHeader(table, "CASH OUTFLOW / EXPENSES");
        addRow(table, "Loans Granted", yearlyData, MonthlyStatementDTO::getTotalLoanGranted, false, null);
        addRow(table, "Fee Refunds", yearlyData, MonthlyStatementDTO::getTotalFeeRefund, false,null);
        addRow(table, "Misc Debits", yearlyData, MonthlyStatementDTO::getExpenseDebit, false,null);
        addRow(table, "TOTAL OUTFLOW", yearlyData, MonthlyStatementDTO::getTotalOutgoing, true, new Color(236, 240, 241));

        // 6. Final Summary Row
        table.addCell(new PdfPCell(new Phrase(" ")) {{ setColspan(14); setBorder(Rectangle.NO_BORDER); setFixedHeight(10); }});
        addRow(table, "NET CLOSING BALANCE", yearlyData, MonthlyStatementDTO::getClosingBalance, true, new Color(236, 240, 241));

        document.add(table);
        document.close();
    }

    private void addRow(PdfPTable table, String label, Map<String, MonthlyStatementDTO> dataMap, 
            Function<MonthlyStatementDTO, Double> extractor, boolean isBoldRow, Color customColor) {

		Color bgColor = customColor != null ? customColor : (isBoldRow ? new Color(236, 240, 241) : null);
		
		// Label Cell
		PdfPCell labelCell = new PdfPCell(new Phrase(label, isBoldRow ? categoryFont : FontFactory.getFont(FontFactory.HELVETICA, 8)));
		if (bgColor != null) labelCell.setBackgroundColor(bgColor);
		table.addCell(labelCell);
		
		double horizontalValue = 0.0;
		
		// 1. Logic for Monthly Columns
		for (String month : financialMonths) {
		MonthlyStatementDTO monthDto = dataMap.get(month);
		double val = (monthDto != null && extractor.apply(monthDto) != null) ? extractor.apply(monthDto) : 0.0;
		
		PdfPCell cell = new PdfPCell(new Phrase(val == 0 ? "-" : String.format("%.0f", val), isBoldRow ? totalFont : dataFont));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		if (bgColor != null) cell.setBackgroundColor(bgColor);
			table.addCell(cell);
		}
		
		// 2. Logic for the "YRLY TOTAL" Column (The Balancing Fix)
	    if (label.contains("OPENING")) {
	        // Find the FIRST month in the sequence that actually has data
	        double firstFoundOpening = 0.0;
	        for (String month : financialMonths) {
	            if (dataMap.containsKey(month) && dataMap.get(month) != null) {
	                firstFoundOpening = dataMap.get(month).getOpeningBal();
	                break; // Stop at the first month found
	            }
	        }
	        horizontalValue = firstFoundOpening;
	    } 
	    else if (label.contains("CLOSING") || label.contains("NET CASH")) {
	        // Find the LAST month in the sequence that actually has data
	        double lastFoundClosing = 0.0;
	        for (int i = financialMonths.length - 1; i >= 0; i--) {
	            String month = financialMonths[i];
	            if (dataMap.containsKey(month) && dataMap.get(month) != null) {
	                lastFoundClosing = dataMap.get(month).getClosingBalance();
	                break; // Stop at the last month found
	            }
	        }
	        horizontalValue = lastFoundClosing;
	    } 
	    else {
	        // Income/Expenses remain cumulative (Sum of all available data)
	        for (String month : financialMonths) {
	            MonthlyStatementDTO mDto = dataMap.get(month);
	            if (mDto != null && extractor.apply(mDto) != null) {
	                horizontalValue += extractor.apply(mDto);
	            }
	        }
	    }
		
		// Yearly Total Cell
		PdfPCell totalCell = new PdfPCell(new Phrase(String.format("%.0f", horizontalValue), totalFont));
		totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		totalCell.setBackgroundColor(new Color(210, 210, 210));
		table.addCell(totalCell);
	}

    private void addSectionHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.DARK_GRAY)));
        cell.setColspan(14);
        cell.setBackgroundColor(new Color(245, 245, 245));
        cell.setPadding(3);
        table.addCell(cell);
    }
}