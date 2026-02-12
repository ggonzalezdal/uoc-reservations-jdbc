// File: src/main/resources/public/js/render.js

import { formatDateTimeForCell } from "./state.js";

// --- DOM helpers ---
const $ = (id) => document.getElementById(id);

function escapeHtml(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function normalizeReservationRow(r) {
    // Your API returns ReservationListItem DTOs (customerName included).
    // Keep tolerant: accept common field variants.
    return {
        id: r.id ?? r.reservationId ?? "",
        customerName: r.customerName ?? r.customer_name ?? r.guestName ?? "",
        startAt: r.startAt ?? r.start_at ?? "",
        endAt: r.endAt ?? r.end_at ?? "",
        partySize: r.partySize ?? r.party_size ?? r.pax ?? "",
        status: String(r.status ?? "").toUpperCase(),
        notes: r.notes ?? ""
    };
}

function badgeHtml(status) {
    const s = String(status ?? "").toUpperCase();
    const cls =
        s === "PENDING" ? "pending" :
            s === "CONFIRMED" ? "confirmed" :
                s === "CANCELLED" ? "cancelled" :
                    s === "NO_SHOW" ? "no_show" : "";
    return `<span class="badge ${cls}">${escapeHtml(s || "-")}</span>`;
}

// --- Error card ---
export function hideError() {
    const card = $("error-card");
    const body = $("error-body");
    if (!card || !body) return;
    card.classList.add("hidden");
    body.textContent = "";
}

export function showError(err) {
    const card = $("error-card");
    const body = $("error-body");
    if (!card || !body) return;

    const payload = err?.payload ? JSON.stringify(err.payload, null, 2) : "";
    const msg = err?.message ? String(err.message) : "Request failed";

    body.textContent = payload ? `${msg}\n\n${payload}` : msg;
    card.classList.remove("hidden");
}

// --- Header status ---
export function setHealthValue(text) {
    const el = $("health-value");
    if (el) el.textContent = String(text ?? "");
}

export function setLoadedCount(n) {
    const el = $("loaded-count");
    if (el) el.textContent = String(n ?? 0);
}

export function setLastFetch(iso) {
    const el = $("last-fetch");
    if (!el) return;
    if (!iso) {
        el.textContent = "—";
        return;
    }
    const d = new Date(iso);
    el.textContent = Number.isNaN(d.getTime()) ? String(iso) : d.toLocaleString();
}

// --- View switching ---
export function setActiveView(viewId) {
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
    const target = document.getElementById(viewId);
    if (target) target.classList.add("active");

    document.querySelectorAll(".nav-link").forEach((b) => b.classList.remove("active"));
    document.querySelectorAll(`.nav-link[data-target="${viewId}"]`).forEach((b) => b.classList.add("active"));
}

// --- Reservations table ---
export function renderReservationsTable(rows) {
    const tbody = $("reservations-tbody");
    if (!tbody) return;

    if (!rows || rows.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="muted">No reservations found.</td></tr>`;
        return;
    }

    tbody.innerHTML = rows.map((raw) => {
        const r = normalizeReservationRow(raw);

        const canConfirm = r.status === "PENDING";
        const canCancel = r.status !== "CANCELLED";

        // Inline actions inside Notes cell (keeps HTML structure unchanged)
        const actions = `
      <div class="table-footer-actions" style="justify-content:flex-start; margin-top:8px;">
        <button class="ghost-btn" data-action="confirm" data-id="${escapeHtml(r.id)}" ${canConfirm ? "" : "disabled"}>
          Confirm
        </button>
        <button class="ghost-btn" data-action="cancel" data-id="${escapeHtml(r.id)}" ${canCancel ? "" : "disabled"}>
          Cancel
        </button>
      </div>
    `;

        return `
      <tr>
        <td>${escapeHtml(r.id)}</td>
        <td>${escapeHtml(r.customerName || "-")}</td>
        <td>${escapeHtml(formatDateTimeForCell(r.startAt) || "-")}</td>
        <td>${escapeHtml(formatDateTimeForCell(r.endAt) || "-")}</td>
        <td>${escapeHtml(r.partySize || "-")}</td>
        <td>${badgeHtml(r.status)}</td>
        <td>
          ${escapeHtml(r.notes || "")}
          ${actions}
        </td>
      </tr>
    `;
    }).join("");
}

export function setReservationsMeta(text) {
    const el = $("reservations-meta");
    if (el) el.textContent = String(text ?? "");
}
