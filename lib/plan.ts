import {books,bookMap,gospelIds,allowed,tagsFor,themes,isLinked} from './bible.ts';
export type Reading={book:string;chapter:number;cycle:number;from?:number;to?:number;order?:number};
export type Day={date:string;readings:Reading[];focus:string;connection:string};
export type Plan={version:number;id:string;year:number;theme:string;extra:boolean;seed:number;days:Day[];createdAt:string};
export const gospelVerses:Record<string,number[]>={
MAT:[25,23,17,25,48,34,29,34,38,42,30,50,58,36,39,28,27,35,30,34,46,46,39,51,46,75,66,20],
MRK:[45,28,35,41,43,56,37,38,50,52,33,44,37,72,47,20],
LUK:[80,52,38,44,39,49,50,56,62,42,54,59,35,35,32,31,37,43,48,47,38,71,56,53],
JHN:[51,25,36,54,47,71,53,59,41,42,57,50,38,31,27,33,26,40,42,31,25]
};
// A proposed harmony by narrative stages. Gospel authors sometimes arrange
// episodes thematically; no historically unique total event order is claimed.
// Splits preserve all verses exactly once, including narrative introductions.
export const stages=['Orígenes y nacimiento','Preparación del ministerio','Primeros encuentros en Judea','Ministerio en Galilea','Formación de los discípulos','Camino a Jerusalén','Entrada en Jerusalén','Enseñanza en Jerusalén','La última cena','Getsemaní y juicio','La crucifixión','Resurrección y envío'];
const stageChapters:Record<string,number[]>={
MAT:[0,0,1,3,3,3,3,3,3,4,3,3,3,4,4,4,4,4,5,5,6,7,7,7,7,8,10,11],
MRK:[3,3,3,3,3,4,4,4,4,5,6,7,7,8,10,11],
LUK:[0,0,1,3,3,3,3,3,4,5,5,5,5,5,5,5,5,5,6,7,7,8,10,11],
JHN:[2,2,2,2,3,4,5,5,5,5,5,6,8,8,8,8,8,9,10,11,11]
};
const splits:Record<string,Array<[number,number,number]>>={
'MAT.1':[[1,17,0],[18,25,0]],'MAT.2':[[1,12,0],[13,23,0]],'MAT.3':[[1,12,1],[13,17,1]],'LUK.1':[[1,4,0],[5,25,0],[26,38,0],[39,56,0],[57,80,0]],'LUK.2':[[1,20,0],[21,39,0],[40,52,0]],
'MAT.4':[[1,11,1],[12,25,3]],'MAT.26':[[1,16,7],[17,35,8],[36,75,9]],'MAT.27':[[1,26,9],[27,66,10]],
'MRK.1':[[1,8,1],[9,11,1],[12,13,1],[14,45,3]],'MRK.14':[[1,11,7],[12,31,8],[32,72,9]],'MRK.15':[[1,20,9],[21,47,10]],
'LUK.3':[[1,20,1],[21,22,1],[23,38,0]],'LUK.4':[[1,13,1],[14,44,3]],'LUK.9':[[1,50,4],[51,62,5]],
'LUK.19':[[1,27,5],[28,48,6]],'LUK.22':[[1,6,7],[7,38,8],[39,71,9]],'LUK.23':[[1,25,9],[26,56,10]],
'JHN.1':[[1,18,0],[19,34,1],[35,51,2]],'JHN.4':[[1,42,2],[43,54,3]],
'JHN.10':[[1,21,5],[22,42,5]],'JHN.12':[[1,11,5],[12,19,6],[20,50,7]],'JHN.19':[[1,16,9],[17,42,10]]
};
const earlyOrder:Record<string,number>={'LUK.1.1':0,'JHN.1.1':1,'MAT.1.1':2,'LUK.3.23':3,'LUK.1.5':10,'LUK.1.26':20,'LUK.1.39':30,'LUK.1.57':40,'MAT.1.18':50,'LUK.2.1':60,'LUK.2.21':70,'MAT.2.1':80,'MAT.2.13':90,'LUK.2.40':100,'MAT.3.1':10010,'MRK.1.1':10011,'LUK.3.1':10012,'MAT.3.13':10020,'MRK.1.9':10021,'LUK.3.21':10022,'MAT.4.1':10030,'MRK.1.12':10031,'LUK.4.1':10032,'JHN.1.19':10040};
export function chronology(){const result:Reading[]=[];for(const book of gospelIds)for(let chapter=1;chapter<=bookMap[book].chapters;chapter++){
const parts=splits[book+'.'+chapter]??[[1,gospelVerses[book][chapter-1],stageChapters[book][chapter-1]]];
for(const [from,to,stage] of parts) result.push({book,chapter,cycle:2,...(parts.length>1?{from,to}:{}),order:earlyOrder[book+'.'+chapter+'.'+from]??(stage*10000+chapter*100+gospelIds.indexOf(book)*10+from/100)});
}return result.sort((a,b)=>a.order!-b.order!)}
export function rng(seed:number){return ()=>{seed|=0;seed=seed+0x6D2B79F5|0;let t=Math.imul(seed^seed>>>15,1|seed);t=t+Math.imul(t^t>>>7,61|t)^t;return ((t^t>>>14)>>>0)/4294967296}}
export function shuffle<T>(a:T[],random:()=>number){const b=[...a];for(let i=b.length-1;i>0;i--){const j=Math.floor(random()*(i+1));[b[i],b[j]]=[b[j],b[i]]}return b}
export function label(r:Reading,short=false){const b=bookMap[r.book];if(['S3Y','SUS','BEL','LJE'].includes(r.book))return short?b.short:b.name;return (short?b.short:b.name)+' '+r.chapter+(r.from?':'+r.from+'-'+r.to:'')}
export function key(r:Reading){return r.book+'.'+r.chapter}
export function getDates(year:number){const dates:string[]=[];for(let d=new Date(Date.UTC(year,0,1));d.getUTCFullYear()===year;d.setUTCDate(d.getUTCDate()+1))dates.push(d.toISOString().slice(0,10));return dates}
export function baseReadings(extra:boolean):Reading[]{return books.flatMap(b=>Array.from({length:b.chapters},(_,i)=>i+1).filter(c=>allowed(b,c,extra)).map(chapter=>({book:b.id,chapter,cycle:1})))}
export function createPlan(year:number,theme:string,extra:boolean,seed:number,id='test'):Plan{
if(!themes.some(t=>t.id===theme)||!Number.isInteger(year)||year<1900||year>2400)throw Error('Configuración de plan no válida.');
const random=rng(seed),dates=getDates(year),days:Day[]=dates.map(date=>({date,readings:[],focus:theme,connection:''}));
const firstHalf=days.filter(d=>+d.date.slice(5,7)<=6).length;
const repeated=(r:Reading)=>r.book==='PSA'||r.book==='PRO'||gospelIds.includes(r.book);
const base=baseReadings(extra),remaining=base.filter(r=>!repeated(r));
function themed(list:Reading[]){return shuffle(list,random).sort((a,b)=>Number(tagsFor(b.book,b.chapter).includes(theme))-Number(tagsFor(a.book,a.chapter).includes(theme)))}
function spread(list:Reading[],start:number,end:number){const n=end-start;list.forEach((r,i)=>{const at=start+Math.floor((i+.45)*n/list.length);days[at].readings.push(r)})}
const psalms=base.filter(r=>r.book==='PSA'),proverbs=base.filter(r=>r.book==='PRO'),gospels=base.filter(r=>gospelIds.includes(r.book));
const firstPsalms=themed(psalms);spread(firstPsalms,0,firstHalf);
let secondPsalms=shuffle(psalms,random);if(secondPsalms.every((r,i)=>r.chapter===firstPsalms[i].chapter))secondPsalms.push(secondPsalms.shift()!);
spread(secondPsalms.map(r=>({...r,cycle:2})),firstHalf,days.length);
const previous:string[]=[];
for(let q=0;q<4;q++){let list=q===0?themed(proverbs):shuffle(proverbs,random);while(previous.includes(list.map(r=>r.chapter).join(',')))list.push(list.shift()!);previous.push(list.map(r=>r.chapter).join(','));const start=dates.findIndex(d=>+d.slice(5,7)===q*3+1),end=q===3?days.length:dates.findIndex(d=>+d.slice(5,7)===(q+1)*3+1);spread(list.map(r=>({...r,cycle:q+1})),start,end)}
spread(themed(gospels),0,firstHalf);spread(chronology(),firstHalf,days.length);
function score(r:Reading,day:Day){const tags=tagsFor(r.book,r.chapter);let s=tags.includes(theme)?1.5:0;for(const a of day.readings){s+=tags.filter(t=>tagsFor(a.book,a.chapter).includes(t)).length*3;s+=isLinked(key(r),key(a))?18:0;s-=a.book===r.book?5:0}return s+random()*.75}
function take(day:Day,ntOnly=false){let best=-1,max=-Infinity;for(let i=0;i<remaining.length;i++){if(ntOnly&&!bookMap[remaining[i].book].nt)continue;const s=score(remaining[i],day);if(s>max){max=s;best=i}}if(best<0)throw Error('No quedan lecturas compatibles.');day.readings.push(remaining.splice(best,1)[0])}
// Ensure every rolling three-day window contains a whole NT chapter.
for(let i=2;i<days.length;i++)if(!days.slice(i-2,i+1).some(d=>d.readings.some(r=>bookMap[r.book].nt&&!r.from)))take(days[i],true);
const total=days.reduce((s,d)=>s+d.readings.length,0)+remaining.length;
const targets=days.map(d=>Math.max(3,d.readings.length));let extraSlots=total-targets.reduce((a,b)=>a+b,0);
if(extraSlots<0)throw Error('No se alcanza el mínimo diario.');
const order=shuffle(days.map((_,i)=>i),random);
for(let round=4;extraSlots>0&&round<=5;round++)for(const i of order){if(extraSlots>0&&targets[i]<round){targets[i]++;extraSlots--}}
if(extraSlots!==0)throw Error('Se excede el máximo diario.');
// Start with constrained small days so they also receive thematic companions.
for(const i of [...order].sort((a,b)=>days[a].readings.length-days[b].readings.length))while(days[i].readings.length<targets[i])take(days[i]);
if(remaining.length)throw Error('Quedan capítulos sin asignar.');
for(const day of days){const counts=Object.fromEntries(themes.map(t=>[t.id,day.readings.filter(r=>tagsFor(r.book,r.chapter).includes(t.id)).length]));day.focus=Object.keys(counts).sort((a,b)=>counts[b]-counts[a]||(a===theme?-1:b===theme?1:0))[0];const pair=day.readings.flatMap((a,i)=>day.readings.slice(i+1).filter(b=>isLinked(key(a),key(b))).map(b=>[a,b]))[0];day.connection=pair?'En diálogo: '+label(pair[0])+' y '+label(pair[1])+'.':'Explora '+themes.find(t=>t.id===day.focus)!.name.toLowerCase()+' en distintos contextos.';day.readings=shuffle(day.readings,random); // Keep chronological gospel fragments ordered within the day.
day.readings.sort((a,b)=>a.order!==undefined&&b.order!==undefined?a.order-b.order:0)}
const plan:Plan={version:1,id,year,theme,extra,seed,days,createdAt:new Date().toISOString()};validatePlan(plan);return plan;
}
export function validatePlan(plan:Plan){
const errors:string[]=[],dates=getDates(plan.year);
if(plan.days.length!==dates.length||plan.days.some((d,i)=>d.date!==dates[i]))errors.push('Calendario incompleto');
for(let i=0;i<plan.days.length;i++){const d=plan.days[i];if(d.readings.length<3||d.readings.length>5)errors.push('Cantidad diaria');if(i>=2&&!plan.days.slice(i-2,i+1).some(x=>x.readings.some(r=>bookMap[r.book]?.nt&&!r.from)))errors.push('Intervalo NT');}
const expected=new Map(baseReadings(plan.extra).map(r=>[key(r),r.book==='PSA'?2:r.book==='PRO'?4:gospelIds.includes(r.book)?2:1]));
const actual=new Map<string,number>();const verses=new Map<string,number[]>();
for(const d of plan.days)for(const r of d.readings){if(!expected.has(key(r)))errors.push('Lectura excluida');if(r.cycle===2&&gospelIds.includes(r.book)){const arr=verses.get(key(r))??[];for(let v=r.from??1;v<=(r.to??gospelVerses[r.book][r.chapter-1]);v++)arr.push(v);verses.set(key(r),arr)}else actual.set(key(r),(actual.get(key(r))??0)+1);
const month=+d.date.slice(5,7);if((r.book==='PSA'||gospelIds.includes(r.book))&&r.cycle!==Math.ceil(month/6))errors.push('Ciclo semestral');if(r.book==='PRO'&&r.cycle!==Math.ceil(month/3))errors.push('Ciclo trimestral');}
for(const [k,v] of verses){const [id,c]=k.split('.');if(v.length!==gospelVerses[id][+c-1]||new Set(v).size!==v.length||v.some(x=>x<1||x>gospelVerses[id][+c-1]))errors.push('Cobertura de versículos');actual.set(k,(actual.get(k)??0)+1)}
for(const [k,n] of expected)if(actual.get(k)!==n)errors.push('Repetición o cobertura: '+k);
let last=-1;for(const d of plan.days)for(const r of d.readings)if(r.order!==undefined){if(r.order<last)errors.push('Orden de los Evangelios');last=r.order;}
for(const id of ['PSA','PRO']){const sequences=Array.from({length:id==='PSA'?2:4},(_,i)=>plan.days.flatMap(d=>d.readings).filter(r=>r.book===id&&r.cycle===i+1).map(r=>r.chapter).join(','));if(new Set(sequences).size!==sequences.length)errors.push('Ciclos iguales');}
if(errors.length)throw Error([...new Set(errors)].join('; '));return {days:dates.length,readings:plan.days.reduce((a,d)=>a+d.readings.length,0),chapters:expected.size};
}
export function bibleUrl(r:Reading,version='RVC'){const ids:Record<string,number>={RVC:146,NTV:127,TLAI:178};if(bookMap[r.book].dc)version='TLAI';const ref=r.book+'.'+r.chapter+(r.from?'.'+r.from+'-'+r.to:'');return 'https://www.bible.com/es/bible/'+ids[version]+'/'+ref+'.'+version}

