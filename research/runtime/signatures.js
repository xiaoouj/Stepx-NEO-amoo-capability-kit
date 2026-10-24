import Java from 'frida-java-bridge';
Java.perform(() => {
  for (const name of ['K6.n','com.stepos.agentframework.adapter.agentbus.AgentBusCoreAgentClient','com.stepos.agentframework.adapter.agentbus.ClawChannelAgentBusBinding$ChatApi']) {
    try {
      const C=Java.use(name);
      send({event:'runtime_signatures',className:name,methods:Array.from(C.class.getDeclaredMethods(),m=>String(m.toString())),fields:Array.from(C.class.getDeclaredFields(),f=>String(f.getName())+':'+String(f.getType().getName()))});
    } catch(e) {send({event:'signature_error',className:name,error:String(e)});}
  }
});
