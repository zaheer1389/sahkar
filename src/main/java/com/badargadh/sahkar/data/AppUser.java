package com.badargadh.sahkar.data;

import com.badargadh.sahkar.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password; // In production, use BCrypt encoding

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50) // Explicitly set length
    private Role role;

    @OneToOne
    @JoinColumn(name = "MemberId")
    private Member member; // The Member record this user belongs to

    public AppUser() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public String getPassword() { return password; }
    public void setPassword(String p) { this.password = p; }
    public Role getRole() { return role; }
    public void setRole(Role r) { this.role = r; }
    public Member getMember() { return member; }
    public void setMember(Member m) { this.member = m; }
}