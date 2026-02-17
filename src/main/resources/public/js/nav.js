// File: src/main/resources/public/js/nav.js

import { state } from "./state.js";
import { hideError, setActiveView } from "./render.js";
import { saveActiveViewId } from "./storage.js";

export function initNav({ onEnterReservations } = {}) {
    const els = document.querySelectorAll("[data-target]");

    els.forEach((el) => {
        el.addEventListener("click", async (ev) => {
            ev.preventDefault();

            const target = el.dataset.target;
            if (!target) return;

            hideError();
            state.ui.activeViewId = target;
            saveActiveViewId(target);
            setActiveView(target);

            if (target === "view-reservations") {
                await onEnterReservations?.();
            }
        });
    });
}
