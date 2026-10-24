# StepX Studio static protocol findings

Sources are inert untrusted data. No scripts or instructions from the archive were executed. No external requests or Android actions were performed by this analysis worker.

## Conclusion

The recovered web client uses cookie-authenticated REST plus JSON WebSocket frames to drive a stateful coding agent. It is not evidence of an exposed OpenAI-compatible model endpoint. An adapter can translate a subset of agent text events after runtime validation; it cannot promise raw-model semantics or tool suppression.

## Protocol

WebSocket: wss://studio-api.stepx.com/ws. The browser constructor supplies no token argument, auth frame or subprotocol; cookies are therefore the likely identity path (runtime confirmation needed). Origin is the studio page.

Subscribe/replay: {"type":"project.switch","projectId":"PID","payload":{"projectSwitch":{"resumeSeq":{}}}}.

Send: {"type":"chat.send","projectId":"PID","sessionId":"SID","userLocale":"zh-CN","payload":{"chatSend":{"text":"text"}}}. sessionId/userLocale optional in static caller.

Ping: {"type":"ping"}; reply pong. Client sends after 30 seconds inactivity and closes if 10 seconds without response.

GET /api/projects/{PID}/sessions returns sessions:[{id,title,branch}]. POST same path with {title} creates a session.

GET /api/projects/{PID}/history?beforeSeq=NUMBER&sessionId=SID loads history.

Text events: message.append -> payload.messageAppend {messageId,kind,delta,snapshot,model,isAuto,startedAt,...}. snapshot true replaces assembled text. kind user is echoed input; kind text is assistant output. tool.use/tool.result carry agent tool activity. turn.completed/turn.failed terminal events carry model, commit/site metadata, error/recovery. Frames have projectId/sessionId/agentId/turnId/seq/ts; dedup is per session + numeric seq.

Model parameter observed on POST /api/projects; chatSend builder only includes text OR doc, plus attachments. Generic PATCH project exists, but model update handler was not recovered, so PATCH {model} remains an unverified hypothesis.

## Recorded capture

capture.har has 261 entries. GET /api/chat-models at indexes 112, 180, 245 returned auto; step-5-preview (Step 5 Preview); deepseek-flash[1m] (DeepSeek V4.1 Flash). Seven /ws upgrades 101 but every frames array in capture.json is empty; this capture has no successful chat completion.

## Evidence

REA parent: ev_bb617470a3cc5c838fd2e9f6857c847820f8fafe53d759a63f1a1ad402f42de3. Summary: ev_9719dc642fdfe42fa0fc59df11c52eac83908bf602da5a166c37d45ca93cef61. Four selected JS files parsed with zero parse failures, 51 modules. Coverage partial; 81 inventoried relevant files unselected.

Local source snippets (one-based source line plus zero-based Unicode character offset):

### js/3212-3cd0a2819eb8df3e.js

SHA256: ed691480c81e02cc1c81114c2c40bda1adea48b63d990b016ae31ccc56950f7e; line 1; char 11956.

