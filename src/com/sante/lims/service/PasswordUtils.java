package com.sante.lims.service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    private PasswordUtils() {}

    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    public static boolean verifyPassword(String plainTextPassword, String bcryptHash) {
        if (plainTextPassword == null || bcryptHash == null) {
            return false;
        }
        return BCrypt.checkpw(plainTextPassword, bcryptHash);
    }
}