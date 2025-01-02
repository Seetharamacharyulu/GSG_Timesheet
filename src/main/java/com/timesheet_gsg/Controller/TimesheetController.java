package com.timesheet_gsg.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.timesheet_gsg.model.Employee;
import com.timesheet_gsg.service.TimesheetService;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
public class TimesheetController {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetController.class);  // Logger initialization

    @Autowired
    private TimesheetService timesheetService;

    // Welcome page with Timesheet and Admin buttons
    @GetMapping("/")
    public String welcome() {
        logger.info("Accessed the welcome page.");
        return "index";
    }

    // Timesheet login page
    @GetMapping("/timesheet")
    public String timesheet() {
        logger.info("Accessed the timesheet login page.");
        return "timesheet";
    }

    // Login functionality
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        logger.info("Login attempt with username: {}", username);
        
        // Check if the employee is already logged in
        LocalDateTime lastLoginTime = (LocalDateTime) session.getAttribute("lastLoginTime");
        if (lastLoginTime != null) {
            long hoursSinceLastLogin = ChronoUnit.HOURS.between(lastLoginTime, LocalDateTime.now());
            if (hoursSinceLastLogin < 5) {
                // If the last login was within 5 hours, show the message and return to the login page
                model.addAttribute("error", "You are already logged in. Are you trying to log in again?");
                return "timesheet";
            }
        }
        
        // Authenticate employee
        Employee employee = timesheetService.authenticateEmployee(username, password);

        if (employee != null) {
            session.setAttribute("username", username); // Store username in session
            session.setAttribute("role", employee.getRole()); // Store role in session (if needed)
            session.setAttribute("lastLoginTime", LocalDateTime.now()); // Store the current login time

            model.addAttribute("username", username);
            model.addAttribute("message", "Welcome, " + username + "! Have a productive day!");

            logger.info("Login successful for username: {}", username);
            
            if ("ADMIN".equals(employee.getRole())) {
                return "adminDashboard"; // Redirect to Admin Dashboard
            }
            return "loggedIn"; // Redirect to User Dashboard
        } else {
            model.addAttribute("error", "Invalid username or password.");
            logger.error("Login failed for username: {}", username);
            return "timesheet"; // Return to login page on failure
        }
    }

    // Logout functionality
    @PostMapping("/logout")
    public String logout(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        logger.info("Attempting to log out user: {}", username);

        if (username != null) {
            timesheetService.logoutEmployee(username);
            session.invalidate(); // Invalidate session
            model.addAttribute("logoutMessage", "You have successfully logged out. Thank you!");
            logger.info("User {} logged out successfully.", username);
        } else {
            model.addAttribute("error", "Logout attempt for non-existent user.");
            logger.warn("Logout attempt for non-existent user.");
        }

        return "redirect:/timesheet"; // Redirect to login page
    }
}
