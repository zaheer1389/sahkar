package com.badargadh.sahkar.service.report;

import java.awt.Color;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.MonthlyStatementDTO;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class MonthlyReportService {
    
    // Use Lohit Gujarati - it is highly reliable for joining characters (ligatures)
    private static final String GUJARATI_FONT_PATH = "/static/fonts/Lohit-Gujarati.ttf";

    private Font gujFontNormal;
    private Font gujFontBold;
    private Font gujFontTable;

    private void initializeFonts() throws Exception {
        if (gujFontNormal == null) {
            URL fontUrl = getClass().getResource(GUJARATI_FONT_PATH);
            if (fontUrl == null) throw new RuntimeException("Font not found: " + GUJARATI_FONT_PATH);
            
            // Identity-H is CRITICAL for Gujarati
            BaseFont bf = BaseFont.createFont(fontUrl.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            gujFontNormal = new Font(bf, 11, Font.NORMAL);
            gujFontBold = new Font(bf, 12, Font.BOLD);
            gujFontTable = new Font(bf, 9, Font.NORMAL);
        }
    }

    // Style Constants for English
    private final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private final Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private final Font amountFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLDITALIC);
    private final Font tableHeadFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

    public void generateMonthlyStatementReport(MonthlyStatementDTO data, String monthName, String filePath) throws Exception {
        initializeFonts();
        Document document = new Document(PageSize.A4, 30, 30, 40, 60);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        
        document.open();
        generateSummaryPage(document, data, monthName);
        document.newPage();
        generateAppendixPage(document, data);
        document.close();
    }

    private void generateSummaryPage(Document doc, MonthlyStatementDTO data, String month) throws Exception {
        // Combined English/Gujarati Header
        Paragraph title = new Paragraph("MONTHLY CASH STATEMENT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph(month, FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        doc.add(sub);

        // Opening Balance
        Paragraph op = new Paragraph();
        op.add(new Chunk("Opening Balance: ", labelFont));
        op.add(new Chunk("₹ " + String.format("%.2f", data.getOpeningBal()) + "/-", FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLDITALIC)));
        doc.add(op);
        doc.add(new Paragraph(" "));

        PdfPTable ledger = new PdfPTable(2);
        ledger.setWidthPercentage(100);

        // Left Side: Inflow
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.RIGHT);
        left.setPaddingRight(10);
        addSummaryRow(left, "New Member Fees", data.getNewMemberFee());
        addSummaryRow(left, "Monthly Fees", data.getMonthlyFee());
        addSummaryRow(left, "Loan Deduction Fees", data.getLoanDeduction());
        addSummaryRowWithBreakdown(left, "Total EMI Collection", data.getTotalEmi(), data.getEmiBreakdown());
        addSummaryRow(left, "Full Payments", data.getFullPaymentAmount());
        addSummaryRow(left, "Misc Credits", data.getExpenseCredit());
        ledger.addCell(left);

        // Right Side: Outflow
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setPaddingLeft(10);
        addSummaryRow(right, "Total Loans Granted", data.getTotalLoanGranted());
        addSummaryRow(right, "Total Fee Refunds", data.getTotalFeeRefund());
        addSummaryRow(right, "Misc Debits", data.getExpenseDebit());
        ledger.addCell(right);

        doc.add(ledger);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.addCell(createTotalCell("TOTAL INFLOW: ₹ " + String.format("%.2f", data.getTotalIncome()) + "/-"));
        totals.addCell(createTotalCell("TOTAL OUTFLOW: ₹ " + String.format("%.2f", data.getTotalOutgoing()) + "/-"));
        doc.add(totals);

        Double closing = data.getClosingBalance();
        Paragraph cl = new Paragraph("\nClosing Balance: ₹ " + String.format("%.2f", closing) + "/-", 
                     FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLDITALIC, closing < 0 ? Color.RED : new Color(0, 100, 0)));
        cl.setAlignment(Element.ALIGN_RIGHT);
        doc.add(cl);
    }

    private void generateAppendixPage(Document doc, MonthlyStatementDTO data) throws Exception {
        PdfPTable container = new PdfPTable(2);
        container.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPaddingRight(10);
        
        // Headers with Gujarati support where needed
        addListSection(left, "NEW MEMBERS JOINED", List.of("M.No", "Name (નામ)"), new float[]{12f, 18f, 70f}, data.getNewMembersData());
        addListSection(left, "FULL PAYMENTS LIST", List.of("M.No", "Name", "Amt"), new float[]{12f, 18f, 50f, 20f}, data.getFullpaymentsData());
        addListSection(left, "LAST MONTH NEW LOANS EMI", List.of("M.No", "Name", "Amt"), new float[]{12f, 18f, 50f, 20f}, data.getPrevMonthNewLoansEMIAmtData());
        
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setPaddingLeft(10);
        addListSection(right, "NEW LOANS GRANTED", List.of("M.No", "Name"), new float[]{12f, 18f, 70f}, data.getNewLoanListMembersData());
        addListSection(right, "FEE REFUNDS", List.of("M.No", "Name", "Amt"), new float[]{12f, 18f, 50f, 20f}, data.getRefundMembersData());
        addListSection(right, "CANCELLED MEMBERS", List.of("M.No", "Name"), new float[]{12f, 18f, 70f}, data.getCancelledMembersData());
        
        container.addCell(left);
        container.addCell(right);
        doc.add(container);
    }

    private void addListSection(PdfPCell column, String title, List<String> headers, float[] widths, List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return;

        PdfPTable table = new PdfPTable(headers.size() + 1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15f);
        table.setHeaderRows(2);

        // Section Title
        PdfPCell titleCell = new PdfPCell(new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        titleCell.setColspan(headers.size() + 1);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingBottom(5f);
        table.addCell(titleCell);

        try {
            if (widths != null) table.setWidths(widths);
        } catch (DocumentException e) { e.printStackTrace(); }

        // Column Headers
        table.addCell(createHeaderCell("Sr."));
        for (String h : headers) table.addCell(createHeaderCell(h));

        // Data Rows
        int sr = 1;
        for (List<String> row : rows) {
            table.addCell(new Phrase(String.valueOf(sr++), FontFactory.getFont(FontFactory.HELVETICA, 8)));
            for (String val : row) {
                // If the value contains Gujarati characters, we must use the Gujarati font
                PdfPCell c = new PdfPCell(new Phrase(val, gujFontTable));
                c.setPaddingBottom(5f); // Important: Gujarati matras need a bit more bottom space
                
                if (val != null && val.matches("-?\\d+(\\.\\d+)?")) {
                    c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                table.addCell(c);
            }
        }
        column.addElement(table);
    }

    private void addSummaryRow(PdfPCell cell, String label, Double amt) {
        PdfPTable t = new PdfPTable(new float[]{65f, 35f});
        t.setWidthPercentage(100);
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBorder(Rectangle.NO_BORDER);
        PdfPCell r = new PdfPCell(new Phrase("₹ " + String.format("%.2f", amt), amountFont));
        r.setBorder(Rectangle.NO_BORDER);
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l); t.addCell(r);
        cell.addElement(t);
    }

    private void addSummaryRowWithBreakdown(PdfPCell cell, String label, Double total, Map<Double, Long> breakdown) {
        addSummaryRow(cell, label, total);
        if (breakdown != null) {
            for (Map.Entry<Double, Long> entry : breakdown.entrySet()) {
                PdfPTable t = new PdfPTable(new float[]{70f, 30f});
                t.setWidthPercentage(100);
                String text = "     " + entry.getKey().intValue() + " x " + entry.getValue();
                String val = String.valueOf((entry.getKey() * entry.getValue()));
                t.addCell(new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLDITALIC, Color.black))) {{ setBorder(Rectangle.NO_BORDER); }});
                t.addCell(new PdfPCell(new Phrase(val, FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLDITALIC, Color.black))) {{ setBorder(Rectangle.NO_BORDER); setHorizontalAlignment(Element.ALIGN_RIGHT); }});
                cell.addElement(t);
            }
        }
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, tableHeadFont));
        c.setBackgroundColor(new Color(44, 62, 80));
        c.setPadding(4f);
        return c;
    }

    private PdfPCell createTotalCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.ITALIC)));
        c.setBorder(Rectangle.TOP);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPaddingTop(5);
        return c;
    }

    private static class FooterEvent extends PdfPageEventHelper {
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            footer.setTotalWidth(527);
            footer.setLockedWidth(true);
            String ts = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            footer.addCell(new PdfPCell(new Phrase(ts, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC))) {{ setBorder(Rectangle.TOP); }});
            footer.addCell(new PdfPCell(new Phrase("Page " + writer.getPageNumber(), FontFactory.getFont(FontFactory.HELVETICA, 8))) {{ setHorizontalAlignment(Element.ALIGN_RIGHT); setBorder(Rectangle.TOP); }});
            footer.writeSelectedRows(0, -1, 36, 30, writer.getDirectContent());
        }
    }
}