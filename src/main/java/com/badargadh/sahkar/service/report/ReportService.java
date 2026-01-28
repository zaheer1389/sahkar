package com.badargadh.sahkar.service.report;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
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
import com.itextpdf.layout.ColumnDocumentRenderer;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

@Service
public class ReportService {

    private static final String GUJARATI_FONT_PATH = "/static/fonts/NotoSansGujarati.ttf";

    // Standard Colors
    private static final DeviceRgb DARK_BLUE = new DeviceRgb(44, 62, 80);
    private static final DeviceRgb SUCCESS_GREEN = new DeviceRgb(39, 174, 96);
    private static final DeviceRgb ACCENT_ORANGE = new DeviceRgb(230, 126, 34);

    // Initialized in a central method to avoid repeating code
    private PdfFont gujFont;
    private PdfFont engFontBold;
    private PdfFont engFontNormal;
    
    private static final Map<String, DeviceRgb> SURNAME_COLORS = new HashMap<>();

    static {
        SURNAME_COLORS.put("KHORAJIYA", new DeviceRgb(255, 200, 200)); // Light Red
        SURNAME_COLORS.put("NONSOLA", new DeviceRgb(200, 255, 200));   // Light Green
        SURNAME_COLORS.put("VARALIYA", new DeviceRgb(200, 200, 255));  // Light Blue
        SURNAME_COLORS.put("KADIVAL", new DeviceRgb(255, 255, 200));   // Light Yellow
        SURNAME_COLORS.put("CHAUDHARI", new DeviceRgb(255, 200, 255)); // Light Pink
        SURNAME_COLORS.put("MALPARA", new DeviceRgb(200, 255, 255));   // Cyan
        SURNAME_COLORS.put("NODOLIYA", new DeviceRgb(255, 220, 150));  // Peach
        SURNAME_COLORS.put("BHORANIYA", new DeviceRgb(220, 220, 220)); // Light Grey
        SURNAME_COLORS.put("AGLODIYA", new DeviceRgb(180, 255, 180));  // Mint
        SURNAME_COLORS.put("MANASIYA", new DeviceRgb(255, 235, 205));  // Bisque
        SURNAME_COLORS.put("MAREDIYA", new DeviceRgb(230, 230, 250));  // Lavender
        SURNAME_COLORS.put("SHERU", new DeviceRgb(240, 255, 240));     // Honeydew
        SURNAME_COLORS.put("SUNASARA", new DeviceRgb(255, 250, 205));  // Lemon Chiffon
    }

    private void initFonts() throws Exception {
        if (gujFont == null) {
            URL fontUrl = getClass().getResource(GUJARATI_FONT_PATH);
            gujFont = PdfFontFactory.createFont(fontUrl.toString(), PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            engFontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            engFontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }
    }

    public void generateMemberSummaryPdf(List<MemberSummaryDTO> data, String filePath) throws Exception {
        initFonts();
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        
        // Add Footer
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());

        // 1. Start with a standard document
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 30, 60, 30);

        // --- STEP 1: ADD LEGEND TO PAGE 1 ---
        document.add(new Paragraph("SURNAME COLOR LEGEND")
                .setFont(engFontBold).setFontSize(14).setTextAlignment(TextAlignment.CENTER));

        // 3-Column legend to save space
        Table legendTable = new Table(UnitValue.createPercentArray(new float[]{20, 13, 20, 13, 20, 13}))
                .useAllAvailableWidth().setMarginTop(10).setMarginBottom(20);
        
        SURNAME_COLORS.forEach((surname, color) -> {
            legendTable.addCell(new Cell().add(new Paragraph(surname).setFontSize(8)).setBorder(Border.NO_BORDER));
            legendTable.addCell(new Cell().setBackgroundColor(color).setHeight(10).setBorder(new SolidBorder(0.5f)));
        });
        document.add(legendTable);

        // --- STEP 2: FORCE PAGE BREAK AND SWITCH TO COLUMNS ---
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Define column areas for Page 2 onwards
        float gutter = 20f;
        float columnWidth = (PageSize.A4.getWidth() - 60 - gutter) / 2;
        float columnHeight = PageSize.A4.getHeight() - 110; // Adjust for margins

        Rectangle[] columns = new Rectangle[] {
            new Rectangle(30, 60, columnWidth, columnHeight),         // Left
            new Rectangle(30 + columnWidth + gutter, 60, columnWidth, columnHeight) // Right
        };
        
        // Apply the multi-column renderer
        document.setRenderer(new ColumnDocumentRenderer(document, columns));

