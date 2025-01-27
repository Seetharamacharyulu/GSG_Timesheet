package com.timesheet_gsg.service;

import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.timesheet_gsg.Repository.EmployeeRepository;
import com.timesheet_gsg.model.Employee;

@Service
public class TimesheetService {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    // Shift definitions
    private static final LocalTime SHIFT_A_START = LocalTime.of(6, 0);
    private static final LocalTime SHIFT_A_END = LocalTime.of(14, 30);
    private static final LocalTime SHIFT_B_START = LocalTime.of(14, 30);
    private static final LocalTime SHIFT_B_END = LocalTime.of(23, 0);
    private static final LocalTime GENERAL_SHIFT_START = LocalTime.of(9, 0);
    private static final LocalTime GENERAL_SHIFT_END = LocalTime.of(18, 0);

    private static final long MAX_WORKING_HOURS = 10;
    private static final long COOLDOWN_PERIOD_HOURS = 12;

    /**
     * Authenticate the employee using username and password.
     */
    public Employee authenticateEmployee(String username, String password) {
        Employee employee = employeeRepository.findByUsername(username);

        if (employee != null && employee.getPassword().equals(password)) {
            employee.setLoggedIn(true);
            employee.setLoginTimestamp(LocalDateTime.now());
            employeeRepository.save(employee);
            logger.info("Login success for user: {} at {}", username, employee.getLoginTimestamp());
            return employee;
        }

        logger.warn("Login failed for user: {}", username);
        return null;
    }

    /**
     * Change the employee's password.
     */
    public void changePassword(Employee employee, String newPassword) {
        employee.setPassword(newPassword);
        employeeRepository.save(employee);
        logger.info("Password updated successfully for user: {}", employee.getUsername());
    }

    /**
     * Get the login PC details including IP and hostname.
     */
    
    private String getLoginPCDetails() {
        StringBuilder ipAddresses = new StringBuilder();
        String deviceName = "Unknown";

        try {
            // Get the local hostname (device name)
            InetAddress localHost = InetAddress.getLocalHost();
            deviceName = localHost.getHostName();  // This should provide the machine name
            
            // If it's still the loopback address, fallback to using the system's user name
            if ("localhost".equals(deviceName) || deviceName.contains(":")) {
                deviceName = System.getProperty("user.name", "Unknown");
            }

            // Get all network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // Get all IP addresses associated with the network interface
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();

                    // Capture only non-loopback IPv4 addresses (filter out IPv6 loopback)
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        if (ipAddresses.length() > 0) {
                            ipAddresses.append(", ");
                        }
                        ipAddresses.append(inetAddress.getHostAddress());
                    }
                }
            }

            // If no IPv4 addresses were found, add a fallback message
            if (ipAddresses.length() == 0) {
                ipAddresses.append("No IPv4 addresses found");
            }
        } catch (SocketException | UnknownHostException e) {
            // Handle exceptions and return error message
            ipAddresses.append("Error fetching IP: ").append(e.getMessage());
            deviceName = "Unknown";  // In case of error, we return "Unknown" as the device name
        }

        // Return the formatted IP and device name details
        return String.format("IP: %s, Device Name: %s", ipAddresses, deviceName);
    }




    /**
     * Determine the employee's shift based on login time.
     */
    private String determineShift(LocalTime loginTime) {
        if (loginTime.isAfter(SHIFT_A_START) && loginTime.isBefore(SHIFT_A_END)) {
            return "Shift A";
        } else if (loginTime.isAfter(SHIFT_B_START) && loginTime.isBefore(SHIFT_B_END)) {
            return "Shift B";
        } else if (loginTime.isAfter(GENERAL_SHIFT_START) && loginTime.isBefore(GENERAL_SHIFT_END)) {
            return "General Shift";
        }
        return "Unknown Shift";
    }

    /**
     * Logout the employee and ensure no policy violations.
     */
    public void logoutEmployee(String username) {
        Employee employee = employeeRepository.findByUsername(username);
        if (employee != null) {
            if (isLogoutLocked(employee)) {
                String message = "You are not allowed to logout as you've exceeded the time limit. Please contact admin.";
                logger.warn("User {} attempted to logout but exceeded the time limit.", username);
                throw new IllegalStateException(message);
            }

            employee.setLoggedIn(false);
            employee.setLogoutTimestamp(LocalDateTime.now());
            employeeRepository.save(employee);
            logger.info("User {} logged out successfully at {}", username, employee.getLogoutTimestamp());
        } else {
            logger.warn("Logout attempt for non-existent user: {}", username);
        }
    }

    /**
     * Check if logout is locked due to exceeding max working hours.
     */
    private boolean isLogoutLocked(Employee employee) {
        if (employee.getLoginTimestamp() != null && employee.getLogoutTimestamp() == null) {
            Duration duration = Duration.between(employee.getLoginTimestamp(), LocalDateTime.now());
            long workedHours = duration.toHours();

            if (workedHours >= MAX_WORKING_HOURS) {
                LocalDateTime nextAllowedLogoutTime = employee.getLoginTimestamp().plusHours(COOLDOWN_PERIOD_HOURS);
                return LocalDateTime.now().isBefore(nextAllowedLogoutTime);
            }
        }
        return false;
    }

    /**
     * Find an employee by username.
     */
    public Employee findEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username);
    }
}