package com.timesheet_gsg.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.timesheet_gsg.model.EmployeeActivity;

public interface EmployeeActivityRepository extends JpaRepository<EmployeeActivity, Integer> {
   EmployeeActivity findTopByUsernameOrderByLoginTimestampDesc(String username);
   
}

