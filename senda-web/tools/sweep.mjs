import {generatePlan,validatePlan} from '../dist/plan.js';
let count=0;
for(let weekdayChapters=1;weekdayChapters<=4;weekdayChapters++)for(let weekendChapters=1;weekendChapters<=3;weekendChapters++)for(const dc of [false,true]){
  const prefs={weekdayChapters,weekendChapters,repeatPsalms:dc,proverbCycles:dc?4:1,repeatGospels:dc};
  const plan=generatePlan('2026-12-31','forgiveness',dc,prefs,1000+count);
  validatePlan(plan);
  count++;
  console.log(`${weekdayChapters}/${weekendChapters} ${dc?'DC':'sin DC'}: ${plan.days.length} días`);
}
console.log(`${count} combinaciones verificadas`);
