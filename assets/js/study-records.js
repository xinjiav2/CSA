const apiUrl = window.STUDY_API_BASE_URL;
const form = document.querySelector("#record-form");
const rows = document.querySelector("#record-rows");
const message = document.querySelector("#message");
const submitButton = document.querySelector("#submit-button");
const cancelButton = document.querySelector("#cancel-button");

function showMessage(text, kind = "info") {
  message.textContent = text;
  message.className = `message ${kind}`;
}

function escapeText(value) {
  const element = document.createElement("span");
  element.textContent = value ?? "";
  return element.innerHTML;
}

async function request(path = "", options = {}) {
  const response = await fetch(`${apiUrl}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}

function updateSummary(records) {
  document.querySelector("#total-count").textContent = records.length;
  document.querySelector("#complete-count").textContent =
    records.filter(record => record.status === "COMPLETED").length;
  const minutes = records.reduce((sum, record) => sum + record.minutesStudied, 0);
  document.querySelector("#minute-count").textContent = minutes;
}

function render(records) {
  updateSummary(records);
  if (!records.length) {
    rows.innerHTML = '<tr><td colspan="7" class="empty">No study records yet. Add your first one above.</td></tr>';
    return;
  }
  rows.innerHTML = records.map(record => `
    <tr>
      <td><strong>${escapeText(record.topic)}</strong><br><small>${escapeText(record.subtopic)}</small></td>
      <td>${escapeText(record.studentName)}</td>
      <td><span class="status ${record.status.toLowerCase()}">${record.status.replace("_", " ")}</span></td>
      <td>${record.minutesStudied}</td>
      <td>${"★".repeat(record.confidence)}${"☆".repeat(5 - record.confidence)}</td>
      <td>${escapeText(record.notes || "—")}</td>
      <td class="actions">
        <button type="button" data-edit="${record.id}">Edit</button>
        <button type="button" class="danger" data-delete="${record.id}">Delete</button>
      </td>
    </tr>`).join("");
}

async function loadRecords() {
  try {
    const records = await request();
    render(records);
    showMessage("Connected to the Spring API.", "success");
  } catch (error) {
    render([]);
    showMessage(`${error.message}. Start the backend on port 8585 and try again.`, "error");
  }
}

function resetForm() {
  form.reset();
  form.elements.id.value = "";
  submitButton.textContent = "Add record";
  cancelButton.hidden = true;
}

function formPayload() {
  return {
    topic: form.elements.topic.value.trim(),
    subtopic: form.elements.subtopic.value.trim(),
    studentName: form.elements.studentName.value.trim(),
    status: form.elements.status.value,
    minutesStudied: Number(form.elements.minutesStudied.value),
    confidence: Number(form.elements.confidence.value),
    notes: form.elements.notes.value.trim()
  };
}

form.addEventListener("submit", async event => {
  event.preventDefault();
  const id = form.elements.id.value;
  try {
    await request(id ? `/${id}` : "", {
      method: id ? "PUT" : "POST",
      body: JSON.stringify(formPayload())
    });
    showMessage(id ? "Record updated." : "Record created.", "success");
    resetForm();
    await loadRecords();
  } catch (error) {
    showMessage(error.message, "error");
  }
});

rows.addEventListener("click", async event => {
  const editId = event.target.dataset.edit;
  const deleteId = event.target.dataset.delete;
  try {
    if (editId) {
      const record = await request(`/${editId}`);
      Object.entries(record).forEach(([key, value]) => {
        if (form.elements[key]) form.elements[key].value = value ?? "";
      });
      submitButton.textContent = "Save changes";
      cancelButton.hidden = false;
      form.scrollIntoView({ behavior: "smooth" });
    }
    if (deleteId && window.confirm("Delete this study record?")) {
      await request(`/${deleteId}`, { method: "DELETE" });
      showMessage("Record deleted.", "success");
      await loadRecords();
    }
  } catch (error) {
    showMessage(error.message, "error");
  }
});

cancelButton.addEventListener("click", resetForm);
document.querySelector("#refresh-button").addEventListener("click", loadRecords);
loadRecords();
