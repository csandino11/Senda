export const themes = [
{id:'faith',name:'Fe y confianza',description:'Confiar en Dios, también cuando el camino no está claro.'},
{id:'love',name:'Amor y misericordia',description:'Reconocer el amor de Dios y aprender a compartirlo.'},
{id:'hope',name:'Esperanza y promesas',description:'Mirar hacia el futuro a la luz de sus promesas.'},
{id:'prayer',name:'Oración y adoración',description:'Cultivar el diálogo con Dios y una vida de gratitud.'},
{id:'wisdom',name:'Sabiduría para vivir',description:'Discernimiento para las decisiones de cada día.'},
{id:'justice',name:'Justicia y compasión',description:'Escuchar el llamado a cuidar al prójimo y al vulnerable.'},
{id:'forgiveness',name:'Perdón y reconciliación',description:'Explorar el arrepentimiento, la gracia y la restauración.'},
{id:'covenant',name:'Alianza y fidelidad',description:'Seguir la historia del compromiso de Dios con su pueblo.'},
{id:'salvation',name:'Jesús y salvación',description:'Contemplar a Jesús y la esperanza de la redención.'},
{id:'spirit',name:'Espíritu y vida nueva',description:'Descubrir la transformación que produce el Espíritu.'},
{id:'community',name:'Servicio y comunidad',description:'Vivir la fe junto a otros, con humildad y generosidad.'},
{id:'endurance',name:'Perseverancia en la prueba',description:'Encontrar fortaleza en medio del sufrimiento.'}
];
export const months=['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
export type Book={id:string;name:string;short:string;chapters:number;tags:string[];nt:boolean;dc:boolean};
const rows=[
'GEN|Génesis|Gn|50|covenant faith hope','EXO|Éxodo|Ex|40|salvation covenant community','LEV|Levítico|Lv|27|covenant justice community','NUM|Números|Nm|36|faith covenant endurance','DEU|Deuteronomio|Dt|34|covenant love justice','JOS|Josué|Jos|24|faith covenant hope','JDG|Jueces|Jue|21|forgiveness salvation justice','RUT|Rut|Rt|4|love community hope','1SA|1 Samuel|1 Sm|31|faith prayer covenant','2SA|2 Samuel|2 Sm|24|covenant forgiveness justice','1KI|1 Reyes|1 R|22|wisdom covenant faith','2KI|2 Reyes|2 R|25|covenant justice endurance','1CH|1 Crónicas|1 Cr|29|covenant prayer community','2CH|2 Crónicas|2 Cr|36|covenant prayer forgiveness','EZR|Esdras|Esd|10|covenant community forgiveness','NEH|Nehemías|Neh|13|community prayer endurance','EST|Ester|Est|10|faith hope endurance','JOB|Job|Job|42|endurance wisdom faith','PSA|Salmos|Sal|150|prayer faith hope','PRO|Proverbios|Pr|31|wisdom justice community','ECC|Eclesiastés|Ec|12|wisdom endurance faith','SNG|Cantar de los Cantares|Ct|8|love covenant','ISA|Isaías|Is|66|hope salvation justice','JER|Jeremías|Jr|52|covenant forgiveness justice','LAM|Lamentaciones|Lm|5|endurance prayer hope','EZK|Ezequiel|Ez|48|spirit covenant hope','DAN|Daniel|Dn|12|faith endurance hope','HOS|Oseas|Os|14|love covenant forgiveness','JOL|Joel|Jl|3|spirit forgiveness hope','AMO|Amós|Am|9|justice covenant forgiveness','OBA|Abdías|Abd|1|justice hope','JON|Jonás|Jon|4|forgiveness love prayer','MIC|Miqueas|Mi|7|justice hope salvation','NAM|Nahúm|Nah|3|justice hope','HAB|Habacuc|Hab|3|faith prayer justice','ZEP|Sofonías|Sof|3|justice forgiveness hope','HAG|Hageo|Ag|2|community covenant hope','ZEC|Zacarías|Zac|14|hope salvation covenant','MAL|Malaquías|Mal|4|covenant justice community',
'MAT|Mateo|Mt|28|salvation covenant justice','MRK|Marcos|Mc|16|salvation faith community','LUK|Lucas|Lc|24|salvation love prayer','JHN|Juan|Jn|21|salvation love faith',
'ACT|Hechos|Hch|28|spirit community salvation','ROM|Romanos|Ro|16|salvation faith forgiveness','1CO|1 Corintios|1 Co|16|community wisdom love','2CO|2 Corintios|2 Co|13|community endurance forgiveness','GAL|Gálatas|Ga|6|faith spirit salvation','EPH|Efesios|Ef|6|community love spirit','PHP|Filipenses|Flp|4|hope community endurance','COL|Colosenses|Col|4|salvation wisdom community','1TH|1 Tesalonicenses|1 Ts|5|hope faith community','2TH|2 Tesalonicenses|2 Ts|3|hope endurance justice','1TI|1 Timoteo|1 Ti|6|community wisdom faith','2TI|2 Timoteo|2 Ti|4|endurance faith community','TIT|Tito|Tit|3|community salvation wisdom','PHM|Filemón|Flm|1|forgiveness love community','HEB|Hebreos|Heb|13|faith covenant salvation','JAS|Santiago|Stg|5|wisdom faith justice','1PE|1 Pedro|1 P|5|endurance hope community','2PE|2 Pedro|2 P|3|hope faith wisdom','1JN|1 Juan|1 Jn|5|love faith salvation','2JN|2 Juan|2 Jn|1|love faith community','3JN|3 Juan|3 Jn|1|community love faith','JUD|Judas|Jud|1|faith endurance justice','REV|Apocalipsis|Ap|22|hope salvation endurance',
'TOB|Tobías|Tb|14|faith community prayer','JDT|Judit|Jdt|16|faith endurance salvation','WIS|Sabiduría|Sab|19|wisdom justice hope','SIR|Eclesiástico|Eclo|51|wisdom community justice','BAR|Baruc|Bar|5|forgiveness wisdom hope','LJE|Carta de Jeremías (Baruc 6)|Bar 6|1|faith covenant wisdom','1MA|1 Macabeos|1 Mac|16|covenant endurance community','2MA|2 Macabeos|2 Mac|15|faith endurance hope','S3Y|Daniel 3 (adición griega)|Dn 3 gr|1|prayer faith endurance','SUS|Daniel 13 · Susana|Dn 13|1|justice faith endurance','BEL|Daniel 14 · Bel y el dragón|Dn 14|1|faith wisdom endurance'
];
export const books:Book[]=rows.map((r,i)=>{const [id,name,short,chapters,tags]=r.split('|');return {id,name,short,chapters:+chapters,tags:tags.split(' '),nt:i>=39&&i<=65,dc:i>65}});
export const bookMap=Object.fromEntries(books.map(b=>[b.id,b]));
export const gospelIds=['MAT','MRK','LUK','JHN'];
export const excluded:Record<string,string>={GEN:'10,36',EXO:'25-32,35-39',LEV:'1-4,6-8,12-14,17,21-25,27',NUM:'1-4,7-8,15,18-19,26-30,34-36',DEU:'17-18,25-26',JOS:'13-21',JDG:'5','2SA':'22-24','1KI':'4,7',EZK:'40-48',EST:'9-10',EZR:'2,4,6,8-10',NEH:'3,7-13','1CH':'1-9,12,15-16,23-28','2CH':'4,8,31',SIR:'44-50',WIS:'15-19','2MA':'10-15',JDT:'1-16','1MA':'1-16'};
export function range(s:string){return s.split(',').flatMap(p=>{const [a,b]=p.split('-').map(Number);return Array.from({length:(b??a)-a+1},(_,i)=>a+i)})}
export function allowed(b:Book,c:number,dc:boolean){return (dc||!b.dc)&&!range(excluded[b.id]??'0').includes(c)}
// Editorial thematic index: chapter/range overrides replace broad book-level themes.
// References are classified by subject, never by chapter number or random hashes.
const index:Record<string,string>={
'GEN.1-2':'covenant wisdom love','GEN.3-5':'forgiveness salvation justice','GEN.6-9':'covenant justice salvation','GEN.12-18':'faith covenant hope','GEN.19':'justice salvation','GEN.20-26':'faith covenant','GEN.27-35':'forgiveness covenant endurance','GEN.37-50':'endurance forgiveness faith',
'EXO.1-6':'endurance salvation hope','EXO.7-15':'salvation faith covenant','EXO.16-20':'faith covenant community','EXO.21-24':'justice covenant community','EXO.33-34':'forgiveness prayer love','EXO.40':'prayer covenant',
'LEV.5':'forgiveness justice','LEV.9-11':'prayer covenant','LEV.15-16':'forgiveness covenant','LEV.18-20':'justice love covenant','LEV.26':'covenant hope',
'NUM.5-6':'covenant prayer','NUM.9-14':'faith endurance','NUM.16-17':'justice community','NUM.20-25':'faith justice covenant','NUM.31-33':'covenant community',
'DEU.1-4':'covenant faith','DEU.5-11':'love covenant faith','DEU.12-16':'prayer community justice','DEU.19-24':'justice community love','DEU.27-30':'covenant hope forgiveness','DEU.31-34':'covenant hope community',
'JOS.1-6':'faith covenant salvation','JOS.7-12':'justice faith','JOS.22-24':'covenant community',
'JDG.1-4':'salvation forgiveness','JDG.6-8':'faith salvation','JDG.9-12':'justice community','JDG.13-16':'faith endurance','JDG.17-21':'justice covenant',
'1SA.1-3':'prayer faith community','1SA.4-8':'covenant justice','1SA.9-15':'covenant wisdom','1SA.16-20':'faith love endurance','1SA.21-31':'endurance faith justice',
'2SA.1-10':'covenant love community','2SA.11-12':'forgiveness justice','2SA.13-21':'endurance justice forgiveness',
'1KI.1-3':'wisdom prayer','1KI.5-6':'prayer covenant','1KI.8-11':'prayer covenant wisdom','1KI.12-16':'covenant justice','1KI.17-22':'faith prayer justice',
'2KI.1-8':'faith salvation community','2KI.9-17':'justice covenant','2KI.18-20':'prayer faith hope','2KI.21-25':'forgiveness covenant justice',
'JOB.1-2':'endurance faith','JOB.3-31':'endurance wisdom justice','JOB.32-37':'wisdom justice','JOB.38-42':'wisdom faith hope',
'PSA.1,19,37,49,73,90,111,112,119,127,128':'wisdom covenant faith',
'PSA.2,22,24,45,69,72,89,110,118':'salvation hope covenant',
'PSA.3-6,10-13,17,25-28,31,35,38,39,42-44,54-57,59-61,64,70,71,74,77,79,80,83,85,86,88,94,102,109,120,123,129,130,137,140-143':'prayer endurance faith',
'PSA.8,18,29,30,33,34,47,48,65-68,75,76,81,84,87,92,93,95-101,103-108,113-117,122,124-126,131-136,138,139,144-150':'prayer love hope',
'PSA.7,9,14,15,50,52,53,58,82':'justice wisdom covenant',
'PSA.16,20,21,23,36,46,62,63,91,121':'faith hope love','PSA.32,51,78':'forgiveness covenant love','PSA.40,41,131':'faith prayer hope',
'PRO.1-4,8-10,14,16,19,22,24,26':'wisdom faith','PRO.5-7,17,18,27,30,31':'wisdom love community','PRO.11-13,15,20,21,23,25,28,29':'wisdom justice community',
'ISA.1-12':'justice forgiveness salvation','ISA.13-35':'justice hope','ISA.36-39':'faith prayer','ISA.40-48':'hope faith salvation','ISA.49-55':'salvation love forgiveness','ISA.56-66':'justice hope community',
'JER.1-20':'justice covenant forgiveness','JER.21-29':'endurance covenant','JER.30-33':'hope covenant forgiveness','JER.34-45':'faith justice endurance','JER.46-52':'justice hope',
'EZK.1-3':'spirit community','EZK.4-24':'justice forgiveness covenant','EZK.25-32':'justice hope','EZK.33-39':'spirit hope salvation',
'DAN.1-6':'faith prayer endurance','DAN.7-12':'hope salvation endurance',
'MAT.1-2':'hope salvation covenant','MAT.3-4':'forgiveness faith spirit','MAT.5-7':'wisdom justice love','MAT.8-9':'faith salvation love','MAT.10':'community endurance','MAT.11-12':'faith salvation wisdom','MAT.13':'wisdom hope','MAT.14-17':'faith salvation','MAT.18':'forgiveness community love','MAT.19-20':'love justice community','MAT.21-23':'justice salvation covenant','MAT.24-25':'hope endurance justice','MAT.26-27':'salvation covenant forgiveness','MAT.28':'hope salvation community',
'MRK.1-3':'salvation faith community','MRK.4':'faith wisdom','MRK.5-9':'faith salvation','MRK.10':'community love salvation','MRK.11-12':'faith justice covenant','MRK.13':'hope endurance','MRK.14-15':'salvation covenant forgiveness','MRK.16':'hope salvation community',
'LUK.1-2':'hope salvation prayer','LUK.3-4':'spirit forgiveness faith','LUK.5-8':'faith love forgiveness','LUK.9-10':'community love salvation','LUK.11':'prayer faith','LUK.12-14':'wisdom justice community','LUK.15':'forgiveness love salvation','LUK.16-17':'justice faith forgiveness','LUK.18':'prayer faith justice','LUK.19-21':'justice hope salvation','LUK.22-23':'salvation forgiveness covenant','LUK.24':'hope salvation community',
'JHN.1-3':'salvation faith spirit','JHN.4':'prayer love salvation','JHN.5-7':'faith salvation hope','JHN.8':'forgiveness salvation justice','JHN.9-12':'faith hope salvation','JHN.13':'love community','JHN.14-16':'spirit love hope','JHN.17':'prayer community love','JHN.18-19':'salvation covenant','JHN.20-21':'faith hope community',
'ACT.1-7':'spirit community prayer','ACT.8-12':'spirit salvation community','ACT.13-20':'community salvation faith','ACT.21-28':'endurance faith hope',
'ROM.1-3':'justice forgiveness salvation','ROM.4-5':'faith salvation hope','ROM.6-8':'spirit salvation hope','ROM.9-11':'covenant salvation love','ROM.12-16':'love community justice',
'1CO.1-4':'wisdom community','1CO.5-11':'community justice covenant','1CO.12-14':'love spirit community','1CO.15-16':'hope salvation community',
'2CO.1-7':'endurance forgiveness community','2CO.8-9':'community love justice','2CO.10-13':'endurance faith community',
'GAL.1-4':'faith salvation covenant','GAL.5-6':'spirit love community','EPH.1-3':'salvation love spirit','EPH.4-6':'community love spirit','HEB.1-10':'salvation covenant faith','HEB.11':'faith hope endurance','HEB.12-13':'endurance faith community',
'JAS.1':'endurance wisdom faith','JAS.2':'justice faith love','JAS.3':'wisdom community','JAS.4':'forgiveness wisdom','JAS.5':'prayer endurance justice','REV.1-3':'faith endurance community','REV.4-7':'prayer salvation hope','REV.8-18':'justice endurance hope','REV.19-22':'hope salvation covenant',
'TOB.1-4':'community justice faith','TOB.5-11':'faith love hope','TOB.12-14':'prayer wisdom hope','WIS.1-5':'justice hope wisdom','WIS.6-9':'wisdom prayer','WIS.10-14':'salvation wisdom faith','BAR.1-2':'forgiveness prayer','BAR.3':'wisdom covenant','BAR.4-5':'hope covenant','2MA.1-2':'prayer covenant','2MA.3-7':'faith endurance hope','2MA.8-9':'justice faith salvation'
};
const chapterTags:Record<string,string[]>={};
for(const [key,tags] of Object.entries(index)){const [id,chapters]=key.split('.');for(const c of range(chapters))chapterTags[id+'.'+c]=tags.split(' ')}
export function tagsFor(id:string,c:number){return chapterTags[id+'.'+c]??bookMap[id].tags}
export const links=[
['GEN.12','ROM.4','HEB.11'],['GEN.15','ROM.4','GAL.3'],['GEN.22','HEB.11','JAS.2'],
['GEN.1','JHN.1','COL.1'],['GEN.3','ROM.5','REV.22'],['EXO.12','JHN.19','1CO.5'],
['EXO.16','JHN.6'],['EXO.20','MAT.5','ROM.13'],['DEU.6','MRK.12','MAT.22'],
['LEV.19','MAT.22','JAS.2'],['NUM.21','JHN.3'],['DEU.8','MAT.4','LUK.4'],
['PSA.22','MAT.27','MRK.15','JHN.19'],['PSA.23','JHN.10','1PE.5'],
['PSA.32','ROM.4'],['PSA.51','LUK.15','1JN.1'],['PSA.110','HEB.7','MAT.22'],
['PSA.118','MAT.21','ACT.4'],['ISA.7','MAT.1'],['ISA.9','MAT.4'],
['ISA.40','MRK.1','JHN.1'],['ISA.53','ACT.8','1PE.2'],['ISA.61','LUK.4'],
['JER.31','HEB.8','LUK.22'],['EZK.36','JHN.3','TIT.3'],['EZK.37','ROM.8'],
['JOL.2','ACT.2'],['HOS.6','MAT.9','MAT.12'],['MIC.5','MAT.2'],['HAB.2','ROM.1','GAL.3'],
['ZEC.9','MAT.21','JHN.12'],['JON.2','MAT.12'],['MAL.3','MRK.1'],
['PRO.3','HEB.12','JAS.4'],['PRO.25','ROM.12'],['1SA.1','LUK.1'],
['2SA.7','LUK.1','ACT.13'],['1KI.17','LUK.4'],['DAN.7','MRK.14','REV.1'],
['MAT.5','JAS.2','ROM.12'],['MAT.6','LUK.11','PHP.4'],['MAT.18','EPH.4','COL.3'],
['JHN.13','1CO.13','1JN.4'],['JHN.15','GAL.5'],['JHN.20','1CO.15','1PE.1'],
['LUK.10','JAS.2'],['LUK.15','2CO.5'],['ACT.2','1CO.12','EPH.4']
];
export function isLinked(a:string,b:string){return links.some(g=>g.includes(a)&&g.includes(b))}

