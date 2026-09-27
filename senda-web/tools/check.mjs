import {strict as assert} from 'node:assert';
import {mkdirSync,writeFileSync} from 'node:fs';
import {generatePlan,validatePlan,totalReadings,completionKey} from '../dist/plan.js';
import {createPlanPdf} from '../dist/portable.js';
import {bookById} from '../dist/catalog.js';

const start='2026-09-26';
const cases=[
  [1,1,false,1,false,false],[1,1,false,1,false,true],
  [2,3,true,2,true,false],[3,2,true,3,true,true],
  [4,1,false,4,true,true],[4,3,true,4,true,false],
];
let sample,longNamePlan;
for(let i=0;i<cases.length;i++){
  const [weekdayChapters,weekendChapters,repeatPsalms,proverbCycles,repeatGospels,dc]=cases[i];
  const prefs={weekdayChapters,weekendChapters,repeatPsalms,proverbCycles,repeatGospels};
  const plan=generatePlan(start,['faith','hope','love','justice','prayer','wisdom'][i],dc,prefs,41+i);
  assert.equal(validatePlan(plan),true);
  assert.equal(plan.days.flatMap(day=>day.readings).length,totalReadings(dc,prefs));
  assert.equal(new Set(plan.days.flatMap(day=>day.readings.map((_,j)=>completionKey(day.date,j)))).size,totalReadings(dc,prefs));
  const normalPdf=createPlanPdf(plan,start,false,'#0077b6');
  const largePdf=createPlanPdf(plan,start,true,'#3547a8');
  for(const [bytes,size] of [[normalPdf,12],[largePdf,16]]){
    const pdfText=Buffer.from(bytes).toString('latin1');
    assert.equal((pdfText.match(/\/Type \/Page \/Parent/g)||[]).length,2);
    assert.equal((pdfText.match(/\/MediaBox \[0 0 612 792\]/g)||[]).length,2);
    assert.equal((pdfText.match(/Tm \(\d{4}\) Tj/g)||[]).length,40);
    assert.match(pdfText,new RegExp(`/F1 ${size} Tf`));
    assert.match(pdfText,new RegExp(`/F2 ${size} Tf`));
  }
  const finalPdf=createPlanPdf(plan,plan.days.at(-1).date,true,'#3547a8');
  assert.equal((Buffer.from(finalPdf).toString('latin1').match(/\/Type \/Page \/Parent/g)||[]).length,2);
  if(i===5)sample=plan;
  if(i===3)longNamePlan=plan;
  console.log(`Caso ${i+1}: ${plan.days.length} días, ${totalReadings(dc,prefs)} capítulos`);
}
mkdirSync('../output/pdf',{recursive:true});
mkdirSync('../tmp/pdfs',{recursive:true});
const pdf=createPlanPdf(sample,start,false,'#c54800');
const large=createPlanPdf(sample,start,true,'#c54800');
const longNameDay=longNamePlan.days.find(day=>day.readings.some(reading=>bookById[reading.book].name.length>22));
assert.ok(longNameDay);
assert.ok(createPlanPdf(longNamePlan,longNameDay.date,true,'#c54800').length>0);
assert.equal(Buffer.from(pdf).subarray(0,8).toString(),'%PDF-1.4');
assert.equal(Buffer.from(large).subarray(0,8).toString(),'%PDF-1.4');
writeFileSync('../output/pdf/Senda-ejemplo-40-dias.pdf',large);
writeFileSync('../tmp/pdfs/Senda-ejemplo-normal.pdf',pdf);
writeFileSync('../tmp/pdfs/Senda-nombres-largos.pdf',createPlanPdf(longNamePlan,longNameDay.date,true,'#c54800'));
console.log(`PDF: ${pdf.length} bytes; grande: ${large.length} bytes`);
