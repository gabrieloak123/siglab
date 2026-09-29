"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";

import { groupsApi, kanbanApi, messageOf } from "@/lib/api";
import { readStoredUser } from "@/lib/storage";
import type { GroupMember, KanbanBoard, KanbanTask, ResearchGroup, User } from "@/types";
import styles from "./page.module.css";

const parseTags = (value: string) =>
  value.split(",").map((tag) => tag.trim()).filter(Boolean);

function TaskCard({ task, members, groupId, reload, onDrag }:{
  task:KanbanTask; members:GroupMember[]; groupId:string;
  reload:()=>Promise<void>; onDrag:(id:number)=>void;
}) {
  const [editing,setEditing]=useState(false);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState("");
  const [title,setTitle]=useState(task.titulo);
  const [description,setDescription]=useState(task.descricao || "");
  const [assignee,setAssignee]=useState(task.id_atribuido_a?.toString() || "");
  const [tags,setTags]=useState((task.tags || []).join(", "));
  const assignedMember=members.find(member=>member.id===task.id_atribuido_a);

  async function save(event:FormEvent){
    event.preventDefault();setBusy(true);setError("");
    try{
      await kanbanApi.updateTask(groupId,task.id,{titulo:title.trim(),descricao:description.trim()||null,id_atribuido_a:assignee?Number(assignee):null,tags:parseTags(tags)});
      setEditing(false);await reload();
    }catch(caught){setError(messageOf(caught,{api:"Não foi possível atualizar a tarefa.",network:"Falha de conexão com o servidor."}));}
    finally{setBusy(false)}
  }

  async function remove(){
    if(!window.confirm(`Excluir a tarefa “${task.titulo}”?`))return;
    setBusy(true);setError("");
    try{await kanbanApi.removeTask(groupId,task.id);await reload()}
    catch(caught){setError(messageOf(caught,{api:"Não foi possível excluir a tarefa.",network:"Falha de conexão com o servidor."}));setBusy(false)}
  }

  if(editing)return <form className={styles.editCard} onSubmit={save}>
    <label>Título<input required value={title} onChange={event=>setTitle(event.target.value)}/></label>
    <label>Descrição<textarea value={description} onChange={event=>setDescription(event.target.value)}/></label>
    <label>Responsável<select value={assignee} onChange={event=>setAssignee(event.target.value)}><option value="">Sem responsável</option>{members.map(member=><option key={member.id} value={member.id}>{member.name}</option>)}</select></label>
    <label>Tags<input placeholder="artigo, urgente" value={tags} onChange={event=>setTags(event.target.value)}/></label>
    {error&&<p className={styles.inlineError}>{error}</p>}
    <div className={styles.actions}><button disabled={busy}>{busy?"Salvando...":"Salvar"}</button><button className={styles.secondary} type="button" onClick={()=>setEditing(false)}>Cancelar</button></div>
  </form>;

  return <article className={styles.task} draggable={!busy} onDragStart={event=>{event.dataTransfer.effectAllowed="move";onDrag(task.id)}}>
    <div className={styles.taskTop}><span>Tarefa #{task.id}</span><button aria-label="Editar tarefa" onClick={()=>setEditing(true)}>•••</button></div>
    <h3>{task.titulo}</h3>
    {task.descricao&&<p>{task.descricao}</p>}
    {!!task.tags?.length&&<div className={styles.tags}>{task.tags.map(tag=><span key={tag}>{tag}</span>)}</div>}
    <div className={styles.owner}><span>{assignedMember?.name?.charAt(0).toUpperCase()||"–"}</span>{assignedMember?.name||"Sem responsável"}</div>
    {error&&<p className={styles.inlineError}>{error}</p>}
    <button className={styles.remove} disabled={busy} onClick={remove}>Excluir tarefa</button>
  </article>;
}

