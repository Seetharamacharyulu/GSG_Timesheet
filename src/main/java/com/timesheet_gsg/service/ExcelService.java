package com.timesheet_gsg.service;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.timesheet_gsg.model.Employee;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    public List<Employee> parseExcelFile(MultipartFile file) throws Exception {
        List<Employee> employees = new ArrayList<>();
        List<String> skippedRows = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                try {
                    Employee employee = parseRow(row);
                    employees.add(employee);
                } catch (IllegalArgumentException e) {
                    // Log and collect skipped rows
                    logger.warn("Skipping row {} due to error: {}", row.getRowNum(), e.getMessage());
                    skippedRows.add("Row " + row.getRowNum() + ": " + e.getMessage());
                }
            }
        }

        // Log skipped rows
        if (!skippedRows.isEmpty()) {
            logger.warn("Skipped rows: {}", String.join(", ", skippedRows));
        }

        return employees;
    }

    private Employee parseRow(Row row) {
        Employee employee = new Employee();

        // Parse employee ID (assuming it's in the first column)
        Cell employeeIdCell = row.getCell(0);
        if (employeeIdCell == null || employeeIdCell.getCellType() != CellType.STRING) {
            throw new IllegalArgumentException("Employee ID is missing or invalid");
        }
        employee.setEmployeeId(employeeIdCell.getStringCellValue());

        // Parse username (assuming it's in the second column)
        Cell usernameCell = row.getCell(1);
        if (usernameCell == null || usernameCell.getStringCellValue().isEmpty()) {
            throw new IllegalArgumentException("Username is missing");
        }
        employee.setUsername(usernameCell.getStringCellValue());

        // Parse login timestamp (assuming it's in the third column)
        Cell loginCell = row.getCell(2);
        if (loginCell == null || loginCell.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException("Invalid or missing login timestamp");
        }
        employee.setLoginTimestamp(loginCell.getLocalDateTimeCellValue());

        // Parse logout timestamp (optional, assuming it's in the fourth column)
        Cell logoutCell = row.getCell(3);
        if (logoutCell != null && logoutCell.getCellType() == CellType.NUMERIC) {
            employee.setLogoutTimestamp(logoutCell.getLocalDateTimeCellValue());
        }

        return employee;
    }
}
