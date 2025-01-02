package com.timesheet_gsg.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.timesheet_gsg.Repository.EmployeeRepository;
import com.timesheet_gsg.model.Employee;

@Service
public class TimesheetService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    // Define shift times
    private static final LocalTime SHIFT_A_START = LocalTime.of(6, 0); // 6:00 AM
    private static final LocalTime SHIFT_A_END = LocalTime.of(14, 30); // 2:30 PM
    private static final LocalTime SHIFT_B_START = LocalTime.of(14, 30); // 2:30 PM
    private static final LocalTime SHIFT_B_END = LocalTime.of(23, 0); // 11:00 PM
    private static final LocalTime GENERAL_SHIFT_START = LocalTime.of(9, 0); // 9:00 AM
    private static final LocalTime GENERAL_SHIFT_END = LocalTime.of(18, 0); // 6:00 PM

    // Define the max working hours (10 hours) and cooldown period (12 hours)
    private static final long MAX_WORKING_HOURS = 10; // Max working hours before logout is locked
    private static final long COOLDOWN_PERIOD_HOURS = 12; // Cooldown period after 10 hours of work

    public Employee authenticateEmployee(String username, String password) {
        Employee employee = employeeRepository.findByUsername(username);
        if (employee != null && employee.getPassword().equals(password)) {
            employee.setLoggedIn(true);
            employee.setLoginTimestamp(LocalDateTime.now()); // Set login timestamp

            // Capture the PC details (IP address and hostname)
            String loginPCDetails = getLoginPCDetails();  // Method to retrieve PC details
            employee.setLoginPCDetails(loginPCDetails);   // Set the captured PC details

            // Check login time and assign the shift
            LocalTime loginTime = employee.getLoginTimestamp().toLocalTime();
            String shift = determineShift(loginTime);
            employee.setShift(shift); // Set the shift
            employeeRepository.save(employee);

            logger.info("Login success for user: {} on shift: {} from PC: {}", username, shift, loginPCDetails);
            return employee;
        }

        // Log login failure
        logger.warn("Login failed for user: {}", username);
        return null; // Authentication failed
    }

    public class LoginDetailsUtil {
        // Utility method to extract IP and Hostname from loginPCDetails string
        public static String[] extractIPAndHostname(String loginPCDetails) {
            String[] details = new String[2];
            if (loginPCDetails != null && loginPCDetails.startsWith("IP: ")) {
                String[] parts = loginPCDetails.split(", ");
                details[0] = parts[0].split(": ")[1];  // Extract IP
                details[1] = parts.length > 1 ? parts[1].split(": ")[1] : "Unknown";  // Extract Hostname or "Unknown"
            } else {
                details[0] = "Unknown";
                details[1] = "Unknown";
            }
            return details;
        }
    }

    private String getLoginPCDetails() {
        // Retrieve the client IP address and hostname (if needed)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            // Check for X-Forwarded-For header in case of proxy
            String ipAddress = attributes.getRequest().getHeader("X-Forwarded-For");
            
            if (ipAddress != null && !ipAddress.isEmpty()) {
                // Get the first IP address from the comma-separated list
                String[] ips = ipAddress.split(",");
                ipAddress = ips[0].trim();
            } else {
                ipAddress = attributes.getRequest().getRemoteAddr();
            }

            // Optionally resolve the hostname
            try {
                InetAddress inetAddress = InetAddress.getByName(ipAddress);
                String hostname = inetAddress.getHostName();  // Resolves the hostname

                // Combine IP and hostname as a string to save both details
                return "IP: " + ipAddress + ", Hostname: " + hostname;
            } catch (UnknownHostException e) {
                // If hostname resolution fails, return just the IP address
                return "IP: " + ipAddress + ", Hostname: Unknown";
            }
        }
     
        return "IP: Unknown, Hostname: Unknown";  // Return "Unknown" if we can't capture the IP address or hostname
    }

    private String determineShift(LocalTime loginTime) {
        if (loginTime.isAfter(SHIFT_A_START) && loginTime.isBefore(SHIFT_A_END)) {
            return "Shift A"; // Morning shift (6 AM to 2:30 PM)
        } else if (loginTime.isAfter(SHIFT_B_START) && loginTime.isBefore(SHIFT_B_END)) {
            return "Shift B"; // Evening shift (2:30 PM to 11 PM)
        } else if (loginTime.isAfter(GENERAL_SHIFT_START) && loginTime.isBefore(GENERAL_SHIFT_END)) {
            return "General Shift"; // General shift (9 AM to 6 PM)
        } else {
            return "Unknown Shift"; // If login is outside the defined shifts
        }
    }

    public void logoutEmployee(String username) {
        Employee employee = employeeRepository.findByUsername(username);
        if (employee != null) {
            if (isLogoutLocked(employee)) {
                String message = "You are not allowed to logout as you've exceeded the time limit. Please contact admin to know more.";
                logger.warn("User {} attempted to logout but exceeded the time limit. Message: {}", username, message);
                throw new IllegalStateException(message);
            }

            // Proceed with logout
            employee.setLoggedIn(false);
            employee.setLogoutTimestamp(LocalDateTime.now());
            employeeRepository.save(employee); // Persist changes
            logger.info("User {} logged out successfully.", username);
        } else {
            logger.warn("Logout attempt for non-existent user: {}", username);
        }
    }

    private boolean isLogoutLocked(Employee employee) {
        // Check if employee has worked more than 10 hours
        if (employee.getLoginTimestamp() != null && employee.getLogoutTimestamp() == null) {
            Duration duration = Duration.between(employee.getLoginTimestamp(), LocalDateTime.now());
            long workedHours = duration.toHours();

            if (workedHours >= MAX_WORKING_HOURS) {
                // Lock logout if working hours exceed 10 hours and apply cooldown
                LocalDateTime nextAllowedLogoutTime = employee.getLoginTimestamp().plusHours(COOLDOWN_PERIOD_HOURS);
                if (LocalDateTime.now().isBefore(nextAllowedLogoutTime)) {
                    // If logout is attempted before cooldown period
                    return true;
                }
            }
        }
        return false;
    }

    public Employee findEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username);
    }
}





