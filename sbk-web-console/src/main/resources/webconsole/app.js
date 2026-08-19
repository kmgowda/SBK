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
const api = {
    browserConnect: '/api/v1/browser/connect',
    browserDisconnect: '/api/v1/browser/disconnect',
    config: '/api/v1/config',
    runs: '/api/v1/runs'
};
const jsonHeaders = {'Content-Type': 'application/json'};
const bytesPerUnit = 1024;
const secondsPerMinute = 60;
const millisecondsPerSecond = 1000;
const displayPercentiles = [50, 95, 99, 99.9];
const primaryPercentile = 99;
const clockFieldWidth = 2;
const chartLayout = {
    padding: {left: 54, right: 14, top: 18, bottom: 28},
    gridLines: 4,
    lineWidth: 2,
    legendSpacing: 125,
    legendWidth: 10,
    legendHeight: 3,
    legendBottom: 12,
    legendTextOffset: 15,
    legendTextBottom: 7,
    axisLabelOffset: 4
};
const config = {};
const state = {run: null, runs: [], completed: false, abandoned: false, snapshots: [], events: null,
    historyTimer: null};
const elements = Object.fromEntries([...document.querySelectorAll('[id]')].map(item => [item.id, item]));
const generatedBrowserId = globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function'
    ? globalThis.crypto.randomUUID()
    : `sbk-${Date.now()}-${Math.random().toString(16).slice(2)}`;
const browserId = sessionStorage.getItem('sbkWebConsoleBrowserId') || generatedBrowserId;
sessionStorage.setItem('sbkWebConsoleBrowserId', browserId);

function updateBrowserLease() {
    fetch(api.browserConnect, {
        method: 'POST',
        headers: jsonHeaders,
        body: JSON.stringify({browserId}),
        keepalive: true
    }).catch(() => {});
}

function releaseBrowserLease() {
    navigator.sendBeacon(api.browserDisconnect, JSON.stringify({browserId}));
}

window.addEventListener('pagehide', releaseBrowserLease);
window.addEventListener('pageshow', updateBrowserLease);

function compact(value) {
    return Intl.NumberFormat(undefined, {notation: 'compact', maximumFractionDigits: 2}).format(value || 0);
}

function bytes(value) {
    const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
    let number = Math.max(0, value || 0);
    let unit = 0;
    while (number >= bytesPerUnit && unit < units.length - 1) { number /= bytesPerUnit; unit++; }
    return `${number.toFixed(unit ? 1 : 0)} ${units[unit]}`;
}

function duration(seconds) {
    const value = Math.max(0, Math.floor(seconds || 0));
    return `${String(Math.floor(value / secondsPerMinute)).padStart(clockFieldWidth, '0')}`
        + `:${String(value % secondsPerMinute).padStart(clockFieldWidth, '0')}`;
}

function elapsedSeconds(run, snapshot) {
    return Math.max(0, (snapshot.timestamp - run.startedAt) / millisecondsPerSecond);
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
    const status = state.abandoned ? 'ABANDONED' : state.completed ? 'COMPLETE' : 'RUNNING';
    elements.status.textContent = status;
    elements.status.className = `status ${status.toLowerCase()}`;
    elements.elapsed.textContent = duration(elapsedSeconds(run, snapshot));
    elements.recordsRate.textContent = compact(snapshot.performance.recordsPerSec);
    elements.throughput.textContent = `${snapshot.performance.mbPerSec.toFixed(2)} MB/s`;
    elements.p99.textContent = `${compact(percentile(snapshot, primaryPercentile))} ${run.timeUnit}`;
    elements.workers.textContent = snapshot.workers.writers + snapshot.workers.readers;
    elements.connections.textContent = snapshot.workers.connections;
    elements.latencyUnit.textContent = run.timeUnit;
    elements.pending.textContent = compact(snapshot.requests.pendingWriteRecords
        + snapshot.requests.pendingReadRecords + snapshot.requests.pendingCombinedRecords);
    elements.timeouts.textContent = compact(snapshot.requests.writeTimeouts + snapshot.requests.readTimeouts);
    elements.invalid.textContent = compact(snapshot.latency.invalid);
    elements.discarded.textContent = compact(snapshot.latency.lowerDiscard + snapshot.latency.higherDiscard);
    elements.windowRecords.textContent = compact(snapshot.performance.records);
    elements.windowBytes.textContent = bytes(snapshot.performance.bytes);
    drawAll();
}

