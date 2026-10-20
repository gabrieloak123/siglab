"use client";

import { FormEvent, useCallback, useState } from "react";
import styles from "./page.module.css";

type Task = { id:number; id_criado_por:number; id_atribuido_a:number|null; titulo:string; descricao:string|null; tags:string[]|null; id_coluna:number };
type Column = { id:number; nome:string; ordem:number; tarefas:Task[] };
type Board = { id:string; id_grupo_pesquisa:string; colunas:Column[] };
type Draft = { titulo:string; descricao:string; id_atribuido_a:string; id_coluna:string; tags:string };

const API = "/backend-api";
const emptyDraft: Draft = { titulo:"", descricao:"", id_atribuido_a:"", id_coluna:"", tags:"" };

async function request<T>(path:string, init?:RequestInit):Promise<T> {
  const response = await fetch(`${API}${path}`, init);
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message || `Erro HTTP ${response.status}`);
  }
  return response.status === 204 ? undefined as T : response.json();
}
const json = (method:string, body:unknown):RequestInit => ({ method, headers:{"Content-Type":"application/json"}, body:JSON.stringify(body) });
const tagsOf = (value:string) => value.split(",").map(tag => tag.trim()).filter(Boolean);

function TaskCard({ task, group, reload, startDrag }:{ task:Task; group:string; reload:()=>Promise<void>; startDrag:(id:number)=>void }) {
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ titulo:task.titulo, descricao:task.descricao || "", responsavel:task.id_atribuido_a?.toString() || "", tags:(task.tags || []).join(", ") });
  const path = `/api/research-groups/${encodeURIComponent(group)}/tasks/${task.id}`;

  async function save(event:FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    try {
      await request(path, json("PUT", { titulo:form.titulo, descricao:form.descricao || null, id_atribuido_a:form.responsavel ? Number(form.responsavel) : null, tags:tagsOf(form.tags) }));
      setEditing(false); await reload();
    } catch (e) { setError(e instanceof Error ? e.message : "Falha ao salvar"); }
    finally { setBusy(false); }
  }
  async function remove() {
    if (!confirm(`Excluir a tarefa #${task.id}?`)) return;
    setBusy(true); setError("");
    try { await request(path, {method:"DELETE"}); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Falha ao excluir"); setBusy(false); }
  }

  if (editing) return <form className={styles.card} onSubmit={save}>
    <label>Título<input required value={form.titulo} onChange={e=>setForm({...form,titulo:e.target.value})}/></label>
    <label>Descrição<textarea value={form.descricao} onChange={e=>setForm({...form,descricao:e.target.value})}/></label>
    <label>ID responsável<input min="1" type="number" value={form.responsavel} onChange={e=>setForm({...form,responsavel:e.target.value})}/></label>
    <label>Tags<input value={form.tags} onChange={e=>setForm({...form,tags:e.target.value})}/></label>
    {error && <p className={styles.error}>{error}</p>}
    <div className={styles.actions}><button disabled={busy}>Salvar</button><button type="button" className={styles.secondary} onClick={()=>setEditing(false)}>Cancelar</button></div>
  </form>;

  return <article className={`${styles.card} ${styles.draggable}`} draggable={!busy} onDragStart={e=>{e.dataTransfer.setData("text/plain",String(task.id));startDrag(task.id)}}>
    <div className={styles.meta}><span>#{task.id}</span><span>Criador #{task.id_criado_por}</span></div>
    <h3>{task.titulo}</h3>{task.descricao && <p>{task.descricao}</p>}
    <div className={styles.tags}>{task.tags?.map(tag=><span key={tag}>{tag}</span>)}</div>
    <small>{task.id_atribuido_a ? `Responsável #${task.id_atribuido_a}` : "Sem responsável"}</small>
    {error && <p className={styles.error}>{error}</p>}
    <div className={styles.actions}><button className={styles.secondary} onClick={()=>setEditing(true)}>Editar</button><button className={styles.danger} disabled={busy} onClick={remove}>Excluir</button></div>
  </article>;
}

