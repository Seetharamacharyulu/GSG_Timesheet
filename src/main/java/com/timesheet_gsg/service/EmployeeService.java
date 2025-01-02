package com.timesheet_gsg.service;

import com.timesheet_gsg.Repository.EmployeeRepository;
import com.timesheet_gsg.model.Employee;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);  // Logger initialization

    @Autowired
    private EmployeeRepository employeeRepository;

    // Authenticate Admin login
    public Employee authenticateAdmin(String username, String password) {
        logger.info("Authenticating admin with username: {}", username);
        Employee admin = employeeRepository.findByUsername(username);
        if (admin != null && admin.getPassword().equals(password) && "ADMIN".equals(admin.getRole())) {
            logger.info("Admin authenticated successfully: {}", username);
            return admin;
        }
        logger.warn("Failed authentication attempt for username: {}", username);
        return null; // Invalid credentials
    }

    // Method to capture login details
    public Employee captureLoginDetails(Employee employee) {
        try {
            // Get the current HTTP request
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

            // Get the IP address from the request
            String loginIP = request.getRemoteAddr();

            // Get the host name of the IP address
            String loginHostname = InetAddress.getByName(loginIP).getHostName();

            // Set the login IP and hostname on the employee object
            employee.setLoginIP(loginIP);
            employee.setLoginHostname(loginHostname);

            // Log the details for verification
            logger.info("Employee {} logged in from IP: {} (Host: {})", employee.getUsername(), loginIP, loginHostname);
        } catch (UnknownHostException e) {
            logger.error("Unable to retrieve hostname for IP address", e);
        }
        return employee;
    }


    // Add new employee to the system
    public boolean addNewEmployee(String username, String password) {
        logger.info("Adding new employee with username: {}", username);
        Employee newEmployee = new Employee();
        newEmployee.setUsername(username);
        newEmployee.setPassword(password);
        newEmployee.setRole("EMPLOYEE");
        try {
            employeeRepository.save(newEmployee);
            logger.info("Employee added successfully: {}", username);
            return true;
        } catch (Exception e) {
            logger.error("Error adding employee: {}", username, e);
            return false;
        }
    }

    // Fetch all employees
    public List<Employee> getAllEmployees() {
        logger.info("Fetching all employees from the database.");
        return employeeRepository.findAll();
    }

    // Find employees by username
    public List<Employee> findEmployeesByUsername(String username) {
        logger.info("Searching for employees with username containing: {}", username);
        return employeeRepository.findByUsernameContainingIgnoreCase(username);
    }

    // Find employees by employee ID
    public List<Employee> findEmployeesByEmployeeId(String employeeId) {
        logger.info("Searching for employees with employeeId: {}", employeeId);
        return employeeRepository.findByEmployeeId(employeeId);
    }

    // Find employees by department
    public List<Employee> findEmployeesByDepartment(String department) {
        logger.info("Searching for employees in department: {}", department);
        return employeeRepository.findByDepartmentContainingIgnoreCase(department);
    }

    // Find employees by role
    public List<Employee> findEmployeesByRole(String role) {
        logger.info("Searching for employees with role: {}", role);
        return employeeRepository.findByRoleContainingIgnoreCase(role);
    }

    // Method to find employees by login date range
    public List<Employee> findEmployeesByLoginDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Searching for employees who logged in between {} and {}", startDate, endDate);
        return employeeRepository.findByLoginTimestampBetween(startDate, endDate);
    }

    // Filter employees by login date range
    public List<Employee> filterEmployeesByDateRange(LocalDateTime start, LocalDateTime end) {
        logger.info("Filtering employees by date range: {} to {}", start, end);
        return employeeRepository.findAll().stream()
                .filter(employee -> (employee.getLoginTimestamp().isAfter(start) || employee.getLoginTimestamp().isEqual(start))
                        && (employee.getLoginTimestamp().isBefore(end) || employee.getLoginTimestamp().isEqual(end)))
                .collect(Collectors.toList());
    }

    // Bulk upload employees from CSV
    @Transactional
    public boolean bulkUploadCSV(MultipartFile file) {
        logger.info("Starting bulk CSV upload.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 5) {
                    Employee employee = new Employee();
                    employee.setEmployeeId(data[0].trim());
                    employee.setUsername(data[1].trim());
                    employee.setPassword(data[2].trim());
                    employee.setRole(data[3].trim());
                    employee.setDepartment(data[4].trim());

                    employeeRepository.save(employee);
                    count++;
                }
            }

            logger.info("CSV upload completed. {} employees added.", count);
            return count > 0;
        } catch (IOException e) {
            logger.error("Error reading CSV file during bulk upload.", e);
            return false; // Error reading the file
        }
    }

    // Add new employee to the system using the Employee object
    public boolean addNewEmployee(Employee employee) {
        logger.info("Adding new employee with username: {}", employee.getUsername());
        try {
            employeeRepository.save(employee);
            logger.info("Employee added successfully: {}", employee.getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Error adding employee: {}", employee.getUsername(), e);
            return false;
        }
    }

    // Bulk upload employees from Excel (XLSX)
    @Transactional
    public boolean bulkUploadExcel(MultipartFile file) {
        logger.info("Starting bulk Excel upload.");
        try (InputStream inputStream = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            var sheet = workbook.getSheetAt(0);
            int count = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row

                Employee employee = new Employee();
                employee.setEmployeeId(row.getCell(0).getStringCellValue());
                employee.setUsername(row.getCell(1).getStringCellValue());
                employee.setPassword(row.getCell(2).getStringCellValue());
                employee.setRole(row.getCell(3).getStringCellValue());
                employee.setDepartment(row.getCell(4).getStringCellValue());

                employeeRepository.save(employee);
                count++;
            }

            logger.info("Excel upload completed. {} employees added.", count);
            return count > 0;
        } catch (IOException e) {
            logger.error("Error reading Excel file during bulk upload.", e);
            return false; // Error reading the Excel file
        }
    }
}
