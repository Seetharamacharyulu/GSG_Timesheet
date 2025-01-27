package com.timesheet_gsg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.timesheet_gsg.Exception.ActivitySaveException;
import com.timesheet_gsg.Repository.EmployeeActivityRepository;
import com.timesheet_gsg.model.EmployeeActivity;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class EmployeeActivityService {
	private static final Logger logger = LoggerFactory.getLogger(EmployeeActivityService.class);
    @Autowired
    private EmployeeActivityRepository employeeActivityRepository;

    // Method to fetch all employee activities
    public List<EmployeeActivity> getAllEmployeeActivities() {
        try {
            logger.info("Fetching all employee activities.");
            return employeeActivityRepository.findAll();  // Assuming findAll() is available in the repository
        } catch (Exception e) {
            logger.error("Error fetching employee activities: {}", e.getMessage());
            throw new RuntimeException("Error fetching employee activities", e);
        }
    }

    @Transactional
    public EmployeeActivity saveEmployeeActivity(EmployeeActivity activity) {
        try {
            logger.info("Attempting to save EmployeeActivity for employee: {} with activity: {}", 
                        activity.getEmployeeId(), activity.getActivity());
            logger.debug("Saving activity: {}", activity);

            EmployeeActivity savedActivity = employeeActivityRepository.save(activity);

            // Check if the activity was saved successfully
            if (savedActivity != null) {
                logger.info("Successfully saved activity: {}", savedActivity);
            } else {
                logger.error("Failed to save activity");
            }

            return savedActivity;
        } catch (Exception e) {
            logger.error("Error while saving activity: {}", e.getMessage());
            throw new ActivitySaveException("Error saving employee activity", e);
        }
    }
    public EmployeeActivity findLatestLoginByUsername(String username) {
        return employeeActivityRepository.findTopByUsernameOrderByLoginTimestampDesc(username);
    }

    }



