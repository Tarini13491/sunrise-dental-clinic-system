const titles = {
  dashboard: ['Staff desk', 'Today at Sunrise', 'Appointments, bills and patient files for the Colombo 03 clinic.'],
  register: ['Menu · Register', 'Register a new appointment', 'Collect name, address, phone, dentist, treatment, date and time. The number is assigned by MySQL.'],
  search: ['Menu · Search', 'Display appointment details', 'Look up a visit by appointment number, patient name or contact number.'],
  billing: ['Menu · Billing', 'Calculate and print bill', 'Consultation fee + treatment cost, then VAT. Print a receipt the patient can take home.'],
  reports: ['Management', 'Decision reports', 'Daily volume, dentist workload, treatment mix and monthly collections.'],
  notifications: ['Alerts', 'Email & SMS log', 'Every booking writes a confirmation. DEMO mode stores the exact message text.'],
  help: ['Menu · Help', 'Help for new staff', 'Step-by-step handbook for the reception desk.'],
};

const state = { dentists: [], treatments: [], occupied: [] };

function toast(message) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = message;
  el.style.display = 'block';
  clearTimeout(toast._t);
  toast._t = setTimeout(() => { el.style.display = 'none'; }, 3200);
}

async function requireSession() {
  const session = await api.get('/api/session');
  if (!session.success || !session.data.authenticated) {
    location.href = '/pages/login.html';
    return null;
  }
  document.getElementById('user-name').textContent = session.data.fullName;
  document.getElementById('user-role').textContent = session.data.role;
  document.getElementById('user-avatar').textContent = session.data.fullName.split(' ').map(p => p[0]).slice(0, 2).join('');
  return session.data;
}

function showView(name) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
  document.getElementById('view-' + name).classList.remove('hidden');
  document.querySelectorAll('.nav-item[data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === name));
  document.getElementById('view-kicker').textContent = titles[name][0];
  document.getElementById('view-title').textContent = titles[name][1];
  document.getElementById('view-sub').textContent = titles[name][2];
  document.getElementById('desk-links').classList.remove('open');
  if (name === 'dashboard') loadDashboard();
  if (name === 'register') loadLookups();
  if (name === 'reports') loadReport('daily');
  if (name === 'notifications') loadNotifications();
  if (name === 'help') loadHelp();
}

async function loadDashboard() {
  const res = await api.get('/api/dashboard');
  if (!res.success) {
    document.getElementById('stat-cards').innerHTML = `<article class="panel"><p class="status err">${res.message || 'Could not load the dashboard.'}</p></article>`;
    return;
  }
  const d = res.data;
  document.getElementById('stat-cards').innerHTML = [
    ['Today’s visits', d.todayAppointments, 'Including completed chairs', '📅'],
    ['Still scheduled', d.scheduled, 'Waiting to be seen', '⏳'],
    ['Collected today', money(d.todayRevenue), 'Bills issued today', '💳'],
    ['Patient files', d.registeredPatients, 'Registered in the system', '👤']
  ].map(([t, v, s, i]) => `<article class="panel"><div class="stat-icon">${i}</div><div class="muted" style="margin-top:12px">${t}</div><div style="font-size:28px;font-weight:800;letter-spacing:-0.04em">${v}</div><div class="muted">${s}</div></article>`).join('');

  const rows = (d.upcoming || []).map(a => `<tr class="clickable" data-open="${a.appointmentNumber}">
    <td><strong>${a.appointmentNumber}</strong></td>
    <td>${a.patientName}</td>
    <td>${a.dentistName}</td>
    <td>${a.treatmentName}</td>
    <td>${a.appointmentDate} ${String(a.appointmentTime).substring(0, 5)}</td>
    <td>${statusBadge(a.status)}</td>
  </tr>`).join('');
  document.getElementById('upcoming-table').innerHTML = `<table>
    <thead><tr><th>Number</th><th>Patient</th><th>Dentist</th><th>Treatment</th><th>When</th><th>Status</th></tr></thead>
    <tbody>${rows || '<tr><td colspan="6"><div class="empty">No upcoming visits yet. Register the first appointment from the top menu.</div></td></tr>'}</tbody>
  </table>`;
}

function times() {
  const list = [];
  for (let h = 8; h <= 17; h++) {
    for (const m of [0, 30]) {
      if (h === 17 && m === 30) continue;
      list.push(String(h).padStart(2, '0') + ':' + String(m).padStart(2, '0'));
    }
  }
  return list;
}

