(async () => {
  try {
    const session = await api("/api/auth/session");
    if (session.authenticated) {
      location.href = "/desk.html";
    }
  } catch (error) {
  }
})();

const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");
const passwordToggle = document.getElementById("password-toggle");
const loginError = document.getElementById("login-error");
const usernameError = document.getElementById("username-error");
const passwordError = document.getElementById("password-error");

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

function setFieldError(input, errorBox, message) {
  if (!message) {
    input.classList.remove("is-invalid");
    input.removeAttribute("aria-invalid");
    errorBox.textContent = "";
    errorBox.hidden = true;
    errorBox.setAttribute("hidden", "hidden");
    return;
  }
  input.classList.add("is-invalid");
  input.setAttribute("aria-invalid", "true");
  errorBox.textContent = message;
  errorBox.hidden = false;
  errorBox.removeAttribute("hidden");
}

function clearFieldErrors() {
  setFieldError(usernameInput, usernameError, "");
  setFieldError(passwordInput, passwordError, "");
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
  clearFieldErrors();
  const username = usernameInput.value.trim();
  const password = passwordInput.value;
  let hasFieldError = false;
  if (!username) {
    setFieldError(usernameInput, usernameError, "Username is required.");
    hasFieldError = true;
  }
  if (!password) {
    setFieldError(passwordInput, passwordError, "Password is required.");
    hasFieldError = true;
  }
  if (hasFieldError) {
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
