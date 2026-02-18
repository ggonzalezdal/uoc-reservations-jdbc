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
    return {
        // IMPORTANT: your API uses reservation_id
        id: r.id ?? r.reservationId ?? r.reservation_id ?? "",
        customerName: r.customerName ?? r.customer_name ?? r.full_name ?? r.guestName ?? "",
        startAt: r.startAt ?? r.start_at ?? "",
        endAt: r.endAt ?? r.end_at ?? "",
        partySize: r.partySize ?? r.party_size ?? r.pax ?? "",
        status: String(r.status ?? "").toUpperCase(),
        notes: r.notes ?? "",
        tableCodes: r.tableCodes || r.table_codes || [],
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
        tbody.innerHTML = `<tr><td colspan="8" class="muted">No reservations found.</td></tr>`;
        return;
    }

    tbody.innerHTML = rows.map((raw) => {
        const r = normalizeReservationRow(raw);

        const rid = r.id;
        if (!rid) {
            console.warn("Row missing id:", raw);
        }

        const canConfirm = r.status === "PENDING";
        const canCancel = r.status !== "CANCELLED";
        const canNoShow = r.status === "PENDING" || r.status === "CONFIRMED";
        const tables = Array.isArray(r.tableCodes) ? r.tableCodes : [];
        const tablesText = tables.length ? tables.join(", ") : "-";

        // Inline actions inside Notes cell (keeps HTML structure unchanged)
        const actions = `
      <div class="table-footer-actions" style="justify-content:flex-start; margin-top:8px;">
        <button class="ghost-btn" data-action="confirm" data-id="${escapeHtml(rid)}" ${canConfirm ? "" : "disabled"}>
          Confirm
        </button>
        <button class="ghost-btn" data-action="cancel" data-id="${escapeHtml(rid)}" ${canCancel ? "" : "disabled"}>
          Cancel
        </button>
        <button class="ghost-btn" data-action="no-show" data-id="${escapeHtml(rid)}" ${canNoShow ? "" : "disabled"}>
          No-show
        </button>
      </div>
    `;

        return `
      <tr>
        <td>${escapeHtml(rid)}</td>
        <td>${escapeHtml(r.customerName || "-")}</td>
        <td>${escapeHtml(formatDateTimeForCell(r.startAt) || "-")}</td>
        <td>${escapeHtml(formatDateTimeForCell(r.endAt) || "-")}</td>
        <td>${escapeHtml(r.partySize || "-")}</td>
        <td>${badgeHtml(r.status)}</td>
        <td>${escapeHtml(tablesText)}</td>
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

function normalizeCustomerRow(c) {
    return {
        id: c.id ?? c.customerId ?? c.customer_id ?? "",
        name: c.name ?? c.fullName ?? c.full_name ?? c.customerName ?? c.customer_name ?? ""
    };
}

export function renderCustomersSelect(customers) {
    const sel = document.getElementById("cr-customer");
    if (!sel) return;

    const rows = Array.isArray(customers) ? customers : [];
    if (rows.length === 0) {
        sel.innerHTML = `<option value="">(no customers)</option>`;
        return;
    }

    sel.innerHTML = rows
        .map((raw) => {
            const c = normalizeCustomerRow(raw);
            const id = c.id;
            const name = String(c.name ?? "").trim();
            const label = name ? `${name} (id:${id})` : `(id:${id})`;

            return `<option value="${escapeHtml(id)}">${escapeHtml(label)}</option>`;
        })
        .join("");
}

export function renderAvailableTablesCheckboxes(tables) {
    const box = document.getElementById("cr-tables-list");
    if (!box) return;

    const rows = Array.isArray(tables) ? tables : [];
    if (rows.length === 0) {
        box.innerHTML = `<div class="muted">No available tables for this window.</div>`;
        return;
    }

    box.innerHTML = rows.map(t => {
        const id = escapeHtml(t.id);
        const code = escapeHtml(t.code ?? "");
        const cap = escapeHtml(t.capacity ?? "");
        return `
      <label style="display:flex; gap:10px; align-items:center; padding:6px 4px; cursor:pointer;">
        <input type="checkbox" class="cr-table" value="${id}" />
        <span style="min-width:70px; font-weight:700;">${code || "TABLE"}</span>
        <span class="muted">cap: ${cap}</span>
        <span class="muted">id: ${id}</span>
      </label>
    `;
    }).join("");
}

export function setAvailableMeta(text) {
    const el = document.getElementById("cr-available-meta");
    if (el) el.textContent = String(text ?? "");
}
