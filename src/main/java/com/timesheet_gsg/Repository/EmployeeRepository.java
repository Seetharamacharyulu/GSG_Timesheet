package com.timesheet_gsg.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timesheet_gsg.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // Find an employee by username
    Employee findByUsername(String username);

    // Query to find employees filtered by login timestamp between start and end date
    List<Employee> findByLoginTimestampBetween(LocalDate startDate, LocalDate endDate);

    // Query to find employees by employee ID
    List<Employee> findByEmployeeId(String employeeId);

    // Query to find employees by department (case insensitive search)
    List<Employee> findByDepartmentContainingIgnoreCase(String department);

    // Query to find employees by role (case insensitive search)
    List<Employee> findByRoleContainingIgnoreCase(String role);
    
 // Query to find employees by username (case insensitive and partial match)
    List<Employee> findByUsernameContainingIgnoreCase(String username);
    
 // Find employees by login timestamp between a start and end date
    List<Employee> findByLoginTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
}

