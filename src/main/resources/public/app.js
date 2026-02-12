const statusEl = document.getElementById("status");
const btnLoad = document.getElementById("btn-load");
const tbody = document.getElementById("reservations-body");

btnLoad.addEventListener("click", loadReservations);

function setStatus(msg) {
    statusEl.textContent = msg || "";
}

function escapeHtml(s) {
    return String(s ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function loadReservations() {
    setStatus("Loading…");
    tbody.innerHTML = "";

    try {
        const res = await fetch("/reservations");
        const data = await res.json();

        if (!res.ok) {
            setStatus(`Error ${res.status}: ${data?.message ?? "Request failed"}`);
            return;
        }

        renderReservations(data);
        setStatus(`Loaded ${data.length} reservations`);
    } catch (err) {
        setStatus(`Network error: ${err.message}`);
    }
}

function renderReservations(items) {
    if (!Array.isArray(items) || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="muted">No reservations</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(r => `
    <tr>
      <td>${escapeHtml(r.reservationId)}</td>
      <td>${escapeHtml(r.customerName ?? r.fullName ?? r.full_name ?? "")}</td>
      <td>${escapeHtml(r.startAt)}</td>
      <td>${escapeHtml(r.endAt)}</td>
      <td>${escapeHtml(r.partySize)}</td>
      <td>${escapeHtml(r.status)}</td>
    </tr>
  `).join("");
}
