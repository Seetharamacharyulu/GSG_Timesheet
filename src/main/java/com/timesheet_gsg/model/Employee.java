package com.timesheet_gsg.model;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String role;
    private boolean loggedIn;
    private LocalDateTime loginTimestamp;
    private LocalDateTime logoutTimestamp;

    // New fields for employee details
    private String employeeId;   // Employee ID
    private String department;    // Employee Department

    // New field for tracking employee's shift
    private String shift;

    // New field for storing PC details during login
    private String loginPCDetails;
    
    // New fields for storing login-related details
    private String loginIP;
    private String loginHostname;

    // Getter and Setter for employeeId
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    // Getter and Setter for department
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // Getters and setters for loginPCDetails, loginIP, and loginHostname
    public String getLoginPCDetails() {
        return loginPCDetails;
    }

    public void setLoginPCDetails(String loginPCDetails) {
        this.loginPCDetails = loginPCDetails;
    }

    public String getLoginIP() {
        return loginIP;
    }

    public void setLoginIP(String loginIP) {
        this.loginIP = loginIP;
    }

    public String getLoginHostname() {
        return loginHostname;
    }

    public void setLoginHostname(String loginHostname) {
        this.loginHostname = loginHostname;
    }

    // Getter and Setter for shift
    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    // Getter and Setter methods for existing fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public LocalDateTime getLoginTimestamp() {
        return loginTimestamp;
    }

    public void setLoginTimestamp(LocalDateTime loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
    }

    public LocalDateTime getLogoutTimestamp() {
        return logoutTimestamp;
    }

    public void setLogoutTimestamp(LocalDateTime logoutTimestamp) {
        this.logoutTimestamp = logoutTimestamp;
    }
}