/*
 * package com.timesheet_gsg.service;
 * 
 * import java.time.LocalDateTime; import java.time.LocalTime;
 * 
 * import org.slf4j.Logger; import org.slf4j.LoggerFactory; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.stereotype.Service;
 * 
 * import com.timesheet_gsg.Repository.EmployeeRepository; import
 * com.timesheet_gsg.model.Employee;
 * 
 * @Service public class TimesheetService {
 * 
 * private static final Logger logger =
 * LoggerFactory.getLogger(TimesheetService.class);
 * 
 * @Autowired private EmployeeRepository employeeRepository;
 * 
 * // Define shift times private static final LocalTime SHIFT_A_START =
 * LocalTime.of(6, 0); // 6:00 AM private static final LocalTime SHIFT_A_END =
 * LocalTime.of(14, 30); // 2:30 PM private static final LocalTime SHIFT_B_START
 * = LocalTime.of(14, 30); // 2:30 PM private static final LocalTime SHIFT_B_END
 * = LocalTime.of(23, 0); // 11:00 PM private static final LocalTime
 * GENERAL_SHIFT_START = LocalTime.of(9, 0); // 9:00 AM private static final
 * LocalTime GENERAL_SHIFT_END = LocalTime.of(18, 0); // 6:00 PM
 * 
 * public Employee authenticateEmployee(String username, String password) {
 * Employee employee = employeeRepository.findByUsername(username); if (employee
 * != null && employee.getPassword().equals(password)) {
 * employee.setLoggedIn(true); employee.setLoginTimestamp(LocalDateTime.now());
 * // Set login timestamp
 * 
 * // Check login time and assign the shift LocalTime loginTime =
 * employee.getLoginTimestamp().toLocalTime(); String shift =
 * determineShift(loginTime); employee.setShift(shift); // Set the shift
 * employeeRepository.save(employee);
 * 
 * logger.info("Login success for user: {} on shift: {}", username, shift);
 * return employee; }
 * 
 * // Log login failure logger.warn("Login failed for user: {}", username);
 * return null; // Authentication failed }
 * 
 * private String determineShift(LocalTime loginTime) { if
 * (loginTime.isAfter(SHIFT_A_START) && loginTime.isBefore(SHIFT_A_END)) {
 * return "Shift A"; // Morning shift (6 AM to 2:30 PM) } else if
 * (loginTime.isAfter(SHIFT_B_START) && loginTime.isBefore(SHIFT_B_END)) {
 * return "Shift B"; // Evening shift (2:30 PM to 11 PM) } else if
 * (loginTime.isAfter(GENERAL_SHIFT_START) &&
 * loginTime.isBefore(GENERAL_SHIFT_END)) { return "General Shift"; // General
 * shift (9 AM to 6 PM) } else { return "Unknown Shift"; // If login is outside
 * the defined shifts } }
 * 
 * public void logoutEmployee(String username) { Employee employee =
 * employeeRepository.findByUsername(username); if (employee != null) {
 * employee.setLoggedIn(false);
 * employee.setLogoutTimestamp(LocalDateTime.now());
 * employeeRepository.save(employee); // Persist changes
 * logger.info("User {} logged out successfully.", username); } else {
 * logger.warn("Logout attempt for non-existent user: {}", username); } }
 * 
 * public Employee findEmployeeByUsername(String username) { return
 * employeeRepository.findByUsername(username); } }
 */
