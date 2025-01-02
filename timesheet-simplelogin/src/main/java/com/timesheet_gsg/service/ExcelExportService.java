package com.timesheet_gsg.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletResponse;
import com.timesheet_gsg.model.Employee;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);

    private static final LocalTime SHIFT_A_START = LocalTime.of(6, 0);
    private static final LocalTime SHIFT_B_START = LocalTime.of(14, 30);
    private static final LocalTime GENERAL_SHIFT_START = LocalTime.of(9, 0);
    private static final int MAX_WORKING_HOURS = 9;
    private static final int DELAY_THRESHOLD_MINUTES = 15;

    public void exportEmployeeDataToExcel(HttpServletResponse response, List<Employee> employees, String reportType) {
        Workbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Employee Data");

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
                "Employee ID", "Username", "Role", "Department", "Login Date", "Login Time",
                "Delayed Login Time", "Logout Date", "Logout Time", "Working Hours", "OT (Overtime)", "Shift", "IP Address", "Hostname"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            // Filter employees based on the reportType (daily, weekly, or monthly)
            List<Employee> filteredEmployees = filterEmployeesByTimeframe(employees, reportType);

            int rowNum = 1;
            for (Employee employee : filteredEmployees) {
                Row row = sheet.createRow(rowNum++);

                // Set default row values
                setEmployeeRowValues(row, employee, dateFormatter, timeFormatter, workbook);

                // Calculate and set login delay and shift
                setLoginDelayAndShift(row, employee, workbook, dateFormatter, timeFormatter);

                // Calculate and set logout and overtime
                setLogoutAndOvertime(row, employee, workbook, dateFormatter, timeFormatter);
            }

            // Auto-size columns
            for (int i = 0; i < 14; i++) {
                sheet.autoSizeColumn(i);
            }

            // Set response headers
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=employee_data_" + reportType + ".xlsx");
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
    

    // Filter employees by selected timeframe (daily, weekly, or monthly)
    private List<Employee> filterEmployeesByTimeframe(List<Employee> employees, String reportType) {
        LocalDate currentDate = LocalDate.now();

        switch (reportType.toLowerCase()) {
            case "daily":
                return employees.stream()
                        .filter(e -> e.getLoginTimestamp() != null && e.getLoginTimestamp().toLocalDate().isEqual(currentDate))
                        .collect(Collectors.toList());
            case "weekly":
                LocalDate startOfWeek = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1); // Get start of the current week (Monday)
                return employees.stream()
                        .filter(e -> e.getLoginTimestamp() != null && !e.getLoginTimestamp().toLocalDate().isBefore(startOfWeek))
                        .collect(Collectors.toList());
            case "monthly":
                return employees.stream()
                        .filter(e -> e.getLoginTimestamp() != null && e.getLoginTimestamp().toLocalDate().getMonth() == currentDate.getMonth())
                        .collect(Collectors.toList());
            default:
                return employees;
        }
    }

    // Helper method to set employee row values
    private void setEmployeeRowValues(Row row, Employee employee, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter, Workbook workbook) {
        row.createCell(0).setCellValue(employee.getEmployeeId());
        row.createCell(1).setCellValue(employee.getUsername());
        row.createCell(2).setCellValue(employee.getRole());
        row.createCell(3).setCellValue(employee.getDepartment());

        if (employee.getLoginTimestamp() != null) {
            LocalDate loginDate = employee.getLoginTimestamp().toLocalDate();
            LocalTime loginTime = employee.getLoginTimestamp().toLocalTime();
            row.createCell(4).setCellValue(loginDate.format(dateFormatter));
            row.createCell(5).setCellValue(loginTime.format(timeFormatter));

            row.createCell(12).setCellValue(employee.getLoginIP());
            row.createCell(13).setCellValue(employee.getLoginHostname());
        } else {
            row.createCell(4).setCellValue("Not Logged In");
            row.createCell(5).setCellValue("N/A");
            row.createCell(12).setCellValue("N/A");
            row.createCell(13).setCellValue("N/A");
        }
    }

    // Helper method to calculate delay and shift
    private void setLoginDelayAndShift(Row row, Employee employee, Workbook workbook, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter) {
        if (employee.getLoginTimestamp() != null) {
            LocalTime loginTime = employee.getLoginTimestamp().toLocalTime();
            LocalTime shiftStartTime = getShiftStartTime(employee.getLoginTimestamp().toLocalTime());
            Duration delayDuration = Duration.between(shiftStartTime, loginTime);

            long delayHours = delayDuration.toHours();
            long delayMinutes = delayDuration.toMinutes() % 60;

            String delayTimeFormatted = String.format("%d hours %d minutes", delayHours, delayMinutes);

            if (delayDuration.toMinutes() > DELAY_THRESHOLD_MINUTES) {
                CellStyle redStyle = workbook.createCellStyle();
                redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                Cell delayedLoginCell = row.createCell(6);
                delayedLoginCell.setCellValue(delayTimeFormatted);
                delayedLoginCell.setCellStyle(redStyle);
            } else {
                row.createCell(6).setCellValue(delayTimeFormatted);
            }

            // Shift determination
            String shift = getShift(employee.getLoginTimestamp().toLocalTime());
            row.createCell(11).setCellValue(shift);
        }
    }

    // Helper method to calculate logout and overtime
    private void setLogoutAndOvertime(Row row, Employee employee, Workbook workbook, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter) {
        if (employee.getLogoutTimestamp() != null) {
            row.createCell(7).setCellValue(employee.getLogoutTimestamp().toLocalDate().toString());
            row.createCell(8).setCellValue(employee.getLogoutTimestamp().toLocalTime().toString());

            if (employee.getLoginTimestamp() != null && employee.getLogoutTimestamp() != null) {
                Duration duration = Duration.between(employee.getLoginTimestamp(), employee.getLogoutTimestamp());
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                String workingHours = String.format("%d hours %d minutes", hours, minutes);
                row.createCell(9).setCellValue(workingHours);

                if (hours > MAX_WORKING_HOURS || (hours == MAX_WORKING_HOURS && minutes > 0)) {
                    long overtimeMinutes = (duration.toMinutes() - MAX_WORKING_HOURS * 60);
                    long overtimeHours = overtimeMinutes / 60;
                    overtimeMinutes = overtimeMinutes % 60;
                    String overtime = String.format("%d hours %d minutes", overtimeHours, overtimeMinutes);
                    row.createCell(10).setCellValue(overtime);
                } else {
                    row.createCell(10).setCellValue("No OT");
                }
            }
        } else {
            row.createCell(7).setCellValue("Not Logged Out");
            row.createCell(8).setCellValue("Not Logged Out");
            row.createCell(9).setCellValue("N/A");
            row.createCell(10).setCellValue("N/A");
        }
    }

    private LocalTime getShiftStartTime(LocalTime loginTime) {
        if (!loginTime.isBefore(SHIFT_A_START) && loginTime.isBefore(SHIFT_B_START)) {
            return SHIFT_A_START;
        } else if (!loginTime.isBefore(SHIFT_B_START) && loginTime.isBefore(GENERAL_SHIFT_START)) {
            return SHIFT_B_START;
        } else {
            return GENERAL_SHIFT_START;
        }
    }

    private String getShift(LocalTime loginTime) {
        if (!loginTime.isBefore(SHIFT_A_START) && loginTime.isBefore(SHIFT_B_START)) {
            return "Shift A";
        } else if (!loginTime.isBefore(SHIFT_B_START) && loginTime.isBefore(GENERAL_SHIFT_START)) {
            return "Shift B";
        } else {
            return "General Shift";
        }
    }
}
