package com.sante.lims.service;

import com.sante.lims.model.User;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

public class EmailService {

    //Configure these with your real SMTP credentials
    private static final String SMTP_HOST     = "smtp.gmail.com";   // e.g. smtp.gmail.com
    private static final int    SMTP_PORT     = 587;
    private static final String SMTP_USERNAME = "your-email@gmail.com";
    private static final String SMTP_PASSWORD = "your-app-password"; // Gmail app password
    private static final String FROM_ADDRESS  = "no-reply@santediagnostics.com";
    //

    private EmailService() {}

    public static void sendVerificationEmail(User user, String verificationToken) throws MessagingException {
        String subject = "Sante Diagnostics – Verify Your Email";
        String body = "Hello " + user.getFullName() + ",\n\n"
                + "Thank you for registering with Sante Diagnostics.\n"
                + "Your email verification token is:\n\n"
                + "    " + verificationToken + "\n\n"
                + "Enter this token on the Verify Email screen to activate your account.\n\n"
                + "Regards,\nSante Diagnostics Team";
        send(user.getEmail(), subject, body);
    }

    public static void sendResultReadyEmail(User customer, String testName) throws MessagingException {
        String subject = "Sante Diagnostics – Your Result is Ready";
        String body = "Hello " + customer.getFullName() + ",\n\n"
                + "Great news! Your result for the following test is now ready:\n\n"
                + "    Test: " + testName + "\n\n"
                + "Please log in to your Sante Diagnostics account to view and download your result.\n\n"
                + "Regards,\nSante Diagnostics Team";
        send(customer.getEmail(), subject, body);
    }

    private static void send(String to, String subject, String body) throws MessagingException {
        Session session = createMailSession();
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_ADDRESS));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body, "UTF-8", "plain");
        Transport.send(message);
    }

    private static Session createMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }
}