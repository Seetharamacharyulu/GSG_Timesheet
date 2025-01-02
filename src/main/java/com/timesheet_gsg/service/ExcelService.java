package com.timesheet_gsg.service;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.timesheet_gsg.model.Employee;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    public List<Employee> parseExcelFile(MultipartFile file) throws Exception {
        List<Employee> employees = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                Employee employee = new Employee();
                employee.setUsername(row.getCell(0).getStringCellValue());
                employee.setLoginTimestamp(row.getCell(1).getLocalDateTimeCellValue());
                employee.setLogoutTimestamp(row.getCell(2).getLocalDateTimeCellValue());
                employees.add(employee);
            }
        }
        return employees;
    }
}