        // --- STEP 3: CREATE THE MEMBER TABLE ---
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.5f, 7.5f}))
                .useAllAvailableWidth();
        
        // Ensure borders show even when table splits
        table.setKeepTogether(false); 

        // Headers
        table.addHeaderCell(new Cell().add(new Paragraph("M.No").setFont(engFontBold).setFontSize(8))
                .setBackgroundColor(new DeviceRgb(44, 62, 80)).setFontColor(ColorConstants.WHITE));
        table.addHeaderCell(new Cell().add(new Paragraph("Member Name / Branch").setFont(engFontBold).setFontSize(8))
                .setBackgroundColor(new DeviceRgb(44, 62, 80)).setFontColor(ColorConstants.WHITE));

        for (MemberSummaryDTO m : data) {
            DeviceRgb rowColor = getSurnameColor(m.getLastName());

            // M.No Cell with explicit border
            table.addCell(new Cell().add(new Paragraph(String.valueOf(m.getMemberNo())).setFontSize(8))
                    .setBackgroundColor(rowColor)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setTextAlignment(TextAlignment.CENTER));

            // Name + Branch Cell with explicit border
            Paragraph namePara = new Paragraph(m.getFullGujName()).setFont(gujFont).setFontSize(9).setBold().setFixedLeading(10f);
            
            table.addCell(new Cell().add(namePara)
                    .setBackgroundColor(rowColor)
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f))
                    .setPaddingLeft(5).setPaddingBottom(2));
        }

        document.add(table);
        document.close();
    }

    private Cell createHeader(String text) {
        return new Cell().add(new Paragraph(text).setFont(engFontBold).setFontSize(8))
                .setBackgroundColor(new DeviceRgb(44, 62, 80)).setFontColor(ColorConstants.WHITE);
    }
    
    private DeviceRgb getSurnameColor(String lastName) {
        if (lastName == null || lastName.isEmpty()) {
            return (DeviceRgb) ColorConstants.WHITE;
        }

        String upperLastName = lastName.toUpperCase().trim();

        // Iterate through the map keys to find a match
        return SURNAME_COLORS.entrySet().stream()
                .filter(entry -> upperLastName.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse((DeviceRgb) ColorConstants.WHITE);
    }

    public void generatePendingCollectionPdf(List<PendingMonthlyCollectionDTO> data, AppConfig config, String filePath) throws Exception {
        initFonts();
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(30, 20, 30, 20);

        document.add(new Paragraph("SAHKAR SOCIETY - PENDING PAYMENT LIST")
                .setFont(engFontBold).setFontSize(16).setFontColor(DARK_BLUE).setTextAlignment(TextAlignment.CENTER));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
        document.add(new Paragraph("Date: " + dtf.format(LocalDate.now()) + " | Total Pending Members: " + data.size())
                .setFont(engFontNormal).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.8f, 3.5f, 0.7f, 0.7f, 4.3f})).useAllAvailableWidth();

        String[] headers = {"M.No", "Member Name", "Fees", "EMI", "Witness Name"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(engFontBold).setFontColor(ColorConstants.WHITE).setFontSize(10))
                    .setBackgroundColor(DARK_BLUE).setTextAlignment(TextAlignment.CENTER).setPadding(6));
        }

        for (PendingMonthlyCollectionDTO member : data) {
            table.addCell(createCell(String.valueOf(member.getMemberNo()), engFontNormal, TextAlignment.CENTER));
            
            String fullName = (member.getFirstName() + " " + member.getMiddleName() + " " + member.getLastName()).toUpperCase();
            table.addCell(new Cell().add(new Paragraph(fullName).setFont(engFontNormal).setFontSize(10)).setPaddingLeft(5));
            
            table.addCell(new Cell().add(new Paragraph(String.valueOf(config.getMonthlyFees())).setFont(engFontBold).setFontColor(SUCCESS_GREEN))
                    .setTextAlignment(TextAlignment.RIGHT));

            Double emi = member.getEmiAmountDue() != null ? member.getEmiAmountDue() : 0;
            table.addCell(new Cell().add(new Paragraph(String.valueOf(emi.intValue())).setFont(engFontBold).setFontColor(ACCENT_ORANGE))
                    .setTextAlignment(TextAlignment.RIGHT));

            table.addCell(new Cell().setHeight(20f));
        }

        document.add(table);
        document.add(new Paragraph("\n\n\n__________________________\nAuthorized Signatory (Chairman)")
                .setFont(engFontNormal).setTextAlignment(TextAlignment.RIGHT));
        document.close();
    }

    public void generateSelectedLoansPdf(List<Member> data, FinancialMonth month, String filePath) throws Exception {
        initFonts();
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());
        Document document = new Document(pdf, PageSize.A4);

        DateTimeFormatter dtfGuj = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("gu", "IN"));

        document.add(new Paragraph("તારીખ : " + dtfGuj.format(LocalDate.now()))
                .setFont(gujFont).setTextAlignment(TextAlignment.RIGHT));

        document.add(new Paragraph("સહકાર સોસાયટી બાદરગઢ")
                .setFont(gujFont).setFontSize(16).setFontColor(DARK_BLUE).setBold().setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("લોન પાસ થયેલ સભ્યો ની યાદી : " + dtfGuj.format(month.getStartDate()))
                .setFont(gujFont).setFontSize(12).setBold().setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

        Table table = new Table(UnitValue.createPercentArray(new float[]{2.0f, 8.0f})).useAllAvailableWidth();

        table.addHeaderCell(new Cell().add(new Paragraph("મેમ્બર નંબર").setFont(gujFont).setBold())
                .setBackgroundColor(new DeviceRgb(230, 230, 230)).setTextAlignment(TextAlignment.CENTER).setPadding(8));
        table.addHeaderCell(new Cell().add(new Paragraph("સભ્ય નું નામ").setFont(gujFont).setBold())
                .setBackgroundColor(new DeviceRgb(230, 230, 230)).setTextAlignment(TextAlignment.CENTER).setPadding(8));

        for (Member m : data) {
            table.addCell(createCell(String.valueOf(m.getMemberNo()), gujFont, TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph(m.getGujFullname()).setFont(gujFont).setFontSize(11))
                    .setPaddingLeft(10).setPaddingBottom(8).setVerticalAlignment(VerticalAlignment.MIDDLE));
        }

        document.add(table);
        document.add(new Paragraph("\nકુલ પસંદગી: " + data.size() + " સભ્યો").setFont(gujFont).setBold());
        document.add(new Paragraph("\n\n\n__________________________\nઅધિકૃત હસ્તાક્ષરકર્તા (ચેરમેન)")
                .setFont(gujFont).setTextAlignment(TextAlignment.RIGHT));
        document.close();
    }

    public boolean generateGenericPdf(String title, String subTitle, List<String> headers, float[] columnWidths, List<List<String>> dataRows, String filePath) throws Exception {
        initFonts();
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());
        
        PageSize size = (headers.size() > 6) ? PageSize.A4.rotate() : PageSize.A4;
        Document document = new Document(pdf, size);

        document.add(new Paragraph(title.toUpperCase()).setFont(engFontBold).setFontSize(18).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(subTitle).setFont(engFontNormal).setFontSize(12).setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));

        Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(engFontBold).setFontSize(10).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(DARK_BLUE).setTextAlignment(TextAlignment.CENTER).setPadding(8));
        }

        for (List<String> row : dataRows) {
            for (String cellData : row) {
                Cell cell = new Cell().add(new Paragraph(cellData != null ? cellData : "-").setFont(engFontNormal).setFontSize(9));
                if (isNumeric(cellData)) cell.setTextAlignment(TextAlignment.RIGHT);
                table.addCell(cell.setPadding(5));
            }
        }

        document.add(table);
        document.add(new Paragraph("\nGenerated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))).setFontSize(8));
        document.close();
        return true;
    }

    private Table createSubTable(List<MemberSummaryDTO> subData) {
        // Widths: M.No (25%), Name (75%)
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.5f, 7.5f}))
                .useAllAvailableWidth();

        // Headers
        table.addHeaderCell(new Cell().add(new Paragraph("M.No").setFont(engFontBold).setFontSize(9).setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240)).setTextAlignment(TextAlignment.CENTER).setPadding(2));
        table.addHeaderCell(new Cell().add(new Paragraph("Member Full Name").setFont(engFontBold).setFontSize(9).setBold())
                .setBackgroundColor(new DeviceRgb(240, 240, 240)).setTextAlignment(TextAlignment.CENTER).setPadding(2));

        for (MemberSummaryDTO m : subData) {
            // M.No - Using your compact style
            table.addCell(new Cell().add(new Paragraph(String.valueOf(m.getMemberNo())).setFont(engFontNormal).setFontSize(9).setBold())
                    .setTextAlignment(TextAlignment.CENTER).setPadding(2));
            
            // Full Name - Using Gujarati font
            table.addCell(new Cell().add(new Paragraph(m.getGujaratiName()).setFont(gujFont).setFontSize(9).setBold())
                    .setPaddingLeft(5).setPaddingTop(1).setPaddingBottom(2)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE));
        }
        return table;
    }
    
    // Helper Methods
    private Cell createCell(String text, PdfFont font, TextAlignment align) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(align).setVerticalAlignment(VerticalAlignment.MIDDLE).setPadding(4);
    }

    private String formatCurrency(Integer amount) {
        return (amount == null || amount == 0) ? "-" : String.valueOf(amount);
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }
    
    private Cell createCompactCell(String text, PdfFont font, TextAlignment align) {
        return new Cell().add(new Paragraph(text != null ? text : "-")
                .setFont(font)
                .setFontSize(8)          // Smaller font
                .setMultipliedLeading(1.0f)) // Tightens line spacing
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(2);          // Very tight padding
    }
    
    private static class FooterHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdf = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
            Canvas canvas = new Canvas(pdfCanvas, pageSize);

            // CREATE A COMPACT FOOTER TABLE
            Table footer = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                    .useAllAvailableWidth();

            // Position it INSIDE the 50pt margin we created.
            // x: 20 (left margin), y: 20 (distance from very bottom), width: page width - margins
            footer.setFixedPosition(20, 20, pageSize.getWidth() - 40);

            String ts = "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            
            footer.addCell(new Cell().add(new Paragraph(ts).setFontSize(8).setItalic())
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
            
            footer.addCell(new Cell().add(new Paragraph("Page " + pdf.getPageNumber(page)).setFontSize(8))
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                    .setTextAlignment(TextAlignment.RIGHT));

            canvas.add(footer);
            canvas.close();
        }
    }
}