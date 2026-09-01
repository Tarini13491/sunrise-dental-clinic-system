package com.sunrisedental.service;

import java.util.List;
import java.util.Map;

public class HelpService {

    public List<Map<String, String>> steps() {
        return List.of(
                Map.of("id", "login", "title", "Sign in",
                        "body", "Open the staff desk and enter the username and password issued by the clinic administrator. Tick “Keep me signed in on this clinic PC” only on a reception computer — never on a shared browser at home."),
                Map.of("id", "register", "title", "Register a new appointment",
                        "body", "From the top menu choose Register. Enter patient name, address and contact number, pick the dentist and treatment, then choose a free date and time. Occupied chair slots are marked “booked” so you cannot double-book. The system assigns a unique appointment number such as SDC-20260824-0001. A confirmation email and SMS are created automatically."),
                Map.of("id", "patients", "title", "Register and manage patients",
                        "body", "Open Patients to keep the clinic file: name, address, phone, email, date of birth and notes. Staff can add a new file, update details, or remove a file that has no appointments. This does not replace booking a visit — use Register when the patient needs a chair time."),
                Map.of("id", "search", "title", "Display appointment details",
                        "body", "Choose Search and type the appointment number, the patient’s name, or their phone number. The screen shows the full record: address, dentist, treatment, date, time, notes and any bill that already exists. Use Cancel this visit only when the patient has asked to postpone."),
                Map.of("id", "bill", "title", "Calculate and print a bill",
                        "body", "Open Billing, load the appointment number, review the consultation fee plus treatment cost (including VAT and any paediatric discount or emergency surcharge), then choose Calculate and save bill. Use Print receipt for the patient copy. Record cash or card when payment is taken. Each visit can have only one bill."),
                Map.of("id", "reports", "title", "Management reports",
                        "body", "Open Reports to see the daily clinic summary, dentist workload, treatment mix, monthly collections and the audit trail. These numbers come from MySQL views and stored procedures so the practice manager can decide staffing and stock from facts, not from a paper diary."),
                Map.of("id", "staff", "title", "Staff accounts (administrator only)",
                        "body", "Only Admin can open Staff. Create a username and password for each Staff member, update their details, block an account so they cannot sign in, or remove it. Staff members cannot manage users. A new Staff account signs in on the same staff portal."),
                Map.of("id", "alerts", "title", "Email and SMS alerts",
                        "body", "Every booking, cancellation, bill and payment writes an email and an SMS row. Open Alerts to show a patient the confirmation that was sent. DEMO mode stores the exact message text so you can mark this feature without a paid SMS gateway."),
                Map.of("id", "exit", "title", "Exit the system",
                        "body", "Always choose Exit on the top right before leaving the desk. This closes the HTTP session on the server and clears the remember-me cookie so the next staff member must log in.")
        );
    }
}