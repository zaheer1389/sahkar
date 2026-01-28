package com.badargadh.sahkar.service.report;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class YearlyReportService {

    private PdfFont headFont;
    private PdfFont categoryFont;
    private PdfFont dataFont;
    private PdfFont totalFont;

    private final String[] financialMonths = {
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    };

    private void initFonts() throws Exception {
        if (headFont == null) {
            headFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            categoryFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            dataFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            totalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE);
        }
    }

    public void generateYearlyReport(Map<String, MonthlyStatementDTO> yearlyData, String yearRange, String filePath) throws Exception {
        initFonts();
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        
        // Add Footer Event
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler(yearRange));

        // Create Landscape Document
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(20, 20, 40, 20);

        // 1. Title
        document.add(new Paragraph("YEARLY CONSOLIDATED FINANCIAL STATEMENT")
                .setFont(headFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Financial Period: " + yearRange)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // 2. Table Setup: 14 Columns
        float[] columnWidths = {4.5f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

        // Table Header
        String[] headers = {"Category", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC", "YRLY TOTAL"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(headFont).setFontSize(8).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(new DeviceRgb(44, 62, 80))
                    .setTextAlignment(TextAlignment.CENTER));
        }

        // 3. Opening Balance
        addRow(table, "MONTH OPENING BALANCE", yearlyData, MonthlyStatementDTO::getOpeningBal, true, new DeviceRgb(224, 236, 248));

        // 4. CASH INFLOW
        addSectionHeader(table, "CASH INFLOW / INCOME");
        addRow(table, "New Member Fees", yearlyData, MonthlyStatementDTO::getNewMemberFee, false, null);
        addRow(table, "Monthly Fees", yearlyData, MonthlyStatementDTO::getMonthlyFee, false, null);
        addRow(table, "EMI Collection", yearlyData, MonthlyStatementDTO::getTotalEmi, false, null);
        addRow(table, "Loan Deductions", yearlyData, MonthlyStatementDTO::getLoanDeduction, false, null);
        addRow(table, "Misc Credits", yearlyData, MonthlyStatementDTO::getExpenseCredit, false, null);
        addRow(table, "TOTAL INFLOW", yearlyData, MonthlyStatementDTO::getTotalIncome, true, new DeviceRgb(236, 240, 241));

        // Spacer
        table.addCell(new Cell(1, 14).setHeight(10).setBorder(Border.NO_BORDER));

        // 5. CASH OUTFLOW
        addSectionHeader(table, "CASH OUTFLOW / EXPENSES");
        addRow(table, "Loans Granted", yearlyData, MonthlyStatementDTO::getTotalLoanGranted, false, null);
        addRow(table, "Fee Refunds", yearlyData, MonthlyStatementDTO::getTotalFeeRefund, false, null);
        addRow(table, "Misc Debits", yearlyData, MonthlyStatementDTO::getExpenseDebit, false, null);
        addRow(table, "TOTAL OUTFLOW", yearlyData, MonthlyStatementDTO::getTotalOutgoing, true, new DeviceRgb(236, 240, 241));

        // 6. Final Summary
        table.addCell(new Cell(1, 14).setHeight(10).setBorder(Border.NO_BORDER));
        addRow(table, "NET CLOSING BALANCE", yearlyData, MonthlyStatementDTO::getClosingBalance, true, new DeviceRgb(236, 240, 241));

        document.add(table);
        document.close();
    }

    private void addRow(Table table, String label, Map<String, MonthlyStatementDTO> dataMap,
                        Function<MonthlyStatementDTO, Double> extractor, boolean isBoldRow, DeviceRgb customColor) {

        try {
			DeviceRgb bgColor = customColor != null ? customColor : (isBoldRow ? new DeviceRgb(236, 240, 241) : null);
			PdfFont font = isBoldRow ? categoryFont : PdfFontFactory.createFont(StandardFonts.HELVETICA);

			// Label Cell
			Cell labelCell = new Cell().add(new Paragraph(label).setFont(font).setFontSize(8));
			if (bgColor != null) labelCell.setBackgroundColor(bgColor);
			table.addCell(labelCell);

			double horizontalValue = 0.0;

			// 1. Monthly Columns
			for (String month : financialMonths) {
			    MonthlyStatementDTO monthDto = dataMap.get(month);
			    double val = (monthDto != null && extractor.apply(monthDto) != null) ? extractor.apply(monthDto) : 0.0;

			    Cell cell = new Cell().add(new Paragraph(val == 0 ? "-" : String.format("%.0f", val))
			            .setFont(isBoldRow ? totalFont : dataFont).setFontSize(8))
			            .setTextAlignment(TextAlignment.RIGHT);
			    if (bgColor != null) cell.setBackgroundColor(bgColor);
			    table.addCell(cell);
			}

			// 2. Logic for "YRLY TOTAL"
			if (label.contains("OPENING")) {
			    for (String month : financialMonths) {
			        if (dataMap.containsKey(month) && dataMap.get(month) != null) {
			            horizontalValue = dataMap.get(month).getOpeningBal();
			            break;
			        }
			    }
			} else if (label.contains("CLOSING")) {
			    for (int i = financialMonths.length - 1; i >= 0; i--) {
			        String month = financialMonths[i];
			        if (dataMap.containsKey(month) && dataMap.get(month) != null) {
			            horizontalValue = dataMap.get(month).getClosingBalance();
			            break;
			        }
			    }
			} else {
			    for (String month : financialMonths) {
			        MonthlyStatementDTO mDto = dataMap.get(month);
			        if (mDto != null && extractor.apply(mDto) != null) {
			            horizontalValue += extractor.apply(mDto);
			        }
			    }
			}

			// Total Cell
			table.addCell(new Cell().add(new Paragraph(String.format("%.0f", horizontalValue)).setFont(totalFont).setFontSize(8))
			        .setTextAlignment(TextAlignment.RIGHT)
			        .setBackgroundColor(new DeviceRgb(210, 210, 210)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void addSectionHeader(Table table, String text) {
        table.addCell(new Cell(1, 14).add(new Paragraph(text).setFont(headFont).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY))
                .setBackgroundColor(new DeviceRgb(245, 245, 245))
                .setPadding(3));
    }

    // --- Footer Handler ---
    private static class FooterHandler implements IEventHandler {
        private final String yearRange;
        public FooterHandler(String yearRange) { this.yearRange = yearRange; }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNum = pdf.getPageNumber(page);
            
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, page.getPageSize());

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            String footerText = "Yearly Report " + yearRange + " | Generated: " + timestamp + " | Page: " + pageNum;

            canvas.showTextAligned(new Paragraph(footerText).setFontSize(7).setItalic(),
                    page.getPageSize().getRight() - 20, 20, TextAlignment.RIGHT);
            canvas.close();
        }
    }
}