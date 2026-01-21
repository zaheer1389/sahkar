package com.badargadh.sahkar.service.report;

import java.awt.Color;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppConfig;
import com.badargadh.sahkar.data.FinancialMonth;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.dto.MemberSummaryDTO;
import com.badargadh.sahkar.dto.PendingMonthlyCollectionDTO;
import com.lowagie.text.Document;
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
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ReportService {

	private static final String GUJARATI_FONT_PATH = "/static/fonts/Shruti.ttf";
	private static final String GUJARATI_FONT_PATH_LOHIT = "/static/fonts/Lohit-Gujarati.ttf";

	
    // Define standard fonts to simulate weights
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_DATA = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK); // Clean 400-500 weight
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);

    public void generateMemberSummaryPdf(List<MemberSummaryDTO> data, String filePath) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        document.open();

        // 1. Title
        Paragraph title = new Paragraph("SAHKAR SOCIETY BADARGADH", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        //document.add(title);
        //document.add(new Paragraph(" "));

        // 2. Table Definition (Optimized Widths)
        float[] columnWidths = {1.0f, 5.5f, 1.8f, 1.8f, 1.5f, 2.5f}; 
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        // 3. Table Headers (Weight 700)
        String[] headers = {"M.No", "Member Full Name", "Fees", "Pending", "EMI", "Remark"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
            cell.setBackgroundColor(new Color(245, 245, 245));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // 4. Data Rows (Weight 400-500)
        for (MemberSummaryDTO m : data) {
            table.addCell(createCenterCell(String.valueOf(m.getMemberNo()), FONT_DATA));
            
            // Member Name (Left Aligned)
            PdfPCell nameCell = new PdfPCell(new Phrase(m.getFullName(), FONT_DATA));
            nameCell.setPaddingLeft(5);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(nameCell);
            
            table.addCell(createRightCell(formatCurrency(m.getTotalFees()), FONT_DATA));
            table.addCell(createRightCell(formatCurrency(m.getPendingLoan()), FONT_DATA));
            table.addCell(createRightCell(formatCurrency(m.getEmiAmount()), FONT_DATA));
            
            // Remark Cell
            PdfPCell remarkCell = new PdfPCell(new Phrase(""));
            remarkCell.setMinimumHeight(18f);
            table.addCell(remarkCell);
        }

        document.add(table);
        
        Paragraph meta = new Paragraph("Total Members : "+data.size());
        meta.setAlignment(Element.ALIGN_CENTER);
        document.add(meta);
        document.add(new Paragraph(" "));
        
        document.close();
    }
    
    public void generatePendingCollectionPdf(List<PendingMonthlyCollectionDTO> data, AppConfig config, String filePath) throws Exception {
        // Standard Date Formatters for English
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
        
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        document.open();

        // --- FONT CONFIGURATION (Standard English) ---
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(44, 62, 80));
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
        Font boldDataFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL);
        
        Color accentOrange = new Color(230, 126, 34);
        Color successGreen = new Color(39, 174, 96);

        // 1. Header (English Title)
        Paragraph title = new Paragraph("SAHKAR SOCIETY - PENDING PAYMENT LIST", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Meta Data
        Paragraph meta = new Paragraph("Date: " + formatter.format(LocalDate.now()) + " | Total Pending Members: " + data.size(), dataFont);
        meta.setAlignment(Element.ALIGN_CENTER);
        document.add(meta);
        document.add(new Paragraph(" "));

        // 2. Table Definition (5 Columns)
        // Widths: No(0.8), Name(3.5), Fees(0.7), EMI(0.7), Witness/Sign(4.3)
        float[] columnWidths = {0.8f, 3.5f, 0.7f, 0.7f, 4.3f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        // 3. Table Headers (English Labels)
        String[] headers = {"M.No", "Member Name", "Fees", "EMI", "Witness Name"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(new Color(44, 62, 80));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // 4. Data Rows
        for (PendingMonthlyCollectionDTO member : data) {
            // Member Number
            table.addCell(createStyledCell(String.valueOf(member.getMemberNo()), dataFont, Element.ALIGN_CENTER));
            
            // English Member Name (Using firstName/lastName instead of GujName)
            String fullName = member.getFirstName()+" "+ member.getMiddleName() + " " + member.getLastName();
            PdfPCell nameCell = new PdfPCell(new Phrase(fullName.toUpperCase(), dataFont));
            nameCell.setPaddingLeft(5);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(nameCell);
            
            // Fees
            Font feesFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, successGreen);
            table.addCell(createStyledCell(String.valueOf(config.getMonthlyFees()), feesFont, Element.ALIGN_RIGHT));
            
            // EMI
            Double emi = member.getEmiAmountDue() != null ? member.getEmiAmountDue() : 0;
            Font emiFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, accentOrange);
            table.addCell(createStyledCell(String.valueOf(emi.intValue()), emiFont, Element.ALIGN_RIGHT));
            
            // Blank Witness Column
            PdfPCell signCell = new PdfPCell(new Phrase(""));
            signCell.setMinimumHeight(20f); 
            table.addCell(signCell);
        }

        document.add(table);
        
        // 5. Note in English
        document.add(new Paragraph(" "));
        Paragraph note = new Paragraph("NOTE: If any cancellation or tampering is found on a member's name, 3 extra remarks will be added to that member. Please take note.", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.RED));
        note.setAlignment(Element.ALIGN_CENTER);
        //document.add(note);
        
        // 6. Signature
        Paragraph signature = new Paragraph("\n\n\n__________________________\nAuthorized Signatory (Chairman)", dataFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);
        
        document.close();
    }
    
    /*public void generatePendingCollectionPdf(List<MemberSummaryDTO> data, AppConfig config, String filePath) throws Exception {
        // Set margins
    	
    	Locale gujaratiLocale = new Locale("gu", "IN");
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", gujaratiLocale);
    	DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MMMM yyyy", gujaratiLocale);
    	
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        document.open();

        // --- FONT CONFIGURATION ---
        URL fontResource = getClass().getResource(GUJARATI_FONT_PATH_LOHIT);
        if (fontResource == null) {
            throw new java.io.IOException("Gujarati font not found!");
        }
        BaseFont gujBase = BaseFont.createFont(fontResource.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        
        // Define font styles
        Font gujTitleFont = new Font(gujBase, 16, Font.BOLD, new Color(44, 62, 80));
        Font gujHeadFont = new Font(gujBase, 10, Font.BOLD, Color.WHITE);
        Font gujDataFont = new Font(gujBase, 10, Font.NORMAL);
        Font gujBoldDataFont = new Font(gujBase, 10, Font.BOLD);
        
        // Standard English fonts for numbers/dates
        Font smallEngFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Color accentOrange = new Color(230, 126, 34);
        Color successGreen = new Color(39, 174, 96);

        // 1. Header (Gujarati Title)
        Paragraph title = new Paragraph("સહકાર સોસાયટી - બાકી વસુલાત રિપોર્ટ", gujTitleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        // Meta Data (English numbers, but labels can be Gujarati if preferred)
        Paragraph meta = new Paragraph("તારીખ: " + formatter.format(LocalDate.now()) + " | કુલ બાકી સભ્યો: " + data.size(), gujDataFont);
        meta.setAlignment(Element.ALIGN_CENTER);
        document.add(meta);
        document.add(new Paragraph(" "));

        // 2. Table Definition (5 Columns)
        // Widths: No(8%), Name(40%), Fees(10%), EMI(10%), Sign/Witness(32%)
        float[] columnWidths = {0.8f, 3.5f, 0.5f, 0.5f, 4.7f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        // 3. Table Headers (Gujarati Labels)
        String[] headers = {"નંબર", "સભ્યનું નામ", "ફી", "હપ્તો", "સાક્ષી"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, gujHeadFont));
            cell.setBackgroundColor(new Color(44, 62, 80));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            cell.setPaddingBottom(8); // Extra padding for Gujarati glyphs
            table.addCell(cell);
        }

        // 4. Data Rows
        for (MemberSummaryDTO member : data) {
            // Member Number
            table.addCell(createStyledCellGuj(String.valueOf(member.getMemberNo()), gujDataFont, Element.ALIGN_CENTER));
            
            // Gujarati Member Name
            PdfPCell nameCell = new PdfPCell(new Phrase(member.getFullGujName(), gujDataFont));
            nameCell.setPaddingLeft(5);
            nameCell.setPaddingBottom(7); // Prevent bottom cut-off
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(nameCell);
            
            // Fees (Using Success Green)
            Font feesFont = new Font(gujBase, 10, Font.BOLD, successGreen);
            table.addCell(createStyledCellGuj(String.valueOf(config.getMonthlyFees()), feesFont, Element.ALIGN_RIGHT));
            
            // EMI (Using Accent Orange)
            int emi = member.getEmiAmount() != null ? member.getEmiAmount() : 0;
            Font emiFont = new Font(gujBase, 10, Font.BOLD, accentOrange);
            table.addCell(createStyledCellGuj(String.valueOf(emi), emiFont, Element.ALIGN_RIGHT));
            
            // Blank Witness/Sign Column
            PdfPCell signCell = new PdfPCell(new Phrase(""));
            signCell.setMinimumHeight(22f); // Increased height for easier handwriting
            table.addCell(signCell);
        }

        document.add(table);
        
        Paragraph note = new Paragraph("નોંધ :- જો કોઈ પણ મેમ્બર ના નામ ઉપર કેન્સલ અથવા બીજા કોઈ પ્રકાર ની છેડછાડ જોવા મળી તો તે મેમ્બર ને ૩ એક્સટ્રા રિમાર્કસ લગાડવા માં આવશે તેની દરેક મેમ્બરે નોંધ લેવી.", gujDataFont);
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);
        
        Paragraph signature = new Paragraph("\n\n\n_____________\nઅધિકૃત હસ્તાક્ષરકર્તા (ચેરમેન)", gujDataFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);
        
        document.close();
    }*/

    
    public void generateSelectedLoansPdf(List<Member> data, FinancialMonth month, String filePath) throws Exception {
    	
    	Locale gujaratiLocale = new Locale("gu", "IN");
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", gujaratiLocale);
    	DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MMMM yyyy", gujaratiLocale);
    	
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        document.open();

        // 1. Load Font
        URL fontResource = getClass().getResource(GUJARATI_FONT_PATH_LOHIT);
        if (fontResource == null) {
            throw new java.io.IOException("Font file not found at: " + GUJARATI_FONT_PATH_LOHIT);
        }
        
        BaseFont gujaratiBase = BaseFont.createFont(fontResource.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font gujaratiFont = new Font(gujaratiBase, 11, Font.NORMAL);
        Font gujaratiFontBold = new Font(gujaratiBase, 12, Font.BOLD);
        Font titleFont = new Font(gujaratiBase, 16, Font.BOLD, new Color(44, 62, 80));

        // 2. Date Section - FIX: Added gujaratiFont
        Paragraph date = new Paragraph("તારીખ : " + formatter.format(LocalDate.now()), gujaratiFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);
        
        //document.add(new Paragraph(" ")); 

        // 3. Header Title - FIX: Added titleFont (Gujarati)
        Paragraph title = new Paragraph("સહકાર સોસાયટી બાદરગઢ", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        //document.add(new Paragraph(" ")); 

        // 4. Subtitle - FIX: Added gujaratiFontBold
        Paragraph subTitle = new Paragraph("લોન પાસ થયેલ સભ્યો ની યાદી : " + formatter2.format((month.getStartDate())) , gujaratiFontBold);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);
        
        document.add(new Paragraph(" ")); 

        // 5. Table setup
        float[] columnWidths = {2.0f, 8.0f}; 
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        // Headers
        String[] headers = {"મેમ્બર નંબર", "સભ્ય નું નામ"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, gujaratiFontBold));
            cell.setBackgroundColor(new Color(230, 230, 230));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Data Rows
        for (Member m : data) {
            // Center cell for number needs to support Gujarati font if symbols are used
            table.addCell(createCenterCell(String.valueOf(m.getMemberNo()), gujaratiFont));
            
            PdfPCell nameCell = new PdfPCell(new Phrase(m.getGujFullname(), gujaratiFont));
            nameCell.setPaddingLeft(10);
            nameCell.setPaddingTop(6);
            nameCell.setPaddingBottom(10); // Increased padding for better rendering of Gujarati matras
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(nameCell);
        }

        document.add(table);

        //document.add(new Paragraph(" "));
        
        // 6. Footer & Signature - FIX: Added gujaratiFont
        Paragraph footer = new Paragraph("કુલ પસંદગી: " + data.size() + " સભ્યો", gujaratiFontBold);
        document.add(footer);

        // Signature - FIX: Added gujaratiFont
        Paragraph signature = new Paragraph("\n\n\n__________________________\nઅધિકૃત હસ્તાક્ષરકર્તા (ચેરમેન)", gujaratiFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);

        document.close();
    }
    
    /*
    public void generateSelectedLoansPdf(List<Member> data, String monthInfo, String filePath) throws Exception {
        // A4 Portrait with 30pt margins
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        document.open();

        Paragraph date = new Paragraph("Date : " + DateTimeFormatter.ofPattern("dd-MMM-yyyy").format(LocalDate.now()), FONT_DATA);
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);
        
        document.add(new Paragraph(" ")); // Spacer
        
        // 1. Header Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(44, 62, 80));
        Paragraph title = new Paragraph("SAHKAR SOCIETY BADARGADH", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph(" ")); // Spacer

        // 2. Subtitle
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Paragraph subTitle = new Paragraph("OFFICIAL SELECTED MEMBER LIST - " + monthInfo.toUpperCase(), subTitleFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subTitle);
        
        document.add(new Paragraph(" ")); // Spacer

        // 3. Table Definition (2 Columns: Mem No and Name)
        // Widths: 20% for No, 80% for Name
        float[] columnWidths = {2.0f, 8.0f}; 
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);

        // 4. Headers
        String[] headers = {"Member No", "Member Full Name"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
            cell.setBackgroundColor(new Color(230, 230, 230));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // 5. Data Rows
        for (Member m : data) {
            // Member Number using your existing helper
            table.addCell(createCenterCell(String.valueOf(m.getMemberNo()), FONT_DATA));
            
            // Member Name (Left Aligned with padding)
            PdfPCell nameCell = new PdfPCell(new Phrase(m.getFullname().toUpperCase(), FONT_DATA));
            nameCell.setPaddingLeft(10);
            nameCell.setPaddingTop(6);
            nameCell.setPaddingBottom(6);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(nameCell);
        }

        document.add(table);

        // 6. Signatory Section
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph("Total Selection: " + data.size() + " Members", FONT_DATA);
        document.add(footer);

        Paragraph signature = new Paragraph("\n\n\n__________________________\nAuthorized Signatory", FONT_DATA);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);

        document.close();
    }
    */
   
    public boolean generateGenericPdf(String title, String subTitle, List<String> headers, float[] columnWidths,
			List<List<String>> dataRows, String filePath) throws Exception {

		// Auto-switch to Landscape if there are more than 6 columns
		Rectangle pageSize = (headers.size() > 6) ? PageSize.A4.rotate() : PageSize.A4;
		Document document = new Document(pageSize, 25, 25, 30, 30);

		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
        writer.setPageEvent(new FooterEvent());
        
		document.open();

		// 1. Header & Subtitle
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
		Paragraph pTitle = new Paragraph(title.toUpperCase(), titleFont);
		pTitle.setAlignment(Element.ALIGN_CENTER);
		document.add(pTitle);

		Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
		Paragraph pSub = new Paragraph(subTitle, subFont);
		pSub.setAlignment(Element.ALIGN_CENTER);
		document.add(pSub);
		document.add(new Paragraph(" ")); // Spacer

		// 2. Dynamic Table Setup
		PdfPTable table = new PdfPTable(headers.size());
		table.setWidthPercentage(100);

		// If columnWidths are provided, apply them
		if (columnWidths != null && columnWidths.length == headers.size()) {
			table.setWidths(columnWidths);
		}

		// 3. Header Styling
		Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
		for (String header : headers) {
			PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
			cell.setBackgroundColor(new Color(44, 62, 80)); // Dark Professional Blue
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setPadding(8);
			table.addCell(cell);
		}

		// 4. Data Rows
		Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
		for (List<String> row : dataRows) {
			for (String cellData : row) {
				PdfPCell cell = new PdfPCell(new Phrase(cellData != null ? cellData : "-", dataFont));
				cell.setPadding(5);
				// Auto-align: If it's a number, right align. Else left align.
				if (isNumeric(cellData)) {
					cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
				}
				table.addCell(cell);
			}
		}

		document.add(table);

		// Footer
		document.add(new Paragraph(
				"\nGenerated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));

		document.close();

		return true;
	}
    
    // Helper method updated to accept Base iText Font types
    private PdfPCell createStyledCellGuj(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setPaddingBottom(7); 
        return cell;
    }

    private PdfPCell createStyledCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private String formatCurrency(Integer amount) {
        return (amount == null || amount == 0) ? "-" : String.valueOf(amount);
    }

    private PdfPCell createCenterCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createRightCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPaddingRight(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
    
	//Helper to detect numbers for alignment
	private boolean isNumeric(String str) {
		if (str == null) return false;
		return str.matches("-?\\d+(\\.\\d+)?");
	}
	
}