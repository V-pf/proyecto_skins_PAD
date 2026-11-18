export function todaySantiago(now=new Date()) { const parts=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Santiago',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(now); const get=(type:string)=>parts.find(p=>p.type===type)!.value; return `${get('year')}-${get('month')}-${get('day')}`; }
export function shiftDay(date:string,days:number) { const d=new Date(date+'T12:00:00Z'); d.setUTCDate(d.getUTCDate()+days); return d.toISOString().slice(0,10); }
export function range(preset:string,today=todaySantiago()) {
  const first=today.slice(0,7)+'-01';
  if(preset==='all')return {from:'',to:today};
  if(preset==='today')return {from:today,to:today};
  if(preset==='month')return {from:first,to:today};
  if(preset==='previous') { const last=shiftDay(first,-1); return {from:last.slice(0,7)+'-01',to:last}; }
  return {from:shiftDay(today,preset==='7'?-6:-29),to:today};
}
