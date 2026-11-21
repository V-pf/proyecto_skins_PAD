import {useCallback,useEffect,useState} from 'react';
import {solicitar} from '../services/http';
export default function useSoporte<T>(ruta:string,intervalo=0){
 const [datos,setDatos]=useState<T|null>(null),[error,setError]=useState(''),[cargando,setCargando]=useState(true),[revision,setRevision]=useState(0);
 const recargar=useCallback(()=>setRevision(x=>x+1),[]);
 useEffect(()=>{const abort=new AbortController();let enCurso=false;setDatos(null);setCargando(true);
  async function cargar(){if(enCurso)return;enCurso=true;try{const d=await solicitar<T>(ruta,'GET',undefined,abort.signal);if(!abort.signal.aborted){setDatos(d);setError('')}}catch(e){if(!abort.signal.aborted)setError((e as Error).message)}finally{enCurso=false;if(!abort.signal.aborted)setCargando(false)}}
  void cargar();const timer=intervalo?setInterval(()=>{if(document.visibilityState!=='hidden')void cargar()},intervalo):undefined;
  return()=>{abort.abort();if(timer)clearInterval(timer)};
 },[ruta,revision,intervalo]);return {datos,error,cargando,recargar};
}
