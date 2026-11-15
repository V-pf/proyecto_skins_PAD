import type { Rank } from '../../lib/stats/types';
import { money, number } from '../../lib/stats/format';
export function RankingList({items}:{items:Rank[]}) {
  return <ol className="stats-ranking">{items.map((r,i)=><li key={r.key}><span className="stats-rank-number">{i+1}</span>{r.image&&<img src={r.image} alt="" loading="lazy" width={48} height={40}/>}
    <div><strong>{r.label}</strong><span>{number(r.units)} {r.units===1?'skin':'skins'} · <span className="stats-status">Aprobada</span></span></div><b>{money(r.income)}<small>sin comisión</small></b></li>)}</ol>;
}
