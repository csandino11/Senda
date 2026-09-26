import {books,bookById,gospels,allowed,tags,longChapters,isRelated,themes} from './catalog.js';

const DAY=86400000;
export const dateAt=(start,offset)=>new Date(Date.parse(start+'T12:00:00Z')+offset*DAY).toISOString().slice(0,10);
export const today=()=>{const d=new Date();return [d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');};
export const weekend=date=>[0,6].includes(new Date(date+'T12:00:00Z').getUTCDay());
export const readingLabel=r=>`${bookById[r.book]?.name??r.book} ${r.chapter}`;
export const shortLabel=r=>`${bookById[r.book]?.short??r.book} ${r.chapter}`;
export const completionKey=(date,index)=>`${date}#${index}`;
export const baseReadings=dc=>books.flatMap(book=>Array.from({length:book.chapters},(_,i)=>i+1).filter(chapter=>allowed(book,chapter,dc)).map(chapter=>({book:book.id,chapter,cycle:1})));
export function totalReadings(dc,prefs){const base=baseReadings(dc);return base.length+150*(prefs.repeatPsalms?1:0)+31*(prefs.proverbCycles-1)+89*(prefs.repeatGospels?1:0);}
export function estimateDays(start,dc,prefs){let n=0,cap=0,total=totalReadings(dc,prefs);while(cap<total){cap+=weekend(dateAt(start,n))?prefs.weekendChapters:prefs.weekdayChapters;n++;}return n;}
function rng(seed){let x=seed>>>0;return ()=>{x=(x+0x6D2B79F5)|0;let t=Math.imul(x^(x>>>15),1|x);t=(t+Math.imul(t^(t>>>7),61|t))^t;return ((t^(t>>>14))>>>0)/4294967296;};}
function shuffled(a,random){const out=[...a];for(let i=out.length-1;i>0;i--){const j=Math.floor(random()*(i+1));[out[i],out[j]]=[out[j],out[i]];}return out;}
const stage={
 MAT:[0,0,1,3,3,3,3,3,3,4,3,3,3,4,4,4,4,4,5,5,6,7,7,7,7,8,10,11],
 MRK:[1,3,3,3,3,4,4,4,4,5,6,7,7,8,10,11],
 LUK:[0,0,1,1,3,3,3,3,4,5,5,5,5,5,5,5,5,5,6,7,7,8,10,11],
 JHN:[1,2,2,2,3,4,5,5,5,5,5,6,8,8,8,8,8,9,10,11,11],
};
function chronology(list){const order=['LUK','MAT','MRK','JHN'];return [...list].sort((a,b)=>stage[a.book][a.chapter-1]-stage[b.book][b.chapter-1]||a.chapter-b.chapter||order.indexOf(a.book)-order.indexOf(b.book));}
const key=r=>r.book+'.'+r.chapter;
const isLong=r=>longChapters.has(key(r));
const score=(r,current,theme)=>{const own=tags(r);let result=own.includes(theme)?12:0;for(const other of current){result+=own.filter(t=>tags(other).includes(t)).length*12;if(isRelated(r,other))result+=90;if(r.book===other.book)result-=8;}return result;};
const weekKey=date=>{const d=new Date(date+'T12:00:00Z');const diff=(d.getUTCDay()+6)%7;d.setUTCDate(d.getUTCDate()-diff);return d.toISOString().slice(0,10);};
function build(start,theme,dc,prefs,seed,base,count){const random=rng(seed);const slots=Array.from({length:count},(_,i)=>{const date=dateAt(start,i);return {date,max:weekend(date)?prefs.weekendChapters:prefs.weekdayChapters,week:weekKey(date),readings:[]};});
 const capacity=s=>s.readings.some(isLong)&&s.max>=3?2:s.max;
 const themed=list=>shuffled(list,random).sort((a,b)=>Number(tags(b).includes(theme))-Number(tags(a).includes(theme)));
 function spread(readings,startIndex,endIndex,ordered=false,maxGap=Infinity,previousIndex=null){let prior=previousIndex??startIndex-1;for(let i=0;i<readings.length;i++){const r=readings[i],ideal=startIndex+Math.floor((i+.5)*(endIndex-startIndex)/readings.length);let lower=ordered?Math.max(startIndex,prior+1,i===readings.length-1?endIndex-maxGap:startIndex):startIndex;let upper=ordered?Math.min(endIndex-(readings.length-i),prior+maxGap):endIndex-1;let best=-1,bestScore=Infinity;
  for(let day=lower;day<=upper;day++){const s=slots[day];if(!s)continue;const limit=isLong(r)&&s.max>=3?2:capacity(s);if(s.readings.length>=limit)continue;if(gospels.has(r.book)&&s.readings.some(x=>gospels.has(x.book)))continue;if(isLong(r)&&slots.some(x=>x.week===s.week&&x.readings.some(isLong)))continue;
   const rank=Math.abs(day-ideal)*40-score(r,s.readings,theme)+(isLong(r)&&weekend(s.date)?-6:0)+s.readings.filter(x=>x.book===r.book).length*16+random()*3;if(rank<bestScore){best=day;bestScore=rank;}}
  if(best<0)throw Error(`Sin espacio para ${key(r)}`);slots[best].readings.push(r);prior=best;
 }}
 const gospel=base.filter(r=>gospels.has(r.book)),psalms=base.filter(r=>r.book==='PSA'),proverbs=base.filter(r=>r.book==='PRO');const ordinary=base.filter(r=>!gospels.has(r.book)&&r.book!=='PSA'&&r.book!=='PRO');
 const nt=ordinary.filter(r=>bookById[r.book].testament==='N'),other=ordinary.filter(r=>bookById[r.book].testament!=='N');
 spread(themed(nt),0,count,true);
 const gospelCycles=prefs.repeatGospels?2:1,gospelGap=Math.max(5,Math.ceil(count/(89*gospelCycles)));let priorGospel=-1;
 for(let c=0;c<gospelCycles;c++){const a=Math.floor(count*c/gospelCycles),b=Math.floor(count*(c+1)/gospelCycles),list=c===1?chronology(gospel):themed(gospel);spread(list.map((r,i)=>({...r,cycle:c+1,...(c===1?{gospelOrder:i}:{})})),a,b,true,gospelGap,priorGospel);priorGospel=slots.findLastIndex(s=>s.readings.some(r=>gospels.has(r.book)));}
 const cycles=(readings,n)=>{for(let c=0;c<n;c++){const a=Math.floor(count*c/n),b=Math.floor(count*(c+1)/n);spread(themed(readings).map(r=>({...r,cycle:c+1})),a,b);}};
 cycles(psalms,prefs.repeatPsalms?2:1);cycles(proverbs,prefs.proverbCycles);
 const longs=other.filter(isLong);let remaining=other.filter(r=>!isLong(r));spread(themed(longs),0,count);
 if(slots.reduce((sum,s)=>sum+capacity(s)-s.readings.length,0)<remaining.length)throw Error('Capacidad insuficiente');
 // Fill one reading per day first. This keeps the plan continuous and avoids a trailing gap.
 for(let day=0;day<count;day++){const s=slots[day];if(s.readings.length||!remaining.length)continue;let best=0,rank=-Infinity;for(let i=0;i<remaining.length;i++){const v=score(remaining[i],slots[day-1]?.readings||[],theme);if(v>rank){rank=v;best=i;}}s.readings.push(remaining.splice(best,1)[0]);}
 for(let day=0;day<count;day++){const s=slots[day];while(s.readings.length<capacity(s)&&remaining.length){let best=0,rank=-Infinity;for(let i=0;i<remaining.length;i++){const v=score(remaining[i],s.readings,theme);if(v>rank){rank=v;best=i;}}s.readings.push(remaining.splice(best,1)[0]);}}
 if(remaining.length)throw Error('Quedaron capítulos sin programar');
 for(const empty of slots.filter(s=>!s.readings.length)){const donor=slots.find(s=>s.readings.length>1&&s.readings.some(r=>!gospels.has(r.book)&&!isLong(r)));if(!donor)throw Error('Día vacío');const index=donor.readings.findIndex(r=>!gospels.has(r.book)&&!isLong(r));empty.readings.push(donor.readings.splice(index,1)[0]);}
 const days=slots.map(s=>{const readings=s.readings.sort((a,b)=>Number(gospels.has(b.book))-Number(gospels.has(a.book))||Number(tags(b).includes(theme))-Number(tags(a).includes(theme)));let pair=null;for(let a=0;a<readings.length;a++)for(let b=a+1;b<readings.length;b++)if(!pair&&isRelated(readings[a],readings[b]))pair=[readings[a],readings[b]];const common=readings.length?tags(readings[0]).filter(t=>readings.every(r=>tags(r).includes(t))):[];const focus=common.includes(theme)?theme:common[0]??theme;return {date:s.date,readings,focus,connection:pair?`En diálogo: ${shortLabel(pair[0])} y ${shortLabel(pair[1])}.`:`Un hilo común: ${themes.find(t=>t[0]===focus)[1].toLowerCase()}.`};});
 return {version:4,id:crypto.randomUUID(),year:+start.slice(0,4),theme,includeDeuterocanon:dc,seed,createdAt:new Date().toISOString(),startDate:start,pace:'legacy',preferences:prefs,days};
}
export function generatePlan(start,theme,dc,prefs,seed=Math.floor(Math.random()*2147483647)){
 if(!/^\d{4}-\d{2}-\d{2}$/.test(start)||!themes.some(t=>t[0]===theme)||!Number.isInteger(prefs.weekdayChapters)||prefs.weekdayChapters<1||prefs.weekdayChapters>4||!Number.isInteger(prefs.weekendChapters)||prefs.weekendChapters<1||prefs.weekendChapters>3||!Number.isInteger(prefs.proverbCycles)||prefs.proverbCycles<1||prefs.proverbCycles>4)throw Error('Opciones no válidas');
 const base=baseReadings(dc),minimum=estimateDays(start,dc,prefs);let failure;
 for(let n=minimum;n<=Math.min(minimum+90,totalReadings(dc,prefs));n++){try{const plan=build(start,theme,dc,prefs,seed,base,n);validatePlan(plan);return plan;}catch(error){failure=error;}}
 throw Error('No se pudo distribuir el plan: '+failure?.message);
}
export function validatePlan(plan){if(!plan||!Array.isArray(plan.days)||!plan.days.length||plan.days.length>5000||!/^\d{4}-\d{2}-\d{2}$/.test(plan.startDate)||!themes.some(t=>t[0]===plan.theme)||!bookById[plan.days[0]?.readings?.[0]?.book]&&plan.days[0]?.readings?.length)throw Error('Plan inválido');
 const p=plan.preferences;if(plan.version>=4&&!p)throw Error('Faltan las preferencias');if(p&&(!Number.isInteger(p.weekdayChapters)||p.weekdayChapters<1||p.weekdayChapters>4||!Number.isInteger(p.weekendChapters)||p.weekendChapters<1||p.weekendChapters>3||!Number.isInteger(p.proverbCycles)||p.proverbCycles<1||p.proverbCycles>4))throw Error('Preferencias inválidas');const all=plan.days.flatMap(d=>d.readings);if(p){if(all.length!==totalReadings(plan.includeDeuterocanon,p))throw Error('Cobertura incompleta');const counts=new Map();for(const r of all){if(!bookById[r.book]||!allowed(bookById[r.book],r.chapter,plan.includeDeuterocanon))throw Error('Lectura inválida');counts.set(key(r),(counts.get(key(r))||0)+1);}for(const r of baseReadings(plan.includeDeuterocanon)){const expected=gospels.has(r.book)?p.repeatGospels?2:1:r.book==='PSA'?p.repeatPsalms?2:1:r.book==='PRO'?p.proverbCycles:1;if(counts.get(key(r))!==expected)throw Error('Capítulo omitido o duplicado: '+key(r));}}
 const longWeeks=new Set();for(let i=0;i<plan.days.length;i++){const day=plan.days[i];if(day.date!==dateAt(plan.startDate,i)||!day.readings?.length)throw Error('Fechas discontinuas');if(!p)continue;const max=weekend(day.date)?p.weekendChapters:p.weekdayChapters,long=day.readings.filter(isLong);if(long.length>1||day.readings.length>(long.length&&max>=3?2:max)||day.readings.filter(r=>gospels.has(r.book)).length>1)throw Error('Límite diario excedido');if(long.length){const week=weekKey(day.date);if(longWeeks.has(week))throw Error('Más de un capítulo largo por semana');longWeeks.add(week);}}
 return true;
}
