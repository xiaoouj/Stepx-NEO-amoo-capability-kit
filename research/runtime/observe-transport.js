import Java from 'frida-java-bridge';
const MARKER='AMOO_RUNTIME_20261023_A5';
let session=null,until=0,n=0;
function out(event,data){if(++n<=100)send(Object.assign({event},data||{}));}
function keys(x){return x&&typeof x==='object'&&!Array.isArray(x)?Object.keys(x):[];}
Java.perform(()=>{
  const Base=Java.use('com.stepos.agentbus.generated.AgentBusBindingBase');
  for(const o of Base.p0.overloads){
    if(o.argumentTypes[0].className!=='java.lang.String')continue;
    o.implementation=function(...args){
      try{
        const raw=String(args[0]);
        if(raw.includes(MARKER)){
          const frame=JSON.parse(raw),p=frame.params||{};
          if(frame.method==='chat.send'&&typeof p.text==='string'&&p.text.includes(MARKER)){
            session=p.sessionId;until=Date.now()+90000;
            out('marked_transport_send',{method:frame.method,frameKeys:keys(frame),parameterKeys:keys(p),traceKeys:keys(p._meta&&p._meta.trace),characters:raw.length});
          }
        }
      }catch(e){out('send_shape_error',{error:String(e),stack:String(e.stack)});}
      return o.apply(this,args);
    };
  }
  const Channel=Java.use('com.stepos.agentframework.adapter.agentbus.ClawChannelAgentBusBinding');
  for(const o of Channel.r.overloads){
    if(o.argumentTypes[0].className!=='org.json.JSONObject')continue;
    o.implementation=function(...args){
      try{
        if(session!==null&&Date.now()<until){
          const raw=String(args[0].toString()),f=JSON.parse(raw);
          const node=f.method==='node.event';
          const event=node&&f.params?f.params.event:f.event;
          const p=node&&f.params?f.params.payload:f.payload;
          if(p&&typeof p==='object'&&(p.sessionId===session||p.sessionKey===session)){
            const m=p.message;
            const d={name:event,frameKeys:keys(f),payloadKeys:keys(p),messageKeys:keys(m),characters:raw.length,containsMarker:raw.includes(MARKER)};
            for(const key of ['state','stream','type','status'])if(typeof p[key]==='string'&&/^[a-zA-Z0-9_.-]{1,60}$/.test(p[key]))d[key]=p[key];
            if(m&&['assistant','user','system','tool'].includes(m.role))d.role=m.role;
            if(p.data&&typeof p.data==='object')d.dataKeys=keys(p.data);
            out('matched_inbound_shape',d);
          }
        }
      }catch(e){out('inbound_shape_error',{error:String(e),stack:String(e.stack)});}
      return o.apply(this,args);
    };
  }
  out('transport_observer_ready',{marker:MARKER,behavior:'passive, marker-scoped; no headers, secrets, or message contents emitted'});
});
