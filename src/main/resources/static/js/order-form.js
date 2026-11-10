const items = document.getElementById("order-items");
const addButton = document.getElementById("add-order-item");
const template = document.getElementById("order-item-template");
const maxRows = 50;

function updateRows() {
    const rows = items.querySelectorAll("[data-order-row]");

    rows.forEach((row, index) => {
        row.querySelectorAll("[data-field]").forEach((input) => {
            input.name = `items[${index}].${input.dataset.field}`;
            input.id = `items-${index}-${input.dataset.field}`;
        });

        row.querySelector("[data-remove-row]").disabled = rows.length === 1;
    });

    addButton.disabled = rows.length >= maxRows;
}

addButton.addEventListener("click", () => {
    if (items.children.length >= maxRows) {
        return;
    }

    const row = template.content.firstElementChild.cloneNode(true);
    items.append(row);
    updateRows();
    row.querySelector("select").focus();
});

items.addEventListener("click", (event) => {
    const button = event.target.closest("[data-remove-row]");
    if (!button || items.children.length <= 1) {
        return;
    }

    button.closest("[data-order-row]").remove();
    updateRows();
    addButton.focus();
});

updateRows();