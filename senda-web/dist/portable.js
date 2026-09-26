import {validatePlan,completionKey} from './plan.js';
import {bookById} from './catalog.js';

const MAGIC=new Uint8Array([83,69,78,68,65,1]);
const MAX_FILE=1000000,MAX_JSON=3000000;
const fileName=plan=>`Senda-plan-${plan.startDate}-${plan.days.at(-1).date}.senda`;
export function downloadBytes(data,name,type){const url=URL.createObjectURL(new Blob([data],{type}));const a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),60000);}
async function transform(bytes,kind){if(!(kind in window))throw Error('Tu navegador no admite archivos comprimidos. Actualízalo para usar respaldos .senda.');const stream=new Blob([bytes]).stream().pipeThrough(new window[kind]('gzip'));return new Uint8Array(await new Response(stream).arrayBuffer());}
export async function exportBackup(plan,completed,bibleVersion){validatePlan(plan);const data={format:1,plan,completed:[...completed].sort(),bibleVersion};const gz=await transform(new TextEncoder().encode(JSON.stringify(data)),'CompressionStream');const file=new Uint8Array(MAGIC.length+gz.length);file.set(MAGIC);file.set(gz,MAGIC.length);downloadBytes(file,fileName(plan),'application/vnd.senda.plan');}
export async function importBackup(file){if(file.size>MAX_FILE)throw Error('El respaldo excede el límite admitido.');const encoded=new Uint8Array(await file.arrayBuffer());if(encoded.length<7||MAGIC.some((x,i)=>encoded[i]!==x))throw Error('Este archivo no es un respaldo .senda válido.');const json=await transform(encoded.slice(6),'DecompressionStream');if(json.length>MAX_JSON)throw Error('El respaldo excede el límite de datos.');const data=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(json));if(data.format!==1)throw Error('Formato de respaldo incompatible.');validatePlan(data.plan);const keys=new Set(data.plan.days.flatMap(day=>day.readings.map((_,index)=>completionKey(day.date,index))));if(!Array.isArray(data.completed)||data.completed.some(key=>!keys.has(key)))throw Error('El progreso del respaldo está dañado.');return {plan:data.plan,completed:new Set(data.completed),bibleVersion:data.bibleVersion||'RVC'};}

