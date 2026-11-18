import { Component, useId, type ReactNode } from 'react';
export function Help({text}:{text:string}) { const id=useId(); return <span className="stats-help"><button type="button" aria-label="Ver definición" aria-describedby={id}>ⓘ</button><span role="tooltip" id={id}>{text}</span></span>; }
class WidgetBoundary extends Component<{children:ReactNode},{failed:boolean}> {
  state={failed:false}; static getDerivedStateFromError(){return {failed:true};}
  render(){return this.state.failed?<p role="alert">No se pudo dibujar este widget. <button className="stats-button" onClick={()=>this.setState({failed:false})}>Reintentar</button></p>:this.props.children;}
}
export function ChartCard({title,help,loading,error,empty=false,retry,actions,children,className=''}:{title:string;help?:string;loading?:boolean;error?:string;empty?:boolean;retry?:()=>void;actions?:ReactNode;children:ReactNode;className?:string}) {
  return <section className={'stats-card '+className} aria-label={title} aria-busy={loading}>
    <header><h2>{title} {help&&<Help text={help}/>}</h2>{actions}</header>
    {loading?<div className="stats-skeleton" role="status" aria-label={'Cargando '+title}/>:error?<div role="alert"><p>{error}</p>{retry&&<button className="stats-button" onClick={retry}>Reintentar</button>}</div>:empty?<p className="stats-empty">Sin datos para esta selección.</p>:<WidgetBoundary>{children}</WidgetBoundary>}
  </section>;
}
