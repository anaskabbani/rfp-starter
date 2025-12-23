package com.acme.saas.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to generate test files for document extraction testing.
 * Run this main method once to create all test files.
 */
public class TestFileGenerator {

    private static final String TEST_FILES_DIR = "src/test/resources/test-files/";

    public static void main(String[] args) throws Exception {
        System.out.println("Generating test files...");

        Path testFilesPath = Paths.get(TEST_FILES_DIR);
        File testFilesDir = testFilesPath.toFile();
        if (!testFilesDir.exists()) {
            testFilesDir.mkdirs();
        }

        generateSamplePdf();
        generateSampleWithTablesDocx();
        generateSampleSimpleDocx();
        generateSampleXlsx();

        System.out.println("Test files generated successfully in: " + testFilesPath.toAbsolutePath());
    }

    /**
     * Generate sample.pdf - 2 pages with key-value pairs for testing.
     */
    private static void generateSamplePdf() throws Exception {
        String fileName = TEST_FILES_DIR + "sample.pdf";

        try (PDDocument document = new PDDocument()) {
            // Page 1 - RFP header with key-value pairs
            PDPage page1 = new PDPage(PDRectangle.LETTER);
            document.addPage(page1);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("RFP Document Test");
                contentStream.endText();

                // Add key-value pairs for extraction testing
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Carrier: Aetna");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Due Date: January 15, 2025");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Contact: John Smith");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Email: john.smith@example.com");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Phone: (555) 123-4567");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("RFP Number: RFP-2025-001");
                contentStream.newLineAtOffset(0, -40);

