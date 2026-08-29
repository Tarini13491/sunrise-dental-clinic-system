const api = {
  async request(url, options = {}) {
    const res = await fetch(url, {
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options
    });
    let body = {};
    try { body = await res.json(); } catch { body = { success: false, message: 'The server sent an unexpected response.' }; }
    if (res.status === 401 && !url.includes('/api/login') && !url.includes('/api/session')) {
      location.href = '/pages/login.html';
    }
    return body;
  },
  get(url) { return this.request(url); },
  post(url, data) { return this.request(url, { method: 'POST', body: JSON.stringify(data) }); }
};

function money(value) {
  const n = Number(value || 0);
  return 'LKR ' + n.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function statusBadge(status) {
  const map = {
    SCHEDULED: 'badge-warn',
    CHECKED_IN: 'badge-warn',
    COMPLETED: 'badge-ok',
    PAID: 'badge-ok',
    CANCELLED: 'badge-stop',
    NO_SHOW: 'badge-stop',
    UNPAID: 'badge-stop',
    PARTIAL: 'badge-warn',
    SENT: 'badge-ok'
  };
  return `<span class="badge ${map[status] || 'badge-warn'}">${status || '—'}</span>`;
}

function showStatus(el, ok, message) {
  el.className = 'status ' + (ok ? 'ok' : 'err');
  el.textContent = message;
}
