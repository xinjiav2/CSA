---
layout: page
title: Study Records
permalink: /study-records/
---

<link rel="stylesheet" href="{{ '/assets/css/study-records.css' | relative_url }}">

<main class="tracker-app">
  <section class="hero">
    <div class="eyebrow">Option A · REST + GitHub Pages</div>
    <h1>AP CSA Study Records</h1>
    <p>Plan a topic, record focused minutes, and update your confidence as you learn.</p>
  </section>

  <section class="summary" aria-label="Study summary">
    <div class="metric"><strong id="total-count">0</strong>Total records</div>
    <div class="metric"><strong id="complete-count">0</strong>Completed</div>
    <div class="metric"><strong id="minute-count">0</strong>Minutes studied</div>
  </section>

  <section class="panel">
    <h2>Add or edit a record</h2>
    <form id="record-form">
      <input type="hidden" name="id">
      <label>Topic<input name="topic" maxlength="120" required placeholder="Unit 2: Using Objects"></label>
      <label>Subtopic<input name="subtopic" maxlength="160" required placeholder="String methods"></label>
      <label>Student name<input name="studentName" maxlength="80" required placeholder="Your name"></label>
      <label>Status<select name="status"><option>PLANNED</option><option>IN_PROGRESS</option><option>COMPLETED</option></select></label>
      <label>Minutes studied<input name="minutesStudied" type="number" min="0" value="0" required></label>
      <label>Confidence<select name="confidence"><option value="1">1 — New</option><option value="2">2</option><option value="3">3 — Developing</option><option value="4">4</option><option value="5">5 — Confident</option></select></label>
      <label class="wide">Notes<textarea name="notes" maxlength="500" rows="2" placeholder="What clicked? What needs review?"></textarea></label>
      <div class="form-actions wide"><button id="submit-button" type="submit">Add record</button><button id="cancel-button" class="secondary" type="button" hidden>Cancel edit</button></div>
    </form>
    <p id="message" class="message" role="status" aria-live="polite"></p>
  </section>

  <section class="panel">
    <div class="form-actions"><h2>All records</h2><button id="refresh-button" class="secondary" type="button">Refresh</button></div>
    <div class="table-wrap"><table><thead><tr><th>Topic</th><th>Student</th><th>Status</th><th>Minutes</th><th>Confidence</th><th>Notes</th><th>Actions</th></tr></thead><tbody id="record-rows"></tbody></table></div>
  </section>
</main>

<script src="{{ '/assets/js/config.js' | relative_url }}"></script>
<script src="{{ '/assets/js/study-records.js' | relative_url }}"></script>