export default function GroupAreaPage(){
  const params=useParams<{groupId:string}>();
  const groupId=decodeURIComponent(params.groupId);
  const [user,setUser]=useState<User|null>(null);
  const [group,setGroup]=useState<ResearchGroup|null>(null);
  const [members,setMembers]=useState<GroupMember[]>([]);
  const [board,setBoard]=useState<KanbanBoard|null>(null);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState("");
  const [showCreate,setShowCreate]=useState(false);
  const [dragged,setDragged]=useState<number|null>(null);
  const [dropTarget,setDropTarget]=useState<number|null>(null);
  const [creating,setCreating]=useState(false);
  const [draft,setDraft]=useState({titulo:"",descricao:"",responsavel:"",coluna:"",tags:""});

  const loadBoard=useCallback(async()=>{
    setBoard(await kanbanApi.board(groupId));
  },[groupId]);

  useEffect(()=>{
    let active=true;
    queueMicrotask(async()=>{
      const stored=readStoredUser();
      if(!active)return;
      setUser(stored);
      if(!stored){setLoading(false);return}
      try{
        const [allGroups,groupMembers,currentBoard]=await Promise.all([groupsApi.list(),groupsApi.members(groupId),kanbanApi.board(groupId)]);
        if(!active)return;
        const currentGroup=allGroups.find(item=>item.group_id===groupId)||null;
        const belongs=groupMembers.some(member=>member.id===stored.id);
        if(!currentGroup||!belongs){setError("Você não possui acesso a este grupo de pesquisa.");return}
        setGroup(currentGroup);setMembers(groupMembers);setBoard(currentBoard);
      }catch(caught){if(active)setError(messageOf(caught,{api:"Não foi possível carregar a área do grupo.",network:"Falha de conexão com o servidor."}))}
      finally{if(active)setLoading(false)}
    });
    return()=>{active=false};
  },[groupId]);

  async function refresh(){
    setError("");
    try{await loadBoard()}
    catch(caught){setError(messageOf(caught,{api:"Não foi possível atualizar o quadro.",network:"Falha de conexão com o servidor."}))}
  }

  async function createTask(event:FormEvent){
    event.preventDefault();if(!user?.id)return;setCreating(true);setError("");
    try{
      await kanbanApi.createTask(groupId,{id_criado_por:user.id,id_atribuido_a:draft.responsavel?Number(draft.responsavel):null,id_coluna:Number(draft.coluna),titulo:draft.titulo.trim(),descricao:draft.descricao.trim()||null,tags:parseTags(draft.tags)});
      setDraft({titulo:"",descricao:"",responsavel:"",coluna:"",tags:""});setShowCreate(false);await loadBoard();
    }catch(caught){setError(messageOf(caught,{api:"Não foi possível criar a tarefa.",network:"Falha de conexão com o servidor."}))}
    finally{setCreating(false)}
  }

  async function moveTask(columnId:number){
    if(!dragged)return;setDropTarget(null);
    const source=board?.colunas.find(column=>column.tarefas.some(task=>task.id===dragged));
    if(source?.id===columnId){setDragged(null);return}
    const snapshot=board;
    if(snapshot){
      const moving=snapshot.colunas.flatMap(column=>column.tarefas).find(task=>task.id===dragged);
      if(moving)setBoard({...snapshot,colunas:snapshot.colunas.map(column=>({...column,tarefas:column.id===columnId?[...column.tarefas,{...moving,id_coluna:columnId}]:column.tarefas.filter(task=>task.id!==dragged)}))});
    }
    try{await kanbanApi.moveTask(groupId,dragged,columnId);setDragged(null);await loadBoard()}
    catch(caught){setBoard(snapshot);setError(messageOf(caught,{api:"Não foi possível mover a tarefa.",network:"Falha de conexão com o servidor."}))}
  }

  if(loading)return <main className={styles.center}><div className={styles.spinner}/><p>Carregando área do grupo...</p></main>;
  if(!user)return <main className={styles.center}><h1>Entre para acessar o grupo</h1><p>Sua sessão não foi encontrada.</p><Link href="/">Voltar à página inicial</Link></main>;
  if(!group||!board)return <main className={styles.center}><h1>Área indisponível</h1><p>{error||"Grupo não encontrado."}</p><Link href="/">Voltar à página inicial</Link></main>;

  const total=board.colunas.reduce((sum,column)=>sum+column.tarefas.length,0);
  return <main className={styles.page}>
    <header className={styles.header}><div className={styles.headerInner}><Link href="/" className={styles.back}>← Meus grupos</Link><div className={styles.identity}><span>{group.group_id}</span><div><p>Grupo de pesquisa</p><h1>{group.name}</h1></div></div><div className={styles.members}><span>{members.slice(0,4).map(member=><i key={member.id} title={member.name}>{member.name.charAt(0).toUpperCase()}</i>)}</span><b>{members.length} {members.length===1?"membro":"membros"}</b></div></div></header>
    <div className={styles.content}>
      <section className={styles.sectionTitle}><div><span className={styles.eyebrow}>Organização do trabalho</span><h2>Tarefas</h2><p>Acompanhe as atividades do grupo e arraste cartões para atualizar o andamento.</p></div><div><span className={styles.total}>{total} {total===1?"tarefa":"tarefas"}</span><button onClick={()=>setShowCreate(!showCreate)}>{showCreate?"Fechar":"+ Nova tarefa"}</button></div></section>
      {error&&<div className={styles.banner}>{error}<button onClick={()=>setError("")}>×</button></div>}
      {showCreate&&<form className={styles.create} onSubmit={createTask}><div><h3>Nova tarefa</h3><p>Adicione uma atividade ao quadro do grupo.</p></div><div className={styles.formGrid}><label>Título<input required value={draft.titulo} onChange={e=>setDraft({...draft,titulo:e.target.value})}/></label><label>Coluna<select required value={draft.coluna} onChange={e=>setDraft({...draft,coluna:e.target.value})}><option value="">Selecione</option>{board.colunas.map(column=><option key={column.id} value={column.id}>{column.nome}</option>)}</select></label><label>Responsável<select value={draft.responsavel} onChange={e=>setDraft({...draft,responsavel:e.target.value})}><option value="">Sem responsável</option>{members.map(member=><option key={member.id} value={member.id}>{member.name}</option>)}</select></label><label>Tags<input placeholder="artigo, urgente" value={draft.tags} onChange={e=>setDraft({...draft,tags:e.target.value})}/></label><label className={styles.full}>Descrição<textarea value={draft.descricao} onChange={e=>setDraft({...draft,descricao:e.target.value})}/></label></div><button disabled={creating}>{creating?"Criando...":"Criar tarefa"}</button></form>}
      <section className={styles.board}>{board.colunas.map(column=><div key={column.id} className={`${styles.column} ${dropTarget===column.id?styles.dropTarget:""}`} onDragEnter={()=>setDropTarget(column.id)} onDragOver={event=>event.preventDefault()} onDrop={event=>{event.preventDefault();void moveTask(column.id)}}><div className={styles.columnHeader}><div><i/><h3>{column.nome}</h3></div><span>{column.tarefas.length}</span></div><div className={styles.taskList}>{column.tarefas.map(task=><TaskCard key={task.id} task={task} members={members} groupId={groupId} reload={refresh} onDrag={setDragged}/>)}{column.tarefas.length===0&&<div className={styles.emptyColumn}>Nenhuma tarefa nesta etapa</div>}</div></div>)}</section>
    </div>
  </main>;
}