const enc=new TextEncoder();
const ascii=s=>enc.encode(s);
const pdfEncoded=s=>Uint8Array.from(Array.from(s,c=>c.charCodeAt(0)<=255?c.charCodeAt(0):63));
const latin=s=>Array.from(s.normalize('NFC')).map(ch=>{if(ch==='–')return String.fromCharCode(150);if(ch==='—')return String.fromCharCode(151);const code=ch.charCodeAt(0);return code>=32&&code<=255?String.fromCharCode(code):'?';}).join('');
const safe=s=>latin(s).replace(/\\/g,'\\\\').replace(/\(/g,'\\(').replace(/\)/g,'\\)');
const rgb=hex=>[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16)/255).map(n=>n.toFixed(3)).join(' ');
const pdfBytes=objects=>{const parts=[pdfEncoded('%PDF-1.4\n%âãÏÓ\n')],offsets=[0];let size=parts[0].length;objects.forEach((object,i)=>{offsets.push(size);const raw=pdfEncoded(`${i+1} 0 obj\n${object}\nendobj\n`);parts.push(raw);size+=raw.length;});const xref=size;const tail=`xref\n0 ${objects.length+1}\n0000000000 65535 f \n${offsets.slice(1).map(o=>String(o).padStart(10,'0')+' 00000 n \n').join('')}trailer\n<< /Size ${objects.length+1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;parts.push(ascii(tail));const out=new Uint8Array(parts.reduce((sum,p)=>sum+p.length,0));let at=0;for(const p of parts){out.set(p,at);at+=p.length;}return out;};
function drawText(text,x,y,font,size,color){return `${color} rg BT /${font} ${size} Tf 1 0 0 1 ${x.toFixed(1)} ${y.toFixed(1)} Tm (${safe(text)}) Tj ET\n`;}
function rect(x,y,w,h,color){return `${color} rg ${x.toFixed(1)} ${y.toFixed(1)} ${w.toFixed(1)} ${h.toFixed(1)} re f\n`;}
const formatPdfDate=date=>{const [year,month,day]=date.split('-');return `${day}-${month}-${year}`;};
const glyphWidths={A:667,B:667,C:722,D:722,E:667,F:611,G:778,H:722,I:278,J:500,K:667,L:556,M:833,N:722,O:778,P:667,Q:778,R:722,S:667,T:611,U:722,V:667,W:944,X:667,Y:667,Z:611,a:556,b:556,c:500,d:556,e:556,f:278,g:556,h:556,i:222,j:222,k:500,l:222,m:833,n:556,o:556,p:556,q:556,r:333,s:500,t:278,u:556,v:500,w:722,x:500,y:500,z:500,' ':278,'-':333,'.':278,',':278,':':278,'/':278};
function textWidth(text,size,bold=false){let width=0;for(const char of text.normalize('NFD').replace(/[\u0300-\u036f]/g,'')){width+=glyphWidths[char]??(/[0-9]/.test(char)?556:char==='–'?556:556);}return width*size*(bold?1.03:1)/1000;}
function wrapReadings(refs,maxWidth,size){const lines=[];let current='';for(const ref of refs){const next=current?`${current}, ${ref}`:ref;if(current&&textWidth(next,size)>maxWidth){lines.push(current);current=ref;}else current=next;}if(current)lines.push(current);return lines;}
export function createPlanPdf(plan,from,large,accent){
 validatePlan(plan);
 const first=plan.days.findIndex(day=>day.date>=from);
 if(first<0)throw Error('El plan ya finalizó.');
 const selected=plan.days.slice(first,first+40);
 const W=612,H=792,margin=24,gap=12,columns=2,rows=10,cell=(W-margin*2-gap)/columns,tableTop=729,barHeight=28,tableBottom=58,rowHeight=(tableTop-barHeight-tableBottom)/rows,dateWidth=93;
 const bodySize=large?16:12,titleSize=large?20:18,headerSize=bodySize,leading=large?18:14;
 const accentRgb=rgb(accent),dark='0.13 0.19 0.20',muted='0.33 0.39 0.40',line='0.80 0.86 0.86',white='1 1 1';
 const footerLeft=`${formatPdfDate(plan.startDate)} - ${formatPdfDate(selected.at(-1).date)}`,footerRight=themesName(plan.theme),title='PLAN DE LECTURA – SENDA';
 const split=Math.ceil(selected.length/2),pageDays=[selected.slice(0,split),selected.slice(split)],pages=[];
 for(let pageIndex=0;pageIndex<2;pageIndex++){
  const page=pageDays[pageIndex],perColumn=Math.ceil(page.length/2);
  let content=rect(0,0,W,H,white);
  content+=drawText(title,(W-textWidth(title,titleSize,true))/2,755,'F2',titleSize,dark);
  for(let column=0;column<columns;column++){
   const x=margin+column*(cell+gap),barY=tableTop-barHeight,readingX=x+dateWidth+7,maxReadingWidth=cell-dateWidth-14;
   content+=rect(x,barY,cell,barHeight,accentRgb);
   content+=drawText('FECHA',x+7,barY+7,'F2',headerSize,white);
   content+=drawText('LECTURAS',readingX,barY+7,'F2',headerSize,white);
   for(let row=0;row<rows;row++){
    const day=row<perColumn?page[column*perColumn+row]:undefined,y=barY-(row+1)*rowHeight,top=y+rowHeight;
    content+=rect(x,y,cell,rowHeight,row%2?'0.97 0.98 0.98':white);
    content+=`${line} RG 0.5 w ${x.toFixed(1)} ${y.toFixed(1)} m ${(x+cell).toFixed(1)} ${y.toFixed(1)} l S\n`;
    content+=`${line} RG 0.4 w ${(x+dateWidth).toFixed(1)} ${y.toFixed(1)} m ${(x+dateWidth).toFixed(1)} ${top.toFixed(1)} l S\n`;
    if(!day)continue;
    const baseline=top-(large?20:17);
    content+=drawText(formatPdfDate(day.date),x+6,baseline,'F1',bodySize,dark);
    let refs=day.readings.map(r=>`${large?bookById[r.book].short:bookById[r.book].name} ${r.chapter}`);
    let lines=wrapReadings(refs,maxReadingWidth,bodySize);
    if(lines.length>(large?3:4)){
     refs=day.readings.map(r=>`${bookById[r.book].short} ${r.chapter}`);
     lines=wrapReadings(refs,maxReadingWidth,bodySize);
    }
    if(lines.length>(large?3:4))throw Error(`Las lecturas del ${day.date} no caben en el PDF.`);
    lines.forEach((reading,index)=>{content+=drawText(reading,readingX,baseline-index*leading,'F1',bodySize,dark);});
   }
  }
  content+=`${line} RG 0.6 w ${margin} 45 m ${W-margin} 45 l S\n`;
  content+=drawText(footerLeft,margin,22,'F1',bodySize,muted);
  content+=drawText(footerRight,W-margin-textWidth(footerRight,bodySize),22,'F1',bodySize,muted);
  pages.push(content);
 }
 const objects=['<< /Type /Catalog /Pages 2 0 R >>',`<< /Type /Pages /Kids [${pages.map((_,i)=>`${5+i*2} 0 R`).join(' ')}] /Count ${pages.length} >>`,'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>','<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>'];
 pages.forEach((content,i)=>{const pageId=5+i*2,streamId=pageId+1;objects.push(`<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${W} ${H}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${streamId} 0 R >>`);objects.push(`<< /Length ${pdfEncoded(content).length} >>\nstream\n${content}endstream`);});
 return pdfBytes(objects);
}
function themesName(id){return ({faith:'Fe y confianza',love:'Amor y misericordia',hope:'Esperanza y promesas',prayer:'Oración y adoración',wisdom:'Sabiduría para vivir',justice:'Justicia y compasión',forgiveness:'Perdón y reconciliación'})[id]||id;}
export function preparePdf(plan,from,large,accent){const data=createPlanPdf(plan,from,large,accent);return {data,url:URL.createObjectURL(new Blob([data],{type:'application/pdf'})),name:`Senda-${from}-40-dias.pdf`,days:plan.days.filter(day=>day.date>=from).slice(0,40).length};}
