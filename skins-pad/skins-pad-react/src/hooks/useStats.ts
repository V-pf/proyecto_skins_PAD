import { useCallback, useEffect } from 'react';
import { solicitar } from '../services/http';
import { useCarga } from './useCarga';
import type { Stats } from '../lib/stats/types';
export function useStats(query:string) {
  const cargar=useCallback((signal:AbortSignal)=>solicitar<Stats>('/admin/stats?'+query,'GET',undefined,signal),[query]);
  const carga=useCarga<Stats|null>(cargar,null);
  useEffect(()=>{const reload=()=>{if(document.visibilityState==='visible')void carga.recargar();};const id=setInterval(reload,60000);window.addEventListener('focus',reload);return()=>{clearInterval(id);window.removeEventListener('focus',reload);};},[carga.recargar]);
  return carga;
}