```javascript

function O(){if(E)return;let e=k,t=new URL("/ws",(0,i.J2)({environment:"prod",apiOrigin:"https://studio-api.stepaos.com",nodeEnvironment:"production",hostname:globalThis.location?.hostname}).apiOrigin);t.protocol="https:"===t.protocol?"wss:":"ws:";let n=s=new WebSocket(t),r=()=>!E&&e===k&&s===n;s.onopen=()=>{if(r()){for(let e of((0,o.f)("ws.open",{projectId:C,generation:k}),g))e();if(m=1e3,T=Date.now(),C&&D(C),L&&s?.readyState===WebSocket.OPEN){let e=L;L=null,s.send(JSON.stringify(e)),(0,l.y8)("sent",e)}!function(){if(s?.readyState!==WebSocket.OPEN||0===R.length)return;let e=R;for(let t of(R=[],e))s.send(JSON.stringify(t)),(0,l.y8)("sent",t);console.info("[ws] 重连后补发",{projectId:C??"",count:e.length})}()}},s.onmessage=e=>{if(!r())return;let t=JSON.parse(e.data);if("user.banned"===t.type){console.warn("[ws] account access revoked",{projectId:C});let e=h;j(),e?.(t);return}if(["context.reset","turn.completed","turn.failed","sandbox.provisioning","ack"].includes(t.type)&&(0,o.f)("ws.frame",{type:t.type,projectId:t.projectId,sessionId:t.sessionId,agentId:t.agentId,turnId:t.turnId,seq:t.seq,ts:t.ts,replayHeld:d,awaitFreshReplay:f}),T=Date.now(),N&&(clearTimeout(N),N=null),"pong"!==t.type){if("context.reset"===t.type){if(t.projectId&&C&&t.projectId!==C||f)return;d?p=!0:(B(),C&&D(C))}else f&&(f=!1);if(void 0!==t.seq&&null!==t.seq){let e=Number(t.seq),n=t.sessionId??"main",r=c[n]??(c[n]=new Set);if(r.has(e)){("turn.completed"===t.type||"context.reset"===t.type)&&(0,o.f)("ws.frame.duplicate",{type:t.type,projectId:t.projectId,sessionId:t.sessionId,seq:t.seq});return}if(r.add(e),e>(u[n]??0)&&(u[n]=e),r.size>1e4){let e=(u[n]??0)-5e3;for(let t of r)t<=e&&r.delete(t)}}(0,l.y8)("received

```

### js/3212-3cd0a2819eb8df3e.js

SHA256: ed691480c81e02cc1c81114c2c40bda1adea48b63d990b016ae31ccc56950f7e; line 1; char 14317.

```javascript

function D(e){E||((0,o.f)("ws.subscribe",{projectId:e,readyState:s?.readyState,generation:k}),C=e,y=[],e&&H({type:"project.switch",projectId:e,payload:{projectSwitch:{resumeSeq:{...u}}}}))}function z(){return s?.readyState===WebSocket.OPEN}function $(){let e=s?.readyState;e!==WebSocket.OPEN&&0!==e&&2!==e&&(w&&(clearTimeout(w),w=null),s&&(s.onopen=null,s.onmessage=null,s.onerror=null,s.onclose=null),console.info("[ws]

```

### js/3212-3cd0a2819eb8df3e.js

SHA256: ed691480c81e02cc1c81114c2c40bda1adea48b63d990b016ae31ccc56950f7e; line 1; char 15579.

```javascript

function J(e,t){if(E)return void console.warn("[ws] first message blocked after access rejection",{projectId:e});let n=U({type:"chat.send",projectId:e,payload:{chatSend:(0,r.uY)(t)}});s?.readyState===WebSocket.OPEN?(s.send(JSON.stringify(n)),(0,l.y8)("sent",n)):L=n}},29164:(e,t,n)=>{"use strict";n.d(t,{A:()=>r});let r=(0,n(73741).v)(e=>({open:!1,show:()=>e({open:!0}),hide:()=>e({open:!1})}))},31476:(e,t,n)=>{"use strict";n.d(t,{HW:()=>p,Mu:()=>a,YT:()=>s,bO:()=>m,cx:()=>i,ig:

```

### js/3212-3cd0a2819eb8df3e.js

SHA256: ed691480c81e02cc1c81114c2c40bda1adea48b63d990b016ae31ccc56950f7e; line 1; char 31962.

```javascript

function v(e){let t=e.attachments?.length?{attachments:e.attachments}:{};return e.doc?{doc:e.doc,...t}:{text:e.text,...t}}},54488:(e,t,n)=>{"use strict";n.d(t,{C0:()=>s,aU:()=>p,ec:()=>i,gr:()=>r,hs:()=>c,kE:()=>l,pL:()=>o,pP:()=>f,qJ:()=>d});let r={

```

### js/6897-2351e91da562448e.js

SHA256: b8c19a892cb71648b1e3ef4abc7d62c6efc73abf830066ba427cfe936808520a; line 1; char 3674.

