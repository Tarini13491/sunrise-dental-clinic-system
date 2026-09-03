let currentUser = null;
let currentView = "dashboard";
let clinicLists = {
  patients: [],
  treatments: [],
  dentists: [],
  openAppointments: [],
  searchHits: []
};

const NAV = {
  ADMIN: [
    ["dashboard", "Dashboard"],
    ["staff", "Staff"],
    ["dentists", "Dentists"],
    ["patients", "Patients"],
    ["appointments", "Appointments"],
    ["search", "Appointment search"],
    ["billing", "Billing"],
    ["reports", "Reports"],
    ["help", "Help"]
  ],
  STAFF: [
    ["dashboard", "Dashboard"],
    ["patients", "Patients"],
    ["appointments", "Appointments"],
    ["search", "Appointment search"],
    ["billing", "Billing"],
    ["reports", "Reports"],
    ["help", "Help"]
  ]
};

const TITLES = {
  dashboard: "Dashboard",
  staff: "Staff management",
  dentists: "Dentists",
  patients: "Patients",
  appointments: "Appointments",
  search: "Appointment details",
  billing: "Billing",
  reports: "Reports",
  help: "Help"
};

function isAdmin() {
  return currentUser && currentUser.role === "ADMIN";
}

function isStaff() {
  return currentUser && currentUser.role === "STAFF";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function money(value) {
  const amount = Number(value || 0);
  return "LKR " + amount.toFixed(2);
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.hidden = false;
  setTimeout(() => { toast.hidden = true; }, 2800);
}

function closeDrawer() {
  const drawer = document.getElementById("drawer");
  drawer.classList.remove("is-open");
  drawer.hidden = true;
  drawer.setAttribute("hidden", "hidden");
}

function openDrawer(title, html) {
  const drawer = document.getElementById("drawer");
  document.getElementById("drawer-title").textContent = title;
  document.getElementById("drawer-body").innerHTML = html;
  drawer.removeAttribute("hidden");
  drawer.hidden = false;
  drawer.classList.add("is-open");
}

function initials(name) {
  return String(name || "?")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map(part => part[0].toUpperCase())
    .join("");
}

function renderNav() {
  const items = NAV[currentUser.role] || NAV.STAFF;
  document.getElementById("side-nav").innerHTML = items.map(([id, label]) =>
    `<button class="nav-btn ${id === currentView ? "active" : ""}" data-view="${id}" type="button">${label}</button>`
  ).join("");
  document.querySelectorAll(".nav-btn").forEach(button => {
    button.addEventListener("click", () => showView(button.dataset.view));
  });
}

async function showView(view) {
  currentView = view;
  document.getElementById("page-title").textContent = TITLES[view];
  renderNav();
  const content = document.getElementById("content");
  content.innerHTML = `<div class="card"><p class="muted">Loading...</p></div>`;
  try {
    if (view === "dashboard") await renderDashboard(content);
    if (view === "staff") await renderStaff(content);
    if (view === "dentists") await renderDentists(content);
    if (view === "patients") await renderPatients(content);
    if (view === "appointments") await renderAppointments(content);
    if (view === "search") await renderSearch(content);
    if (view === "billing") await renderBilling(content);
    if (view === "reports") await renderReports(content);
    if (view === "help") renderHelp(content);
  } catch (error) {
    content.innerHTML = `<div class="card notice notice-error">${escapeHtml(error.message)}</div>`;
  }
}

async function renderDashboard(content) {
  const summary = await api("/api/reports/summary");
  content.innerHTML = `
    <section class="stats">
      <article class="card"><p class="eyebrow">Patients</p><div class="stat-value">${summary.patientCount}</div></article>
      <article class="card"><p class="eyebrow">Appointments</p><div class="stat-value">${summary.appointmentCount}</div></article>
      <article class="card"><p class="eyebrow">Today</p><div class="stat-value">${summary.todayAppointmentCount}</div></article>
      <article class="card"><p class="eyebrow">Revenue</p><div class="stat-value">${money(summary.totalRevenue)}</div></article>
    </section>
    <section class="card">
      <h3>${isAdmin() ? "Administrator workspace" : "Front-desk workspace"}</h3>
      <p class="muted">${isAdmin()
        ? "Use this workspace to look after staff accounts, keep the dentist list up to date, and review how the clinic is running. Day-to-day patient care, appointments, and billing stay with the front desk."
        : "Register patients, book appointments with a clinic dentist, calculate bills, and keep clinic records accurate."}</p>
    </section>
  `;
}

async function renderStaff(content) {
  if (!isAdmin()) {
    content.innerHTML = `<div class="card notice notice-error">Staff management is available to administrators only.</div>`;
    return;
  }
  const staff = await api("/api/staff");
  content.innerHTML = `
    <div class="toolbar">
      <h3>Staff accounts</h3>
      <button class="btn btn-primary" id="new-staff" data-action="register-staff" type="button">Register staff</button>
    </div>
    <section class="card">${staffTable(staff)}</section>
  `;
}

function staffTable(staff) {
  if (!staff.length) {
    return `<p class="empty">No staff accounts yet. Register the first staff member to begin clinic operations.</p>`;
  }
  return `<div class="table-wrap"><table>
    <thead><tr><th>Name</th><th>Username</th><th>Contact</th><th>Status</th><th></th></tr></thead>
    <tbody>${staff.map(member => `
      <tr>
        <td>${escapeHtml(member.fullName)}<br><small class="muted">${escapeHtml(member.email)}</small></td>
        <td>${escapeHtml(member.username)}</td>
        <td>${escapeHtml(member.contactNumber)}</td>
        <td><span class="badge ${member.status === "ACTIVE" ? "active" : "blocked"}">${escapeHtml(member.status)}</span></td>
        <td class="row-actions">
          <button class="btn btn-secondary" data-action="edit-staff" data-id="${member.userId}" type="button">Update</button>
          ${member.status === "ACTIVE"
            ? `<button class="btn btn-danger" data-action="block-staff" data-id="${member.userId}" type="button">Block</button>`
            : `<button class="btn btn-secondary" data-action="activate-staff" data-id="${member.userId}" type="button">Activate</button>`}
        </td>
      </tr>`).join("")}
    </tbody></table></div>`;
}

function staffForm(member) {
  openDrawer(member ? "Update staff" : "Register staff", `
    <form id="staff-form">
      ${member ? "" : `<label class="field"><span>Username</span><input name="username" required></label>
      <label class="field"><span>Password</span><input name="password" type="password" required></label>`}
      <label class="field"><span>Full name</span><input name="fullName" value="${escapeHtml(member?.fullName || "")}" required></label>
      <label class="field"><span>Email</span><input name="email" type="email" value="${escapeHtml(member?.email || "")}" required></label>
      <label class="field"><span>Contact number</span><input name="contactNumber" value="${escapeHtml(member?.contactNumber || "")}" required></label>
      ${member ? `<label class="field"><span>New password (optional)</span><input name="password" type="password"></label>` : ""}
      <div class="notice notice-error" id="form-error" hidden></div>
      <button class="btn btn-primary" type="submit">${member ? "Save changes" : "Create staff account"}</button>
    </form>
  `);
  document.getElementById("staff-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const body = Object.fromEntries(form.entries());
    const errorBox = document.getElementById("form-error");
    try {
      if (member) {
        await api("/api/staff/" + member.userId, { method: "PUT", body });
        showToast("Staff details were updated.");
      } else {
        await api("/api/staff", { method: "POST", body });
        showToast("Staff account was created.");
      }
      closeDrawer();
      showView("staff");
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

async function renderDentists(content) {
  if (!isAdmin()) {
    content.innerHTML = `<div class="card notice notice-error">Dentist management is available to administrators only.</div>`;
    return;
  }
  const dentists = await api("/api/dentists");
  content.innerHTML = `
    <div class="toolbar">
      <h3>Clinic dentists</h3>
      <button class="btn btn-primary" data-action="register-dentist" type="button">Add dentist</button>
    </div>
    <section class="card">${dentistTable(dentists)}</section>
  `;
}

function dentistTable(dentists) {
  if (!dentists.length) {
    return `<p class="empty">No dentists are on the clinic list yet. Add a dentist so staff can assign one when registering appointments.</p>`;
  }
  return `<div class="table-wrap"><table>
    <thead><tr><th>Dentist name</th><th>Status</th><th></th></tr></thead>
    <tbody>${dentists.map(dentist => `
      <tr>
        <td>${escapeHtml(dentist.fullName)}</td>
        <td><span class="badge ${dentist.status === "ACTIVE" ? "active" : "blocked"}">${escapeHtml(dentist.status)}</span></td>
        <td class="row-actions">
          ${dentist.status === "ACTIVE"
            ? `<button class="btn btn-danger" data-action="remove-dentist" data-id="${dentist.dentistId}" type="button">Remove</button>`
            : `<button class="btn btn-secondary" data-action="restore-dentist" data-id="${dentist.dentistId}" type="button">Restore</button>`}
        </td>
      </tr>`).join("")}
    </tbody></table></div>`;
}

function dentistForm() {
  openDrawer("Add dentist", `
    <form id="dentist-form">
      <label class="field"><span>Dentist name</span><input name="fullName" required></label>
      <p class="muted">Dentists are listed for appointment booking only. They cannot sign in to this system.</p>
      <div class="notice notice-error" id="form-error" hidden></div>
      <button class="btn btn-primary" type="submit">Save dentist</button>
    </form>
  `);
  document.getElementById("dentist-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const errorBox = document.getElementById("form-error");
    try {
      await api("/api/dentists", { method: "POST", body: { fullName: form.get("fullName") } });
      showToast("Dentist was added to the clinic list.");
      closeDrawer();
      showView("dentists");
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

async function handleWorkspaceClick(event) {
  const button = event.target.closest("[data-action]");
  if (!button) {
    return;
  }
  const action = button.dataset.action;
  const id = button.dataset.id;
  try {
    if (action === "register-staff") {
      staffForm();
    } else if (action === "edit-staff") {
      staffForm(await api("/api/staff/" + id));
    } else if (action === "block-staff") {
      await api("/api/staff/" + id + "/block", { method: "POST", body: {} });
      showToast("Staff account was blocked.");
      showView("staff");
    } else if (action === "activate-staff") {
      await api("/api/staff/" + id + "/activate", { method: "POST", body: {} });
      showToast("Staff account was reactivated.");
      showView("staff");
    } else if (action === "register-dentist") {
      dentistForm();
    } else if (action === "remove-dentist") {
      await api("/api/dentists/" + id + "/remove", { method: "POST", body: {} });
      showToast("Dentist was removed from the clinic list.");
      showView("dentists");
    } else if (action === "restore-dentist") {
      await api("/api/dentists/" + id + "/restore", { method: "POST", body: {} });
      showToast("Dentist was restored to the clinic list.");
      showView("dentists");
    } else if (action === "register-patient") {
      patientForm();
    } else if (action === "edit-patient") {
      patientForm(await api("/api/patients/" + id));
    } else if (action === "search-patients") {
      const query = document.getElementById("patient-query").value;
      const results = await api("/api/patients?q=" + encodeURIComponent(query));
      document.getElementById("patient-table").innerHTML = patientTable(results);
    } else if (action === "register-appointment") {
      const dentists = await api("/api/dentists?active=true");
      clinicLists.dentists = dentists;
      appointmentForm(null, clinicLists.patients, clinicLists.treatments, dentists);
    } else if (action === "cancel-appointment") {
      await api("/api/appointments/" + id + "/cancel", { method: "POST", body: {} });
      showToast("Appointment was cancelled.");
      showView("appointments");
    } else if (action === "restore-appointment") {
      await api("/api/appointments/" + id + "/restore", { method: "POST", body: {} });
      showToast("Appointment was restored.");
      showView("appointments");
    } else if (action === "search-appointment") {
      await runAppointmentSearch();
    } else if (action === "view-search-appointment") {
      const hit = clinicLists.searchHits[Number(id)];
      if (hit) {
        document.getElementById("search-result").innerHTML = appointmentDetails(hit);
      }
    } else if (action === "create-bill") {
      billForm(clinicLists.openAppointments);
    } else if (action === "print-bill") {
      printReceipt(await api("/api/bills/" + id));
    }
  } catch (error) {
    showToast(error.message);
  }
}

async function renderPatients(content) {
  const patients = await api("/api/patients");
  content.innerHTML = `
    <div class="toolbar">
      <div class="search-row">
        <input id="patient-query" placeholder="Search by name, ID, or contact">
        <button class="btn btn-secondary" data-action="search-patients" type="button">Search</button>
      </div>
      ${isStaff() ? `<button class="btn btn-primary" id="new-patient" data-action="register-patient" type="button">Register patient</button>` : ""}
    </div>
    <section class="card" id="patient-table">${patientTable(patients)}</section>
  `;
}

function patientTable(patients) {
  if (!patients.length) {
    return `<p class="empty">No patient records match this view.</p>`;
  }
  return `<div class="table-wrap"><table>
    <thead><tr><th>Patient ID</th><th>Name</th><th>Age</th><th>Contact</th><th>Address</th><th></th></tr></thead>
    <tbody>${patients.map(patient => `
      <tr>
        <td>${escapeHtml(patient.patientCode)}</td>
        <td>${escapeHtml(patient.fullName)}</td>
        <td>${patient.age}</td>
        <td>${escapeHtml(patient.contactNumber)}</td>
        <td>${escapeHtml(patient.address)}</td>
        <td class="row-actions">
          ${isStaff() ? `<button class="btn btn-secondary" data-action="edit-patient" data-id="${patient.patientId}" type="button">Update</button>` : ""}
        </td>
      </tr>`).join("")}
    </tbody></table></div>`;
}

function patientForm(patient) {
  openDrawer(patient ? "Update patient" : "Register patient", `
    <form id="patient-form">
      <label class="field"><span>Patient name</span><input name="fullName" value="${escapeHtml(patient?.fullName || "")}" required></label>
      <label class="field"><span>Age</span><input name="age" type="number" min="1" max="120" value="${patient?.age || ""}" required></label>
      <label class="field"><span>Contact number</span><input name="contactNumber" value="${escapeHtml(patient?.contactNumber || "")}" required></label>
      <label class="field"><span>Address</span><textarea name="address" required>${escapeHtml(patient?.address || "")}</textarea></label>
      <div class="notice notice-error" id="form-error" hidden></div>
      <button class="btn btn-primary" type="submit">${patient ? "Save changes" : "Register patient"}</button>
    </form>
  `);
  document.getElementById("patient-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const body = Object.fromEntries(form.entries());
    body.age = Number(body.age);
    const errorBox = document.getElementById("form-error");
    try {
      if (patient) {
        await api("/api/patients/" + patient.patientId, { method: "PUT", body });
        showToast("Patient details were updated.");
      } else {
        await api("/api/patients", { method: "POST", body });
        showToast("Patient was registered.");
      }
      closeDrawer();
      showView("patients");
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

async function renderAppointments(content) {
  const [appointments, patients, treatments, dentists] = await Promise.all([
    api("/api/appointments"),
    api("/api/patients"),
    api("/api/treatments"),
    api("/api/dentists?active=true")
  ]);
  clinicLists.patients = patients;
  clinicLists.treatments = treatments;
  clinicLists.dentists = dentists;
  content.innerHTML = `
    <div class="toolbar">
      <h3>Clinic appointments</h3>
      ${isStaff() ? `<button class="btn btn-primary" id="new-appointment" data-action="register-appointment" type="button">Register appointment</button>` : ""}
    </div>
    <section class="card">${appointmentTable(appointments)}</section>
  `;
}

function appointmentTable(records) {
  if (!records.length) {
    return `<p class="empty">No appointments have been registered yet.</p>`;
  }
  const allowCancel = isStaff() && currentView === "appointments";
  return `<div class="table-wrap"><table>
    <thead><tr><th>Number</th><th>Patient</th><th>Dentist</th><th>Treatment</th><th>Date & time</th><th>Status</th>${allowCancel ? "<th></th>" : ""}</tr></thead>
    <tbody>${records.map(record => {
      const status = record.appointment.status;
      const badgeClass = status === "CANCELLED" ? "cancelled" : status === "COMPLETED" ? "" : "active";
      return `
      <tr>
        <td>${escapeHtml(record.appointment.appointmentNumber)}</td>
        <td>${escapeHtml(record.patient.fullName)}<br><small class="muted">${escapeHtml(record.patient.contactNumber)}</small></td>
        <td>${escapeHtml(record.appointment.dentistName)}</td>
        <td>${escapeHtml(record.appointment.treatmentType)}</td>
        <td>${escapeHtml(record.appointment.appointmentDate)} ${escapeHtml(record.appointment.appointmentTime)}</td>
        <td><span class="badge ${badgeClass}">${escapeHtml(status)}</span></td>
        ${allowCancel ? `<td class="row-actions">${status === "SCHEDULED"
          ? `<button class="btn btn-danger" data-action="cancel-appointment" data-id="${record.appointment.appointmentId}" type="button">Cancel</button>`
          : status === "CANCELLED"
            ? `<button class="btn btn-secondary" data-action="restore-appointment" data-id="${record.appointment.appointmentId}" type="button">Restore</button>`
            : ""}</td>` : ""}
      </tr>`;
    }).join("")}
    </tbody></table></div>`;
}

function appointmentForm(record, patients, treatments, dentists) {
  const dentistOptions = (dentists || []).map(dentist =>
    `<option value="${escapeHtml(dentist.fullName)}">${escapeHtml(dentist.fullName)}</option>`
  ).join("");
  openDrawer("Register appointment", `
    <form id="appointment-form">
      <label class="field"><span>Patient</span>
        <select name="patientId" required>
          <option value="">Select a patient</option>
          ${patients.map(patient => `<option value="${patient.patientId}">${escapeHtml(patient.patientCode)} · ${escapeHtml(patient.fullName)}</option>`).join("")}
        </select>
      </label>
      <label class="field"><span>Dentist</span>
        <select name="dentistName" required ${dentistOptions ? "" : "disabled"}>
          <option value="">${dentistOptions ? "Select a dentist" : "No dentists on the clinic list"}</option>
          ${dentistOptions}
        </select>
      </label>
      ${dentistOptions ? "" : `<p class="muted">Ask an administrator to add dentists before registering appointments.</p>`}
      <label class="field"><span>Treatment type</span>
        <select name="treatmentType" required>
          <option value="">Select a treatment</option>
          ${treatments.map(item => `<option value="${escapeHtml(item.name)}">${escapeHtml(item.name)}</option>`).join("")}
        </select>
      </label>
      <label class="field"><span>Appointment date</span><input name="appointmentDate" type="date" required></label>
      <label class="field"><span>Appointment time</span>
        <select name="appointmentTime" required>
          ${timeOptions()}
        </select>
      </label>
      <div class="notice notice-error" id="form-error" hidden></div>
      <button class="btn btn-primary" type="submit" ${dentistOptions ? "" : "disabled"}>Save appointment</button>
    </form>
  `);
  document.getElementById("appointment-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const body = Object.fromEntries(form.entries());
    body.patientId = Number(body.patientId);
    const errorBox = document.getElementById("form-error");
    try {
      await api("/api/appointments", { method: "POST", body });
      showToast("Appointment was registered.");
      closeDrawer();
      showView("appointments");
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

function timeOptions() {
  const slots = [];
  for (let hour = 8; hour <= 17; hour += 1) {
    slots.push(`${String(hour).padStart(2, "0")}:00`);
    if (hour !== 17) slots.push(`${String(hour).padStart(2, "0")}:30`);
  }
  slots.push("17:30");
  return `<option value="">Select a time</option>` + slots.map(slot => `<option value="${slot}">${slot}</option>`).join("");
}

async function renderSearch(content) {
  clinicLists.searchHits = [];
  content.innerHTML = `
    <section class="card">
      <h3>Search appointments</h3>
      <p class="muted">Enter a patient name, dentist name, or a full appointment number such as APT-20260901-0001.</p>
      <div class="search-row" style="margin-top:14px">
        <input id="appointment-query" placeholder="Patient name or APT-YYYYMMDD-0001">
        <button class="btn btn-primary" id="search-appointment" data-action="search-appointment" type="button">Search</button>
      </div>
      <div id="search-result"></div>
    </section>
  `;
}

async function runAppointmentSearch() {
  const query = document.getElementById("appointment-query").value;
  const result = document.getElementById("search-result");
  result.innerHTML = "";
  clinicLists.searchHits = [];
  try {
    const records = await api("/api/appointments?q=" + encodeURIComponent(query));
    const list = Array.isArray(records) ? records : [records];
    clinicLists.searchHits = list;
    if (list.length === 1) {
      result.innerHTML = appointmentDetails(list[0]);
      return;
    }
    result.innerHTML = `<p class="muted" style="margin-top:18px">${list.length} appointments found.</p>` + searchHitTable(list);
  } catch (error) {
    result.innerHTML = `<div class="notice notice-error">${escapeHtml(error.message)}</div>`;
  }
}

function searchHitTable(records) {
  return `<div class="table-wrap" style="margin-top:8px"><table>
    <thead><tr><th>Number</th><th>Patient</th><th>Dentist</th><th>Date & time</th><th>Status</th><th></th></tr></thead>
    <tbody>${records.map((record, index) => `
      <tr>
        <td>${escapeHtml(record.appointment.appointmentNumber)}</td>
        <td>${escapeHtml(record.patient.fullName)}</td>
        <td>${escapeHtml(record.appointment.dentistName)}</td>
        <td>${escapeHtml(record.appointment.appointmentDate)} ${escapeHtml(record.appointment.appointmentTime)}</td>
        <td><span class="badge">${escapeHtml(record.appointment.status)}</span></td>
        <td class="row-actions">
          <button class="btn btn-secondary" data-action="view-search-appointment" data-id="${index}" type="button">View</button>
        </td>
      </tr>`).join("")}
    </tbody></table></div>`;
}

function appointmentDetails(record) {
  const appointment = record.appointment;
  const patient = record.patient;
  return `<div class="details" style="margin-top:18px">
    <div><span>Appointment number</span><strong>${escapeHtml(appointment.appointmentNumber)}</strong></div>
    <div><span>Status</span><strong>${escapeHtml(appointment.status)}</strong></div>
    <div><span>Patient name</span><strong>${escapeHtml(patient.fullName)}</strong></div>
    <div><span>Patient ID</span><strong>${escapeHtml(patient.patientCode)}</strong></div>
    <div><span>Age</span><strong>${patient.age}</strong></div>
    <div><span>Contact number</span><strong>${escapeHtml(patient.contactNumber)}</strong></div>
    <div><span>Address</span><strong>${escapeHtml(patient.address)}</strong></div>
    <div><span>Dentist name</span><strong>${escapeHtml(appointment.dentistName)}</strong></div>
    <div><span>Treatment type</span><strong>${escapeHtml(appointment.treatmentType)}</strong></div>
    <div><span>Appointment date</span><strong>${escapeHtml(appointment.appointmentDate)}</strong></div>
    <div><span>Appointment time</span><strong>${escapeHtml(appointment.appointmentTime)}</strong></div>
  </div>`;
}

async function renderBilling(content) {
  const [bills, appointments] = await Promise.all([
    api("/api/bills"),
    api("/api/appointments")
  ]);
  const openAppointments = appointments.filter(record => record.appointment.status !== "CANCELLED");
  clinicLists.openAppointments = openAppointments;
  content.innerHTML = `
    <div class="toolbar">
      <h3>Patient bills</h3>
      ${isStaff() ? `<button class="btn btn-primary" id="new-bill" data-action="create-bill" type="button">Calculate bill</button>` : ""}
    </div>
    <section class="card">${billTable(bills)}</section>
  `;
}

function billTable(records) {
  if (!records.length) {
    return `<p class="empty">No bills have been calculated yet.</p>`;
  }
  return `<div class="table-wrap"><table>
    <thead><tr><th>Bill</th><th>Appointment</th><th>Patient</th><th>Treatment</th><th>Total</th><th></th></tr></thead>
    <tbody>${records.map(record => `
      <tr>
        <td>${escapeHtml(record.bill.billNumber)}</td>
        <td>${escapeHtml(record.appointment.appointmentNumber)}</td>
        <td>${escapeHtml(record.patient.fullName)}</td>
        <td>${escapeHtml(record.bill.treatmentType)}</td>
        <td>${money(record.bill.totalAmount)}</td>
        <td class="row-actions">
          <button class="btn btn-secondary" data-action="print-bill" data-id="${record.bill.billId}" type="button">Print</button>
        </td>
      </tr>`).join("")}
    </tbody></table></div>`;
}

function billForm(appointments) {
  openDrawer("Calculate bill", `
    <form id="bill-form">
      <label class="field"><span>Appointment</span>
        <select name="appointmentId" id="bill-appointment" required>
          <option value="">Select an appointment</option>
          ${appointments.map(record => `<option value="${record.appointment.appointmentId}">${escapeHtml(record.appointment.appointmentNumber)} · ${escapeHtml(record.patient.fullName)}</option>`).join("")}
        </select>
      </label>
      <div id="bill-preview" class="muted">Select an appointment to see the calculated total.</div>
      <div class="notice notice-error" id="form-error" hidden></div>
      <button class="btn btn-primary" type="submit">Create bill</button>
    </form>
  `);
  document.getElementById("bill-appointment").addEventListener("change", async (event) => {
    const preview = document.getElementById("bill-preview");
    if (!event.target.value) {
      preview.textContent = "Select an appointment to see the calculated total.";
      return;
    }
    try {
      const amounts = await api("/api/bills/preview?appointmentId=" + encodeURIComponent(event.target.value));
      preview.innerHTML = `Treatment cost ${money(amounts.treatmentCost)} + consultation fee ${money(amounts.consultationFee)} = <strong>${money(amounts.totalAmount)}</strong>`;
    } catch (error) {
      preview.innerHTML = `<span class="notice notice-error">${escapeHtml(error.message)}</span>`;
    }
  });
  document.getElementById("bill-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.target);
    const errorBox = document.getElementById("form-error");
    try {
      const record = await api("/api/bills", {
        method: "POST",
        body: { appointmentId: Number(form.get("appointmentId")) }
      });
      closeDrawer();
      showToast("Bill was created.");
      await showView("billing");
      printReceipt(record);
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
    }
  });
}

function printReceipt(record) {
  const bill = record.bill;
  const appointment = record.appointment;
  const patient = record.patient;
  const printer = window.open("", "receipt");
  printer.document.write(`
    <html><head><title>${bill.billNumber}</title>
    <style>
      body { font-family: Inter, Segoe UI, sans-serif; padding: 32px; color: #001b44; }
      h1 { margin-bottom: 4px; }
      table { width: 100%; border-collapse: collapse; margin-top: 18px; }
      td { padding: 8px 0; border-bottom: 1px solid #e7e9fb; }
    </style></head>
    <body>
      <h1>Sunrise Dental Clinic</h1>
      <p>42 Galle Road, Colombo 03<br>Patient receipt</p>
      <table>
        <tr><td>Bill number</td><td>${escapeHtml(bill.billNumber)}</td></tr>
        <tr><td>Appointment number</td><td>${escapeHtml(appointment.appointmentNumber)}</td></tr>
        <tr><td>Patient</td><td>${escapeHtml(patient.fullName)} (${patient.age})</td></tr>
        <tr><td>Contact</td><td>${escapeHtml(patient.contactNumber)}</td></tr>
        <tr><td>Address</td><td>${escapeHtml(patient.address)}</td></tr>
        <tr><td>Treatment</td><td>${escapeHtml(bill.treatmentType)}</td></tr>
        <tr><td>Treatment cost</td><td>${money(bill.treatmentCost)}</td></tr>
        <tr><td>Consultation fee</td><td>${money(bill.consultationFee)}</td></tr>
        <tr><td><strong>Total</strong></td><td><strong>${money(bill.totalAmount)}</strong></td></tr>
        <tr><td>Issued</td><td>${escapeHtml(bill.issuedAt || "")}</td></tr>
      </table>
    </body></html>
  `);
  printer.document.close();
  printer.focus();
  printer.print();
}

async function renderReports(content) {
  const [summary, patients, appointments, bills] = await Promise.all([
    api("/api/reports/summary"),
    api("/api/reports/patients"),
    api("/api/reports/appointments"),
    api("/api/reports/billing")
  ]);
  content.innerHTML = `
    <section class="stats">
      <article class="card"><p class="eyebrow">Patients</p><div class="stat-value">${summary.patientCount}</div></article>
      <article class="card"><p class="eyebrow">Appointments</p><div class="stat-value">${summary.appointmentCount}</div></article>
      <article class="card"><p class="eyebrow">Bills</p><div class="stat-value">${summary.billCount}</div></article>
      <article class="card"><p class="eyebrow">Total billed</p><div class="stat-value">${money(summary.totalRevenue)}</div></article>
    </section>
    <section class="card" style="margin-bottom:14px"><h3>Patients</h3>${patientTable(patients)}</section>
    <section class="card" style="margin-bottom:14px"><h3>Appointments</h3>${appointmentTable(appointments)}</section>
    <section class="card"><h3>Billing</h3>${billTable(bills)}</section>
  `;
}

function renderHelp(content) {
  const staffHelp = `
    <article class="help-step"><h3>1. Log in</h3><p class="muted">Open the shared login page, enter the username and password issued by the administrator, then choose Enter workspace. Tick Keep me logged in on this PC only on a trusted computer. That option stores a secure sign-in token, not your password. Use Log out to end it.</p></article>
    <article class="help-step"><h3>2. Register a patient</h3><p class="muted">Open Patients, choose Register patient, and enter name, age, address, and a 10-digit contact number starting with 0.</p></article>
    <article class="help-step"><h3>3. Manage patients</h3><p class="muted">Search by name, patient ID, or contact number. Use Update to correct stored details. Duplicate contact numbers are not allowed.</p></article>
    <article class="help-step"><h3>4. Create an appointment</h3><p class="muted">Open Appointments and choose Register appointment. Select the patient, a dentist from the clinic list, treatment, date, and a 30-minute slot between 08:00 and 17:30. Sundays are closed. The system assigns a unique appointment number and blocks double-booking of the same dentist or patient at the same time. Use Cancel if the patient asks to drop a scheduled appointment. Use Restore if that booking should return to the diary. Completed appointments cannot be cancelled.</p></article>
    <article class="help-step"><h3>5. Search appointment details</h3><p class="muted">Open Appointment search and enter a patient name, dentist name, or the full appointment number. Matching visits are listed so you can open the complete record. You will see a clear message if nothing is entered or no match is found.</p></article>
    <article class="help-step"><h3>6. Calculate and print a bill</h3><p class="muted">Open Billing, select an appointment, review treatment cost plus the consultation fee, then create the bill. Use Print to produce the patient receipt.</p></article>
    <article class="help-step"><h3>7. View reports</h3><p class="muted">Reports show patient, appointment, and billing totals from the live clinic records.</p></article>
    <article class="help-step"><h3>8. Log out</h3><p class="muted">Choose Log out when you leave the desk. This ends the session, removes any stay-signed-in token for this PC, and returns you to the login page.</p></article>
  `;
  const adminHelp = `
    <article class="help-step"><h3>1. Log in</h3><p class="muted">Use the same login page as Staff. After sign-in, the system opens the administrator dashboard.</p></article>
    <article class="help-step"><h3>2. Register staff</h3><p class="muted">Open Staff and choose Register staff. Create the username and password for the new employee. The account is always created as STAFF. Nobody can self-register, and the system will not accept an Admin role from the form.</p></article>
    <article class="help-step"><h3>3. Update staff information</h3><p class="muted">Use Update to change name, email, contact number, or password. Usernames stay unique.</p></article>
    <article class="help-step"><h3>4. Block or deactivate staff</h3><p class="muted">Choose Block to stop a staff member logging in. The record is kept for history. Use Activate if the person should return to work.</p></article>
    <article class="help-step"><h3>5. Manage dentists</h3><p class="muted">Open Dentists to add a clinic dentist or remove one who is retiring or leaving. Dentists do not receive login accounts. Staff use this list when booking appointments. Use Restore if a removed dentist should return to the dropdown.</p></article>
    <article class="help-step"><h3>6. View clinic records</h3><p class="muted">Administrators can view patients, appointments, billing, and reports. Day-to-day registration of patients, appointments, and bills remains a Staff duty.</p></article>
    <article class="help-step"><h3>8. Log out</h3><p class="muted">Choose Log out when you leave the desk. This ends the session, removes any stay-signed-in token for this PC, and returns you to the login page.</p></article>
  `;
  content.innerHTML = `<section class="help-list">${isAdmin() ? adminHelp : staffHelp}</section>`;
}

async function start() {
  const session = await api("/api/auth/session");
  if (!session.authenticated) {
    location.href = "/login.html";
    return;
  }
  currentUser = session.user;
  document.getElementById("role-label").textContent = currentUser.role === "ADMIN" ? "Administrator" : "Clinic staff";
  document.getElementById("user-name").textContent = currentUser.fullName;
  document.getElementById("user-role").textContent = currentUser.role === "ADMIN" ? "admin" : "staff";
  document.getElementById("user-initials").textContent = initials(currentUser.fullName);
  document.getElementById("logout-btn").addEventListener("click", async () => {
    await api("/api/auth/logout", { method: "POST", body: {} });
    location.href = "/login.html";
  });
  document.getElementById("drawer-close").addEventListener("click", closeDrawer);
  document.getElementById("drawer").addEventListener("click", (event) => {
    if (event.target.id === "drawer") closeDrawer();
  });
  document.getElementById("content").addEventListener("click", handleWorkspaceClick);
  document.getElementById("content").addEventListener("keydown", (event) => {
    if (event.key === "Enter" && event.target.id === "appointment-query") {
      event.preventDefault();
      runAppointmentSearch();
    }
  });
  await showView("dashboard");
}

start().catch(() => {
  location.href = "/login.html";
});
