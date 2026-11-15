import { useState } from 'react';
import { range, todaySantiago } from '../../lib/stats/period';
export function PeriodFilter({params,setParams,categories}:{params:URLSearchParams;setParams:(p:URLSearchParams)=>void;categories:{id:number;name:string}[]}) {
  const [validation,setValidation]=useState('');
  function update(key:string,value:string){const p=new URLSearchParams(params);value?p.set(key,value):p.delete(key);p.delete('page');setParams(p);}
  function preset(value:string){const p=new URLSearchParams(params);p.set('period',value);if(value==='all')p.delete('compare');if(value!=='custom'){const r=range(value);r.from?p.set('from',r.from):p.delete('from');p.set('to',r.to);}p.delete('page');setParams(p);}
  return <div className="stats-filters">
    <label>Período<select value={params.get('period')??'30'} onChange={e=>preset(e.target.value)}>{[['today','Hoy'],['7','7 días'],['30','30 días'],['month','Este mes'],['previous','Mes anterior'],['all','Todo el historial'],['custom','Rango personalizado']].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>
    {params.get('period')==='custom'&&<form className="stats-custom-range" key={params.get('from')+'-'+params.get('to')} onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);const from=String(f.get('from'));const to=String(f.get('to'));if(from>to){setValidation('El inicio debe ser anterior al fin.');return;}const p=new URLSearchParams(params);p.set('from',from);p.set('to',to);p.delete('page');setValidation('');setParams(p);}}>
      <label>Desde<input name="from" type="date" required max={todaySantiago()} defaultValue={params.get('from')??''}/></label><label>Hasta<input name="to" type="date" required max={todaySantiago()} defaultValue={params.get('to')??todaySantiago()}/></label><button className="stats-button">Aplicar</button>{validation&&<span role="alert">{validation}</span>}
    </form>}
    <label>Categoría<select value={params.get('categoria')??''} onChange={e=>update('categoria',e.target.value)}><option value="">Todas</option>{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
    <label>Método de pago<select value={params.get('metodoPago')??''} onChange={e=>update('metodoPago',e.target.value)}>{[['','Todos'],['MERCADO_PAGO','Mercado Pago'],['SALDO','Saldo interno'],['TRANSFERENCIA','Transferencia'],['TARJETA','Tarjeta']].map(([v,l])=><option key={v} value={v}>{l}</option>)}</select></label>
    <label className="stats-check"><input type="checkbox" checked={params.get('compare')==='true'} disabled={params.get('period')==='all'} onChange={e=>update('compare',String(e.target.checked))}/> Comparar con período anterior</label>
  </div>;
}
