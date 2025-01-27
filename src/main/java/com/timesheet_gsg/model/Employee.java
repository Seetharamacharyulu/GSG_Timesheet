package com.timesheet_gsg.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employeeId", nullable = false, length = 50)
    private String employeeId;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "shift", length = 20)
    private String shift;

    @Column(name = "loggedIn", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean loggedIn = false;

    @Column(name = "login_timestamp")
    private LocalDateTime loginTimestamp;

    @Column(name = "logout_timestamp")
    private LocalDateTime logoutTimestamp;

    @Column(name = "loginIP", length = 255)
    private String loginIP;

    @Column(name = "loginHostname", length = 255)
    private String loginHostname;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public Boolean getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(Boolean loggedIn) {
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
