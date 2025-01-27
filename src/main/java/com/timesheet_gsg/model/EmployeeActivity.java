package com.timesheet_gsg.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_activity")
public class EmployeeActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Primary Key

    @Column(name = "employee_id", nullable = false, length = 50)
    private String employeeId;  // Employee ID (matches Employee table's employee_id)

    @Column(name = "username", nullable = false, length = 50)
    private String username;  // Username associated with the activity

    @Column(name = "login_timestamp", columnDefinition = "DATETIME(6)")
    private LocalDateTime loginTimestamp;  // Login timestamp

    @Column(name = "logout_timestamp", columnDefinition = "DATETIME(6)")
    private LocalDateTime logoutTimestamp;  // Logout timestamp

    @Column(name = "activity", length = 255)
    private String activity;  // Description of the activity

    @Column(name = "login_ip", length = 255)
    private String loginIP;  // IP address from where the user logged in

    @Column(name = "login_hostname", length = 255)
    private String loginHostname;  // Hostname from where the user logged in

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
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
}
