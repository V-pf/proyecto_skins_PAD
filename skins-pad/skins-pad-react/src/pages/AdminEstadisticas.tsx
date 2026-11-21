import AlertasSoporte from "../components/soporte/AlertasSoporte";
import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useStats } from '../hooks/useStats';
import { ChartCard } from '../components/stats/ChartCard';
import { KpiCard } from '../components/stats/KpiCard';
import { PeriodFilter } from '../components/stats/PeriodFilter';
import { RankingList } from '../components/stats/RankingList';
import { DataTable } from '../components/stats/DataTable';
import { dateLabel, exportCsv, labels, money, number } from '../lib/stats/format';
import { range } from '../lib/stats/period';
import type { Group, Summary } from '../lib/stats/types';
import '../components/stats/stats.css';

const colors=['#63b3ff','#f9c969','#67d9bf','#c6a0ff','#ff9cb0','#a3d978','#ffae78','#cbd5e1'];
function GroupTable({groups,amount=true}:{groups:Group[];amount?:boolean}) {return <table className="stats-group-table"><thead><tr><th>Grupo</th><th>Cantidad</th>{amount&&<th>Monto</th>}</tr></thead><tbody>{groups.map(g=><tr key={g.key}><td>{labels[g.label]??g.label}</td><td>{number(g.count)}</td>{amount&&<td>{money(g.amount)}</td>}</tr>)}</tbody></table>;}
function ResumenCustodia({ groups }: { groups: Group[] }) {
  if (!groups.length) return <p>Sin datos para esta selección.</p>;
  const retenido = groups.find(g => g.key === 'EN_CUSTODIA');
  const activos = groups.filter(g => g.key !== 'EN_CUSTODIA' && (g.count > 0 || !!g.amount));
  if (!activos.length && !retenido?.count && !retenido?.amount) {
    return <p className="mb-0">Sin operaciones en el período.</p>;
  }
  const excepciones = activos.filter(g => ['EN_DISPUTA', 'CONGELADA'].includes(g.key))
    .reduce((total, g) => total + g.count, 0);
  const nombres: Record<string, string> = {
    PAGO_PENDIENTE: 'Pago pendiente', PAGO_RETENIDO: 'Pago retenido',
    ACEPTADA_POR_VENDEDOR: 'Aceptada por el vendedor', ENTREGADA: 'Entregada',
    EN_DISPUTA: 'En disputa', CONGELADA: 'Congelada', COMPLETADA: 'Completada',
    REEMBOLSADA: 'Reembolsada', CANCELADA: 'Cancelada',
    EXPIRADA: 'Expirada', RECHAZADA_POR_VENDEDOR: 'Rechazada por el vendedor',
  };
  return <>
    <p><strong>{money(retenido?.amount ?? null)}</strong> retenidos
      {excepciones > 0 && <> · <Link to="/admin/pagos">Revisar {number(excepciones)} {excepciones === 1 ? 'caso' : 'casos'} en disputa o congelación</Link></>}
    </p>
    {activos.length > 0 && <details className="stats-accessible-data">
      <summary>Ver desglose</summary>
      <GroupTable groups={activos.map(g => ({ ...g, label: nombres[g.key] ?? g.label }))}/>
    </details>}
  </>;
}
export default function AdminEstadisticas() {
  const [params,setParams]=useSearchParams();
  const [measure,setMeasure]=useState<'gross'|'operations'>('gross');
  const [options,setOptions]=useState<{id:number;name:string}[]>([]);
  const effective=useMemo(()=>{const p=new URLSearchParams(params);if(!p.has('period'))p.set('period',p.has('from')?'custom':'30');if(!p.has('from')&&p.get('period')!=='all'){const r=range(p.get('period')!);p.set('from',r.from);if(!p.has('to'))p.set('to',r.to);}return p;},[params]);
  useEffect(()=>{if(params.toString()!==effective.toString())setParams(effective,{replace:true});},[params,effective,setParams]);
  const query=new URLSearchParams([...effective].filter(([k])=>['from','to','compare','categoria','metodoPago'].includes(k))).toString();
  const load=useStats(query),data=load.datos;
  useEffect(()=>{if(data)setOptions(data.categoryOptions);},[data]);
  const loading=load.cargaInicial,retry=()=>void load.recargar(),compare=effective.get('compare')==='true';
  const catalogError=load.error||data?.errors.catalog;
  const moneyError=load.error||(effective.has('categoria')?data?.errors.catalog:undefined);
  const base={loading,retry};
  const monthly=!!data && (Date.parse(data.to)-Date.parse(data.from??data.timeline[0]?.date??data.to))/86400000>62;
  const kpis:{key:keyof Summary;label:string;help:string;kind:'money'|'count'|'percent'}[]=[
    {key:'gross',label:'Ventas brutas',kind:'money',help:'Suma del monto total de pagos aprobados, incluida la comisión pagada por el comprador. Con categoría, se atribuye proporcionalmente por precio histórico; no es ingreso neto de la plataforma.'},
    {key:'commission',label:'Comisión ganada',kind:'money',help:'Total cobrado menos precios históricos de las skins. Comisión devengada, antes de costos del proveedor de pago; no representa utilidad neta. Pendientes y reembolsos se excluyen.'},
    {key:'operations',label:'Compras con ventas completadas',kind:'count',help:'Pagos únicos con al menos una skin COMPLETADA; histórico aprobado conservado. Una operación puede contener varias skins. Al filtrar categoría se cuentan las operaciones que incluyen esa categoría.'},
    {key:'ticket',label:'Ticket promedio',kind:'money',help:'Ventas brutas divididas por operaciones aprobadas. Con categoría es el importe atribuido por operación, no el ticket completo.'},
    {key:'units',label:'Skins vendidas',kind:'count',help:'Skins COMPLETADAS; histórico aprobado conservado. Se contabilizan unidades aunque el nombre de la publicación contenga otra palabra; no se infiere el estado desde el nombre.'},
    {key:'conversion',label:'Conversión de publicaciones',kind:'percent',help:'Proxy de cohorte: publicaciones creadas en el período que aparecen en una compra aprobada creada en ese período / publicaciones creadas. No es conversión desde aprobación de publicación: esa fecha no existe. Sin método de pago y sin cohorte vacía.'},
  ];
  return <div className="stats-dashboard"><AlertasSoporte/>
    <div className="stats-heading"><p>Actividad comercial y control de operaciones <span>· CLP · America/Santiago</span></p><button className="stats-button" disabled={loading||!!load.error||!data?.sales.length} onClick={()=>exportCsv([['Operación','Publicación ID','Nombre','Fecha de creación pago (America/Santiago)','Categoría','Vendedor ID','Método','Sin comisión CLP','Bruto atribuido CLP','Estado'],...data!.sales.map(r=>[r.paymentId,r.productId,r.name,r.date,r.category,r.sellerId,r.method,r.net,r.gross,r.state])])}>↓ Exportar CSV</button></div>
    <PeriodFilter params={effective} setParams={setParams} categories={options}/>
    <p className="stats-period-note">{data?`${data.from??'Inicio del historial'} → ${data.to}`:'Consultando período'} · Fecha de finalización (histórico: creación del pago){data?.previous&&` · Comparación: ${data.compareFrom} → ${data.compareTo}`}</p>
    {load.error&&<div className="stats-error" role="alert">{load.error} <button className="stats-button" onClick={retry}>Reintentar</button></div>}
    <div className="stats-kpis">{kpis.map(k=>{const value=data?.current[k.key]??null;return <KpiCard key={k.key} label={k.label} raw={value} previous={data?.previous?.[k.key]} compare={compare} help={k.help} loading={loading} error={k.key==='conversion'?catalogError:moneyError} value={k.kind==='money'?money(value):value===null?'No disponible':number(value)+(k.kind==='percent'?'%':'')} detail={k.key==='gross'&&data?`Sin comisión: ${money(data.current.net)}`:k.key==='commission'&&data?`Promedio: ${number(data.current.averageCommission)}${data.current.averageCommission==null?'':'% del precio'}`:undefined}/>;})}</div>
    <ChartCard {...base} title="Custodia y entregas" help="Operaciones por skin creadas en el período. Total retenido incluye aceptadas, entregadas, disputas y congeladas con fondos. Expiradas y rechazadas son causas de reembolso, no importes adicionales." error={moneyError}><ResumenCustodia groups={data?.custody??[]}/></ChartCard>
    <div className="stats-charts">
      <ChartCard {...base} title="Evolución de ventas" help="Ventas nuevas agrupadas por finalización; históricas por creación del pago. Días y meses civiles de Santiago. Los períodos sin operaciones muestran cero." error={moneyError} empty={!data?.current.operations} actions={<label className="stats-inline-label">Ver<select aria-label="Métrica del gráfico temporal" value={measure} onChange={e=>setMeasure(e.target.value as typeof measure)}><option value="gross">Monto bruto</option><option value="operations">Operaciones</option></select></label>}>
        <div className="stats-chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={data?.timeline??[]} accessibilityLayer margin={{top:12,right:12,bottom:12,left:16}}><CartesianGrid vertical={false} stroke="#344357"/><XAxis dataKey="date" stroke="#b9c7d8" tickFormatter={v=>dateLabel(String(v),monthly)} minTickGap={28}/><YAxis width={92} stroke="#b9c7d8" tickFormatter={v=>measure==='gross'?money(Number(v)):number(Number(v))} allowDecimals={false}/><Tooltip contentStyle={{background:'#172333',border:'1px solid #697e99',color:'#f2f6fc'}} labelFormatter={v=>dateLabel(String(v),monthly)} formatter={v=>[measure==='gross'?money(Number(v)):number(Number(v)),measure==='gross'?'Ventas brutas':'Operaciones']}/><Bar dataKey={measure} fill="#63b3ff" radius={[4,4,0,0]} maxBarSize={56} isAnimationActive={false}/></BarChart></ResponsiveContainer></div>
        <details className="stats-accessible-data"><summary>Ver datos del gráfico</summary><table><thead><tr><th>Período</th><th>Bruto</th><th>Operaciones</th></tr></thead><tbody>{data?.timeline.map(b=><tr key={b.date}><td>{dateLabel(b.date,monthly)}</td><td>{money(b.gross)}</td><td>{b.operations}</td></tr>)}</tbody></table></details>
      </ChartCard>
      <ChartCard {...base} title="Ventas por categoría" help="Única clasificación: categoria_id de la publicación. Participación por unidades; los nombres provienen del catálogo. La suma coincide con Skins vendidas." error={catalogError} empty={!data?.categories.length}>
        <div className="stats-donut"><ResponsiveContainer width="100%" height="100%"><PieChart accessibilityLayer><Pie data={data?.categories??[]} dataKey="count" nameKey="label" innerRadius="65%" outerRadius="88%" paddingAngle={2} isAnimationActive={false}>{data?.categories.map((c,i)=><Cell key={c.key} fill={colors[i%colors.length]} stroke="#172333"/>)}</Pie><Tooltip contentStyle={{background:'#172333',border:'1px solid #697e99',color:'#f2f6fc'}} formatter={v=>[`${v} skins`,'Unidades']}/></PieChart></ResponsiveContainer><div className="stats-donut-total" aria-hidden="true"><strong>{number(data?.current.units)}</strong><span>skins vendidas</span></div></div>
        <ul className="stats-category-legend">{data?.categories.map((c,i)=><li key={c.key}><span style={{background:colors[i%colors.length]}} aria-hidden="true"/><b>{c.label}</b><span>{c.count} · {number(data.current.units?c.count/data.current.units*100:0)}%</span></li>)}</ul>
      </ChartCard>
    </div>
    <div className="stats-three">
      <ChartCard {...base} title="Skins más vendidas" help="Nombre normalizado; misma arma y skin si el nombre del catálogo es consistente. Desempate por ingreso sin comisión. No existe un identificador canónico de modelo de skin." error={catalogError} empty={!data?.skins.length}><RankingList items={data?.skins??[]}/></ChartCard>
      <ChartCard {...base} title="Top vendedores" help="Agrupados por vendedor_id y ordenados por ingreso sin comisión. Se muestran identificadores, no correos ni datos personales." error={catalogError} empty={!data?.sellers.length}><RankingList items={data?.sellers??[]}/></ChartCard>
      <ChartCard {...base} title="Métodos de pago" help="Compras con ventas completadas e importe bruto. Los pagos con varias skins se cuentan una sola vez." error={moneyError} empty={!data?.methods.length}><GroupTable groups={data?.methods??[]}/></ChartCard>
    </div>
    <div className="stats-two">
      <ChartCard {...base} title="Operaciones que requieren atención" help="Alertas dentro de los filtros seleccionados. Umbral de pendiente: 24 horas desde creación. Rechazos y reembolsos muestran el estado actual; no existe su fecha de transición." error={moneyError}>
        {data?.alerts.length?<ul className="stats-alerts">{data.alerts.map(a=><li key={a.label}><span aria-hidden="true">{a.kind==='warning'?'⚠':'ⓘ'}</span><div><strong>{a.count} · {a.label}</strong><Link to={a.href}>Revisar en Gestión de Pagos →</Link></div></li>)}</ul>:<p className="stats-empty">Sin alertas en la selección.</p>}
      </ChartCard>
      <ChartCard {...base} title="Estado de las operaciones" help="Distribución de estados actuales; no es un embudo de transiciones históricas. Conteo de pagos únicos e importe bruto de cada estado." error={moneyError} empty={!data?.funnel.some(g=>g.count)}><GroupTable groups={data?.funnel??[]}/></ChartCard>
    </div>
    <ChartCard {...base} title="Publicaciones vendidas" help="Las mismas líneas aprobadas usadas en los KPI, rankings y gráficos. CSV exporta todas las filas del período, sin limitarse a la página de la tabla." error={moneyError} empty={!data?.sales.length}><DataTable rows={data?.sales??[]}/></ChartCard>
    <div className="stats-three stats-secondary">
      <ChartCard {...base} title="Comportamiento de clientes" help="Clientes registrados actuales, excluyendo administradores. Actividad: compras aprobadas en el período. No existe fecha de registro para filtrar altas." error={load.error||data?.errors.users||data?.errors.catalog}>
        <dl><dt>Clientes con ventas aprobadas</dt><dd>{data?`${data.users.sellers} de ${data.users.registered} (${number(data.users.sellerPercent)}%)`:'—'}</dd><dt>Compradores activos</dt><dd>{number(data?.users.buyers)}</dd><dt>Publicaciones creadas en el período</dt><dd>{number(data?.current.created)}</dd></dl>
      </ChartCard>
      <ChartCard {...base} title="Publicaciones y moderación" help="Estados actuales de las publicaciones creadas en el período, filtradas por categoría. No se filtran por método de pago. Los motivos son texto libre, sin taxonomía normalizada." error={catalogError} empty={!data?.publicationStates.length}>
        <GroupTable groups={data?.publicationStates??[]} amount={false}/><details><summary>Motivos registrados</summary><GroupTable groups={data?.rejectionReasons??[]} amount={false}/></details>
      </ChartCard>
      <ChartCard {...base} title="Atributos de skins vendidas" help="Atributos actuales del catálogo para las líneas vendidas. Precios históricos en CLP; float nulo (por ejemplo stickers) queda fuera del cálculo, nunca se trata como cero." error={catalogError} empty={!data?.current.units}>
        <dl><dt>Precio mínimo / máximo</dt><dd>{money(data?.attributes.priceMin)} / {money(data?.attributes.priceMax)}</dd><dt>Mediana del precio</dt><dd>{money(data?.attributes.priceMedian)}</dd><dt>Float mínimo / máximo / promedio</dt><dd>{[data?.attributes.floatMin,data?.attributes.floatMax,data?.attributes.floatAverage].map(v=>v==null?'Sin dato':v.toFixed(5)).join(' / ')}</dd></dl>
        <details><summary>Desgaste, grado y StatTrak</summary><h3>Desgaste</h3><GroupTable groups={data?.wear??[]} amount={false}/><h3>Grado</h3><GroupTable groups={data?.grades??[]} amount={false}/><h3>StatTrak</h3><GroupTable groups={data?.stattrak??[]} amount={false}/></details>
      </ChartCard>
    </div>
    <details className="stats-definitions"><summary>Definiciones y calidad de datos</summary><ul>{data?.warnings.map(w=><li key={w}>{w}</li>)}</ul><p>El comprador paga la comisión adicional. “Ventas sin comisión” corresponde al precio de las skins; “Comisión ganada” no descuenta tarifas del proveedor ni impuestos. Categorías y atributos son los actuales, no una copia histórica.</p></details>
  </div>;
}
