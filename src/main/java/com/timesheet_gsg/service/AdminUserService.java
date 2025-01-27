package com.timesheet_gsg.service;

import com.timesheet_gsg.Repository.AdminUserRepository;
import com.timesheet_gsg.model.AdminUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminUserService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    // Authenticate admin login by username and password (without hashing)
    public boolean authenticateAdmin(String username, String password) {
        Optional<AdminUser> adminOptional = adminUserRepository.findByUsername(username);

        // Check if admin exists and if the passwords match
        return adminOptional.isPresent() && adminOptional.get().getPassword().equals(password);
    }

}
