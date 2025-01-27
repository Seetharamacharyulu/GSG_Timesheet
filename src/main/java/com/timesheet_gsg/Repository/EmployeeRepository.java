package com.timesheet_gsg.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.timesheet_gsg.model.Employee;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

	// Find an employee by username
	Employee findByUsername(String username);

	// Find an employee by username (case-insensitive)
	Employee findByUsernameIgnoreCase(String username); // Case-insensitive search

	// Query to find employees filtered by login timestamp between start and end
	// date
	List<Employee> findByLoginTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

	// Query to find employees by employee ID
	List<Employee> findByEmployeeId(String employeeId);

	// Query to find employees by department (case-insensitive search)
	List<Employee> findByDepartmentContainingIgnoreCase(String department);

	// Query to find employees by role (case-insensitive search)
	List<Employee> findByRoleContainingIgnoreCase(String role);

	// Query to find employees by username (case-insensitive and partial match)
	List<Employee> findByUsernameContainingIgnoreCase(String username);

	// Delete an employee by employee ID
	void deleteByEmployeeId(String employeeId);

	// Delete an employee by username
	// void deleteByUsername(String username);

	// Custom method to delete multiple employees by their usernames
	//void deleteByUsernameIn(List<String> usernames);

	// Bulk delete employees by list of IDs
	void deleteByIdIn(List<Integer> ids);

	// Find employee by username and employeeId as strings
	Employee findByUsernameAndEmployeeId(String username, String employeeId);

	// Check if an employee exists by username
	boolean existsByUsername(String username);

	// Check if an employee exists by employeeId (this method was missing earlier)
	boolean existsByEmployeeId(String employeeId); // Make sure this is here

	// Custom find all method (unnecessary as it's already inherited from
	// JpaRepository)
	List<Employee> findAll(); // Explicitly added, though JpaRepository already provides it
	 @Transactional
	    void deleteByUsernameIn(List<String> usernames);
}
