package com.sante.lims.service;

import com.sante.lims.dao.UserDAO;
import com.sante.lims.model.User;
import jakarta.mail.MessagingException;

import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public User authenticate(String email, String password) throws AuthenticationException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Email and password are required.");
        }

        Optional<User> optionalUser = userDAO.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = optionalUser.get();
        if (!PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            throw new AuthenticationException("Please verify your email before logging in.");
        }

        return user;
    }

    public void changePassword(User user, String newPassword) throws AuthenticationException {
        if (user == null || user.getId() == null) {
            throw new AuthenticationException("Unable to change password for an unknown user.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new AuthenticationException("New password cannot be empty.");
        }

        String hash = PasswordUtils.hashPassword(newPassword);
        boolean updated = userDAO.updatePassword(user.getId(), hash);
        if (!updated) {
            throw new AuthenticationException("Failed to save the new password. Try again later.");
        }

        user.setPasswordHash(hash);
        user.setForcePwChange(false);
    }

    public User registerCustomer(String fullName, String email, String password) throws AuthenticationException {
        if (fullName == null || fullName.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Full name, email, and password are required.");
        }

        if (!email.contains("@") || !email.contains(".")) {
            throw new AuthenticationException("Enter a valid email address.");
        }

        if (password.length() < 8) {
            throw new AuthenticationException("Password must be at least 8 characters.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.findByEmail(normalizedEmail).isPresent()) {
            throw new AuthenticationException("An account with that email already exists.");
        }

        String hash = PasswordUtils.hashPassword(password);
        String verifyToken = java.util.UUID.randomUUID().toString();
        User user = userDAO.createCustomer(fullName, normalizedEmail, hash, verifyToken)
                .orElseThrow(() -> new AuthenticationException("Unable to complete registration. Try again."));

        System.out.println("[AuthService] Verify token for " + user.getEmail() + ": " + verifyToken);

        return user;
    }

    public void verifyEmail(String email, String token) throws AuthenticationException {
        if (email == null || email.isBlank() || token == null || token.isBlank()) {
            throw new AuthenticationException("Email and verification token are required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> optionalUser = userDAO.findByVerifyToken(normalizedEmail, token);
        if (optionalUser.isEmpty()) {
            throw new AuthenticationException("Invalid email or verification token.");
        }

        User user = optionalUser.get();
        if (user.isEmailVerified()) {
            throw new AuthenticationException("Email is already verified.");
        }

        boolean verified = userDAO.verifyEmail(user.getId());
        if (!verified) {
            throw new AuthenticationException("Unable to verify email at this time. Try again later.");
        }
    }
}
