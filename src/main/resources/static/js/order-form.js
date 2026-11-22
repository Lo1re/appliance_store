"use strict";

// The form keeps the existing items[index].applianceId/quantity contract.
const items = document.getElementById("order-items");
const template = document.getElementById("order-item-template");
const picker = document.getElementById("product-picker");
const search = document.getElementById("product-search");
const results = picker.querySelector("[data-picker-results]");
const status = picker.querySelector("[data-picker-status]");
const form = items.closest("form");
let pending;
let timer;
let revision = 0;

function closeSuggestions() {
    results.hidden = true;
    search.setAttribute("aria-expanded", "false");
}

function updateRows() {
    items.querySelectorAll("[data-order-row]").forEach((row, index) => {
        row.querySelectorAll("[data-field]").forEach(input => {
            input.name = `items[${index}].${input.dataset.field}`;
            input.id = `items-${index}-${input.dataset.field}`;
        });
        row.querySelector("[data-remove-row]").disabled = false;
    });
    search.setCustomValidity("");
    document.getElementById("order-empty").hidden = items.children.length > 0;
}

function lockProduct(row, product) {
    const select = row.querySelector("select");
    const id = document.createElement("input");
    id.type = "hidden";
    id.dataset.field = "applianceId";
    id.value = product.id;
    const name = document.createElement("strong");
    name.className = "order-product-name";
    name.textContent = product.label;
    select.replaceWith(name, id);
}

function chooseProduct(product) {
    const existing = [...items.querySelectorAll('[data-field="applianceId"]')]
        .find(input => input.value === String(product.id));
    if (existing) {
        const quantity = existing.closest("[data-order-row]").querySelector('[data-field="quantity"]');
        quantity.value = Math.min(10000, Number(quantity.value || 0) + 1);
    } else {
        if (items.children.length >= 50) {
            status.textContent = picker.dataset.max;
            return;
        }
        const row = template.content.firstElementChild.cloneNode(true);
        lockProduct(row, product);
        row.querySelector('[data-field="quantity"]').value = "1";
        items.append(row);
    }
    updateRows();
    closeSuggestions();
    search.value = "";
    status.textContent = `${picker.dataset.chosen}: ${product.label}`;
    search.focus();
}

async function lookup() {
    if (!search.value.trim()) return;
    const current = ++revision;
    pending = new AbortController();
    status.textContent = picker.dataset.loading;
    try {
        const url = new URL(picker.dataset.url, location.origin);
        url.searchParams.set("query", search.value.trim());
        const response = await fetch(url, {signal: pending.signal, headers: {Accept: "application/json"}});
        if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) throw new Error();
        const options = await response.json();
        if (current !== revision || !picker.contains(document.activeElement)) return;
        results.replaceChildren();
        options.forEach(product => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = product.label;
            button.addEventListener("click", () => chooseProduct(product));
            results.append(button);
        });
        results.hidden = options.length === 0;
        search.setAttribute("aria-expanded", String(options.length > 0));
        status.textContent = options.length === 0 ? picker.dataset.empty : options.length === 20 ? picker.dataset.limit : "";
    } catch (error) {
        if (error.name !== "AbortError" && current === revision) status.textContent = picker.dataset.failed;
    }
}

search.addEventListener("input", () => {
    clearTimeout(timer);
    ++revision;
    if (pending) pending.abort();
    closeSuggestions();
    status.textContent = "";
    search.setCustomValidity("");
    timer = setTimeout(lookup, 250);
});
search.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        if (!results.hidden && results.firstElementChild) results.firstElementChild.click();
        else { clearTimeout(timer); lookup(); }
    }
    if (event.key === "ArrowDown" && !results.hidden) {
        event.preventDefault();
        results.firstElementChild?.focus();
    }
    if (event.key === "Escape") closeSuggestions();
});
results.addEventListener("keydown", event => {
    if (event.key === "Escape") { closeSuggestions(); search.focus(); }
    if (event.key === "ArrowDown" || event.key === "ArrowUp") {
        event.preventDefault();
        (event.key === "ArrowDown" ? event.target.nextElementSibling : event.target.previousElementSibling)?.focus();
    }
});
document.addEventListener("click", event => { if (!picker.contains(event.target)) closeSuggestions(); });
picker.addEventListener("focusout", event => { if (!picker.contains(event.relatedTarget)) closeSuggestions(); });
items.addEventListener("click", event => {
    const remove = event.target.closest("[data-remove-row]");
    if (!remove) return;
    remove.closest("[data-order-row]").remove();
    updateRows();
});
form.addEventListener("submit", event => {
    if (!items.querySelector('[data-field="applianceId"]')) {
        event.preventDefault();
        search.setCustomValidity(picker.dataset.required);
        search.reportValidity();
    }
});
items.querySelectorAll("[data-order-row]").forEach(row => {
    const select = row.querySelector("select");
    if (!select.value) { row.remove(); return; }
    lockProduct(row, {id: select.value, label: select.selectedOptions[0].textContent});
});
picker.hidden = false;
updateRows();
