import { Help } from './ChartCard';
import { number } from '../../lib/stats/format';
export function KpiCard({label,value,raw,previous,compare,help,detail,loading,error}:{label:string;value:string;raw:number|null;previous?:number|null;compare:boolean;help:string;detail?:string;loading:boolean;error?:string}) {
  const delta=raw!=null&&previous!=null&&previous!==0?(raw-previous)/Math.abs(previous)*100:null;
  return <article className="stats-card stats-kpi" aria-label={label} aria-busy={loading}>
    <h2>{label} <Help text={help}/></h2>
    {loading?<div className="stats-skeleton" role="status" aria-label="Cargando indicador"/>:error?<p role="alert">No disponible</p>:<><strong>{value}</strong>{detail&&<p>{detail}</p>}
    {compare&&<p className={'stats-delta '+(delta===null?'':delta>0?'up':delta<0?'down':'')}>
      {delta===null?(previous===0&&raw!==0?'Sin base previa':'Sin comparación'):`${delta>0?'↑':delta<0?'↓':'→'} ${number(Math.abs(delta))}% vs. anterior`}
    </p>}</>}
  </article>;
}
