package com.timesheet_gsg.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletResponse;
import com.timesheet_gsg.model.EmployeeActivity;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);


    // Export Employee Activity Data to Excel
    public void exportEmployeeActivityDataToExcel(HttpServletResponse response, List<EmployeeActivity> activities, String reportType) {
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Employee Activity Data");

            // Create header row with bold and background color
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {
                "Employee ID", "Username", "Login Date", "Login Time", "Logout Date", "Logout Time", "Working Hours", "Overtime", "Activity", "IP Address", "Hostname"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            // Filter activities based on the reportType (daily, weekly, or monthly)
            List<EmployeeActivity> filteredActivities = filterActivitiesByTimeframe(activities, reportType);

            int rowNum = 1;
            for (EmployeeActivity activity : filteredActivities) {
                Row row = sheet.createRow(rowNum++);
                setEmployeeActivityRowValues(row, activity, workbook, dateFormatter, timeFormatter);
            }

            // Auto-size columns
            for (int i = 0; i < 11; i++) {  // Includes all columns (11 columns now)
                sheet.autoSizeColumn(i);
            }

            // Add Summary sheet with login and logout timestamps
            addSummarySheet(workbook, filteredActivities);

            // Set response headers
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=employee_activity_data_" + reportType + ".xlsx");
            workbook.write(response.getOutputStream());

        } catch (IOException e) {
            logger.error("Error occurred while exporting data: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Error occurred while exporting data. Please try again.");
            } catch (IOException ex) {
                logger.error("Error occurred while sending error message: ", ex);
            }
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error("Error occurred while closing the workbook: ", e);
                }
            }
        }
    }

    // Create a new summary sheet for login and logout timestamps
    private void addSummarySheet(Workbook workbook, List<EmployeeActivity> activities) {
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create header row for summary sheet
        Row headerRow = summarySheet.createRow(0);
        headerRow.createCell(0).setCellValue("Username");
        headerRow.createCell(1).setCellValue("Login Timestamp");
        headerRow.createCell(2).setCellValue("Logout Timestamp");

        // Populate the summary sheet with employee data
        int rowNum = 1;
        for (EmployeeActivity activity : activities) {
            Row row = summarySheet.createRow(rowNum++);
            row.createCell(0).setCellValue(activity.getUsername());
            row.createCell(1).setCellValue(activity.getLoginTimestamp() != null ? activity.getLoginTimestamp().toString() : "N/A");
            row.createCell(2).setCellValue(activity.getLogoutTimestamp() != null ? activity.getLogoutTimestamp().toString() : "N/A");
        }

        // Auto-size columns for the summary sheet
        for (int i = 0; i < 3; i++) {
            summarySheet.autoSizeColumn(i);
        }

        logger.info("Summary sheet added with login and logout timestamps.");
    }

    // Filter activities by selected timeframe (daily, weekly, or monthly)
    private List<EmployeeActivity> filterActivitiesByTimeframe(List<EmployeeActivity> activities, String reportType) {
        LocalDate currentDate = LocalDate.now();

        switch (reportType.toLowerCase()) {
            case "daily":
                return activities.stream()
                        .filter(a -> a.getLoginTimestamp() != null && a.getLoginTimestamp().toLocalDate().isEqual(currentDate))
                        .collect(Collectors.toList());
            case "weekly":
                LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1); // Get start of the current week (Monday)
                return activities.stream()
                        .filter(a -> a.getLoginTimestamp() != null && !a.getLoginTimestamp().toLocalDate().isBefore(startOfWeek))
                        .collect(Collectors.toList());
            case "monthly":
                return activities.stream()
                        .filter(a -> a.getLoginTimestamp() != null && a.getLoginTimestamp().getMonth() == currentDate.getMonth())
                        .collect(Collectors.toList());
            default:
                return activities;
        }
    }

    // Helper method to set employee activity row values
    private void setEmployeeActivityRowValues(Row row, EmployeeActivity activity, Workbook workbook, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter) {
        row.createCell(0).setCellValue(activity.getEmployeeId());
        row.createCell(1).setCellValue(activity.getUsername());

        // Set Login Date and Time
        if (activity.getLoginTimestamp() != null) {
            row.createCell(2).setCellValue(activity.getLoginTimestamp().format(dateFormatter));
            row.createCell(3).setCellValue(activity.getLoginTimestamp().format(timeFormatter));
        } else {
            row.createCell(2).setCellValue("N/A");
            row.createCell(3).setCellValue("N/A");
        }

        // Set Logout Date and Time
        if (activity.getLogoutTimestamp() != null) {
            row.createCell(4).setCellValue(activity.getLogoutTimestamp().format(dateFormatter));
            row.createCell(5).setCellValue(activity.getLogoutTimestamp().format(timeFormatter));

            // Calculate Working Hours if Login and Logout are on the same day
            if (activity.getLoginTimestamp() != null && activity.getLogoutTimestamp() != null &&
                activity.getLoginTimestamp().toLocalDate().isEqual(activity.getLogoutTimestamp().toLocalDate())) {
                
                // Calculate working hours as the difference between login and logout timestamps
                Duration duration = Duration.between(activity.getLoginTimestamp(), activity.getLogoutTimestamp());
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;

                // Display working hours in decimal format (e.g., 7.5 for 7 hours 30 minutes)
                double workingHours = hours + (minutes / 60.0);
                row.createCell(6).setCellValue(String.format("%.2f", workingHours));  // Working Hours column

                // Apply conditional formatting (blue color if working hours > 9)
                if (workingHours > 9) {
                    CellStyle blueStyle = workbook.createCellStyle();
                    blueStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                    blueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    row.getCell(6).setCellStyle(blueStyle);

                    // Calculate OT (Overtime) in hours
                    double overtime = workingHours - 9;
                    row.createCell(7).setCellValue(String.format("%.2f", overtime));  // Overtime column

                    // Apply blue color to Overtime column if it's greater than 0
                    if (overtime > 0) {
                        CellStyle otStyle = workbook.createCellStyle();
                        otStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
                        otStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        row.getCell(7).setCellStyle(otStyle);
                    }

                } else {
                    row.createCell(7).setCellValue("0.00");  // No Overtime
                }

            } else {
                row.createCell(6).setCellValue("N/A");
                row.createCell(7).setCellValue("N/A");
            }

        } else {
            row.createCell(4).setCellValue("N/A");
            row.createCell(5).setCellValue("N/A");
            row.createCell(6).setCellValue("N/A");
            row.createCell(7).setCellValue("N/A");
        }

        row.createCell(8).setCellValue(activity.getActivity());

        // Set IP Address
        row.createCell(9).setCellValue(activity.getLoginIP() != null ? activity.getLoginIP() : "N/A");

        // Set Hostname
        row.createCell(10).setCellValue(activity.getLoginHostname() != null ? activity.getLoginHostname() : "N/A");
    }
}