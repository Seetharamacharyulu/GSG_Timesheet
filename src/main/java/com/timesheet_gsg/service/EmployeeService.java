package com.timesheet_gsg.service;

import com.timesheet_gsg.Repository.EmployeeActivityRepository;
import com.timesheet_gsg.Repository.EmployeeRepository;
import com.timesheet_gsg.model.Employee;
import com.timesheet_gsg.model.EmployeeActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Service
public class EmployeeService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);
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
	@Autowired
	private EmployeeRepository employeeRepository;

	// Fetch all employees
	public List<Employee> getAllEmployees() {
		return employeeRepository.findAll();
	}

	// Add new employee
	public void addNewEmployee(Employee employee) {
		if (employeeRepository.existsByUsername(employee.getUsername())) {
			throw new IllegalArgumentException("Username already exists: " + employee.getUsername());
		}
		employeeRepository.save(employee);
	}

	public Employee authenticateEmployee(String username, String password) {
		// Attempt to find employee by username
		Employee employee = employeeRepository.findByUsername(username);

		// Check if the employee exists
		if (employee != null) {
			// Check if the password matches
			if (employee.getPassword().equals(password)) {
				// Set logged-in status and capture login timestamp
				employee.setLoggedIn(true);
				employee.setLoginTimestamp(LocalDateTime.now()); // Capture current date and time as login timestamp

				// Save employee with the updated login timestamp
				employeeRepository.save(employee);

				// Log successful login
				logger.info("Login success for user: {} at {}", username, employee.getLoginTimestamp());
				return employee; // Return authenticated employee
			}
		}

		// Log failed authentication attempt
		logger.warn("Login failed for user: {}", username);
		return null; // Return null if authentication fails
	}

	// `canLogin` method removed or kept as a placeholder
	// If you decide to remove it, simply omit the `canLogin` method from your code.

	// Change the password for an employee
	public boolean changePassword(String username, String newPassword) {
		logger.info("Changing password for user: {}", username);
		Employee employee = employeeRepository.findByUsername(username); // Find the employee by username
		if (employee != null) {
			employee.setPassword(newPassword); // Set the new password
			employeeRepository.save(employee); // Save the updated employee record
			logger.info("Password changed successfully for employee: {}", username);
			return true;
		}
		return false;
	}

	// Delete a single employee by ID
	public void deleteEmployeeById(int id) {
		employeeRepository.deleteById(id);
	}

	// Delete multiple employees by IDs
	public void deleteEmployeesByIds(List<Integer> ids) {
		employeeRepository.deleteByIdIn(ids);
	}

	public boolean usernameExists(String username) {
		return employeeRepository.findByUsername(username) != null;
	}

	@Transactional
	public boolean deleteEmployeesByEmployeeId(List<String> employeeIds) {
		try {
			for (String employeeId : employeeIds) {
				logger.info("Attempting to delete employee with ID: {}", employeeId);
				employeeRepository.deleteByEmployeeId(employeeId); // Delete employee by employeeId
				logger.info("Deleted employee with ID: {}", employeeId);
			}
			return true;
		} catch (Exception e) {
			logger.error("Error occurred while deleting employees: {}", e.getMessage());
			return false;
		}
	}

	// Method to reset password for an employee by username
	public boolean resetPassword(String username, String newPassword) {
		Employee employee = employeeRepository.findByUsername(username);
		if (employee != null) {
			// Set the new password
			employee.setPassword(newPassword);
			employeeRepository.save(employee);
			return true; // Return true if password reset is successful
		}
		return false; // Return false if employee not found
	}

	// Helper method to generate a random password
	public String generateRandomPassword() {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
		StringBuilder password = new StringBuilder();
		Random rand = new Random();
		for (int i = 0; i < 8; i++) { // Password length of 8
			password.append(characters.charAt(rand.nextInt(characters.length())));
		}
		return password.toString();
	}

	public void logoutEmployee(String username) {
		// Fetch employee by username
		Employee employee = employeeRepository.findByUsername(username);

		if (employee != null) {
			// Check if logout is locked due to exceeding time limit
			if (isLogoutLocked(employee)) {
				String message = "You are not allowed to logout as you've exceeded the time limit. Please contact admin to know more.";
				logger.warn("User {} attempted to logout but exceeded the time limit. Message: {}", username, message);
				throw new IllegalStateException(message);
			}

			// Proceed with logout process
			employee.setLoggedIn(false); // Mark the employee as logged out
			employee.setLogoutTimestamp(LocalDateTime.now()); // Capture the logout timestamp

			// Save the updated employee with the logout timestamp
			employeeRepository.save(employee);

			// Log the successful logout with timestamp
			logger.info("User {} logged out successfully at {}", username, employee.getLogoutTimestamp());
		} else {
			// Log failure if employee is not found
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

	public void updateEmployee(Employee employee) {
		employeeRepository.save(employee); // Save or update the employee in the database
	}

	public void updateEmployeePassword(String username, String newPassword) {
		Employee employee = employeeRepository.findByUsername(username);
		if (employee != null) {
			employee.setPassword(newPassword);
			employeeRepository.save(employee); // Save the updated password in the database
		}
	}

	public Employee getEmployeeByUsername(String username) {
		return employeeRepository.findByUsername(username); // Adjust based on your repository's method
	}

	private final EmployeeActivityRepository employeeActivityRepository;

	@Autowired
	public EmployeeService(EmployeeActivityRepository employeeActivityRepository) {
		this.employeeActivityRepository = employeeActivityRepository;
	}

	// Method to fetch all employee activities
	public List<EmployeeActivity> getAllEmployeeActivities() {
		return employeeActivityRepository.findAll(); // Fetch all activities
	}

	public LocalDateTime getLatestLoginTimestamp(String username) {
		// Query the database to fetch the latest login timestamp for the user
		// You may need to write a custom query to get this information
		Employee employee = employeeRepository.findByUsername(username);
		return employee != null ? employee.getLoginTimestamp() : null;
	}

	public void updateLoginTimestamp(Employee employee) {
		// Update the login timestamp to the current time
		employee.setLoginTimestamp(LocalDateTime.now());

		// Save the updated employee entity to the database
		employeeRepository.save(employee);
	}

	public Employee getEmployeeByUsernameAndId(String username, String employeeId) {
		// Implement logic to find employee by both username and employeeId
		return employeeRepository.findByUsernameAndEmployeeId(username, employeeId);
	}
}
