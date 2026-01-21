package com.badargadh.sahkar.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.EmiPayment;
import com.badargadh.sahkar.data.EmiPaymentGroup;
import com.badargadh.sahkar.data.FeePayment;
import com.badargadh.sahkar.data.LoanAccount;
import com.badargadh.sahkar.data.LoanApplication;
import com.badargadh.sahkar.data.Member;
import com.badargadh.sahkar.data.MemberFeesRefundDTO;
import com.badargadh.sahkar.data.MonthlyPayment;
import com.badargadh.sahkar.enums.CollectionType;
import com.badargadh.sahkar.util.AppLogger;

@Service
public class ReceiptPrintingService {

    private static final int LINE_WIDTH = 48;
    // Java 8 compatible repeat logic for the separator line
    private static final String SEP_LINE = new String(new char[LINE_WIDTH]).replace('\0', '-') + "\n";

    // ESC/POS Commands
    private static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1b, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1b, 0x61, 0x00};
    private static final byte[] ESC_BOLD_ON = new byte[]{0x1b, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = new byte[]{0x1b, 0x45, 0x00};
    private static final byte[] ESC_DOUBLE_SIZE_ON = new byte[]{0x1d, 0x21, 0x11};
    private static final byte[] ESC_DOUBLE_SIZE_OFF = new byte[]{0x1d, 0x21, 0x00};
    private static final byte[] ESC_CUT_PAPER = new byte[]{0x1d, 0x56, 0x41, 0x00};
    
    /**
     * Helper method to find the POS printer among all connected devices
     */
    private PrintService findPosPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            String printerName = service.getName().toUpperCase();
            
            // Add your specific POS printer's name keywords here
            if (printerName.contains("POS") || 
                printerName.contains("THERMAL") || 
                printerName.contains("XP-") || 
                printerName.contains("80MM") || 
                printerName.contains("TM-T82")|| 
                printerName.contains("TC4")) {
                return service;
            }
        }
        return null; 
    }

    /*public void printCollectionReceipt(EmiPaymentGroup group, boolean isDuplicate) {
        try {
            List<MonthlyPayment> payments = group.getMonthlyPayments();
            Member depositor = group.getDepositedBy();
            
            PrintService service = PrintServiceLookup.lookupDefaultPrintService();
            if (service == null) return;

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            StringBuilder sb = new StringBuilder();

            // 1. Duplicate Header
            if (isDuplicate) {
                sb.append(new String(ESC_ALIGN_CENTER));
                sb.append(new String(ESC_BOLD_ON));
                sb.append("*** DUPLICATE COPY ***\n");
                sb.append(new String(ESC_BOLD_OFF));
                sb.append("\n"); 
            }

            // 2. Society Header
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append(new String(ESC_BOLD_ON));
            sb.append("SAHKAR SOCIETY MANAGEMENT\n");
            sb.append(new String(ESC_BOLD_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE);
            sb.append("RECEIPT NO: ").append(group.getId()).append("\n");
            sb.append("Date      : ").append(dtf.format(LocalDateTime.now())).append("\n");
            sb.append("Deposited By : ").append(group.getDepositedByName()).append("\n");
            sb.append(SEP_LINE);

            sb.append(String.format("%-5s %-25s %12s\n", "No.", "Member Name", "Total"));
            sb.append(SEP_LINE);

            double grandTotal = 0;
            for (MonthlyPayment p : payments) {
                double total = p.getMonthlyFees() + p.getEmiAmount() + p.getFullAmount();
                grandTotal += total;

                String name = p.getMember().getFullname();
                if (name.length() > 25) name = name.substring(0, 22) + "...";

                // Main Row
                sb.append(String.format("%-5s %-25s %12.2f\n", p.getMember().getMemberNo(), name, total));
                
                // Sub-row Logic
                String balText = (p.getBalanceAmount() == null || p.getBalanceAmount() <= 0) 
                                 ? "No Loan" 
                                 : "Bal:" + p.getBalanceAmount().intValue();

                // REDUCED WIDTHS: 
                // Total width: 4 (indent) + 8 + 8 + 8 + 10 = 38 characters (Fits safely on 80mm)
                sb.append(String.format("    %-10s %-10s %12s\n", 
                    "FEE:" + p.getMonthlyFees().intValue(), 
                    "EMI:" + p.getEmiAmount().intValue(), 
                    balText));
            }

            // 3. Footer and Cut
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("TOTAL COLLECTED\n");
            sb.append(new String(ESC_DOUBLE_SIZE_ON));
            sb.append(String.format("INR %.2f\n", grandTotal));
            sb.append(new String(ESC_DOUBLE_SIZE_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("Thank You for Paying!\n\n\n");
            sb.append(new String(ESC_CUT_PAPER));

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(sb.toString().getBytes(), flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);

        } catch (Exception e) {
        	AppLogger.error("Thermal Printer Error", e);
        }
    }*/
    
    public void printCollectionReceipt(EmiPaymentGroup group, boolean isDuplicate) {
        try {
        	// 1. Find the specific POS printer
            PrintService service = findPosPrinter();
            
            if (service == null) {
            	System.err.println("POS Printer not found. Please check connections.");
                AppLogger.error("POS Printer not found. Please check connections.");
                // Optional: Fallback to default if POS is not found
                // service = PrintServiceLookup.lookupDefaultPrintService();
                return;
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            StringBuilder sb = new StringBuilder();

            // 1. Duplicate Header
            if (isDuplicate) {
                sb.append(new String(ESC_ALIGN_CENTER));
                sb.append(new String(ESC_BOLD_ON));
                sb.append("*** DUPLICATE COPY ***\n");
                sb.append(new String(ESC_BOLD_OFF));
                sb.append("\n"); 
            }

            // 2. Society Header
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append(new String(ESC_BOLD_ON));
            sb.append("SAHKAR SOCIETY MANAGEMENT\n");
            sb.append(new String(ESC_BOLD_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE);
            sb.append("RECEIPT NO: ").append(group.getId()).append("\n");
            sb.append("Date      : ").append(dtf.format(group.getTransactionDateTime())).append("\n");
            sb.append("Depositor : ").append(group.getDepositorName()).append("\n");
            //sb.append("Location  : ").append(group.getCollectionLocation()).append("\n");
            sb.append(SEP_LINE);

            sb.append(String.format("%-5s %-25s %12s\n", "No.", "Member Name", "Total"));
            sb.append(SEP_LINE);

            double grandTotal = 0;
            for (MonthlyPayment s : group.getMonthlyPayments()) {
                // Calculate total as double for currency formatting
                double rowTotal = (double) s.getMonthlyFees() + s.getEmiAmount() + s.getFullAmount();
                grandTotal += rowTotal;

                String name = s.getMember().getFullname();
                if (name.length() > 25) name = name.substring(0, 22) + "...";

                // 1. Member Header Row - Keep .2f for the grand total for professional currency look
                sb.append(String.format("%-5d %-25s %12.2f\n", 
                    s.getMember().getMemberNo(), 
                    name, 
                    rowTotal));
                
                // 2. Sub-row Breakdown - Use %d for Integers
                String balText = (s.getBalanceAmount() <= 0) 
                    ? "No Loan" 
                    : "Bal:" + s.getBalanceAmount(); // Simple string concat for Integer

                // Logic: Use %-7d for left-aligned integers
                sb.append(String.format("    FEE:%-7d EMI:%-7d %12s\n", 
                    s.getMonthlyFees(), 
                    (s.getEmiAmount() + s.getFullAmount()), 
                    balText));
            }

            // 3. Footer and Cut
            sb.append(SEP_LINE);
            sb.append(String.format("%-20s %18d\n", "Total Payments :", group.getPaymentCount()));
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("TOTAL COLLECTED\n");
            sb.append(new String(ESC_DOUBLE_SIZE_ON));
            // Using the stored total from the group if available, else the calculated one
            sb.append(String.format("INR %.2f\n", group.getTotalAmount() > 0 ? group.getTotalAmount() : grandTotal));
            sb.append(new String(ESC_DOUBLE_SIZE_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("Thank You for Paying!\n\n\n");
            sb.append(new String(ESC_CUT_PAPER));

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(sb.toString().getBytes(), flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);

        } catch (Exception e) {
        	e.printStackTrace();
            AppLogger.error("Thermal Printer Error", e);
        }
    }

    
    public void printLoanDisbursementReceipt(LoanApplication app, LoanAccount acc, boolean isDuplicate) {
        try {
        	// 1. Find the specific POS printer
            PrintService service = findPosPrinter();
            
            if (service == null) {
            	System.err.println("POS Printer not found. Please check connections.");
                AppLogger.error("POS Printer not found. Please check connections.");
                // Optional: Fallback to default if POS is not found
                // service = PrintServiceLookup.lookupDefaultPrintService();
                return;
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            StringBuilder sb = new StringBuilder();

            // 1. Duplicate Header
            if (isDuplicate) {
                sb.append(new String(ESC_ALIGN_CENTER));
                sb.append(new String(ESC_BOLD_ON));
                sb.append("*** DUPLICATE COPY ***\n");
                sb.append(new String(ESC_BOLD_OFF));
                sb.append("\n"); 
            }
            
            // 1. Society Header
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append(new String(ESC_BOLD_ON));
            sb.append("SAHKAR SOCIETY MANAGEMENT\n");
            sb.append("LOAN DISBURSEMENT VOUCHER\n");
            sb.append(new String(ESC_BOLD_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE); // Your defined separator line "---..."
            sb.append("Date       : ").append(dtf.format(LocalDateTime.now())).append("\n");
            sb.append("Member No  : ").append(app.getMember().getMemberNo()).append("\n");
            sb.append("Member Name: ").append(app.getMember().getFullname().toUpperCase()).append("\n");
            sb.append(SEP_LINE);

            // 2. Breakup Section
            sb.append(String.format("%-25s %12s\n", "Description", "Amount"));
            sb.append(SEP_LINE);

            double principal = app.getAppliedAmount();
            double prevPendingAmt = app.getPrevLoanFullAmountDeduction();
            double feesDeduction = app.getFeesDeduction();
            double netDisbursed = principal - prevPendingAmt - feesDeduction;

            sb.append(String.format("%-25s %12.2f\n", "Loan Amount", principal));
            sb.append(String.format("%-25s %12.2f\n", "(-) Previous Balance", prevPendingAmt));
            sb.append(String.format("%-25s %12.2f\n", "(-) Fees Deduction", feesDeduction));

            // 3. Grand Total Section
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("NET PAYABLE AMOUNT\n");
            sb.append(new String(ESC_DOUBLE_SIZE_ON));
            sb.append(String.format("INR %.2f\n", netDisbursed));
            sb.append(new String(ESC_DOUBLE_SIZE_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            sb.append(SEP_LINE);

            // 4. Payout Information
            String receivedBy = (app.getCollectionType() == CollectionType.SELF) ? "SELF" : "AUTHORITY";
            sb.append("Received By : ").append(receivedBy).append("\n");
            
            if (app.getCollectionType() == CollectionType.AUTHORITY) {
                String remark = app.getCollectionRemarks() != null ? app.getCollectionRemarks() : "N/A";
                sb.append("Auth Remark : ").append(remark).append("\n");
            }

            // 5. Signature and Paper Cut
            sb.append("\n\n\n");
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("Member Sign          Cashier Sign\n\n");
            sb.append("Thank You!\n\n\n");
            sb.append(new String(ESC_CUT_PAPER));

            // 6. Send to Printer
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(sb.toString().getBytes(), flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);

        } catch (Exception e) {
        	AppLogger.error("Thermal Printer Error", e);
        }
    }
    
    public void printMemberJoiningReceipt(Member member, double joiningFees, boolean isDuplicate) {
        try {
        	// 1. Find the specific POS printer
            PrintService service = findPosPrinter();
            
            if (service == null) {
            	System.err.println("POS Printer not found. Please check connections.");
                AppLogger.error("POS Printer not found. Please check connections.");
                // Optional: Fallback to default if POS is not found
                // service = PrintServiceLookup.lookupDefaultPrintService();
                return;
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            StringBuilder sb = new StringBuilder();

            // 1. Duplicate Header
            if (isDuplicate) {
                sb.append(new String(ESC_ALIGN_CENTER));
                sb.append(new String(ESC_BOLD_ON));
                sb.append("*** DUPLICATE COPY ***\n");
                sb.append(new String(ESC_BOLD_OFF));
                sb.append("\n"); 
            }
            
            // 1. Society Header
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append(new String(ESC_BOLD_ON));
            sb.append("SAHKAR SOCIETY MANAGEMENT\n");
            sb.append("NEW MEMBER RECEIPT\n");
            sb.append(new String(ESC_BOLD_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE);
            sb.append("Date      : ").append(dtf.format(LocalDateTime.now())).append("\n");
            sb.append(SEP_LINE);

            // 2. Member Details
            ///sb.append(new String(ESC_BOLD_ON));
            sb.append("MEMBER NO : ").append(member.getMemberNo()).append("\n");
            //sb.append(new String(ESC_BOLD_OFF));
            
            String name = member.getFullname().toUpperCase();
            if (name.length() > 30) name = name.substring(0, 27) + "...";
            sb.append("NAME      : ").append(name).append("\n");
            
            //sb.append("JOIN DATE : ").append(member.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).append("\n");
            //sb.append(SEP_LINE);

            // 3. Payment Details
            sb.append(String.format("%-25s %12s\n", "Description", "Amount"));
            sb.append(SEP_LINE);
            
            // You can break this down if there are sub-fees (e.g., Admission Fee + Share Capital)
            sb.append(String.format("%-25s %12.2f\n", "Joining Fee", joiningFees));
            
            sb.append(SEP_LINE);

            // 4. Total and Footer
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("TOTAL PAID\n");
            sb.append(new String(ESC_DOUBLE_SIZE_ON));
            sb.append(String.format("INR %.2f\n", joiningFees));
            sb.append(new String(ESC_DOUBLE_SIZE_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE);
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("Welcome to our Society!\n");
            sb.append("Please keep this for your record.\n\n\n");
            
            sb.append("Manager Sign           Member Sign\n\n\n");
            sb.append(new String(ESC_CUT_PAPER));

            // 5. Send to Printer
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(sb.toString().getBytes(), flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);

        } catch (Exception e) {
        	AppLogger.error("Thermal Printer Error", e);
        }
    }
    
    public void printFeesRefundReceipt(MemberFeesRefundDTO member, double refundAmount, String reason, boolean isDuplicate) {
        try {
        	// 1. Find the specific POS printer
            PrintService service = findPosPrinter();
            
            if (service == null) {
            	System.err.println("POS Printer not found. Please check connections.");
                AppLogger.error("POS Printer not found. Please check connections.");
                // Optional: Fallback to default if POS is not found
                // service = PrintServiceLookup.lookupDefaultPrintService();
                return;
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            StringBuilder sb = new StringBuilder();

            // 1. Duplicate Header
            if (isDuplicate) {
                sb.append(new String(ESC_ALIGN_CENTER));
                sb.append(new String(ESC_BOLD_ON));
                sb.append("*** DUPLICATE COPY ***\n");
                sb.append(new String(ESC_BOLD_OFF));
                sb.append("\n"); 
            }
            
            // 1. Society Header
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append(new String(ESC_BOLD_ON));
            sb.append("SAHKAR SOCIETY MANAGEMENT\n");
            sb.append("FEES REFUND RECEIPT\n");
            sb.append(new String(ESC_BOLD_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            
            sb.append(SEP_LINE);
            sb.append("Date      : ").append(dtf.format(LocalDateTime.now())).append("\n");
            sb.append("Member No : ").append(member.getMemberNo()).append("\n");
            sb.append("Member    : ").append(member.getMember().getFullname().toUpperCase()).append("\n");
            sb.append(SEP_LINE);

            // 2. Refund Details
            sb.append(String.format("%-20s %17s\n", "Description", "Amount"));
            sb.append(SEP_LINE);
            
            // Truncate reason if too long
            String shortReason = reason.length() > 20 ? reason.substring(0, 17) + "..." : reason;
            sb.append(String.format("%-20s %17.2f\n", shortReason, refundAmount));
            
            sb.append(SEP_LINE);

            // 3. Grand Total (Refunded Amount)
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("TOTAL REFUNDED\n");
            sb.append(new String(ESC_DOUBLE_SIZE_ON));
            sb.append(String.format("INR %.2f\n", refundAmount));
            sb.append(new String(ESC_DOUBLE_SIZE_OFF));
            sb.append(new String(ESC_ALIGN_LEFT));
            sb.append(SEP_LINE);

            // 4. Verification Footer
            sb.append("\nI hereby acknowledge the receipt of\n");
            sb.append("the above refund amount.\n\n\n");
            
            sb.append("Member Sign           Cashier Sign\n\n\n");
            
            // 5. Printer Commands
            sb.append(new String(ESC_ALIGN_CENTER));
            sb.append("Processed Successfully\n\n");
            sb.append(new String(ESC_CUT_PAPER));

            // 6. Send to Printer
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(sb.toString().getBytes(), flavor, null);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);

        } catch (Exception e) {
        	AppLogger.error("Thermal Printer Error", e);
        }
    }
}