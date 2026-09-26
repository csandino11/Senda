// Canon and exclusions mirror Android Senda 2.0.0; independent of the earlier web prototype.
export const themes = [
  ['faith','Fe y confianza','Confiar aun cuando el camino no está claro.'],
  ['love','Amor y misericordia','Reconocer y compartir el amor de Dios.'],
  ['hope','Esperanza y promesas','Mirar el futuro a la luz de sus promesas.'],
  ['prayer','Oración y adoración','Cultivar el diálogo y la gratitud.'],
  ['wisdom','Sabiduría para vivir','Discernir las decisiones de cada día.'],
  ['justice','Justicia y compasión','Cuidar al prójimo y a quien es vulnerable.'],
  ['forgiveness','Perdón y reconciliación','Volver a la gracia y la restauración.'],
];
const rows = `GEN|Génesis|Gn|50|faith hope love|O
EXO|Éxodo|Ex|40|faith hope justice|O
LEV|Levítico|Lv|27|prayer justice forgiveness|O
NUM|Números|Nm|36|faith hope prayer|O
DEU|Deuteronomio|Dt|34|faith love justice wisdom|O
JOS|Josué|Jos|24|faith hope|O
JDG|Jueces|Jue|21|justice forgiveness|O
RUT|Rut|Rt|4|love hope faith|O
1SA|1 Samuel|1 Sm|31|faith prayer wisdom|O
2SA|2 Samuel|2 Sm|24|love forgiveness justice|O
1KI|1 Reyes|1 R|22|wisdom prayer faith|O
2KI|2 Reyes|2 R|25|justice hope faith|O
1CH|1 Crónicas|1 Cr|29|prayer faith hope|O
2CH|2 Crónicas|2 Cr|36|prayer forgiveness hope|O
EZR|Esdras|Esd|10|hope prayer forgiveness|O
NEH|Nehemías|Neh|13|prayer faith hope|O
EST|Ester|Est|10|faith hope justice|O
JOB|Job|Job|42|faith wisdom hope|O
PSA|Salmos|Sal|150|prayer faith hope|O
PRO|Proverbios|Pr|31|wisdom justice love|O
ECC|Eclesiastés|Ec|12|wisdom hope faith|O
SNG|Cantar de los Cantares|Ct|8|love|O
ISA|Isaías|Is|66|hope justice forgiveness|O
JER|Jeremías|Jr|52|justice forgiveness hope|O
LAM|Lamentaciones|Lm|5|prayer hope faith|O
EZK|Ezequiel|Ez|48|hope forgiveness justice|O
DAN|Daniel|Dn|12|faith hope prayer|O
HOS|Oseas|Os|14|love forgiveness hope|O
JOL|Joel|Jl|3|hope forgiveness prayer|O
AMO|Amós|Am|9|justice forgiveness|O
OBA|Abdías|Abd|1|justice hope|O
JON|Jonás|Jon|4|forgiveness love prayer|O
MIC|Miqueas|Mi|7|justice hope forgiveness|O
NAM|Nahúm|Nah|3|justice hope|O
HAB|Habacuc|Hab|3|faith prayer justice|O
ZEP|Sofonías|Sof|3|justice forgiveness hope|O
HAG|Hageo|Ag|2|hope prayer|O
ZEC|Zacarías|Zac|14|hope faith|O
MAL|Malaquías|Mal|4|justice hope|O
MAT|Mateo|Mt|28|faith love hope justice forgiveness|N
MRK|Marcos|Mc|16|faith hope forgiveness|N
LUK|Lucas|Lc|24|love prayer justice forgiveness|N
JHN|Juan|Jn|21|love faith hope prayer|N
ACT|Hechos|Hch|28|faith hope prayer love|N
ROM|Romanos|Ro|16|faith forgiveness hope love|N
1CO|1 Corintios|1 Co|16|love wisdom prayer|N
2CO|2 Corintios|2 Co|13|hope forgiveness love|N
GAL|Gálatas|Ga|6|faith love forgiveness|N
EPH|Efesios|Ef|6|love forgiveness wisdom|N
PHP|Filipenses|Flp|4|hope prayer love|N
COL|Colosenses|Col|4|wisdom faith love|N
1TH|1 Tesalonicenses|1 Ts|5|hope faith love|N
2TH|2 Tesalonicenses|2 Ts|3|hope faith justice|N
1TI|1 Timoteo|1 Ti|6|wisdom faith love|N
2TI|2 Timoteo|2 Ti|4|faith hope wisdom|N
TIT|Tito|Tit|3|wisdom love faith|N
PHM|Filemón|Flm|1|forgiveness love|N
HEB|Hebreos|Heb|13|faith hope forgiveness|N
JAS|Santiago|Stg|5|wisdom justice faith|N
1PE|1 Pedro|1 P|5|hope faith love|N
2PE|2 Pedro|2 P|3|hope faith wisdom|N
1JN|1 Juan|1 Jn|5|love faith forgiveness|N
2JN|2 Juan|2 Jn|1|love faith|N
3JN|3 Juan|3 Jn|1|love faith|N
JUD|Judas|Jud|1|faith justice|N
REV|Apocalipsis|Ap|22|hope faith justice|N
TOB|Tobías|Tb|14|faith prayer love justice|O|D
WIS|Sabiduría|Sab|19|wisdom justice hope|O|D
SIR|Eclesiástico|Eclo|51|wisdom justice love|O|D
BAR|Baruc|Bar|5|forgiveness prayer hope|O|D
LJE|Carta de Jeremías (Baruc 6)|Bar 6|1|faith wisdom|O|D
2MA|2 Macabeos|2 Mac|15|faith hope prayer|O|D
S3Y|Daniel 3 (adición griega)|Dn 3 gr|1|prayer faith|O|D
SUS|Daniel 13 · Susana|Dn 13|1|justice faith|O|D
BEL|Daniel 14 · Bel y el dragón|Dn 14|1|faith wisdom|O|D`;
export const books = rows.split('\n').map(line => { const [id,name,short,chapters,tags,testament,dc] = line.split('|'); return {id,name,short,chapters:+chapters,tags:tags.split(' '),testament,dc:dc==='D'}; });
export const bookById = Object.fromEntries(books.map(book=>[book.id,book]));
export const gospels = new Set(['MAT','MRK','LUK','JHN']);
const exclusions = {GEN:'10,36',EXO:'25-32,35-39',LEV:'1-4,6-8,12-14,17,21-25,27',NUM:'1-4,7-8,15,18-19,26-30,34-36',DEU:'17-18,25-26',JOS:'13-21',JDG:'5','2SA':'22-24','1KI':'4,7',EZK:'40-48',EST:'9-10',EZR:'2,4,6,8-10',NEH:'3,7-13','1CH':'1-9,12,15-16,23-28','2CH':'4,8,31',SIR:'44-50',WIS:'15-19','2MA':'10-15'};
function ranges(value) { return value.split(',').flatMap(part=>{const [a,b] = part.split('-').map(Number);return Array.from({length:(b??a)-a+1},(_,i)=>a+i);}); }
export const excluded = Object.fromEntries(Object.entries(exclusions).map(([id,list])=>[id,new Set(ranges(list))]));
export const allowed = (book,chapter,dc) => (dc || !book.dc) && !excluded[book.id]?.has(chapter);
const chapterTags = {};
function add(id,r,tags){for(const c of ranges(r))chapterTags[id+'.'+c]=tags.split(' ');}
add('GEN','1-2','wisdom love hope');add('GEN','3-5','forgiveness justice');add('GEN','12-22','faith hope');add('GEN','37-50','faith forgiveness hope');
add('EXO','1-15','hope faith justice');add('EXO','16-24','faith wisdom justice');add('DEU','5-11','love faith wisdom');add('DEU','27-30','hope forgiveness');
add('JOB','1-31','faith hope justice');add('JOB','32-42','wisdom faith');
add('PSA','1,19,37,49,73,90,111-112,119,127-128','wisdom faith');add('PSA','2,22,24,45,69,72,89,110,118','hope faith');
add('PSA','3-6,10-13,17,25-28,31,35,38-39,42-44,54-57,59-61,64,70-71,74,77,79-80,83,85-86,88,94,102,109,120,123,129-130,137,140-143','prayer faith hope');
add('PSA','8,18,29-30,33-34,47-48,65-68,75-76,81,84,87,92-93,95-101,103-108,113-117,122,124-126,131-136,138-139,144-150','prayer love hope');
add('PSA','7,9,14-15,50,52-53,58,82','justice wisdom');add('PSA','16,20-21,23,36,46,62-63,91,121','faith hope love');add('PSA','32,51,78','forgiveness love prayer');
add('PRO','1-4,8-10,14,16,19,22,24,26','wisdom faith');add('PRO','5-7,17-18,27,30-31','wisdom love');add('PRO','11-13,15,20-21,23,25,28-29','wisdom justice');
add('ISA','1-12','justice forgiveness hope');add('ISA','40-55','hope faith forgiveness');add('JER','30-33','hope forgiveness faith');add('EZK','33-39','hope forgiveness');
add('MAT','5-7','wisdom justice love');add('MAT','18-20','forgiveness love justice');add('LUK','10-18','love prayer justice forgiveness');add('JHN','13-17','love prayer hope');
add('ROM','1-8','faith forgiveness hope');add('ROM','12-16','love justice wisdom');add('1CO','12-14','love prayer wisdom');add('HEB','10-13','faith hope');add('JAS','1-5','wisdom justice faith');add('REV','19-22','hope faith');
add('WIS','1-14','wisdom justice hope');add('SIR','1-43','wisdom justice love');
export const tags = r => chapterTags[r.book+'.'+r.chapter] || bookById[r.book].tags;
export const longChapters = new Set('GEN.24 DEU.28 1KI.8 PSA.78 PSA.119 JER.51 LAM.3 EZK.16 MAT.26 MAT.27 MRK.14 LUK.1 LUK.9 LUK.22 JHN.6 ACT.7 LJE.1 S3Y.1 SUS.1'.split(' '));
const pairs = `GEN.1 JHN.1 COL.1|GEN.3 ROM.5 REV.22|GEN.12 ROM.4 HEB.11|GEN.22 HEB.11 JAS.2|EXO.12 JHN.19 1CO.5|EXO.16 JHN.6|EXO.20 MAT.5 ROM.13|LEV.19 MAT.22 JAS.2|NUM.21 JHN.3|DEU.6 MRK.12 MAT.22|PSA.22 MAT.27 MRK.15 JHN.19|PSA.23 JHN.10 1PE.5|PSA.32 ROM.4|PSA.51 LUK.15 1JN.1|PSA.110 HEB.7 MAT.22|PSA.118 MAT.21 ACT.4|ISA.7 MAT.1|ISA.9 MAT.4|ISA.40 MRK.1 JHN.1|ISA.53 ACT.8 1PE.2|ISA.61 LUK.4|JER.31 HEB.8 LUK.22|EZK.36 JHN.3 TIT.3|EZK.37 ROM.8|JOL.2 ACT.2|HOS.6 MAT.9 MAT.12|MIC.5 MAT.2|HAB.2 ROM.1 GAL.3|ZEC.9 MAT.21 JHN.12|JON.2 MAT.12|MAT.5 JAS.2 ROM.12|MAT.18 EPH.4 COL.3|JHN.13 1CO.13 1JN.4|JHN.15 GAL.5|JHN.20 1CO.15 1PE.1|LUK.15 2CO.5`;
export const related = pairs.split('|').map(x=>new Set(x.split(' ')));
export const isRelated = (a,b) => related.some(set=>set.has(a.book+'.'+a.chapter)&&set.has(b.book+'.'+b.chapter));
