"use strict";
document.querySelectorAll('input[type="password"]').forEach(input => {
    const wrapper = document.createElement("div");
    wrapper.className = "password-wrap";
    input.before(wrapper);
    wrapper.append(input);
    const button = document.createElement("button");
    button.type = "button";
    button.className = "password-toggle";
    button.textContent = input.dataset.show;
    button.setAttribute("aria-label", input.dataset.show);
    button.setAttribute("aria-pressed", "false");
    button.addEventListener("click", () => {
        const visible = input.type === "password";
        input.type = visible ? "text" : "password";
        button.textContent = visible ? input.dataset.hide : input.dataset.show;
        button.setAttribute("aria-label", button.textContent);
        button.setAttribute("aria-pressed", String(visible));
    });
    wrapper.append(button);
});
