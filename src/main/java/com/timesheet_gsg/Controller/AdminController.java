package com.timesheet_gsg.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.timesheet_gsg.Repository.EmployeeRepository;
import com.timesheet_gsg.model.Employee;
import com.timesheet_gsg.model.EmployeeActivity;
import com.timesheet_gsg.service.AdminUserService;
import com.timesheet_gsg.service.EmployeeActivityService;
import com.timesheet_gsg.service.EmployeeService;
import com.timesheet_gsg.service.ExcelExportService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeActivityService employeeActivityService; 

    // Admin login page
    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "adminLogin";  // Login page view
    }

    // Admin login authentication
    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String username, @RequestParam String password, Model model) {
        boolean isAuthenticated = adminUserService.authenticateAdmin(username, password);

        if (isAuthenticated) {
            model.addAttribute("username", username);  // Passing the username to the view if login is successful
            return "adminDashboard";  // Redirect to admin dashboard
        } else {
            model.addAttribute("error", "Invalid credentials.");  // Return error if login fails
            return "adminLogin";  // Return to login page
        }
    }

 // Export Employee Activity data to Excel with optional filters
    @GetMapping("/admin/exportExcel")
    public void exportToExcel(@RequestParam(required = false) String reportType,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String employeeId,
                               HttpServletResponse response) throws IOException {

        logger.info("Exporting employee activity data to Excel with filters: reportType={}, startDate={}, endDate={}, username={}, employeeId={}",
                reportType, startDate, endDate, username, employeeId);

        // Default to "all" report type if not provided
        if (reportType == null || reportType.isEmpty()) {
            reportType = "all";
        }

        // Fetch all employee activities initially
        List<EmployeeActivity> activities = employeeActivityService.getAllEmployeeActivities();
        logger.info("Fetched {} employee activities from the database.", activities.size());

        // Apply filters (if any)
        activities = applyFilters(activities, username, employeeId, startDate, endDate);

        // Export to Excel
        excelExportService.exportEmployeeActivityDataToExcel(response, activities, reportType);
        logger.info("Exported employee activity data to Excel with {} activities.", activities.size());
    }

    // Apply filters to the employee activity list
    private List<EmployeeActivity> applyFilters(List<EmployeeActivity> activities, String username, String employeeId,
                                                String startDate, String endDate) {
        // Filter by username
        if (username != null && !username.isEmpty()) {
            activities = activities.stream()
                    .filter(activity -> activity.getUsername().contains(username))  // Assuming EmployeeActivity has a username field
                    .collect(Collectors.toList());
        }

        // Filter by employeeId
        if (employeeId != null && !employeeId.isEmpty()) {
            activities = activities.stream()
                    .filter(activity -> activity.getEmployeeId().contains(employeeId))  // Assuming EmployeeActivity has an employeeId field
                    .collect(Collectors.toList());
        }

        // Apply date range filter (startDate to endDate)
        if (startDate != null && endDate != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
                activities = activities.stream()
                        .filter(activity -> activity.getLoginTimestamp() != null &&
                                !activity.getLoginTimestamp().isBefore(start) &&
                                !activity.getLoginTimestamp().isAfter(end))
                        .collect(Collectors.toList());
                logger.info("Applied date range filter: {} to {}", startDate, endDate);
            } catch (DateTimeParseException e) {
                logger.error("Invalid date format for startDate or endDate: {} - {}", startDate, endDate);
            }
        }

        return activities;
    }



    // Download sample Excel file
    @GetMapping("/admin/downloadSampleExcel")
    public ResponseEntity<ClassPathResource> downloadSampleExcel() throws IOException {
        ClassPathResource file = new ClassPathResource("static/sample_employee_register.xlsx");

        // Set up the response to prompt for a file download
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_employee_register.xlsx")
                .body(file);
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

    // Parse Excel file and add employees to the database
    private List<Employee> parseExcel(MultipartFile file, List<String> duplicateUsernames) throws IOException {
        List<Employee> addedEmployees = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (currentRow.getRowNum() == 0) continue;  // Skip header row

                String username = currentRow.getCell(1).getStringCellValue();
                if (employeeService.usernameExists(username)) {  // Check if username exists
                    logger.info("Username already exists: {}", username);
                    duplicateUsernames.add(username);  // Add to list of duplicates
                    continue;  // Skip adding this employee
                }

                Employee employee = new Employee();
                employee.setEmployeeId(currentRow.getCell(0).getStringCellValue());
                employee.setUsername(username);

                // Generate a random password for each employee (8 characters long)
                String generatedPassword = RandomStringUtils.randomAlphanumeric(8);
                employee.setPassword(generatedPassword);  // Set the generated password

                employee.setRole(currentRow.getCell(3).getStringCellValue());
                employee.setDepartment(currentRow.getCell(4).getStringCellValue());

                employeeService.addNewEmployee(employee);
                addedEmployees.add(employee);
                logger.info("Added employee: {} with generated password: {}", employee.getUsername(), generatedPassword);
            }
        }
        return addedEmployees;
    }

    @GetMapping("/admin/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        try {
            // Fetch all employees from the service layer
            List<Employee> employees = employeeService.getAllEmployees();

            // If the list is empty, return a not found status with an appropriate message
            if (employees.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Return a successful response with the list of employees
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            // Return an error response in case of an exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/admin/employeeList")
    public String showEmployeeList(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        if (employees == null) {
            logger.error("Employees list is null!");
        } else {
            logger.info("Employees fetched: " + employees.size());
        }
        model.addAttribute("employees", employees);
        return "employeeList";
    }



    @PostMapping("/admin/deleteEmployees")
    public String deleteEmployees(@RequestParam(value = "selectedUsernames", required = false) List<String> selectedUsernames,
                                  RedirectAttributes redirectAttributes) {
        // Check if any employees are selected for deletion
        if (selectedUsernames == null || selectedUsernames.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No employees selected for deletion.");
            return "redirect:/admin/employeeList";  // Redirect back to the employee list page
        }

        try {
            logger.info("Attempting to delete employees with usernames: {}", selectedUsernames);

            // Attempt to delete employees by their usernames
            boolean isDeleted = employeeService.deleteEmployeesByUsernames(selectedUsernames);

            if (isDeleted) {
                redirectAttributes.addFlashAttribute("success", "Selected employees have been deleted successfully.");
                logger.info("Employees deleted successfully: {}", selectedUsernames);
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete some or all employees. Please try again.");
                logger.warn("Failed to delete employees: {}", selectedUsernames);
            }
        } catch (Exception e) {
            logger.error("Error deleting employees: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while deleting employees. Please try again.");
        }

        return "redirect:/admin/employeeList";  // Redirect back to the employee list page
    }


    // Reset passwords for multiple employees
    @PostMapping("/resetPasswords")
    public String resetPasswords(@RequestParam("selectedUsernames") List<String> selectedUsernames,
                                 RedirectAttributes redirectAttributes) {
        try {
            for (String username : selectedUsernames) {
                String newPassword = employeeService.generateRandomPassword();
                boolean resetSuccess = employeeService.resetPassword(username, newPassword);
                if (resetSuccess) {
                    redirectAttributes.addFlashAttribute("success", "Passwords for selected employees have been reset.");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Failed to reset password for: " + username);
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resetting passwords: " + e.getMessage());
        }
        return "redirect:/admin/employeeList";  // Redirect back to the employee list page
    }

    @PostMapping("/admin/uploadEmployeeFile")
    public String uploadEmployeeFile(@RequestParam("employeeFile") MultipartFile file, Model model) {
        logger.info("Processing file upload for file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            logger.error("No file selected for upload.");
            model.addAttribute("error", "No file selected!");
            return "adminDashboard";
        }

        List<Employee> newlyAddedEmployees = new ArrayList<>();
        List<String> duplicateUsernames = new ArrayList<>();  // Track duplicate usernames

        try {
            if (file.getOriginalFilename().endsWith(".xls") || file.getOriginalFilename().endsWith(".xlsx")) {
                logger.info("Excel file detected. Parsing...");
                if (isValidExcel(file)) {
                    newlyAddedEmployees = parseExcel(file, duplicateUsernames);  // Pass the list for duplicates
                    // Automatically generate and assign passwords
                    for (Employee employee : newlyAddedEmployees) {
                        String generatedPassword = RandomStringUtils.randomAlphanumeric(8);  // Generate a random password
                        employee.setPassword(generatedPassword);  // Set the password for the employee
                        // Update the employee record with the generated password in the database
                        employeeService.updateEmployeePassword(employee.getUsername(), generatedPassword);
                    }
                } else {
                    model.addAttribute("error", "Invalid Excel file format! Please check the file content.");
                    return "adminDashboard";
                }
            } else {
                model.addAttribute("error", "Invalid file type! Please upload a valid Excel file.");
                return "adminDashboard";
            }

            if (!duplicateUsernames.isEmpty()) {
                model.addAttribute("warning", "The following usernames already exist and were skipped: " + String.join(", ", duplicateUsernames));
            }

            // Add the generated passwords to the model to display them in the UI
            model.addAttribute("message", "File uploaded and employees added successfully!");
            model.addAttribute("newEmployees", newlyAddedEmployees);
            logger.info("File processed successfully. {} employees added.", newlyAddedEmployees.size());

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage());
            model.addAttribute("error", "Error uploading file: " + e.getMessage());
        }

        return "adminDashboard";  // Redirect to the admin dashboard
    }

}
