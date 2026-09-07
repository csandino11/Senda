import {env} from 'cloudflare:workers';
import type {Plan} from '../lib/plan';
export function database(){if(!env.DB)throw Error('Almacenamiento no disponible');return env.DB}
export async function readPlan(owner:string,year:number):Promise<Plan|null>{const row=await database().prepare('SELECT data FROM plans WHERE owner = ? AND year = ?').bind(owner,year).first<{data:string}>();return row?JSON.parse(row.data):null}
export async function savePlan(owner:string,plan:Plan,replace:boolean){const sql=replace?'INSERT INTO plans (owner, year, data, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(owner, year) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at':'INSERT INTO plans (owner, year, data, updated_at) VALUES (?, ?, ?, ?) ON CONFLICT(owner, year) DO NOTHING';await database().prepare(sql).bind(owner,plan.year,JSON.stringify(plan),plan.createdAt).run();return (await readPlan(owner,plan.year))!;}