```javascript

async function r(e){let t=await (0,i.SS)(`/api/projects/${encodeURIComponent(e)}/sessions`),n=t&&"object"==typeof t?t.sessions:void 0;if(!Array.isArray(n)||!n.every(s))throw Error("会话列表格式不正确，请检查后端版本。");return n}async function o(e,t){let n=await (0,i.SS)(`/api/projects/${encodeURIComponent(e)}/sessions`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({title:t})}),r=n&&"object"==typeof n?n.session:void 0;if(!s(r))throw Error("新建会话响应格式不正确，请刷新会话列表确认结果。");return r}var d=n(27663),a=n(82799),u=n(55347),l=n(49905),c=n(54488),p=n(80338),f=n(82395),g=n(73741),I=n(53025),m=n(90935),y=n(3517);function S(e){let t=Number(e);r

```

### js/6897-2351e91da562448e.js

SHA256: b8c19a892cb71648b1e3ef4abc7d62c6efc73abf830066ba427cfe936808520a; line 1; char 4692.

```javascript

function _(e,t){let n=e.slice(),i=t.payload;if("message.append"===t.type&&i&&"messageAppend"in i){let e=i.messageAppend,s=S(t.ts),r="user"===e.kind&&t.turnId&&null==s?Math.floor(Date.now()/1e3):s,o="user"===e.kind?(0,l.e5)(e.doc):void 0,d=S(e.startedAt),a="user"===e.kind?{}:{...e.model&&e.model!==y.zB.auto?{model:e.model}:{},...void 0!==e.isAuto?{isAuto:e.isAuto}:{},...d?{startedAt:d}:{}};h(n,t=>t.key===e.messageId,()=>({key:e.messageId,kind:e.kind,text:e.delta??"",...o?{doc:o}:{},...e.attachments?.length?{attachments:e.attachments}:{},...e.images?.length?{images:e.images}:{},...r?{ts:r}:{},..."user"===e.kind&&t.turnId?{turnId:t.turnId}:{},...a}),n=>({...n,text:e.snapshot?e.delta??"":(n.text??"")+(e.delta??""),...o?{doc:o}:{},...e.attachments?.length?{attachments:e.attachments}:{},...e.images?.length?{images:e.images}:{},...null==n.ts&&r?{ts:r}:{},..."user"===e.kind&&null==n.turnId&&t.turnId?{turnId:t.turnId}:{},...a}))}else if("tool.use"===t.type&&i&&"toolUse"in i)h(n,e=>e.key===i.toolUse.toolCallId,()=>({key:i.toolUse.toolCallId,kind:"tool",toolName:i.toolUse.name,toolStatus:"running",text:i.toolUse.inputJson,turnId:t.turnId}),e=>e);else if("tool.result"===t.type&&i&&"toolResult"in i)h(n,e=>e.key===i.toolResult.toolCallId,()=>({key:i.toolResult.toolCallId,kind:"tool",toolName:"?"}),e=>({...e,toolStatus:i.toolResult.isError?"error":"done",toolContent:i.toolResult.content}));else if("turn.completed"===t.type&&i&&"turnCompleted"in i){k(n,"（轮次已结束——未收到工具结果）");let e=i.turnCompleted,s=S(e.startedAt),r=t.turnId??String(Math.random());return v(n,{key:r,kind:"turn",commitSha:e.commitSha,hasCodeChanges:!0===e.hasCodeChanges,siteUrl:e.siteArtifact?.status===y.To.ready?e.siteArtifact.siteUrl:void 0,sessionId:t.sessionId,turnNo:e.turnNo,...1===e.feedbackRating||-1===e.feedbackRating?{feedbackRating:e.feedbackRating}:{},ts:S(t.ts),...e.model?{model:e.model}:{},...e.isAuto?{isAuto:!0}:{},...s?{startedAt:s}:{}}),(0,m.vH)(n,r)}else if("turn.failed"===t.type&&i&&"turnFailed"in i){k(n,"（轮次失败——未收到工具结果）");let e=i.turnFailed,s=S(e.startedAt),r=t.turnId??"err";return v(n,{key:r,kind:"turn",error:(e.error??"").replace(/Claude Code/g,"Coding Agent"),...e.recovery===y.f3.turn_recovery_continue_offered?{continueOffered:!0}:{},ts:S(t.ts),...e.model?{model:e.model}:{},...e.isAuto?{isAuto:!0}:{},...s?{startedAt:s}:{}}),(0,m.vH)(n,r)}else if("compact.status"===t.type&&i&&"compactStatus"in i){let e=i.compactStatus,s=e.phase;if("compacting"===s||"done"===s||"failed"===s){let i=n.findIndex(e=>"compact"===e.kind&&"running"===e.toolStatus),r=S(e.preTokens),o=S(e.postTokens),d="done"===s&&null!=r&&null!=o?`${Math.round(r/1e3)}k→${Math.round(o/1e3)}k`:void 0,a={key:"compacting"===s?i>=0?n[i].key:"compact:live":t.seq?`compact:${t.seq}`:`compact:${n.length}`,kind:"compact",toolStatus:"compacting"===s?"running":"failed"===s?"error":"done",...d?{text:d}:{},...t.turnId?{turnId:t.turnId}:{}};i>=0?n[i]=a:n.push(a)}}else"action.fold"===t.type||"ack"===t.type&&k(n,"（新一轮开始——上一轮调用未完成）",e=>e.turnId!==t.turnId);return n}var B=n(25664),j=n(94912),q=n(15093);let N=(e,t)=>`${e??""}|${t}`,A=new WeakMap;function b(e){let t=A.get(e);return t||(t={latest:new Map,liveTurns:new Map,pending:new Map,lastSid:null,timer:null},A.set(e,t)),t}function x(e){let t=A.get(e);t&&(t.timer&&clearTimeout(t.timer),A.delete(e))}function E(e,t,n){if(!n)return;let i=b(e);i.latest.get(t)!==n&&(i.latest.set(t,n),i.pe

```