async function loadLookups() {
  const res = await api.get('/api/appointments?lists=1');
  if (!res.success) return;
  state.dentists = res.data.dentists || [];
  state.treatments = res.data.treatments || [];
  const dentistSel = document.getElementById('dentistId');
  const treatSel = document.getElementById('treatmentId');
  dentistSel.innerHTML = state.dentists.map(d => `<option value="${d.dentistId}">${d.fullName} · ${d.specialization}</option>`).join('');
  treatSel.innerHTML = state.treatments.map(t => `<option value="${t.treatmentId}">${t.treatmentName} (${money(t.baseCost)})</option>`).join('');
  const date = document.getElementById('appointmentDate');
  if (!date.value) {
    const today = new Date();
    date.value = today.toISOString().slice(0, 10);
    date.min = today.toISOString().slice(0, 10);
  }
  await refreshSlots();
}

async function refreshSlots() {
  const dentistId = document.getElementById('dentistId').value;
  const date = document.getElementById('appointmentDate').value;
  const sel = document.getElementById('appointmentTime');
  let occupied = [];
  if (dentistId && date) {
    const res = await api.get(`/api/appointments?slots=1&dentistId=${dentistId}&date=${date}`);
    occupied = (res.data && res.data.occupied) || [];
  }
  const current = sel.value;
  sel.innerHTML = times().map(t => {
    const taken = occupied.includes(t);
    return `<option value="${t}" ${taken ? 'disabled' : ''}>${t}${taken ? ' — booked' : ''}</option>`;
  }).join('');
  if (current && ![...sel.options].some(o => o.value === current && !o.disabled)) {
    sel.selectedIndex = [...sel.options].findIndex(o => !o.disabled);
  }
}

function detailsCard(a) {
  return `<div class="receipt" style="margin-top:16px">
    <div class="kicker">${a.appointmentNumber}</div>
    <h3 style="margin:6px 0 12px">${a.patientName}</h3>
    <p><strong>Address:</strong> ${a.address || '—'}<br>
    <strong>Contact:</strong> ${a.contactNumber}${a.patientEmail ? ' · ' + a.patientEmail : ''}<br>
    <strong>Dentist:</strong> ${a.dentistName}${a.specialization ? ' (' + a.specialization + ')' : ''}<br>
    <strong>Treatment:</strong> ${a.treatmentName}<br>
    <strong>When:</strong> ${a.appointmentDate} at ${String(a.appointmentTime).substring(0, 5)}<br>
    <strong>Status:</strong> ${statusBadge(a.status)}
    ${a.billNumber ? `<br><strong>Bill:</strong> ${a.billNumber} · ${money(a.billTotal)} · ${a.paymentStatus}` : ''}</p>
    ${a.notes ? `<p class="muted">${a.notes}</p>` : ''}
    ${a.status === 'CANCELLED' ? '' : `<button class="btn btn-ghost" data-cancel="${a.appointmentNumber}" type="button">Cancel this visit</button>
    <button class="btn btn-navy" data-bill="${a.appointmentNumber}" type="button">Open billing</button>`}
  </div>`;
}

function renderMatches(list) {
  if (!list || !list.length) return '<p class="muted">No matching visits.</p>';
  if (list.length === 1) return detailsCard(list[0]);
  return `<p class="muted">${list.length} visits found. Open one to see the full file.</p>` + list.map(a => `
    <article class="reason" style="margin:10px 0;cursor:pointer" data-pick="${a.appointmentNumber}">
      <strong>${a.appointmentNumber}</strong>
      <div><strong>${a.patientName}</strong><p class="muted">${a.appointmentDate} ${String(a.appointmentTime).substring(0, 5)} · ${a.dentistName} · ${a.treatmentName}</p></div>
      ${statusBadge(a.status)}
    </article>`).join('');
}

