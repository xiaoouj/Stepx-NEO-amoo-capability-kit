import Java from 'frida-java-bridge';
const MARKER='AMOO_NEWS_20261023_N1';
let session=null,until=0,n=0;
const seen=new Set();
function keys(x){return x&&typeof x==='object'&&!Array.isArray(x)?Object.keys(x):[];}
function out(event,data){if(++n<=800)send(Object.assign({event,elapsedMs:until?Date.now()-(until-600000):0},data||{}));}
function type(x){return x===null?'null':Array.isArray(x)?'array':typeof x;}
function shape(x,depth){
  if(typeof x==='string')return {type:'string',characters:x.length};
  if(x===null||typeof x!=='object')return {type:type(x)};
  if(Array.isArray(x))return {type:'array',length:x.length,items:depth>0?x.slice(0,4).map(v=>shape(v,depth-1)):undefined};
  const v={};if(depth>0)for(const k of Object.keys(x).slice(0,48))v[k]=shape(x[k],depth-1);
  return {type:'object',keys:keys(x),fields:depth>0?v:undefined};
}
function toolSummary(t){
  if(!t||typeof t!=='object')return {type:type(t)};
  const d={keys:keys(t)};
  const fn=t.function&&typeof t.function==='object'?t.function:t;
  const name=fn.name||t.toolName||t.tool_name;
  if(typeof name==='string'&&/^[a-zA-Z0-9_.:\/-]{1,160}$/.test(name))d.name=name;
  for(const k of ['status','type','state'])if(typeof t[k]==='string'&&/^[a-zA-Z0-9_.-]{1,60}$/.test(t[k]))d[k]=t[k];
  for(const k of ['arguments','args','result','output','error'])if(k in fn)d[k]=shape(fn[k],1);
  if(typeof fn.arguments==='string'){try{d.argumentKeys=keys(JSON.parse(fn.arguments));}catch(_){}}
  return d;
}
Java.perform(()=>{
  const Base=Java.use('com.stepos.agentbus.generated.AgentBusBindingBase');
  for(const o of Base.p0.overloads){
    if(o.argumentTypes[0].className!=='java.lang.String')continue;
    o.implementation=function(...args){
      try{
        const raw=String(args[0]);
        if(raw.includes(MARKER)){
          const f=JSON.parse(raw),p=f.params||{};
          if(f.method==='chat.send'&&typeof p.text==='string'&&p.text.includes(MARKER)){
            session=p.sessionId;until=Date.now()+600000;
            out('news_request_sent',{method:f.method,frameKeys:keys(f),parameterKeys:keys(p),stepclawShape:shape(p.stepclaw,3),characters:raw.length});
          }
        }
      }catch(e){out('observer_error',{phase:'send',error:String(e)});}
      return o.apply(this,args);
    };
  }
  const Channel=Java.use('com.stepos.agentframework.adapter.agentbus.ClawChannelAgentBusBinding');
  for(const o of Channel.r.overloads){
    if(o.argumentTypes[0].className!=='org.json.JSONObject')continue;
    o.implementation=function(...args){
      try{
        if(session!==null&&Date.now()<until){
          const raw=String(args[0].toString()),f=JSON.parse(raw),node=f.method==='node.event';
          const event=node&&f.params?f.params.event:f.event;
          const p=node&&f.params?f.params.payload:f.payload;
          if(p&&typeof p==='object'&&(p.sessionId===session||p.sessionKey===session)){
            // Coroutine resumption may expose the same frame twice; retain one exact observation.
            if(!seen.has(raw)){
              seen.add(raw);
              const m=p.message&&typeof p.message==='object'?p.message:p;
              const d={name:event,payloadKeys:keys(p),characters:raw.length};
              if(['assistant','user','system','tool'].includes(m.role))d.role=m.role;
              if(typeof m.text==='string')d.textCharacters=m.text.length;
              if(typeof m.delta==='boolean')d.delta=m.delta;
              for(const k of ['state','stream','type','status'])if(typeof p[k]==='string'&&/^[a-zA-Z0-9_.-]{1,60}$/.test(p[k]))d[k]=p[k];
              if(Array.isArray(m.toolCalls))d.toolCalls=m.toolCalls.map(toolSummary);
              if(Array.isArray(m.tool_calls))d.toolCalls=m.tool_calls.map(toolSummary);
              if(m.metadata)d.metadataShape=shape(m.metadata,3);
              if(p.data)d.dataShape=shape(p.data,3);
              if(p.actions)d.actionsShape=shape(p.actions,3);
              if(event&&event.startsWith('stepclaw.tasks.'))d.taskShape=shape(p,3);
              out('news_inbound',d);
            }
          }
        }
      }catch(e){out('observer_error',{phase:'receive',error:String(e)});}
      return o.apply(this,args);
    };
  }
  out('news_observer_ready',{marker:MARKER,behavior:'passive; scoped to controlled news session; tool names/states and field shapes only'});
});
