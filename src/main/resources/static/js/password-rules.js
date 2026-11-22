"use strict";
// Guidance only: the server remains the authority for password validation.
document.querySelectorAll("[data-password-rules]").forEach(input => {
    const rules = input.parentElement.querySelector(".password-rules") || input.closest("form").querySelector(".password-rules");
    if (!rules) return;
    const checks = {
        length: value => value.length >= 12,
        uppercase: value => /\p{Lu}/u.test(value),
        lowercase: value => /\p{Ll}/u.test(value),
        digit: value => /\p{Nd}/u.test(value),
        special: value => /[\p{P}\p{S}]/u.test(value)
    };
    input.addEventListener("input", () => {
        rules.querySelectorAll("[data-password-rule]").forEach(rule => {
            rule.classList.toggle("is-met", checks[rule.dataset.passwordRule](input.value));
        });
    });
});
