package com.safewatch.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelReporter {
    private static List<TestResult> results = new ArrayList<>();

    public static class TestResult {
        public String id, module, name, status, priority, time;
        public TestResult(String id, String module, String name, String priority, String status, String time) {
            this.id = id; this.module = module; this.name = name; this.priority = priority; this.status = status; this.time = time;
        }
    }

    public static void logResult(String id, String module, String name, String priority, String status, String time) {
        results.add(new TestResult(id, module, name, priority, status, time));
    }

    public static void generateReport(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            try (Workbook workbook = new XSSFWorkbook()) {
            createSheet(workbook, "Executed Test Cases", results);
            createFilteredSheet(workbook, "Passed Tests", "PASS");
            createFilteredSheet(workbook, "Failed Tests", "FAIL");
            createFilteredSheet(workbook, "Skipped Tests", "SKIP");
            
            // Metrics Sheet
            Sheet summary = workbook.createSheet("Execution Metrics");
            Row header = summary.createRow(0);
            header.createCell(0).setCellValue("Metric");
            header.createCell(1).setCellValue("Value");
            
            long pass = results.stream().filter(r -> r.status.equalsIgnoreCase("PASS")).count();
            long fail = results.stream().filter(r -> r.status.equalsIgnoreCase("FAIL")).count();
            long skip = results.stream().filter(r -> r.status.equalsIgnoreCase("SKIP")).count();
            
            summary.createRow(1).createCell(0).setCellValue("Total Executed");
            summary.getRow(1).createCell(1).setCellValue(results.size());
            summary.createRow(2).createCell(0).setCellValue("Passed");
            summary.getRow(2).createCell(1).setCellValue(pass);
            summary.createRow(3).createCell(0).setCellValue("Failed");
            summary.getRow(3).createCell(1).setCellValue(fail);
            summary.createRow(4).createCell(0).setCellValue("Skipped");
            summary.getRow(4).createCell(1).setCellValue(skip);
            summary.createRow(5).createCell(0).setCellValue("Pass Rate");
            summary.getRow(5).createCell(1).setCellValue((results.size() > 0 ? (pass * 100 / results.size()) : 0) + "%");

            // Defect Summary Sheet
            Sheet defects = workbook.createSheet("Defect Summary");
            Row defHeader = defects.createRow(0);
            defHeader.createCell(0).setCellValue("Test ID");
            defHeader.createCell(1).setCellValue("Module");
            defHeader.createCell(2).setCellValue("Test Name");
            defHeader.createCell(3).setCellValue("Failure Reason");
            
            int defIdx = 1;
            for(TestResult res : results) {
                if(res.status.equalsIgnoreCase("FAIL")) {
                    Row dRow = defects.createRow(defIdx++);
                    dRow.createCell(0).setCellValue(res.id);
                    dRow.createCell(1).setCellValue(res.module);
                    dRow.createCell(2).setCellValue(res.name);
                    dRow.createCell(3).setCellValue("Assertion Timeout / Element Not Found");
                }
            }

            // Pass Rate Summary Sheet
            Sheet passRateSheet = workbook.createSheet("Pass Rate Summary");
            Row prHeader = passRateSheet.createRow(0);
            prHeader.createCell(0).setCellValue("Module");
            prHeader.createCell(1).setCellValue("Total");
            prHeader.createCell(2).setCellValue("Passed");
            prHeader.createCell(3).setCellValue("Failed");
            prHeader.createCell(4).setCellValue("Pass Rate");

            java.util.Set<String> modules = new java.util.HashSet<>();
            for(TestResult r : results) modules.add(r.module);
            int prIdx = 1;
            for(String mod : modules) {
                long mTotal = results.stream().filter(r -> r.module.equals(mod)).count();
                long mPass = results.stream().filter(r -> r.module.equals(mod) && r.status.equalsIgnoreCase("PASS")).count();
                long mFail = results.stream().filter(r -> r.module.equals(mod) && r.status.equalsIgnoreCase("FAIL")).count();
                Row prRow = passRateSheet.createRow(prIdx++);
                prRow.createCell(0).setCellValue(mod);
                prRow.createCell(1).setCellValue(mTotal);
                prRow.createCell(2).setCellValue(mPass);
                prRow.createCell(3).setCellValue(mFail);
                prRow.createCell(4).setCellValue((mTotal > 0 ? (mPass * 100 / mTotal) : 0) + "%");
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void createSheet(Workbook wb, String name, List<TestResult> data) {
        Sheet sheet = wb.createSheet(name);
        Row header = sheet.createRow(0);
        String[] cols = {"Test ID", "Module", "Test Name", "Priority", "Status", "Execution Time"};
        for(int i=0; i<cols.length; i++) header.createCell(i).setCellValue(cols[i]);
        
        int rowIdx = 1;
        for(TestResult res : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(res.id);
            row.createCell(1).setCellValue(res.module);
            row.createCell(2).setCellValue(res.name);
            row.createCell(3).setCellValue(res.priority);
            row.createCell(4).setCellValue(res.status);
            row.createCell(5).setCellValue(res.time);
        }
    }

    private static void createFilteredSheet(Workbook wb, String name, String filter) {
        List<TestResult> filtered = new ArrayList<>();
        for(TestResult r : results) if(r.status.equalsIgnoreCase(filter)) filtered.add(r);
        createSheet(wb, name, filtered);
    }
}
