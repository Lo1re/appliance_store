"use strict";
const productPhoto = document.getElementById("product-main-image");
if (productPhoto) {
    document.querySelectorAll("[data-photo]").forEach(button => {
        button.addEventListener("click", () => {
            productPhoto.src = button.dataset.photo;
            document.querySelectorAll("[data-photo]").forEach(other => {
                other.setAttribute("aria-pressed", String(other === button));
            });
        });
    });
}
