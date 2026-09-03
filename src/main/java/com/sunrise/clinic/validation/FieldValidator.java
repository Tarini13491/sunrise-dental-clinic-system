package com.sunrise.clinic.validation;

import com.sunrise.clinic.exception.ValidationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public final class FieldValidator {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,29}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,98}[A-Za-z]$");
    private static final Pattern CONTACT = Pattern.compile("^0[0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern APPOINTMENT_NUMBER = Pattern.compile("^APT-[0-9]{8}-[0-9]{4}$");
    private static final LocalTime OPENING = LocalTime.of(8, 0);
    private static final LocalTime LAST_SLOT = LocalTime.of(17, 30);

    private FieldValidator() {
    }

    public static String required(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static String username(String value) {
        String username = required(value, "Username");
        if (!USERNAME.matcher(username).matches()) {
            throw new ValidationException("Username must start with a letter and contain 4 to 30 letters, numbers, or underscores.");
        }
        return username;
    }

    public static String password(String value) {
        String password = required(value, "Password");
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (!password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit)) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
        return password;
    }

    public static String optionalPassword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return password(value);
    }

    public static String personName(String value, String fieldName) {
        String name = required(value, fieldName);
        if (name.length() < 2 || name.length() > 100 || !PERSON_NAME.matcher(name).matches()) {
            throw new ValidationException(fieldName + " may contain letters, spaces, hyphens, and apostrophes only.");
        }
        return name;
    }

    public static String email(String value) {
        String email = required(value, "Email").toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        return email;
    }

    public static String contactNumber(String value) {
        String contact = required(value, "Contact number").replace(" ", "");
        if (!CONTACT.matcher(contact).matches()) {
            throw new ValidationException("Contact number must be 10 digits and start with 0.");
        }
        return contact;
    }

    public static String address(String value) {
        String address = required(value, "Address");
        if (address.length() < 5 || address.length() > 255) {
            throw new ValidationException("Address must be between 5 and 255 characters.");
        }
        return address;
    }

    public static int age(String value) {
        String raw = required(value, "Age");
        int age;
        try {
            age = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new ValidationException("Age must be a whole number.");
        }
        if (age < 1 || age > 120) {
            throw new ValidationException("Age must be between 1 and 120.");
        }
        return age;
    }

    public static int age(Integer value) {
        if (value == null) {
            throw new ValidationException("Age is required.");
        }
        return age(String.valueOf(value));
    }

    public static String appointmentNumber(String value) {
        String number = required(value, "Appointment number").toUpperCase();
        if (!APPOINTMENT_NUMBER.matcher(number).matches()) {
            throw new ValidationException("Enter a valid appointment number, for example APT-20260901-0001.");
        }
        return number;
    }

    public static LocalDate appointmentDate(String value) {
        String raw = required(value, "Appointment date");
        LocalDate date;
        try {
            date = LocalDate.parse(raw);
        } catch (Exception exception) {
            throw new ValidationException("Appointment date must use the YYYY-MM-DD format.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("Appointment date cannot be in the past.");
        }
        if (date.isAfter(LocalDate.now().plusYears(1))) {
            throw new ValidationException("Appointments can be booked up to one year in advance.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ValidationException("The clinic is closed on Sundays.");
        }
        return date;
    }

    public static LocalTime appointmentTime(String value) {
        String raw = required(value, "Appointment time");
        LocalTime time;
        try {
            time = LocalTime.parse(raw);
        } catch (Exception exception) {
            throw new ValidationException("Appointment time must use the HH:MM format.");
        }
        if (time.isBefore(OPENING) || time.isAfter(LAST_SLOT)) {
            throw new ValidationException("Appointment time must be between 08:00 and 17:30.");
        }
        if (time.getMinute() != 0 && time.getMinute() != 30) {
            throw new ValidationException("Appointments are booked in 30-minute slots.");
        }
        return time;
    }

    public static boolean isValidAppointmentNumberFormat(String value) {
        return value != null && APPOINTMENT_NUMBER.matcher(value.trim().toUpperCase()).matches();
    }
}
