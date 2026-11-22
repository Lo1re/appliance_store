"use strict";
document.querySelectorAll("[data-retry-seconds]").forEach(message => {
    const initial = Number(message.dataset.retrySeconds);
    if (!Number.isFinite(initial) || initial <= 0) return;
    const deadline = Date.now() + initial * 1000;
    const form = message.closest("main").querySelector("form");
    const buttons = form ? [...form.querySelectorAll('[type="submit"]')] : [];
    buttons.forEach(button => { button.disabled = true; });
    // Keep the alert announcement static; a separate visual timer avoids announcing every second.
    message.removeAttribute("role");
    const refresh = () => {
        const remaining = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
        message.textContent = remaining > 0
            ? message.dataset.retryTemplate.replace("__SECONDS__", String(remaining))
            : message.dataset.retryReady;
        if (!remaining) {
            clearInterval(timer);
            buttons.forEach(button => { button.disabled = false; });
            message.setAttribute("role", "status");
        }
    };
    const timer = setInterval(refresh, 1000);
    refresh();
    window.addEventListener("pageshow", refresh);
});
