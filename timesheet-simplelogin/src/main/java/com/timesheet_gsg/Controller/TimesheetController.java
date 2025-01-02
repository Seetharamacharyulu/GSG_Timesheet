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
        
        Employee employee = timesheetService.authenticateEmployee(username, password);

        if (employee != null) {
            session.setAttribute("username", username); // Store username in session
            model.addAttribute("username", username);
            model.addAttribute("message", "Welcome, " + username + "! Have a productive day!");

            logger.info("Login successful for username: {}", username);
            
            if ("ADMIN".equals(employee.getRole())) {
                return "adminDashboard";
            }
            return "loggedIn";
        }

        logger.error("Login failed for username: {}", username);
        model.addAttribute("error", "Invalid username or password.");
        return "timesheet";
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
