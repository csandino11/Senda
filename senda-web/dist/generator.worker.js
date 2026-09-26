import {generatePlan} from './plan.js';
self.onmessage=event=>{try{const {start,theme,dc,prefs}=event.data;self.postMessage({plan:generatePlan(start,theme,dc,prefs)});}catch(error){self.postMessage({error:error instanceof Error?error.message:'No se pudo generar el plan.'});}};
