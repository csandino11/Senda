const base='http://localhost:3000';const url=base+'/api/plan?tz=America%2FManagua';
let a=await fetch(url);let cookie=a.headers.get('set-cookie')?.split(';')[0];let first=await a.json();if(a.status!==200||first.plan!==null)throw Error(JSON.stringify(first));
let b=await fetch(url,{method:'POST',headers:{origin:base,cookie,'content-type':'application/json'},body:JSON.stringify({theme:'faith',extra:true})});let created=await b.json();if(!created.plan)throw Error(JSON.stringify(created));
let c=await fetch(url,{headers:{cookie}});let restored=await c.json();if(restored.plan.id!==created.plan.id)throw Error('Persistence failed');
let other=await (await fetch(url)).json();if(other.plan!==null)throw Error('Visitor isolation failed');
let d=await fetch(url,{method:'POST',headers:{origin:base,cookie,'content-type':'application/json'},body:JSON.stringify({theme:'love',extra:false,replace:true})});let changed=await d.json();if(!changed.plan||changed.plan.id===created.plan.id||changed.plan.extra!==false)throw Error('Regeneration failed');
let denied=await fetch(url,{method:'POST',headers:{origin:'https://other.test',cookie},body:'{}'});if(denied.status!==403)throw Error('Origin protection failed');
console.log('PASS: creation, cookie persistence, visitor isolation, regeneration, origin protection.');
