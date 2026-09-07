import {createPlan,validatePlan,key} from './lib/plan.ts';
import {themes,bookMap} from './lib/bible.ts';
import {layoutPdf} from './lib/pdf.ts';
import assert from 'node:assert/strict';
let count=0;
for(const year of [2026,2028])for(const extra of [true,false])for(const t of themes){
 const p=createPlan(year,t.id,extra,42+count);validatePlan(p);
 assert.equal(p.days.length,year===2028?366:365);
 assert.equal(p.days[0].date,year+'-01-01');assert.equal(p.days.at(-1)!.date,year+'-12-31');
 for(let i=0;i<p.days.length;i++){const d=p.days[i];assert.ok(d.readings.length>=3&&d.readings.length<=5);if(i>1)assert.ok(p.days.slice(i-2,i+1).some(d=>d.readings.some(r=>bookMap[r.book].nt&&!r.from)));for(const r of d.readings){assert.notEqual(key(r),'GEN.10');assert.notEqual(key(r),'GEN.36');assert.ok(!['JDT','1MA','ESG'].includes(r.book));if(!extra)assert.ok(!bookMap[r.book].dc)}}
 for(let q=0;q<4;q++)for(const large of [false,true])assert.equal(layoutPdf(p,large,new Date(year,q*3,1)).pages.length,4);
 count++;
}
assert.notDeepEqual(createPlan(2026,'faith',true,1).days,createPlan(2026,'faith',true,2).days);
assert.throws(()=>layoutPdf(createPlan(2026,'faith',true,1),false,new Date(2027,0,1)));
console.log('PASS: '+count+' plans; 384 quarterly PDF layouts; unique seeds; expired-year export blocked.');


