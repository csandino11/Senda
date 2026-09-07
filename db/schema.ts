import {sqliteTable,text,integer,primaryKey} from 'drizzle-orm/sqlite-core';
export const plans=sqliteTable('plans',{owner:text('owner').notNull(),year:integer('year').notNull(),data:text('data').notNull(),updatedAt:text('updated_at').notNull()},t=>[primaryKey({columns:[t.owner,t.year]})]);
