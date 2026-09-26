import {strict as assert} from 'node:assert';
import {mkdirSync,writeFileSync} from 'node:fs';
import {generatePlan,validatePlan,totalReadings,completionKey} from '../dist/plan.js';
import {createPlanPdf} from '../dist/portable.js';

const start='2026-09-26';
const cases=[
  [1,1,false,1,false,false],[1,1,false,1,false,true],
  [2,3,true,2,true,false],[3,2,true,3,true,true],
  [4,1,false,4,true,true],[4,3,true,4,true,false],
];
let sample;
for(let i=0;i<cases.length;i++){
  const [weekdayChapters,weekendChapters,repeatPsalms,proverbCycles,repeatGospels,dc]=cases[i];
  const prefs={weekdayChapters,weekendChapters,repeatPsalms,proverbCycles,repeatGospels};
  const plan=generatePlan(start,['faith','hope','love','justice','prayer','wisdom'][i],dc,prefs,41+i);
  assert.equal(validatePlan(plan),true);
  assert.equal(plan.days.flatMap(day=>day.readings).length,totalReadings(dc,prefs));
  assert.equal(new Set(plan.days.flatMap(day=>day.readings.map((_,j)=>completionKey(day.date,j)))).size,totalReadings(dc,prefs));
  if(i===3)sample=plan;
  console.log(`Caso ${i+1}: ${plan.days.length} días, ${totalReadings(dc,prefs)} capítulos`);
}
mkdirSync('../output/pdf',{recursive:true});
const pdf=createPlanPdf(sample,start,false,'#0077b6');
const large=createPlanPdf(sample,start,true,'#3547a8');
assert.equal(Buffer.from(pdf).subarray(0,8).toString(),'%PDF-1.4');
assert.equal(Buffer.from(large).subarray(0,8).toString(),'%PDF-1.4');
writeFileSync('../output/pdf/Senda-ejemplo-45-dias.pdf',pdf);
writeFileSync('../tmp/Senda-ejemplo-grande.pdf',large);
console.log(`PDF: ${pdf.length} bytes; grande: ${large.length} bytes`);
