"use strict";
document.querySelectorAll("form").forEach(form => {
    let submitting = false;
    form.addEventListener("submit", event => {
        if (event.defaultPrevented) return;
        if (submitting) { event.preventDefault(); return; }
        if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) { event.preventDefault(); return; }
        if (form.method.toLowerCase() !== "post") return;
        submitting = true;
        form.setAttribute("aria-busy", "true");
        // Keep successful form controls (including the submitter) in the request.
        form.querySelectorAll('[type="submit"]').forEach(button => button.classList.add("busy"));
    });
    window.addEventListener("pageshow", () => {
        submitting = false;
        form.removeAttribute("aria-busy");
        form.querySelectorAll(".busy").forEach(button => button.classList.remove("busy"));
    });
});
