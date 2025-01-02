package com.timesheet_gsg.Controller;

import com.timesheet_gsg.model.Employee;
import com.timesheet_gsg.service.EmployeeService;
import com.timesheet_gsg.service.ExcelExportService;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);  // Logger initialization
    
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ExcelExportService excelExportService;

    // Admin login page
    @GetMapping("/admin/login")
    public String adminLoginPage() {
        logger.info("Accessed admin login page.");
        return "adminLogin";
    }

    // Admin login functionality
    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String username, @RequestParam String password, Model model) {
        logger.info("Admin login attempt with username: {}", username);
        Employee admin = employeeService.authenticateAdmin(username, password);
        if (admin != null) {
            logger.info("Admin login successful for username: {}", username);
            return "adminDashboard"; // Redirect to admin dashboard
        }
        logger.error("Admin login failed for username: {}", username);
        model.addAttribute("error", "Invalid admin credentials.");
        return "adminLogin";
    }

    // Add new employee functionality
    @PostMapping("/admin/addEmployee")
    public String addEmployee(@RequestParam String username, @RequestParam String password, Model model) {
        logger.info("Adding new employee with username: {}", username);
        boolean isAdded = employeeService.addNewEmployee(username, password);
        if (isAdded) {
            logger.info("Employee added successfully with username: {}", username);
            model.addAttribute("message", "Employee added successfully.");
        } else {
            logger.error("Error adding employee with username: {}", username);
            model.addAttribute("error", "Error adding employee.");
        }
        return "adminDashboard";
    }

    // Export Employee data to Excel with optional filters
    @GetMapping("/admin/exportExcel")
    public void exportToExcel(@RequestParam(required = false) String reportType,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String employeeId,
                               @RequestParam(required = false) String department,
                               @RequestParam(required = false) String role,
                               HttpServletResponse response) throws IOException {

        logger.info("Exporting employee data to Excel with filters: reportType={}, startDate={}, endDate={}", 
                    reportType, startDate, endDate);

        // Default to "all" report type if not provided
        if (reportType == null || reportType.isEmpty()) {
            reportType = "all";
        }

        // Fetch all employees initially
        List<Employee> employees = employeeService.getAllEmployees();
        logger.info("Fetched {} employees from the database.", employees.size());

        // Apply filters (if any)
        if (username != null && !username.isEmpty()) {
            employees = employees.stream()
                                 .filter(emp -> emp.getUsername().contains(username))
                                 .collect(Collectors.toList());
        }

        if (employeeId != null && !employeeId.isEmpty()) {
            employees = employees.stream()
                                 .filter(emp -> emp.getEmployeeId().contains(employeeId))
                                 .collect(Collectors.toList());
        }

        if (department != null && !department.isEmpty()) {
            employees = employees.stream()
                                 .filter(emp -> emp.getDepartment().contains(department))
                                 .collect(Collectors.toList());
        }

        if (role != null && !role.isEmpty()) {
            employees = employees.stream()
                                 .filter(emp -> emp.getRole().contains(role))
                                 .collect(Collectors.toList());
        }

        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            employees = employees.stream()
                                 .filter(emp -> emp.getLoginTimestamp() != null && 
                                                !emp.getLoginTimestamp().isBefore(start) && 
                                                !emp.getLoginTimestamp().isAfter(end))
                                 .collect(Collectors.toList());
            logger.info("Applied date range filter: {} to {}", startDate, endDate);
        }

        // Export to Excel
        excelExportService.exportEmployeeDataToExcel(response, employees, reportType);
        logger.info("Exported employee data to Excel with {} employees.", employees.size());
    }

    // Upload Employee File (CSV/Excel)
    @PostMapping("/admin/uploadEmployeeFile")
    public String uploadEmployeeFile(@RequestParam("employeeFile") MultipartFile file, Model model) {
        logger.info("Processing file upload for file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            logger.error("No file selected for upload.");
            model.addAttribute("error", "No file selected!");
            return "adminDashboard";
        }

        List<Employee> newlyAddedEmployees = new ArrayList<>();

        try {
            if (file.getOriginalFilename().endsWith(".csv")) {
                logger.info("CSV file detected. Parsing...");
                // Validate CSV file before processing
                if (isValidCSV(file)) {
                    newlyAddedEmployees = parseCSV(file);  // return the list of added employees
                } else {
                    logger.error("Invalid CSV file format.");
                    model.addAttribute("error", "Invalid CSV file format! Please check the file content.");
                    return "adminDashboard";
                }
            } else if (file.getOriginalFilename().endsWith(".xls") || file.getOriginalFilename().endsWith(".xlsx")) {
                logger.info("Excel file detected. Parsing...");
                // Validate Excel file before processing
                if (isValidExcel(file)) {
                    newlyAddedEmployees = parseExcel(file);  // return the list of added employees
                } else {
                    logger.error("Invalid Excel file format.");
                    model.addAttribute("error", "Invalid Excel file format! Please check the file content.");
                    return "adminDashboard";
                }
            } else {
                logger.error("Invalid file type uploaded: {}", file.getOriginalFilename());
                model.addAttribute("error", "Invalid file type! Please upload a CSV or Excel file.");
                return "adminDashboard";
            }

            model.addAttribute("message", "File uploaded and employees added successfully!");
            model.addAttribute("newEmployees", newlyAddedEmployees);  // Add the list to the model
            logger.info("File processed successfully. {} employees added.", newlyAddedEmployees.size());

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            model.addAttribute("error", "Error uploading file: " + e.getMessage());
        }

        return "adminDashboard";  // Redirect back to the dashboard page
    }

    // Validate if the uploaded CSV file is valid
    private boolean isValidCSV(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length != 5) {
                    logger.error("Invalid CSV format. Each line should have 5 columns.");
                    return false;
                }
            }
        }
        return true;
    }

    // Validate if the uploaded Excel file is valid
    private boolean isValidExcel(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip the header row and check the structure of data rows
            while (rows.hasNext()) {
                Row row = rows.next();
                if (row.getRowNum() == 0) continue;  // Skip header row
                if (row.getCell(0) == null || row.getCell(1) == null || row.getCell(2) == null || row.getCell(3) == null || row.getCell(4) == null) {
                    logger.error("Missing required fields in the Excel row.");
                    return false;
                }
            }
        }
        return true;
    }

    // Parse CSV file and add employees to the database
    private List<Employee> parseCSV(MultipartFile file) throws IOException {
        List<Employee> addedEmployees = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 5) {
                    Employee employee = new Employee();
                    employee.setEmployeeId(data[0].trim());
                    employee.setUsername(data[1].trim());
                    employee.setPassword(data[2].trim());
                    employee.setRole(data[3].trim());
                    employee.setDepartment(data[4].trim());

                    employeeService.addNewEmployee(employee);  // Save employee to DB
                    addedEmployees.add(employee);  // Add to the list of added employees
                    logger.info("Added employee: {}", employee.getUsername());
                }
            }
        }
        return addedEmployees;
    }

    // Parse Excel file and add employees to the database
    private List<Employee> parseExcel(MultipartFile file) throws IOException {
        List<Employee> addedEmployees = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (currentRow.getRowNum() == 0) {
                    // Skip header row
                    continue;
                }

                Employee employee = new Employee();
                employee.setEmployeeId(currentRow.getCell(0).getStringCellValue());
                employee.setUsername(currentRow.getCell(1).getStringCellValue());
                employee.setPassword(currentRow.getCell(2).getStringCellValue());
                employee.setRole(currentRow.getCell(3).getStringCellValue());
                employee.setDepartment(currentRow.getCell(4).getStringCellValue());

                employeeService.addNewEmployee(employee);  // Save employee to DB
                addedEmployees.add(employee);  // Add to the list of added employees
                logger.info("Added employee: {}", employee.getUsername());
            }
        }
        return addedEmployees;
    }
}
