"use strict";
document.documentElement.classList.add("js");
const menuButton = document.querySelector(".menu-toggle");
const menuLinks = document.getElementById("store-nav-links");
if (menuButton && menuLinks) {
    menuButton.addEventListener("click", () => {
        const open = menuButton.getAttribute("aria-expanded") !== "true";
        menuButton.setAttribute("aria-expanded", String(open));
        menuLinks.classList.toggle("is-open", open);
    });
}
document.querySelectorAll(".nav-links a").forEach(link => {
    const path = new URL(link.href).pathname;
    if (location.pathname === path || (path !== "/" && location.pathname.startsWith(path + "/"))) {
        link.setAttribute("aria-current", "page");
    }
});
document.querySelectorAll(".header-top [hreflang]").forEach(link => {
    if (link.hreflang === document.documentElement.lang) link.setAttribute("aria-current", "true");
});
document.addEventListener("keydown", event => {
    if (event.key === "Escape") document.querySelectorAll(".account-menu[open]").forEach(menu => { menu.open = false; menu.querySelector("summary").focus(); });
});
document.addEventListener("click", event => {
    document.querySelectorAll(".account-menu[open]").forEach(menu => { if (!menu.contains(event.target)) menu.open = false; });
});
