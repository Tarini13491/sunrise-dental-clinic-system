async function api(path, options = {}) {
  const config = {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin"
  };
  if (options.body !== undefined) {
    config.body = JSON.stringify(options.body);
  }
  const response = await fetch(path, config);
  let payload = {};
  try {
    payload = await response.json();
  } catch (error) {
    payload = { ok: false, error: "The server returned an unexpected response." };
  }
  if (response.status === 401 && shouldSendToLogin(path)) {
    location.href = "/login.html";
    throw new Error(payload.error || "Please log in again.");
  }
  if (!payload.ok) {
    const failure = new Error(payload.error || "The request could not be completed.");
    failure.status = response.status;
    throw failure;
  }
  return payload.data;
}

function shouldSendToLogin(path) {
  if (path === "/api/auth/login" || path === "/api/auth/session") {
    return false;
  }
  const page = location.pathname || "";
  return !page.endsWith("/login.html") && page !== "/";
}
