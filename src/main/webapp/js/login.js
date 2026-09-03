(async () => {
  try {
    const session = await api("/api/auth/session");
    if (session.authenticated) {
      location.href = "/desk.html";
    }
  } catch (error) {
  }
})();

const passwordInput = document.getElementById("password");
const passwordToggle = document.getElementById("password-toggle");
const loginError = document.getElementById("login-error");

function showLoginError(message) {
  loginError.textContent = message;
  loginError.hidden = false;
  loginError.removeAttribute("hidden");
}

function hideLoginError() {
  loginError.textContent = "";
  loginError.hidden = true;
  loginError.setAttribute("hidden", "hidden");
}

function setPasswordVisible(visible) {
  passwordInput.type = visible ? "text" : "password";
  passwordToggle.setAttribute("aria-pressed", visible ? "true" : "false");
  passwordToggle.setAttribute("aria-label", visible ? "Hide password" : "Show password");
  passwordToggle.classList.toggle("is-visible", visible);
}

passwordToggle.addEventListener("click", () => {
  setPasswordVisible(passwordInput.type === "password");
});

document.getElementById("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  event.stopPropagation();
  hideLoginError();
  const username = document.getElementById("username").value.trim();
  const password = passwordInput.value;
  if (!username || !password) {
    showLoginError("Username and password are required.");
    return;
  }
  try {
    await api("/api/auth/login", {
      method: "POST",
      body: {
        username,
        password,
        rememberMe: document.getElementById("remember-me").checked
      }
    });
    location.href = "/desk.html";
  } catch (error) {
    showLoginError(error.message || "The username or password is incorrect.");
  }
});