async function loadReport(type) {
  const box = document.getElementById('report-box');
  box.innerHTML = '<p class="muted">Loading report…</p>';
  const res = await api.get('/api/reports?type=' + type);
  if (!res.success) {
    box.innerHTML = `<p class="status err">${res.message}</p>`;
    return;
  }
  const d = res.data;
  const rows = d.rows || [];
  if (!rows.length) {
    box.innerHTML = `<h3>${d.title || 'Report'}</h3><div class="empty">No rows yet. Book a few visits, then return.</div>`;
    return;
  }
  const keys = Object.keys(rows[0]);
  box.innerHTML = `
    <h3>${d.title || 'Report'}</h3>
    <p class="muted">${d.purpose || ''}</p>
    <div class="table-wrap"><table>
      <thead><tr>${keys.map(k => `<th>${k.replaceAll('_', ' ')}</th>`).join('')}</tr></thead>
      <tbody>${rows.map(r => `<tr>${keys.map(k => `<td>${formatCell(r[k])}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></div>`;
}

function formatCell(value) {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'number') return Number.isInteger(value) ? value : money(value).replace('LKR ', '');
  return value;
}

async function loadNotifications() {
  const res = await api.get('/api/reports?type=notifications');
  const rows = (res.data && res.data.rows) || [];
  document.getElementById('notify-box').innerHTML = rows.map(n => `
    <article class="reason" style="margin-bottom:10px">
      <div>${statusBadge(n.channel)} ${statusBadge(n.status)}</div>
      <div><strong>${n.subject || n.channel}</strong>
      <p class="muted">${n.recipient}<br>${n.message}</p></div>
    </article>`).join('') || '<div class="empty">No alerts yet. Book an appointment with an email or mobile number.</div>';
}

async function loadHelp() {
  const res = await api.get('/api/help');
  const steps = (res.data && res.data.steps) || [];
  document.getElementById('help-box').innerHTML = `<div class="kicker">Staff handbook</div>
    <h3>How to use the Sunrise desk</h3>
    <p class="muted">New receptionists should follow these steps on their first morning. The top menu is Register · Search · Billing · Help · Exit.</p>
    ${steps.map((s, i) => `<div class="reason" style="margin:12px 0"><strong>0${i + 1}</strong><div><strong>${s.title}</strong><p class="muted">${s.body}</p></div></div>`).join('')}
    <p class="muted">Always use Exit before you leave the reception PC.</p>`;
}

function renderBill(data) {
  const a = data.appointment || {};
  const p = data.preview || {};
  const b = data.bill;
  document.getElementById('bill-box').innerHTML = `
    <div class="receipt" id="receipt">
      <div class="brand"><img src="/images/logo.svg" width="28" alt=""> Sunrise Dental Clinic</div>
      <p class="muted">42 Galle Road, Colombo 03 · +94 11 234 5678</p>
      <h3>${a.patientName || ''} · ${a.appointmentNumber || ''}</h3>
      <p>${a.address || ''}<br>${a.contactNumber || ''}<br>${a.dentistName || ''} · ${a.treatmentName || ''}</p>
      <table>
        <tr><td>Consultation fee</td><td style="text-align:right">${money(p.consultationFee)}</td></tr>
        <tr><td>Treatment</td><td style="text-align:right">${money(p.treatmentCost)}</td></tr>
        <tr><td>Emergency surcharge</td><td style="text-align:right">${money(p.surcharge)}</td></tr>
        <tr><td>Discount</td><td style="text-align:right">− ${money(p.discount)}</td></tr>
        <tr><td>VAT 8%</td><td style="text-align:right">${money(p.tax)}</td></tr>
        <tr><td><strong>Total</strong></td><td style="text-align:right"><strong>${money((b && b.totalAmount) || p.total)}</strong></td></tr>
      </table>
      <p class="muted">Pricing rule: ${p.strategy || ''}${b ? '<br>Bill ' + b.billNumber + ' · ' + b.paymentStatus : ''}</p>
    </div>
    <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:14px">
      ${b ? '' : '<button class="btn btn-navy" id="calc-bill" type="button">Calculate and save bill</button>'}
      <button class="btn btn-ghost" type="button" onclick="window.print()">Print receipt</button>
      ${b && b.paymentStatus !== 'PAID' ? '<button class="btn btn-ghost" id="pay-bill" type="button">Record payment (cash)</button>' : ''}
    </div>`;
  const calc = document.getElementById('calc-bill');
  if (calc) calc.onclick = async () => {
    const extra = Number(document.getElementById('extra-discount').value || 0);
    const res = await api.post('/api/bills', { appointmentNumber: a.appointmentNumber, extraDiscount: extra });
    showStatus(document.getElementById('bill-status'), res.success, res.message);
    if (res.success) {
      toast(res.message);
      renderBill(res.data);
    }
  };
  const pay = document.getElementById('pay-bill');
  if (pay) pay.onclick = async () => {
    const res = await api.post('/api/bills', { action: 'pay', billNumber: b.billNumber, method: 'CASH', amount: b.totalAmount });
    showStatus(document.getElementById('bill-status'), res.success, res.message);
    if (res.success) {
      toast(res.message);
      const again = await api.get('/api/bills?appointmentNumber=' + encodeURIComponent(a.appointmentNumber));
      renderBill(again.data);
    }
  };
}

async function openSearch(number) {
  showView('search');
  document.getElementById('search-q').value = number;
  const res = await api.get('/api/search?appointmentNumber=' + encodeURIComponent(number));
  showStatus(document.getElementById('search-status'), res.success, res.message);
  document.getElementById('search-result').innerHTML = res.success ? renderMatches(res.data.matches || [res.data.appointment]) : '';
}

document.querySelectorAll('.nav-item[data-view]').forEach(btn => {
  btn.addEventListener('click', () => showView(btn.dataset.view));
});
document.getElementById('menu-btn').addEventListener('click', () => {
  document.getElementById('desk-links').classList.toggle('open');
});
document.getElementById('logout-btn').addEventListener('click', async () => {
  await api.post('/api/logout', {});
  location.href = '/pages/login.html';
});
document.getElementById('dentistId').addEventListener('change', refreshSlots);
document.getElementById('appointmentDate').addEventListener('change', refreshSlots);

document.getElementById('register-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  const payload = Object.fromEntries(form.entries());
  const res = await api.post('/api/appointments', payload);
  const status = document.getElementById('register-status');
  showStatus(status, res.success, res.message);
  const box = document.getElementById('register-result');
  if (res.success && res.data.appointment) {
    status.textContent = res.message + ' Number: ' + res.data.appointment.appointmentNumber;
    toast('Saved ' + res.data.appointment.appointmentNumber);
    box.innerHTML = detailsCard(res.data.appointment);
    e.target.reset();
    await loadLookups();
  } else {
    box.innerHTML = '';
  }
});

