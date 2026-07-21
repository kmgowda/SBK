/*
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
'use strict';

const colors = ['#39d5ff', '#46e6a7', '#9b8cff', '#ffb454'];
const state = {run: null, completed: false, abandoned: false, snapshots: [], events: null, historyTimer: null};
const elements = Object.fromEntries([...document.querySelectorAll('[id]')].map(item => [item.id, item]));
const generatedBrowserId = globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `sbk-${Date.now()}-${Math.random().toString(16).slice(2)}`;
const browserId = sessionStorage.getItem('sbkDashboardBrowserId') || generatedBrowserId;
sessionStorage.setItem('sbkDashboardBrowserId', browserId);

function updateBrowserLease() {
    fetch('/api/v1/browser/connect', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({browserId}),
        keepalive: true
    }).catch(() => {});
}

function releaseBrowserLease() {
    navigator.sendBeacon('/api/v1/browser/disconnect', JSON.stringify({browserId}));
}

updateBrowserLease();
setInterval(updateBrowserLease, 15000);
window.addEventListener('pagehide', releaseBrowserLease);
window.addEventListener('pageshow', updateBrowserLease);

function compact(value) {
    return Intl.NumberFormat(undefined, {notation: 'compact', maximumFractionDigits: 2}).format(value || 0);
}

function bytes(value) {
    const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
    let number = Math.max(0, value || 0);
    let unit = 0;
    while (number >= 1024 && unit < units.length - 1) { number /= 1024; unit++; }
    return `${number.toFixed(unit ? 1 : 0)} ${units[unit]}`;
}

function duration(seconds) {
    const value = Math.max(0, Math.floor(seconds || 0));
    return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`;
}

function percentile(snapshot, target) {
    const labels = snapshot.latency.percentileLabels;
    if (!labels.length) return 0;
    let index = labels.findIndex(value => value >= target);
    if (index < 0) index = labels.length - 1;
    return snapshot.latency.percentiles[index] || 0;
}

function update() {
    const snapshot = state.snapshots[state.snapshots.length - 1];
    if (!state.run) return;
    const run = state.run;
    elements.title.textContent = run.name || `${run.storage} ${run.action.replace(/_/g, ' ')}`;
    elements.subtitle.textContent = `${run.source} ${run.sbkVersion}  •  ${run.storage}  •  Java ${run.javaVersion}`;
    if (!snapshot) {
        elements.status.textContent = state.abandoned ? 'ABANDONED' : 'WAITING';
        elements.status.className = `status ${state.abandoned ? 'abandoned' : 'waiting'}`;
        return;
    }
    const status = state.abandoned ? 'ABANDONED' : state.completed || snapshot.total ? 'COMPLETE' : 'RUNNING';
    elements.status.textContent = status;
    elements.status.className = `status ${status.toLowerCase()}`;
    elements.elapsed.textContent = duration(snapshot.performance.seconds);
    elements.recordsRate.textContent = compact(snapshot.performance.recordsPerSec);
    elements.throughput.textContent = `${snapshot.performance.mbPerSec.toFixed(2)} MB/s`;
    elements.p99.textContent = `${compact(percentile(snapshot, 99))} ${run.timeUnit}`;
    elements.workers.textContent = snapshot.workers.writers + snapshot.workers.readers;
    elements.connections.textContent = snapshot.workers.connections;
    elements.latencyUnit.textContent = run.timeUnit;
    elements.pending.textContent = compact(snapshot.requests.pendingWriteRecords
        + snapshot.requests.pendingReadRecords + snapshot.requests.pendingCombinedRecords);
    elements.timeouts.textContent = compact(snapshot.requests.writeTimeouts + snapshot.requests.readTimeouts);
    elements.invalid.textContent = compact(snapshot.latency.invalid);
    elements.discarded.textContent = compact(snapshot.latency.lowerDiscard + snapshot.latency.higherDiscard);
    elements.totalRecords.textContent = compact(snapshot.performance.records);
    elements.totalBytes.textContent = bytes(snapshot.performance.bytes);
    drawAll();
}

function mergeSnapshot(snapshot) {
    const existing = state.snapshots.findIndex(item => item.timestamp === snapshot.timestamp
        && item.total === snapshot.total);
    if (existing >= 0) {
        state.snapshots[existing] = snapshot;
    } else {
        state.snapshots.push(snapshot);
    }
    state.snapshots.sort((left, right) => left.timestamp - right.timestamp);
    if (state.snapshots.length > 3600) state.snapshots.splice(0, state.snapshots.length - 3600);
}

async function refreshHistory(runId) {
    const response = await fetch(`/api/v1/runs/${runId}/history`, {cache: 'no-store'});
    if (!response.ok) throw new Error(`History request returned HTTP ${response.status}`);
    const snapshots = await response.json();
    if (!state.run || state.run.runId !== runId) return;
    snapshots.forEach(mergeSnapshot);
    if (snapshots.some(snapshot => snapshot.total)) state.completed = true;
    update();
}

function drawChart(canvas, series) {
    const ratio = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = Math.max(1, rect.width * ratio);
    canvas.height = Math.max(1, rect.height * ratio);
    const ctx = canvas.getContext('2d');
    ctx.scale(ratio, ratio);
    const width = rect.width;
    const height = rect.height;
    const pad = {left: 54, right: 14, top: 18, bottom: 28};
    const plotWidth = width - pad.left - pad.right;
    const plotHeight = height - pad.top - pad.bottom;
    const values = series.flatMap(item => item.values);
    const maximum = Math.max(1, ...values);
    ctx.clearRect(0, 0, width, height);
    ctx.strokeStyle = 'rgba(140,164,186,.14)';
    ctx.fillStyle = '#7891a8';
    ctx.font = '11px system-ui';
    for (let line = 0; line <= 4; line++) {
        const y = pad.top + plotHeight * line / 4;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.stroke();
        ctx.fillText(compact(maximum * (1 - line / 4)), 4, y + 4);
    }
    series.forEach((item, seriesIndex) => {
        ctx.strokeStyle = colors[seriesIndex % colors.length];
        ctx.lineWidth = 2;
        ctx.beginPath();
        item.values.forEach((value, index) => {
            const x = pad.left + plotWidth * index / Math.max(1, item.values.length - 1);
            const y = pad.top + plotHeight * (1 - value / maximum);
            if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
        ctx.fillStyle = colors[seriesIndex % colors.length];
        ctx.fillRect(pad.left + seriesIndex * 125, height - 12, 10, 3);
        ctx.fillText(item.name, pad.left + 15 + seriesIndex * 125, height - 7);
    });
}

function drawAll() {
    const data = state.snapshots.slice(-600);
    drawChart(elements.rateChart, [
        {name: 'Completed', values: data.map(item => item.performance.recordsPerSec)},
        {name: 'Write requests', values: data.map(item => item.requests.writeRecordsPerSec)},
        {name: 'Read requests', values: data.map(item => item.requests.readRecordsPerSec)}
    ]);
    drawChart(elements.throughputChart, [
        {name: 'Completed', values: data.map(item => item.performance.mbPerSec)},
        {name: 'Write', values: data.map(item => item.requests.writeMbPerSec)},
        {name: 'Read', values: data.map(item => item.requests.readMbPerSec)}
    ]);
    drawChart(elements.latencyChart, [
        {name: 'p50', values: data.map(item => percentile(item, 50))},
        {name: 'p95', values: data.map(item => percentile(item, 95))},
        {name: 'p99', values: data.map(item => percentile(item, 99))},
        {name: 'p99.9', values: data.map(item => percentile(item, 99.9))}
    ]);
    drawChart(elements.workerChart, [
        {name: 'Writers', values: data.map(item => item.workers.writers)},
        {name: 'Readers', values: data.map(item => item.workers.readers)},
        {name: 'Connections', values: data.map(item => item.workers.connections)}
    ]);
}

async function selectRun(runView) {
    if (state.events) state.events.close();
    if (state.historyTimer) clearInterval(state.historyTimer);
    state.run = runView.run;
    state.completed = runView.completed;
    state.abandoned = runView.abandoned;
    state.snapshots = [];
    history.replaceState(null, '', `/?run=${state.run.runId}`);
    update();
    const runId = state.run.runId;
    await refreshHistory(runId);
    state.historyTimer = setInterval(() => {
        refreshHistory(runId).catch(error => {
            elements.subtitle.textContent = `Dashboard refresh error: ${error.message}`;
        });
    }, 2000);
    state.events = new EventSource(`/api/v1/runs/${runId}/events`);
    state.events.addEventListener('snapshot', event => {
        mergeSnapshot(JSON.parse(event.data));
        update();
    });
    state.events.addEventListener('complete', event => {
        state.completed = true;
        state.abandoned = JSON.parse(event.data).abandoned === true;
        update();
    });
}

async function loadRuns() {
    const response = await fetch('/api/v1/runs', {cache: 'no-store'});
    if (!response.ok) throw new Error(`Runs request returned HTTP ${response.status}`);
    const runs = await response.json();
    runs.sort((a, b) => b.run.startedAt - a.run.startedAt);
    elements.runs.innerHTML = '';
    runs.forEach(view => {
        const option = document.createElement('option');
        option.value = view.run.runId;
        option.textContent = `${view.run.name || view.run.storage + ' ' + view.run.action} — ${new Date(view.run.startedAt).toLocaleTimeString()}`;
        elements.runs.append(option);
    });
    if (!runs.length) return;
    const requested = new URLSearchParams(location.search).get('run');
    const selected = runs.find(item => item.run.runId === requested) || runs[0];
    elements.runs.value = selected.run.runId;
    elements.runs.onchange = () => selectRun(runs.find(item => item.run.runId === elements.runs.value));
    await selectRun(selected);
}

window.addEventListener('resize', drawAll);
loadRuns().catch(error => { elements.subtitle.textContent = `Dashboard error: ${error.message}`; });