                // Add some regular content
                contentStream.showText("This is a Request for Proposal (RFP) for employee benefits.");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("We are seeking competitive quotes for medical, dental, and vision coverage.");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("The proposal should include plan details, pricing, and implementation timeline.");
                contentStream.endText();
            }

            // Page 2 - Additional details
            PDPage page2 = new PDPage(PDRectangle.LETTER);
            document.addPage(page2);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Coverage Requirements");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText("Employee Count: 250");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Coverage Start Date: April 1, 2025");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Current Carrier: Blue Cross");
                contentStream.newLineAtOffset(0, -40);
                contentStream.showText("Please provide detailed plan options including deductibles,");
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("copays, coinsurance, and out-of-pocket maximums.");
                contentStream.endText();
            }

            document.save(fileName);
        }

        System.out.println("Created: " + fileName);
    }

    /**
     * Generate sample-with-tables.docx - Document with text and a table.
     */
    private static void generateSampleWithTablesDocx() throws Exception {
        String fileName = TEST_FILES_DIR + "sample-with-tables.docx";

        try (XWPFDocument document = new XWPFDocument()) {
            // Add title
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText("Insurance Proposal Summary");

            // Add paragraph
            XWPFParagraph para1 = document.createParagraph();
            XWPFRun run1 = para1.createRun();
            run1.setText("This document outlines the proposed insurance plans for your organization. " +
                    "Below is a comparison of the available plan options.");

            // Create a 4x4 table
            XWPFTable table = document.createTable(4, 4);

            // Header row
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("Plan Type");
            headerRow.getCell(1).setText("Deductible");
            headerRow.getCell(2).setText("Monthly Premium");
            headerRow.getCell(3).setText("Coverage Level");

            // Data rows
            XWPFTableRow row1 = table.getRow(1);
            row1.getCell(0).setText("PPO Gold");
            row1.getCell(1).setText("$500");
            row1.getCell(2).setText("$450");
            row1.getCell(3).setText("80/20");

            XWPFTableRow row2 = table.getRow(2);
            row2.getCell(0).setText("PPO Silver");
            row2.getCell(1).setText("$1,000");
            row2.getCell(2).setText("$350");
            row2.getCell(3).setText("70/30");

            XWPFTableRow row3 = table.getRow(3);
            row3.getCell(0).setText("HMO");
            row3.getCell(1).setText("$250");
            row3.getCell(2).setText("$300");
            row3.getCell(3).setText("90/10");

            // Add another paragraph
            XWPFParagraph para2 = document.createParagraph();
            XWPFRun run2 = para2.createRun();
            run2.addBreak();
            run2.setText("All plans include prescription drug coverage, preventive care, and access to a nationwide network.");

            try (FileOutputStream out = new FileOutputStream(fileName)) {
                document.write(out);
            }
        }

        System.out.println("Created: " + fileName);
    }

    /**
     * Generate sample-simple.docx - Document with only text, no tables.
     */
    private static void generateSampleSimpleDocx() throws Exception {
        String fileName = TEST_FILES_DIR + "sample-simple.docx";

        try (XWPFDocument document = new XWPFDocument()) {
            // Add title
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText("Benefits Overview");

            // Add paragraphs
            XWPFParagraph para1 = document.createParagraph();
            XWPFRun run1 = para1.createRun();
            run1.setText("Company: Acme Corporation");

            XWPFParagraph para2 = document.createParagraph();
            XWPFRun run2 = para2.createRun();
            run2.setText("Effective Date: January 1, 2025");

            XWPFParagraph para3 = document.createParagraph();
            XWPFRun run3 = para3.createRun();
            run3.setText("This document provides an overview of the employee benefits package. " +
                    "The package includes comprehensive medical coverage, dental and vision plans, " +
                    "life insurance, and a 401(k) retirement savings plan with employer matching.");

            XWPFParagraph para4 = document.createParagraph();
            XWPFRun run4 = para4.createRun();
            run4.setText("Medical coverage is provided through a PPO network with access to thousands of providers nationwide. " +
                    "Employees can choose from multiple plan tiers to best fit their needs and budget.");

            XWPFParagraph para5 = document.createParagraph();
            XWPFRun run5 = para5.createRun();
            run5.setText("For questions about enrollment or coverage details, please contact HR at hr@acme.com " +
                    "or call (555) 987-6543.");

            try (FileOutputStream out = new FileOutputStream(fileName)) {
                document.write(out);
            }
        }

        System.out.println("Created: " + fileName);
    }

    /**
     * Generate sample.xlsx - Excel workbook with 2 sheets and mixed data types.
     */
    private static void generateSampleXlsx() throws Exception {
        String fileName = TEST_FILES_DIR + "sample.xlsx";

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Sheet 1: Plan Details
            XSSFSheet sheet1 = workbook.createSheet("Plan Details");

            // Header row
            XSSFRow headerRow = sheet1.createRow(0);
            headerRow.createCell(0).setCellValue("Plan Name");
            headerRow.createCell(1).setCellValue("Type");
            headerRow.createCell(2).setCellValue("Deductible");
            headerRow.createCell(3).setCellValue("Out-of-Pocket Max");
            headerRow.createCell(4).setCellValue("Active");

            // Data rows
            XSSFRow row1 = sheet1.createRow(1);
            row1.createCell(0).setCellValue("Platinum Plus");
            row1.createCell(1).setCellValue("PPO");
            row1.createCell(2).setCellValue(500);
            row1.createCell(3).setCellValue(3000);
            row1.createCell(4).setCellValue(true);

            XSSFRow row2 = sheet1.createRow(2);
            row2.createCell(0).setCellValue("Gold Standard");
            row2.createCell(1).setCellValue("PPO");
            row2.createCell(2).setCellValue(1000);
            row2.createCell(3).setCellValue(5000);
            row2.createCell(4).setCellValue(true);

            XSSFRow row3 = sheet1.createRow(3);
            row3.createCell(0).setCellValue("Silver Select");
            row3.createCell(1).setCellValue("HMO");
            row3.createCell(2).setCellValue(1500);
            row3.createCell(3).setCellValue(6500);
            row3.createCell(4).setCellValue(true);

            XSSFRow row4 = sheet1.createRow(4);
            row4.createCell(0).setCellValue("Bronze Basic");
            row4.createCell(1).setCellValue("HMO");
            row4.createCell(2).setCellValue(2500);
            row4.createCell(3).setCellValue(8000);
            row4.createCell(4).setCellValue(false);

            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet1.autoSizeColumn(i);
            }

            // Sheet 2: Pricing
            XSSFSheet sheet2 = workbook.createSheet("Pricing");

            // Header row
            XSSFRow pricingHeader = sheet2.createRow(0);
            pricingHeader.createCell(0).setCellValue("Tier");
            pricingHeader.createCell(1).setCellValue("Employee Only");
            pricingHeader.createCell(2).setCellValue("Employee + Spouse");
            pricingHeader.createCell(3).setCellValue("Employee + Children");
            pricingHeader.createCell(4).setCellValue("Family");

            // Data rows
            XSSFRow pRow1 = sheet2.createRow(1);
            pRow1.createCell(0).setCellValue("Platinum");
            pRow1.createCell(1).setCellValue(450.00);
            pRow1.createCell(2).setCellValue(900.00);
            pRow1.createCell(3).setCellValue(850.00);
            pRow1.createCell(4).setCellValue(1200.00);

            XSSFRow pRow2 = sheet2.createRow(2);
            pRow2.createCell(0).setCellValue("Gold");
            pRow2.createCell(1).setCellValue(350.00);
            pRow2.createCell(2).setCellValue(700.00);
            pRow2.createCell(3).setCellValue(650.00);
            pRow2.createCell(4).setCellValue(950.00);

            XSSFRow pRow3 = sheet2.createRow(3);
            pRow3.createCell(0).setCellValue("Silver");
            pRow3.createCell(1).setCellValue(275.00);
            pRow3.createCell(2).setCellValue(550.00);
            pRow3.createCell(3).setCellValue(500.00);
            pRow3.createCell(4).setCellValue(750.00);

            XSSFRow pRow4 = sheet2.createRow(4);
            pRow4.createCell(0).setCellValue("Bronze");
            pRow4.createCell(1).setCellValue(200.00);
            pRow4.createCell(2).setCellValue(400.00);
            pRow4.createCell(3).setCellValue(375.00);
            pRow4.createCell(4).setCellValue(550.00);

            // Auto-size columns
            for (int i = 0; i < 5; i++) {
                sheet2.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(fileName)) {
                workbook.write(out);
            }
        }

        System.out.println("Created: " + fileName);
    }
}
