package com.sante.lims.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {

    public enum Role { SUPER_ADMIN, LAB_ATTENDANT, CUSTOMER }

    private UUID          id;
    private String        fullName;
    private String        email;
    private String        passwordHash;
    private Role          role;
    private boolean       emailVerified;
    private String        verifyToken;
    private boolean       forcePwChange;
    private UUID          createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    // ── Getters ─────────────────────────────────────────────────
    public UUID          getId()           { return id; }
    public String        getFullName()     { return fullName; }
    public String        getEmail()        { return email; }
    public String        getPasswordHash() { return passwordHash; }
    public Role          getRole()         { return role; }
    public boolean       isEmailVerified() { return emailVerified; }
    public String        getVerifyToken()  { return verifyToken; }
    public boolean       isForcePwChange() { return forcePwChange; }
    public UUID          getCreatedBy()    { return createdBy; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }

    // ── Setters ─────────────────────────────────────────────────
    public void setId(UUID id)                       { this.id = id; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setEmail(String email)               { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role)                   { this.role = role; }
    public void setEmailVerified(boolean v)          { this.emailVerified = v; }
    public void setVerifyToken(String t)             { this.verifyToken = t; }
    public void setForcePwChange(boolean f)          { this.forcePwChange = f; }
    public void setCreatedBy(UUID createdBy)         { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
    public void setUpdatedAt(LocalDateTime t)        { this.updatedAt = t; }
}
