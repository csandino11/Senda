import {label,type Plan} from './plan.ts';
import {themes,months} from './bible.ts';
import {widths} from './font-widths.ts';
const encoder=new TextEncoder();
function clean(s:string){return s.replace(/[–—]/g,'-').replace(/·/g,' | ').replace(/[“”]/g,'"').replace(/[’]/g,"'")}
function hex(s:string){return [...clean(s)].map(c=>Math.min(255,c.charCodeAt(0)).toString(16).padStart(2,'0')).join('')}
function width(s:string,size:number){return [...clean(s)].reduce((a,c)=>a+(widths[c.charCodeAt(0)]??556),0)*size/1000}
export function wrap(s:string,size:number,max:number){const lines:string[]=[];let line='';for(const word of clean(s).split(' ')){if(width((line?line+' ':'')+word,size)>max&&line){lines.push(line);line=word}else line+=(line?' ':'')+word}if(line)lines.push(line);return lines}
export type PdfLayout={width:number;height:number;font:number;pages:Array<{date:string;lines:string[]}[]>;quarter:number};
export function layoutPdf(plan:Plan,large:boolean,now=new Date()):PdfLayout{
if(plan.year!==now.getFullYear())throw Error('Solo puedes descargar el plan del año en curso.');
const quarter=Math.floor(now.getMonth()/3),selected=plan.days.filter(d=>Math.floor((+d.date.slice(5,7)-1)/3)===quarter);
if(selected.length<90||selected.length>92)throw Error('El trimestre no está completo.');
const pageWidth=large?841.89:595.28,pageHeight=large?1190.55:841.89,font=large?18:12,margin=large?42:34,dateWidth=large?102:72,usable=pageWidth-margin*2-dateWidth;
const rows=selected.map(d=>({date:d.date,lines:wrap(d.readings.map(r=>label(r,true)).join('   /   '),font,usable)}));
const pages:Array<typeof rows>=[];for(let i=0;i<rows.length;i+=23)pages.push(rows.slice(i,i+23));
const rowHeight=(pageHeight-(large?178:142))/23;
if(pages.length>4||rows.some(r=>r.lines.length*font*1.12+2>rowHeight))throw Error('No se puede conservar el tamaño de letra con esta distribución.');
return {width:pageWidth,height:pageHeight,font,pages,quarter};
}
export async function createPdf(plan:Plan,large:boolean,now=new Date()):Promise<Uint8Array>{
const l=layoutPdf(plan,large,now),margin=large?42:34,dateWidth=large?102:72,top=large?132:108,rowHeight=(l.height-(large?178:142))/23;
const objects:Uint8Array[]=[];const add=(s:string|Uint8Array)=>{objects.push(typeof s==='string'?encoder.encode(s):s);return objects.length};
add('<< /Type /Catalog /Pages 2 0 R >>');add('');add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>');add('<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>');
const pageIds:number[]=[];
for(let p=0;p<l.pages.length;p++){
let commands='';const text=(s:string,x:number,y:number,size:number,bold=false)=>{commands+='BT /F'+(bold?2:1)+' '+size+' Tf 0.08 0.23 0.20 rg 1 0 0 1 '+x+' '+(l.height-y)+' Tm <'+hex(s)+'> Tj ET\n'};
text('SENDA',margin,large?44:35,large?20:15,true);text(plan.year+' / '+months[l.quarter*3]+' - '+months[l.quarter*3+2],margin,large?78:60,large?25:19);
text(themes.find(t=>t.id===plan.theme)!.name+' | '+(plan.extra?'Con deuterocanónicos':'Sin deuterocanónicos'),margin,large?106:82,l.font);
commands+='0.80 0.89 0.85 RG 0.8 w '+margin+' '+(l.height-top+10)+' m '+(l.width-margin)+' '+(l.height-top+10)+' l S\n';
l.pages[p].forEach((r,i)=>{const y=top+i*rowHeight;if(i%2===0)commands+='0.94 0.97 0.95 rg '+margin+' '+(l.height-y-rowHeight+6)+' '+(l.width-margin*2)+' '+rowHeight+' re f\n';const m=+r.date.slice(5,7)-1;text(r.date.slice(8)+' '+months[m].slice(0,3).toLowerCase(),margin+6,y+l.font+2,l.font,true);r.lines.forEach((line,j)=>text(line,margin+dateWidth,y+l.font+2+j*l.font*1.12,l.font));});
text('Programa con exclusiones | Lecturas separadas por / | '+(p+1)+' de '+l.pages.length,margin,l.height-22,l.font);
const raw=encoder.encode(commands);let stream=raw;let filter='';
if(typeof CompressionStream!=='undefined'){const compressed=new Blob([raw]).stream().pipeThrough(new CompressionStream('deflate'));stream=new Uint8Array(await new Response(compressed).arrayBuffer());filter=' /Filter /FlateDecode'}
const prefix=encoder.encode('<< /Length '+stream.length+filter+' >>\nstream\n'),suffix=encoder.encode('\nendstream'),combined=new Uint8Array(prefix.length+stream.length+suffix.length);combined.set(prefix);combined.set(stream,prefix.length);combined.set(suffix,prefix.length+stream.length);
const streamId=add(combined);pageIds.push(add('<< /Type /Page /Parent 2 0 R /MediaBox [0 0 '+l.width+' '+l.height+'] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents '+streamId+' 0 R >>'));
}
objects[1]=encoder.encode('<< /Type /Pages /Kids ['+pageIds.map(id=>id+' 0 R').join(' ')+'] /Count '+pageIds.length+' >>');
const chunks:Uint8Array[]=[encoder.encode('%PDF-1.4\n')],offsets=[0];let offset=chunks[0].length;
objects.forEach((o,i)=>{offsets.push(offset);const a=encoder.encode((i+1)+' 0 obj\n'),b=encoder.encode('\nendobj\n');chunks.push(a,o,b);offset+=a.length+o.length+b.length});
const xref=offset;const tail=encoder.encode('xref\n0 '+(objects.length+1)+'\n0000000000 65535 f \n'+offsets.slice(1).map(n=>String(n).padStart(10,'0')+' 00000 n \n').join('')+'trailer\n<< /Size '+(objects.length+1)+' /Root 1 0 R >>\nstartxref\n'+xref+'\n%%EOF');chunks.push(tail);
const result=new Uint8Array(offset+tail.length);let at=0;for(const c of chunks){result.set(c,at);at+=c.length}return result;
}
export async function downloadPdf(plan:Plan,large:boolean){const now=new Date(),data=await createPdf(plan,large,now),url=URL.createObjectURL(new Blob([data as BlobPart],{type:'application/pdf'}));const a=document.createElement('a');a.href=url;a.download='Senda-'+plan.year+'-T'+(Math.floor(now.getMonth()/3)+1)+'-'+(large?'grande':'normal')+'.pdf';a.click();setTimeout(()=>URL.revokeObjectURL(url),10000)}