export default function KanbanDebug() {
  const [input, setInput] = useState("");
  const [group, setGroup] = useState("");
  const [board, setBoard] = useState<Board|null>(null);
  const [draft, setDraft] = useState<Draft>(emptyDraft);
  const [creating, setCreating] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [loading, setLoading] = useState(false);
  const [dragged, setDragged] = useState<number|null>(null);
  const [target, setTarget] = useState<number|null>(null);
  const [error, setError] = useState("");

  const load = useCallback(async (id=group) => {
    if (!id) return; setLoading(true); setError("");
    try { setBoard(await request(`/api/research-groups/${encodeURIComponent(id)}/board`)); }
    catch (e) { setBoard(null); setError(e instanceof Error ? e.message : "Falha ao carregar"); }
    finally { setLoading(false); }
  },[group]);

  function open(event:FormEvent) { event.preventDefault(); const id=input.trim(); if(!id)return; setGroup(id); history.replaceState(null,"",`/kanban-debug?groupId=${encodeURIComponent(id)}`); void load(id); }
  async function move(column:number) {
    if (!dragged || !group) return; setTarget(null);
    if (board?.colunas.find(c=>c.tarefas.some(t=>t.id===dragged))?.id===column) { setDragged(null); return; }
    setLoading(true); setError("");
    try { await request(`/api/research-groups/${encodeURIComponent(group)}/tasks/${dragged}/column`,json("PATCH",{id_coluna_destino:column})); setDragged(null); await load(); }
    catch(e){setError(e instanceof Error?e.message:"Falha ao mover");setLoading(false)}
  }
  async function create(event:FormEvent) {
    event.preventDefault(); setCreating(true); setError("");
    try {
      await request(`/api/research-groups/${encodeURIComponent(group)}/tasks`,json("POST",{titulo:draft.titulo,descricao:draft.descricao||null,id_atribuido_a:draft.id_atribuido_a?Number(draft.id_atribuido_a):null,id_coluna:Number(draft.id_coluna),tags:tagsOf(draft.tags)}));
      setDraft(emptyDraft); setShowCreate(false); await load();
    } catch(e){setError(e instanceof Error?e.message:"Falha ao criar")}
    finally{setCreating(false)}
  }

  const taskCount=board?.colunas.reduce((sum,c)=>sum+c.tarefas.length,0)||0;
  return <main className={styles.page}>
    <header><div><span className={styles.kicker}>SIGLab · ferramenta interna</span><h1>Kanban debugger</h1><p>Inspecione o estado e teste todas as mutações da API.</p></div><code><i/> {API}</code></header>
    <section className={styles.toolbar}><form onSubmit={open}><label>ID do grupo</label><div><input placeholder="Ex.: lab-ia" value={input} onChange={e=>setInput(e.target.value)}/><button disabled={loading}>{loading?"Carregando…":"Carregar quadro"}</button></div></form>{board&&<nav><button className={styles.secondary} onClick={()=>void load()}>Atualizar</button><button onClick={()=>setShowCreate(!showCreate)}>{showCreate?"Fechar":"+ Nova tarefa"}</button></nav>}</section>
    {error&&<div className={styles.banner}><strong>Falha na operação</strong> {error}</div>}
    {board&&showCreate&&<form className={styles.create} onSubmit={create}><div className={styles.createTitle}><div><span className={styles.kicker}>POST /tasks</span><h2>Criar tarefa</h2></div><code>grupo: {group}</code></div><div className={styles.grid}>
      <label>Título<input required value={draft.titulo} onChange={e=>setDraft({...draft,titulo:e.target.value})}/></label>
      <label>Coluna<select required value={draft.id_coluna} onChange={e=>setDraft({...draft,id_coluna:e.target.value})}><option value="">Selecione</option>{board.colunas.map(c=><option key={c.id} value={c.id}>{c.nome} (#{c.id})</option>)}</select></label>
      <label>ID do responsável<input min="1" type="number" value={draft.id_atribuido_a} onChange={e=>setDraft({...draft,id_atribuido_a:e.target.value})}/></label>
      <label className={styles.wide}>Descrição<textarea value={draft.descricao} onChange={e=>setDraft({...draft,descricao:e.target.value})}/></label>
      <label className={styles.wide}>Tags, separadas por vírgula<input value={draft.tags} onChange={e=>setDraft({...draft,tags:e.target.value})}/></label>
    </div><button disabled={creating}>{creating?"Criando…":"Criar tarefa"}</button></form>}
    {!board&&!loading&&!error&&<section className={styles.empty}><b>⌘</b><h2>Nenhum quadro carregado</h2><p>Informe o ID de um grupo para começar.</p></section>}
    {board&&<><aside className={styles.stats}><span>Estado <b>{board.id}</b></span><span>Grupo <b>{board.id_grupo_pesquisa}</b></span><span>Colunas <b>{board.colunas.length}</b></span><span>Tarefas <b>{taskCount}</b></span></aside><section className={styles.board}>{board.colunas.map(column=><div key={column.id} className={`${styles.column} ${target===column.id?styles.target:""}`} onDragEnter={()=>setTarget(column.id)} onDragOver={e=>e.preventDefault()} onDrop={e=>{e.preventDefault();void move(column.id)}}><div className={styles.columnTitle}><div><b>{column.ordem}</b><h2>{column.nome}</h2></div><span>{column.tarefas.length}</span></div><div className={styles.list}>{column.tarefas.map(task=><TaskCard key={task.id} task={task} group={group} reload={()=>load()} startDrag={setDragged}/>)}{!column.tarefas.length&&<div className={styles.drop}>Solte uma tarefa aqui</div>}</div><footer>ID da coluna: {column.id}</footer></div>)}</section></>}
  </main>;
}
