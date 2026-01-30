package com.badargadh.sahkar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.enums.Role;
import com.badargadh.sahkar.exception.BusinessException;
import com.badargadh.sahkar.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder; // Now resolved!

    @Autowired
    private UserRepository userRepo;
    
    public List<AppUser> findAllUsers() {
    	return userRepo.findAll();
    }

    public AppUser createInitialAdmin() {
        Optional<AppUser> existingAdmin = userRepo.findByUsername("admin");
        
        // Instead of !existingAdmin.isEmpty(), use:
        if (existingAdmin.isEmpty()) {
            AppUser newUser = new AppUser();
            newUser.setUsername("admin");
            
            // Use the BCrypt encoder we set up
            newUser.setPassword(passwordEncoder.encode("sahkar5410$"));
            
            newUser.setRole(Role.ADMIN);
            newUser = userRepo.save(newUser);
            System.out.println("Default Admin created successfully.");
            
            return newUser;
        }
        
        return existingAdmin.get();
    }
    
    /**
     * Authenticates a user by checking the username and hashed password.
     * @return The AppUser if successful, null or throws exception if failed.
     */
    public AppUser authenticate(String username, String rawPassword) {
        System.out.println(username);
        // 1. Find user by username
        Optional<AppUser> userOpt = userRepo.findByUsername(username);

        // 2. Check if user exists (Java 8 compatible)
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            System.out.println(user.getUsername());
            System.out.println(user.getPassword());
            // 3. Compare raw input password with the stored BCrypt hash
            // .matches(raw, encoded) returns true if they match
            System.out.println("password matched ?? "+passwordEncoder.matches(rawPassword, user.getPassword()));
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                return user; // Authentication Successful
            }
        }

        // 4. If user not found or password doesn't match
        throw new BusinessException("Invalid Username or Password!");
    }

    public AppUser saveAppUser(AppUser appUser) {
        // If we're updating a user and the password field hasn't changed (still hashed),
        // don't re-encode it.

        // A quick way to check if it's already a BCrypt hash is looking for the prefix "$2a$"
        String pass = appUser.getPassword();
        if (pass != null && !pass.startsWith("$2a$")) {
            appUser.setPassword(passwordEncoder.encode(pass));
        }

        return userRepo.save(appUser);
    }
    
    public AppUser getAdminUser() {
        return userRepo.findByUsername("admin").get();
    }
}