### js/4764-b27121fa8faf6af0.js

SHA256: c7b234f7b4cfbe401ec90491c3cfdbf2ba0e0ab58f30bbd60f4849e27343ba39; line 1; char 15923.

```javascript

history:(e,t,r)=>{let n=new URLSearchParams({beforeSeq:String(t)});return r&&n.set("sessionId",r),(0,o.pq)(`${i(e)}/history?${n}`)},feedback:(e,t)=>(0,o.pq)(`${i(e)}/feedback`,n("POST",t))}},92216:(e,t,r)=>{r.d(t,{n:()=>n});var o=r(82395),i=r(39304);function n(){let e=(0,o.useId)(),[t]=(0,o.useState

```

### js/4764-b27121fa8faf6af0.js

SHA256: c7b234f7b4cfbe401ec90491c3cfdbf2ba0e0ab58f30bbd60f4849e27343ba39; line 1; char 6998.

```javascript

model:(0,n.gA)(o),builderType:f,..."app"===f?{productType:e.productType??p.kM.app}:{},description:u,sourceProjectId:e.sourceProjectId,...a?{template:a}:{},...e.source?{source:e.source}:{}});try{r=await c.json()}catch(e){throw new s.hD("创建项目响应格式不正确",void 0,{cause:e})}let d=r?.project;if(!d||"string"!=typeof d.id||!d.id.trim()||"string"!=typeof d.name)throw new s.hD("创建项目响应格式不正确，请刷新项目列表确认创建结果。");if(t={...d,projectType:(0,l._v)(d.projectType),builderType:d.builderType||f,model:d.model??(0,n.gA)(o)},b=d.id,g=d.name,"app"!==f&&d.builderType!==f)throw v(e=>({projects:[...e.projects,d]})),new s.hD("当

```

### js/3212-3cd0a2819eb8df3e.js

SHA256: ed691480c81e02cc1c81114c2c40bda1adea48b63d990b016ae31ccc56950f7e; line 1; char 64720.

```javascript

let r=new Map([["studio.stepaos.com",{apiOrigin:"https://studio-api.stepaos.com",loginHost:"https://login.stepaos.com",ucHost:"https://i.stepaos.com"}],["studio.stepx.com",{apiOrigin:"https://studio-api.stepx.com",loginHost:"https://login-studio.stepx.com",ucHost:"https://login-studio.stepx.com"}]]);function i({environment:e,apiOrigin:t,nodeEnvironment:n}={}){let r,o=e||"test";if("test"!==o&&"prod"!==o)throw Error("NEXT_PUBLIC

```