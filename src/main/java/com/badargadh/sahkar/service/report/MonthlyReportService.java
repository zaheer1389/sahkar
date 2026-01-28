package com.badargadh.sahkar.service.report;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class MonthlyReportService {

    private static final String GUJARATI_FONT_PATH_NOTOSANS = "/static/fonts/NotoSansGujarati.ttf";

    private PdfFont gujFont;
    private PdfFont titleFont;
    private PdfFont labelFont;
    private PdfFont amountFont;
    private PdfFont tableHeadFont;

    private void initializeFonts() throws Exception {
        if (gujFont == null) {
            URL fontUrl = getClass().getResource(GUJARATI_FONT_PATH_NOTOSANS);
            if (fontUrl == null) throw new RuntimeException("Font not found: " + GUJARATI_FONT_PATH_NOTOSANS);
            
            // iText 7 handles the URL string directly
            gujFont = PdfFontFactory.createFont(fontUrl.toString(), PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            
            titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            labelFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            amountFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLDOBLIQUE);
            tableHeadFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        }
    }

    public void generateMonthlyStatementReport(MonthlyStatementDTO data, String monthName, String filePath) throws Exception {
        initializeFonts();
        
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        
        // Add Footer Event
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());

        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(30, 30, 30, 30);

        generateSummaryPage(document, data, monthName);
        document.add(new AreaBreak()); // iText 7 replacement for newPage()
        generateAppendixPage(document, data);
        
        document.close();
    }

    private void generateSummaryPage(Document doc, MonthlyStatementDTO data, String month) {
        // Combined English/Gujarati Header
        doc.add(new Paragraph("MONTHLY CASH STATEMENT")
                .setFont(titleFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph(month)
                .setFont(labelFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Opening Balance
        Double opening = data.getOpeningBal();
        doc.add(new Paragraph("\nOpening Balance: ₹ " + String.format("%.2f", opening) + "/-")
                .setFont(amountFont)
                .setFontSize(18)
                .setFontColor(opening < 0 ? ColorConstants.RED : new DeviceRgb(0, 100, 0))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingBottom(20f));

        // Main Ledger Table (2 Columns)
        Table ledger = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        // Left Side: Inflow
        Cell left = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(10f).setBorderRight(new SolidBorder(2f));
        addSummaryRow(left, "New Member Fees", data.getNewMemberFee());
        addSummaryRow(left, "Monthly Fees", data.getMonthlyFee());
        addSummaryRow(left, "Loan Deduction Fees", data.getLoanDeduction());
        addSummaryRowWithBreakdown(left, "Total EMI Collection", data.getTotalEmi(), data.getEmiBreakdown());
        addSummaryRow(left, "Full Payments", data.getFullPaymentAmount());
        addSummaryRow(left, "Misc Credits", data.getExpenseCredit());
        ledger.addCell(left);

        // Right Side: Outflow
        Cell right = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(10);
        addSummaryRow(right, "Total Loans Granted", data.getTotalLoanGranted());
        addSummaryRow(right, "Total Fee Refunds", data.getTotalFeeRefund());
        addSummaryRow(right, "Misc Debits", data.getExpenseDebit());
        ledger.addCell(right);

        doc.add(ledger);

        // Totals Table
        Table totals = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        totals.addCell(createTotalCell("TOTAL INFLOW: ₹ " + String.format("%.2f", data.getTotalIncome()) + "/-"));
        totals.addCell(createTotalCell("TOTAL OUTFLOW: ₹ " + String.format("%.2f", data.getTotalOutgoing()) + "/-"));
        doc.add(totals);

        Double closing = data.getClosingBalance();
        doc.add(new Paragraph("\nClosing Balance: ₹ " + String.format("%.2f", closing) + "/-")
                .setFont(amountFont)
                .setFontSize(18)
                .setFontColor(closing < 0 ? ColorConstants.RED : new DeviceRgb(0, 100, 0))
                .setTextAlignment(TextAlignment.RIGHT));
    }

    private void generateAppendixPage(Document doc, MonthlyStatementDTO data) {
        Table container = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        Cell left = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(5);
        addListSection(left, "NEW MEMBERS JOINED", List.of("M.No", "Name"), new float[]{8f, 14f, 78f}, data.getNewMembersData());
        addListSection(left, "FULL PAYMENTS LIST", List.of("M.No", "Name", "Amt"), new float[]{8f, 14f, 63f, 15f}, data.getFullpaymentsData());
        addListSection(left, "LAST MONTH NEW LOANS EMI", List.of("M.No", "Name", "Amt"), new float[]{8f, 14f, 63f, 15f}, data.getPrevMonthNewLoansEMIAmtData());

        Cell right = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(5);
        addListSection(right, "NEW LOANS GRANTED", List.of("M.No", "Name"), new float[]{8f, 14f, 78f}, data.getNewLoanListMembersData());
        addListSection(right, "FEE REFUNDS", List.of("M.No", "Name", "Amt"), new float[]{8f, 14f, 63f, 15f}, data.getRefundMembersData());
        addListSection(right, "CANCELLED MEMBERS", List.of("M.No", "Name"), new float[]{8f, 14f, 78f}, data.getCancelledMembersData());

        container.addCell(left);
        container.addCell(right);
        doc.add(container);
    }

    private void addListSection(Cell column, String title, List<String> headers, float[] widths, List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return;

        column.add(new Paragraph(title).setFont(tableHeadFont).setFontSize(10).setMarginTop(15));
        
        Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();

        table.addCell(createHeaderCell("Sr."));
        for (String h : headers) table.addCell(createHeaderCell(h));

        // Data
        int sr = 1;
        for (List<String> row : rows) {
        	table.addCell(new Cell().add(new Paragraph(String.valueOf(sr++)).setFont(gujFont).setFontSize(9)).setBold()
                    .setPaddingBottom(1));
            for (String val : row) {
            	Cell cell = new Cell().add(new Paragraph(val != null ? val : "").setFont(gujFont).setFontSize(9)).setBold()
                        .setPaddingBottom(1);
                if (val != null && val.matches("-?\\d+(\\.\\d+)?")) {
                	cell.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                }
            	table.addCell(cell);
            }
        }
        column.add(table);
    }

    private void addSummaryRow(Cell parentCell, String label, Double amt) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth();
        t.addCell(new Cell().add(new Paragraph(label).setFont(labelFont)).setBorder(Border.NO_BORDER));
        t.addCell(new Cell().add(new Paragraph("₹ " + String.format("%.2f", amt)).setFont(amountFont))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
        parentCell.add(t);
    }

    private void addSummaryRowWithBreakdown(Cell parentCell, String label, Double total, Map<Double, Long> breakdown) {
        addSummaryRow(parentCell, label, total);
        if (breakdown != null) {
            for (Map.Entry<Double, Long> entry : breakdown.entrySet()) {
                Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
                String text = "     " + entry.getKey().intValue() + " x " + entry.getValue();
                t.addCell(new Cell().setPaddingLeft(20f).add(new Paragraph(text).setFont(amountFont).setFontSize(9)).setBorder(Border.NO_BORDER));
                t.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getKey() * entry.getValue())).setFont(amountFont).setFontSize(9))
                        .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
                parentCell.add(t);
            }
        }
    }
    
    private Cell createHeaderCell(String text) {
    	Cell cell = new Cell().add(new Paragraph(text).setFont(tableHeadFont).setFontSize(8).setFontColor(ColorConstants.WHITE))
        .setBackgroundColor(new DeviceRgb(44, 62, 80))
        .setPadding(2);
    	
    	return cell;
    }

    private Cell createTotalCell(String text) {
        return new Cell().add(new Paragraph(text).setFont(titleFont).setFontSize(11).setItalic())
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(2f))
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingTop(20);
    }

    // --- Footer Event Handler for iText 7 ---
    private static class FooterHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdf.getPageNumber(page);
            Rectangle pageSize = page.getPageSize();
            
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pageSize);
            
            String ts = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            
            Table footer = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
            footer.setFixedPosition(30, 30, pageSize.getWidth() - 60);
            
            footer.addCell(new Cell().add(new Paragraph(ts).setFontSize(8).setItalic()).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(0.5f)));
            footer.addCell(new Cell().add(new Paragraph("Page " + pageNumber).setFontSize(8)).setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(0.5f)).setTextAlignment(TextAlignment.RIGHT));
            
            canvas.add(footer);
            canvas.close();
        }
    }
}