// File: src/main/resources/public/js/app.js

import { state } from "./state.js";
import { setActiveView } from "./render.js";
import { loadActiveViewId } from "./storage.js";
import { initNav } from "./nav.js";
import { initDashboard, refreshHealth } from "./dashboard.js";
import { initReservationsControls, loadReservations } from "./reservations.js";
import { initCreateControls } from "./create.js";

function boot() {
    initNav({ onEnterReservations: () => loadReservations(false) });
    initDashboard();
    initReservationsControls();
    initCreateControls();

    const savedView = loadActiveViewId();
    if (savedView && document.getElementById(savedView)) {
        state.ui.activeViewId = savedView;
    }
    setActiveView(state.ui.activeViewId);

    if (state.ui.activeViewId === "view-reservations") {
        loadReservations(false).catch(() => {});
    }

    if (state.ui.activeViewId === "view-landing") {
        refreshHealth().catch(() => {});
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
} else {
    boot();
}
