// File: src/main/resources/public/js/app.js
"use strict";

// Same-origin API (served by Javalin at http://localhost:7070)
const API_BASE = "";

document.addEventListener("DOMContentLoaded", () => {
    // ---- Navigation (WAR-style views) ----
    const navButtons = Array.from(document.querySelectorAll(".nav-link"));
    const navClickables = Array.from(document.querySelectorAll(".nav-clickable"));

    function showView(id) {
        document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
        const view = document.getElementById(id);
        if (view) view.classList.add("active");

        navButtons.forEach((b) => b.classList.toggle("active", b.dataset.target === id));
    }

    function wireNav(el) {
        el.addEventListener("click", () => {
            const target = el.dataset.target;
            if (target) showView(target);
        });
    }

    navButtons.forEach(wireNav);
    navClickables.forEach(wireNav);

    // ---- Dashboard bits ----
    const apiBaseEl = document.getElementById("api-base");
    if (apiBaseEl) apiBaseEl.textContent = window.location.origin;

    const healthValue = document.getElementById("health-value");
    const loadedCount = document.getElementById("loaded-count");
    const lastFetch = document.getElementById("last-fetch");
    const btnHealth = document.getElementById("btn-health");

    btnHealth?.addEventListener("click", async () => {
        try {
            const data = await apiGetJson("/health");
            healthValue.textContent = data?.status ?? "ok";
        } catch (e) {
            healthValue.textContent = "error";
        }
    });

    // ---- Reservations UI ----
    const dateInput = document.getElementById("date");
    const statusSelect = document.getElementById("status");
    const btnLoad = document.getElementById("btn-load");
    const btnClear = document.getElementById("btn-clear");
    const tbody = document.getElementById("reservations-tbody");
    const meta = document.getElementById("reservations-meta");

    const errorCard = document.getElementById("error-card");
    const errorBody = document.getElementById("error-body");

    // default date = today
    if (dateInput) {
        const today = new Date();
        dateInput.value = today.toISOString().slice(0, 10);
    }

    btnLoad?.addEventListener("click", async () => {
        hideError();
        setMeta("Loading…");

        try {
            const qs = buildReservationQuery(dateInput?.value, statusSelect?.value);
            const list = await apiGetJson(`/reservations${qs}`);

            renderReservations(Array.isArray(list) ? list : []);
            setMeta(`Loaded ${Array.isArray(list) ? list.length : 0} reservations`);
            loadedCount.textContent = String(Array.isArray(list) ? list.length : 0);
            lastFetch.textContent = new Date().toLocaleTimeString();
        } catch (e) {
            showError(e);
            setMeta("Failed to load");
        }
    });

    btnClear?.addEventListener("click", () => {
        hideError();
        if (tbody) tbody.innerHTML = "";
        setMeta("Cleared");
    });

    // Optional: auto-load once when entering reservations view
    // (keep simple for now; you can uncomment if you want)
    // btnLoad?.click();

    // ---- Helpers ----

    function buildReservationQuery(dateYYYYMMDD, status) {
        const params = new URLSearchParams();

        // If a day is selected -> convert to from/to window in local time with explicit offset.
        // We keep +01:00 because you're in Spain (and your examples use +01:00).
        // If you later want DST-safe behavior, we’ll generate offsets dynamically.
        if (dateYYYYMMDD) {
            const from = `${dateYYYYMMDD}T00:00:00+01:00`;
            const toDate = addDays(dateYYYYMMDD, 1);
            const to = `${toDate}T00:00:00+01:00`;

            params.set("from", from);
            params.set("to", to);
        }

        if (status) params.set("status", status);

        const s = params.toString();
        return s ? `?${encodeURI(s)}` : "";
    }

    function addDays(yyyyMmDd, days) {
        const [y, m, d] = yyyyMmDd.split("-").map(Number);
        const dt = new Date(Date.UTC(y, m - 1, d));
        dt.setUTCDate(dt.getUTCDate() + days);
        return dt.toISOString().slice(0, 10);
    }

    function renderReservations(items) {
        if (!tbody) return;

        tbody.innerHTML = "";

        for (const r of items) {
            const tr = document.createElement("tr");

            const start = formatIso(r.startAt);
            const end = r.endAt ? formatIso(r.endAt) : "—";
            const notes = r.notes ?? "";

            tr.appendChild(td(String(r.reservationId)));
            tr.appendChild(td(String(r.customerName ?? "")));
            tr.appendChild(td(start));
            tr.appendChild(td(end));
            tr.appendChild(td(String(r.partySize ?? "")));
            tr.appendChild(tdBadge(r.status));
            tr.appendChild(td(notes));

            tbody.appendChild(tr);
        }
    }

    function td(text) {
        const el = document.createElement("td");
        el.textContent = text;
        return el;
    }

    function tdBadge(status) {
        const el = document.createElement("td");
        const s = String(status ?? "").toUpperCase();

        const span = document.createElement("span");
        span.className = `badge ${badgeClass(s)}`;
        span.textContent = s || "—";

        el.appendChild(span);
        return el;
    }

    function badgeClass(s) {
        if (s === "PENDING") return "pending";
        if (s === "CONFIRMED") return "confirmed";
        if (s === "CANCELLED") return "cancelled";
        if (s === "NO_SHOW") return "no_show";
        return "";
    }

    function formatIso(iso) {
        if (!iso) return "—";
        // Your API returns Z timestamps; show in local human-readable format.
        try {
            const d = new Date(iso);
            // 2026-02-21 20:00
            const yyyy = d.getFullYear();
            const mm = String(d.getMonth() + 1).padStart(2, "0");
            const dd = String(d.getDate()).padStart(2, "0");
            const hh = String(d.getHours()).padStart(2, "0");
            const mi = String(d.getMinutes()).padStart(2, "0");
            return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
        } catch {
            return String(iso);
        }
    }

    async function apiGetJson(path) {
        const res = await fetch(`${API_BASE}${path}`, {
            headers: { Accept: "application/json" },
        });

        const text = await res.text();
        let json = null;
        try {
            json = text ? JSON.parse(text) : null;
        } catch {
            // non-json fallback
        }

        if (!res.ok) {
            const msg =
                (json && json.message) ||
                `HTTP ${res.status} ${res.statusText}\n\n${text || ""}`;
            throw new Error(msg);
        }

        return json;
    }

    function setMeta(text) {
        if (meta) meta.textContent = text;
    }

    function showError(err) {
        if (!errorCard || !errorBody) return;
        errorCard.classList.remove("hidden");
        errorBody.textContent = String(err?.message ?? err);
    }

    function hideError() {
        if (!errorCard || !errorBody) return;
        errorCard.classList.add("hidden");
        errorBody.textContent = "";
    }
});
