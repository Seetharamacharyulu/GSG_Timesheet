package com.timesheet_gsg.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.timesheet_gsg.model.Employee;
import com.timesheet_gsg.model.EmployeeActivity;
import com.timesheet_gsg.service.EmployeeActivityService;
import com.timesheet_gsg.service.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class TimesheetController {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetController.class);

    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private EmployeeActivityService employeeActivityService;

    // Welcome page
    @GetMapping("/")
    public String welcome() {
        logger.info("Accessed the welcome page.");
        return "index";
    }

    @GetMapping("/timesheet")
    public String timesheet() {
        logger.info("Accessed the timesheet page.");
        return "timesheet";
    }

 // Login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

   
    @PostMapping("/login")
    public String login(@RequestParam(name = "username", defaultValue = "") String username,
                        @RequestParam(name = "password", defaultValue = "") String password,
                        Model model, 
                        HttpServletRequest request) {
        logger.info("Login attempt with username: {}", username);

        if (username.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Username and password are required.");
            return "login";
        }

        // Authenticate the user
        Employee employee = employeeService.authenticateEmployee(username, password);

        if (employee != null) {
            logger.debug("Employee authenticated successfully: {}", employee);
            LocalDate today = LocalDate.now();

            // Check the most recent login activity
            EmployeeActivity lastActivity = employeeActivityService.findLatestLoginByUsername(username);

            if (lastActivity != null && lastActivity.getLoginTimestamp() != null 
                    && lastActivity.getLoginTimestamp().toLocalDate().equals(today)) {
                model.addAttribute("error", "You are already logged in today at " 
                    + lastActivity.getLoginTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                model.addAttribute("redirectUrl", "/timesheet");
                logger.warn("Login attempt denied for username {}: Already logged in today at {}", 
                    username, lastActivity.getLoginTimestamp());
                return "login";
            }

            // Capture login details (IP, Hostname, Logged-in User)
            String loginDetails = getLoginPCDetails(request);  // Pass HttpServletRequest to capture client details
            logger.debug("Captured login details: {}", loginDetails);

            String[] details = loginDetails.split(",");
            String ip = "Unknown";
            String hostname = "Unknown";
            String loggedInUser = "Unknown";

            if (details.length >= 3) {
                ip = details[0].replace("IP: ", "").trim();
                hostname = details[1].replace("Hostname: ", "").trim();
                loggedInUser = details[2].replace("Logged-in User: ", "").trim();
            } else {
                logger.warn("Invalid login details format: {}", loginDetails);
            }

            logger.info("Login attempt from IP: {}, Hostname: {}, Logged-in User: {}", ip, hostname, loggedInUser);

            // Set login information in the employee record
            employee.setLoginIP(ip);
            employee.setLoginHostname(hostname);
            employee.setLoginTimestamp(LocalDateTime.now());
            employee.setLoggedIn(true);

            try {
                // Update employee record in the database
                employeeService.updateEmployee(employee);

                // Log the login activity in EmployeeActivity
                EmployeeActivity newActivity = new EmployeeActivity();
                newActivity.setEmployeeId(employee.getEmployeeId());
                newActivity.setUsername(username);
                newActivity.setLoginTimestamp(employee.getLoginTimestamp());
                newActivity.setActivity("Login");
                newActivity.setLoginIP(ip);
                newActivity.setLoginHostname(hostname);

                employeeActivityService.saveEmployeeActivity(newActivity);
            } catch (Exception e) {
                logger.error("Error while updating employee record or saving login activity", e);
                model.addAttribute("error", "Error during login. Please try again later.");
                return "login";
            }

            // Prepare success message and delay redirection
            model.addAttribute("successMessage", "You have successfully logged in at " 
                + employee.getLoginTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) 
                + ". Welcome, " + username + "!");
            model.addAttribute("redirectUrl", "/timesheet");

            logger.info("Login successful for username: {}", username);
            return "login";  // Return login page to show success message before redirecting
        } else {
            model.addAttribute("error", "Invalid username or password.");
            logger.error("Login failed for username: {}", username);
            return "login";
        }
    }

    private String getLoginPCDetails(HttpServletRequest request) {
        // Get the client's IP from the request
        String ipAddress = getClientIP(request);
        
        // Resolve hostname based on the IP address
        String hostname = getClientHostname(ipAddress);
        
        // Get the logged-in user on the server (could be fetched via client-side logic)
        String loggedInUser = System.getProperty("user.name");  // You may need to get the client logged-in user via other means

        logger.info("Login attempt detected on system with IP: {}, Hostname: {}, Logged-in User: {}", ipAddress, hostname, loggedInUser);

        return "IP: " + ipAddress + ", Hostname: " + hostname + ", Logged-in User: " + loggedInUser;
    }

    private String getClientIP(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String getClientHostname(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress.getHostName();  // Return the hostname based on IP
        } catch (UnknownHostException e) {
            logger.error("Error fetching hostname for IP: {}", ipAddress, e);
            return "Unknown Host";
        }
    }




    // Logout page
    @GetMapping("/logout")
    public String showLogoutPage() {
        return "logout"; // Display the logout form
    }

    // Handle logout attempt
    @PostMapping("/logout")
    public String logout(@RequestParam(name = "username", required = false) String username,
                         @RequestParam(name = "password", required = false) String password,
                         RedirectAttributes redirectAttributes) {

        // Only attempt to log out if both username and password are provided
        if (username != null && password != null) {
            logger.info("Logout attempt for user: {}", username);

            // Validate username and password
            Employee employee = employeeService.authenticateEmployee(username, password);
            if (employee == null) {
                // If authentication fails, add error message and redirect
                String errorMessage = "Invalid username or password.";
                redirectAttributes.addFlashAttribute("error", errorMessage);
                logger.warn("Logout failed: Invalid credentials for username: {}", username);
                return "redirect:/logout"; // Redirect back to logout page with error
            }

            // Check if the employee is logged in and if the login timestamp is from today
            LocalDate currentDate = LocalDate.now();

            
         // Check if employee has already logged out today
            if (employee.getLogoutTimestamp() != null && employee.getLogoutTimestamp().toLocalDate().equals(currentDate)) {
                // If employee has already logged out today, show error message with the logout timestamp
                String logoutTimestamp = employee.getLogoutTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String errorMessage = "User " + username + " has already logged out today at " + logoutTimestamp + ".";
                redirectAttributes.addFlashAttribute("error", errorMessage);
                logger.warn("Logout attempt failed for user {}. Already logged out today at {}", username, logoutTimestamp);
                return "redirect:/logout"; // Redirect back to logout page with error
            }


            // Check if employee is logged in today
            if (employee.getLoggedIn() != null && employee.getLoggedIn() &&
                employee.getLoginTimestamp() != null &&
                employee.getLoginTimestamp().toLocalDate().equals(currentDate)) {

                // Log the logout activity
                LocalDateTime logoutTimestamp = LocalDateTime.now();
                employee.setLoggedIn(false); // Set loggedIn to false
                employee.setLogoutTimestamp(logoutTimestamp); // Mark logout timestamp
                employeeService.updateEmployee(employee);

                // Save the logout activity
                EmployeeActivity activity = new EmployeeActivity();
                activity.setEmployeeId(employee.getEmployeeId());
                activity.setUsername(username);
                activity.setActivity("Logout");
                activity.setLogoutTimestamp(logoutTimestamp);
                activity.setLoginIP(employee.getLoginIP());
                activity.setLoginHostname(employee.getLoginHostname());
                employeeActivityService.saveEmployeeActivity(activity);

                // Add success message with username and timestamp
                String timestamp = logoutTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String logoutMessage = "User " + username + " logged out successfully at " + timestamp + ".";
                redirectAttributes.addFlashAttribute("logoutMessage", logoutMessage);
                redirectAttributes.addFlashAttribute("username", username);
                redirectAttributes.addFlashAttribute("logoutTimestamp", timestamp);
                logger.info("User {} logged out successfully at {}", username, timestamp);

            } else {
                // If the user is not logged in today or their session doesn't match, show error
                String errorMessage = "No active session found for this user or login was not on today's date.";
                redirectAttributes.addFlashAttribute("error", errorMessage);
                logger.warn("Logout attempt failed for user {}. No valid session found.", username);
            }

            // Redirect to timesheet page or back to logout page with message
            return "redirect:/timesheet";
        } else {
            // Handle case where username or password are missing on page load (to prevent error display)
            return "redirect:/logout"; // No error unless credentials are provided
        }
    }






    // Password Reset page
    @GetMapping("/passwordReset")
    public String showPasswordResetPage() {
        return "passwordReset";
    }
    @PostMapping("/resetPassword")
    public String resetPassword(@RequestParam String username, @RequestParam String currentPassword,
            @RequestParam String newPassword, @RequestParam String confirmPassword, Model model) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirm password do not match.");
            return "passwordReset";
        }

        Employee employee = employeeService.authenticateEmployee(username, currentPassword);
        if (employee == null) {
            model.addAttribute("error", "Invalid username or current password.");
            return "passwordReset";
        }

        boolean isPasswordChanged = employeeService.changePassword(username, newPassword);
        if (isPasswordChanged) {
            model.addAttribute("message", "Password successfully reset.");
            logger.info("Password reset successful for username: {}", username);
            return "redirect:/login";
        } else {
            model.addAttribute("error", "Failed to reset password.");
            return "passwordReset";
        }
    }

   
}
