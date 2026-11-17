// Englow çevrimiçi sıralama: günlük XP, aylık XP, güncel seri, en iyi seri
import { getStore } from "@netlify/blobs";
const H = { "access-control-allow-origin": "*", "access-control-allow-headers": "content-type", "access-control-allow-methods": "GET,POST,OPTIONS", "content-type": "application/json", "cache-control": "no-store" };
const clean = s => String(s || "").replace(/[^\p{L}\p{N} ._-]/gu, "").replace(/\s+/g, " ").trim().slice(0, 16) || "Player";
const num = (x, max) => Math.max(0, Math.min(max, Math.floor(+x || 0)));
export function build(board, D, M, id) {
  const E = Object.entries(board).map(([k, v]) => ({ k, ...v }));
  const top = (arr, f) => { const s = arr.filter(e => f(e) > 0).sort((a, b) => f(b) - f(a)); return { list: s.slice(0, 50).map(e => ({ n: e.n, a: e.a, v: f(e), lv: e.lv, me: e.k === id })), rank: id ? s.findIndex(e => e.k === id) + 1 : 0, total: s.length }; };
  const recent = e => Date.now() - e.t < 2 * 864e5;
  return { day: top(E.filter(e => e.d === D), e => e.dx), month: top(E.filter(e => e.m === M), e => e.mx), streak: top(E.filter(recent), e => e.s), best: top(E, e => e.bs), players: E.length };
}
export function upsert(board, b) {
  if (!b || !/^[a-z0-9]{8,40}$/i.test(b.id || "")) return false;
  if (b.off) { delete board[b.id]; return true; }
  board[b.id] = { n: clean(b.n), a: num(b.a, 99), d: String(b.d || "").slice(0, 10), dx: num(b.dx, 5000), m: String(b.m || "").slice(0, 7), mx: num(b.mx, 150000), s: num(b.s, 3650), bs: num(b.bs, 3650), lv: num(b.lv, 100), t: Date.now() };
  const ids = Object.keys(board); if (ids.length > 5000) ids.sort((x, y) => board[x].t - board[y].t).slice(0, ids.length - 5000).forEach(k => delete board[k]);
  return true;
}
export default async (req) => {
  if (req.method === "OPTIONS") return new Response("", { headers: H });
  const store = getStore("englow-league"), u = new URL(req.url);
  let board = (await store.get("board", { type: "json" })) || {};
  let id = u.searchParams.get("id") || "";
  if (req.method === "POST") {
    const b = await req.json().catch(() => null);
    if (!upsert(board, b)) return new Response('{"ok":false}', { status: 400, headers: H });
    await store.setJSON("board", board); id = b.id;
  }
  const D = u.searchParams.get("d") || new Date().toISOString().slice(0, 10), M = u.searchParams.get("m") || D.slice(0, 7);
  return new Response(JSON.stringify({ ok: true, ...build(board, D, M, id) }), { headers: H });
};
export const config = { path: "/api/lb" };