function mergeSnapshot(snapshot) {
    const existing = state.snapshots.findIndex(item => item.timestamp === snapshot.timestamp);
    if (existing >= 0) {
        state.snapshots[existing] = snapshot;
    } else {
        state.snapshots.push(snapshot);
    }
    state.snapshots.sort((left, right) => left.timestamp - right.timestamp);
    if (state.snapshots.length > config.browserSnapshotLimit) {
        state.snapshots.splice(0, state.snapshots.length - config.browserSnapshotLimit);
    }
}

async function refreshHistory(runId) {
    const response = await fetch(`${api.runs}/${runId}/history`, {cache: 'no-store'});
    if (!response.ok) throw new Error(`History request returned HTTP ${response.status}`);
    const snapshots = await response.json();
    if (!state.run || state.run.runId !== runId) return;
    snapshots.forEach(mergeSnapshot);
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
    const pad = chartLayout.padding;
    const plotWidth = width - pad.left - pad.right;
    const plotHeight = height - pad.top - pad.bottom;
    const values = series.flatMap(item => item.values);
    const maximum = Math.max(1, ...values);
    ctx.clearRect(0, 0, width, height);
    ctx.strokeStyle = 'rgba(140,164,186,.14)';
    ctx.fillStyle = '#7891a8';
    ctx.font = '11px system-ui';
    for (let line = 0; line <= chartLayout.gridLines; line++) {
        const y = pad.top + plotHeight * line / chartLayout.gridLines;
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.stroke();
        ctx.fillText(compact(maximum * (1 - line / chartLayout.gridLines)), chartLayout.axisLabelOffset,
            y + chartLayout.axisLabelOffset);
    }
    series.forEach((item, seriesIndex) => {
        ctx.strokeStyle = colors[seriesIndex % colors.length];
        ctx.lineWidth = chartLayout.lineWidth;
        ctx.beginPath();
        item.values.forEach((value, index) => {
            const x = pad.left + plotWidth * index / Math.max(1, item.values.length - 1);
            const y = pad.top + plotHeight * (1 - value / maximum);
            if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
        ctx.fillStyle = colors[seriesIndex % colors.length];
        ctx.fillRect(pad.left + seriesIndex * chartLayout.legendSpacing,
            height - chartLayout.legendBottom, chartLayout.legendWidth, chartLayout.legendHeight);
        ctx.fillText(item.name, pad.left + chartLayout.legendTextOffset
            + seriesIndex * chartLayout.legendSpacing, height - chartLayout.legendTextBottom);
    });
}

function drawAll() {
    const data = state.snapshots.slice(-config.chartSnapshotLimit);
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
    drawChart(elements.latencyChart, displayPercentiles.map(value => ({
        name: `p${value}`,
        values: data.map(item => percentile(item, value))
    })));
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
            elements.subtitle.textContent = `Web Console refresh error: ${error.message}`;
        });
    }, config.refreshMillis);
    state.events = new EventSource(`${api.runs}/${runId}/events`);
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
    const response = await fetch(api.runs, {cache: 'no-store'});
    if (!response.ok) throw new Error(`Runs request returned HTTP ${response.status}`);
    const runs = await response.json();
    runs.sort((a, b) => b.run.startedAt - a.run.startedAt);
    state.runs = runs;
    elements.runs.innerHTML = '';
    runs.forEach(view => {
        const option = document.createElement('option');
        option.value = view.run.runId;
        option.textContent = `${view.run.name || view.run.storage + ' ' + view.run.action} — ${new Date(view.run.startedAt).toLocaleTimeString()}`;
        elements.runs.append(option);
    });
    if (!runs.length) return;
    const requested = (state.run ? state.run.runId : null)
        || new URLSearchParams(location.search).get('run');
    const selected = runs.find(item => item.run.runId === requested) || runs[0];
    elements.runs.value = selected.run.runId;
    elements.runs.onchange = () => selectRun(
        state.runs.find(item => item.run.runId === elements.runs.value));
    if (state.run && state.run.runId === selected.run.runId) {
        state.completed = selected.completed;
        state.abandoned = selected.abandoned;
        update();
        return;
    }
    await selectRun(selected);
}

async function start() {
    const response = await fetch(api.config, {cache: 'no-store'});
    if (!response.ok) throw new Error(`Configuration request returned HTTP ${response.status}`);
    Object.assign(config, await response.json());
    updateBrowserLease();
    setInterval(updateBrowserLease, config.browserHeartbeatMillis);
    await loadRuns();
    setInterval(() => loadRuns().catch(error => {
        elements.subtitle.textContent = `Web Console refresh error: ${error.message}`;
    }), config.refreshMillis);
}

window.addEventListener('resize', drawAll);
start().catch(error => { elements.subtitle.textContent = `Web Console error: ${error.message}`; });