document.getElementById('search-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const number = new FormData(e.target).get('appointmentNumber');
  const res = await api.get('/api/search?q=' + encodeURIComponent(number));
  showStatus(document.getElementById('search-status'), res.success, res.message);
  document.getElementById('search-result').innerHTML = res.success ? renderMatches(res.data.matches || [res.data.appointment]) : '';
});

document.getElementById('search-result').addEventListener('click', async (e) => {
  const pick = e.target.closest('[data-pick]');
  if (pick) {
    await openSearch(pick.dataset.pick);
    return;
  }
  const bill = e.target.dataset.bill;
  if (bill) {
    document.getElementById('bill-number').value = bill;
    showView('billing');
    document.getElementById('bill-load').requestSubmit();
    return;
  }
  const number = e.target.dataset.cancel;
  if (!number) return;
  if (!confirm('Cancel appointment ' + number + '? The patient will be notified by SMS and email.')) return;
  const res = await api.post('/api/appointments', { action: 'cancel', appointmentNumber: number });
  showStatus(document.getElementById('search-status'), res.success, res.message);
  if (res.success) {
    toast('Visit cancelled');
    const again = await api.get('/api/search?appointmentNumber=' + encodeURIComponent(number));
    document.getElementById('search-result').innerHTML = again.success ? detailsCard(again.data.appointment) : '';
  }
});

document.getElementById('upcoming-table').addEventListener('click', (e) => {
  const row = e.target.closest('[data-open]');
  if (row) openSearch(row.dataset.open);
});

document.getElementById('register-result').addEventListener('click', (e) => {
  const bill = e.target.dataset.bill;
  if (bill) {
    document.getElementById('bill-number').value = bill;
    showView('billing');
    document.getElementById('bill-load').requestSubmit();
  }
});

document.getElementById('bill-load').addEventListener('submit', async (e) => {
  e.preventDefault();
  const number = document.getElementById('bill-number').value.trim();
  const res = await api.get('/api/bills?appointmentNumber=' + encodeURIComponent(number));
  showStatus(document.getElementById('bill-status'), res.success, res.message);
  if (res.success) renderBill(res.data);
});

document.querySelectorAll('[data-report]').forEach(btn => {
  btn.addEventListener('click', () => loadReport(btn.dataset.report));
});

requireSession().then(user => { if (user) loadDashboard(); });
