// File: src/main/resources/public/js/create.js

import { state, todayDateInputValue, datetimeLocalToOffset } from "./state.js";
import * as api from "./actions.js";
import {
    hideError,
    showError,
    setActiveView,
    renderCustomersSelect,
    renderAvailableTablesCheckboxes,
    setAvailableMeta
} from "./render.js";
import { saveActiveViewId } from "./storage.js";
import { loadReservations } from "./reservations.js";

function getCreateMode() {
    const checked = document.querySelector('input[name="cr-mode"]:checked');
    return checked?.value === "manual" ? "manual" : "auto";
}

function setManualBoxVisible(visible) {
    const box = document.getElementById("cr-manual-box");
    if (!box) return;
    box.style.display = visible ? "block" : "none";
}

function readSelectedTableIds() {
    return Array.from(document.querySelectorAll(".cr-table:checked"))
        .map(cb => Number(cb.value))
        .filter(n => Number.isFinite(n) && n > 0);
}

function clearManualSelectionUI() {
    document.querySelectorAll(".cr-table:checked").forEach(cb => { cb.checked = false; });
    renderAvailableTablesCheckboxes([]);
    setAvailableMeta("Not loaded");
}

async function loadCustomersIntoSelect() {
    hideError();

    const btn = document.getElementById("cr-load-customers");
    if (btn) btn.disabled = true;

    try {
        const customers = await api.listCustomers();
        renderCustomersSelect(Array.isArray(customers) ? customers : []);
    } catch (e) {
        showError(e);
    } finally {
        if (btn) btn.disabled = false;
    }
}

async function checkAvailabilityForCreate() {
    hideError();

    const startVal = document.getElementById("cr-start")?.value || "";
    const endVal = document.getElementById("cr-end")?.value || "";

    const start = datetimeLocalToOffset(startVal);
    const end = datetimeLocalToOffset(endVal);

    if (!start) {
        showError(new Error("Start datetime is required to check availability."));
        return;
    }

    const btn = document.getElementById("cr-check-availability");
    if (btn) btn.disabled = true;

    setAvailableMeta("Loading…");

    try {
        const tables = await api.listAvailableTables(start, end);
        const list = Array.isArray(tables) ? tables : [];
        renderAvailableTablesCheckboxes(list);
        setAvailableMeta(`Available: ${list.length}`);
    } catch (e) {
        setAvailableMeta("Failed");
        showError(e);
    } finally {
        if (btn) btn.disabled = false;
    }
}

async function submitCreateReservation() {
    hideError();

    const btn = document.getElementById("cr-submit");
    if (btn) btn.disabled = true;

    try {
        const customerId = Number(document.getElementById("cr-customer")?.value);
        const partySize = Number(document.getElementById("cr-party")?.value);
        const notes = (document.getElementById("cr-notes")?.value ?? "").trim();

        const start = datetimeLocalToOffset(document.getElementById("cr-start")?.value || "");
        const end = datetimeLocalToOffset(document.getElementById("cr-end")?.value || "");

        if (!Number.isFinite(customerId) || customerId <= 0) {
            showError(new Error("Customer is required."));
            return;
        }
        if (!start) {
            showError(new Error("Start datetime is required."));
            return;
        }
        if (!Number.isFinite(partySize) || partySize <= 0) {
            showError(new Error("Party size must be > 0."));
            return;
        }

        const mode = getCreateMode();

        const payload = {
            customerId,
            startAt: start,
            partySize,
            notes: notes || null
        };

        if (end) payload.endAt = end;

        if (mode === "manual") {
            const tableIds = readSelectedTableIds();
            if (tableIds.length === 0) {
                showError(new Error("Manual mode requires selecting at least one table."));
                return;
            }
            payload.tableIds = tableIds;
        }

        await api.createReservation(payload);

        const dateEl = document.getElementById("date");
        if (dateEl) dateEl.value = start.slice(0, 10);

        state.ui.activeViewId = "view-reservations";
        saveActiveViewId("view-reservations");
        setActiveView("view-reservations");

        await loadReservations(true);

    } catch (e) {
        showError(e);
    } finally {
        if (btn) btn.disabled = false;
    }
}

export function initCreateControls() {
    const startEl = document.getElementById("cr-start");
    if (startEl && !startEl.value) {
        const today = todayDateInputValue();
        startEl.value = `${today}T20:00`;
    }

    document.getElementById("cr-load-customers")
        ?.addEventListener("click", loadCustomersIntoSelect);

    document.querySelectorAll('input[name="cr-mode"]').forEach(r => {
        r.addEventListener("change", () => {
            const mode = getCreateMode();
            const isManual = (mode === "manual");

            setManualBoxVisible(isManual);

            if (!isManual) {
                clearManualSelectionUI();
            }
        });
    });

    const isManualInitial = (getCreateMode() === "manual");
    setManualBoxVisible(isManualInitial);
    if (!isManualInitial) clearManualSelectionUI();

    document.getElementById("cr-check-availability")
        ?.addEventListener("click", checkAvailabilityForCreate);

    document.getElementById("cr-submit")
        ?.addEventListener("click", submitCreateReservation);

    loadCustomersIntoSelect().catch(() => {});
}
