package com.badargadh.sahkar.service.report;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class FooterEvent extends PdfPageEventHelper {
    private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfPTable footer = new PdfPTable(2);
        try {
            footer.setWidths(new int[]{70, 30});
            footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            footer.setLockedWidth(true);
            
            // 1. Timestamp (Left)
            String ts = "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            PdfPCell cellTs = new PdfPCell(new Phrase(ts, footerFont));
            cellTs.setBorder(Rectangle.TOP); // Add a line above the footer
            cellTs.setBorderColor(Color.LIGHT_GRAY);
            cellTs.setFixedHeight(20);
            footer.addCell(cellTs);

            // 2. Page Number (Right)
            String pg = String.format("Page %d", writer.getPageNumber());
            PdfPCell cellPg = new PdfPCell(new Phrase(pg, footerFont));
            cellPg.setBorder(Rectangle.TOP);
            cellPg.setBorderColor(Color.LIGHT_GRAY);
            cellPg.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(cellPg);

            // Draw the table at the bottom of the page
            footer.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin(), writer.getDirectContent());
        } catch (DocumentException de) {
            throw new ExceptionConverter(de);
        }
    }
}