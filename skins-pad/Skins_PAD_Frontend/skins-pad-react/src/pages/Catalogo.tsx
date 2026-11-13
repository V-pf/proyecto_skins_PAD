import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import ListaProductos from "../components/ListaProductos";
import Migas from "../components/Migas";
import { useTienda } from "../context/TiendaContext";
import { GRADOS, TIPOS } from "../lib/catalogos";
import { formatoCLP } from "../lib/formato";
import type { Grado, Tipo } from "../types";

type Orden = "relevancia" | "precio-asc" | "precio-desc" | "nombre";

function alternar<T>(lista: T[], valor: T): T[] {
  return lista.includes(valor) ? lista.filter((x) => x !== valor) : [...lista, valor];
}

export default function Catalogo() {
  const { productos, cargando } = useTienda();
  const [params,setParams] = useSearchParams();
  const categoria=params.get("categoria");

  // los filtros viven en el estado; los valores iniciales vienen de la URL
  // (catalogo?tipo=rifle desde la home, catalogo?q=awp desde el buscador)
  const [tipos, setTipos] = useState<Tipo[]>(() => {
    const t = params.get("tipo") as Tipo | null;
    return t && TIPOS.some((x) => x.valor === t) ? [t] : [];
  });
  const [grados, setGrados] = useState<Grado[]>(() => {
    const g = params.get("grado") as Grado | null;
    return g && GRADOS.some((x) => x.valor === g) ? [g] : [];
  });
  const [soloStattrak, setSoloStattrak] = useState(false);
  const [precioMin, setPrecioMin] = useState(0);
  const [precioMax, setPrecioMax] = useState(0);
  const [texto, setTexto] = useState(params.get("q") ?? "");
  const [orden, setOrden] = useState<Orden>("relevancia");

  // si ya estás en el catálogo y usas el buscador de la cabecera, la URL
  // cambia pero el componente no se vuelve a crear: este efecto lo sincroniza
  useEffect(() => {
    setTexto(params.get("q") ?? "");
    const t = params.get("tipo") as Tipo | null;
    if (t && TIPOS.some((x) => x.valor === t)) setTipos([t]);
    const g = params.get("grado") as Grado | null;
    if (g && GRADOS.some((x) => x.valor === g)) setGrados([g]);
  }, [params]);

  const visibles = useMemo(() => {
    const busqueda = texto.trim().toLowerCase();
    const lista = productos.filter(
      (p) =>
        p.estado === "activa" && (!categoria || p.categoriaId===Number(categoria)) &&
        (tipos.length === 0 || tipos.includes(p.tipo)) &&
        (grados.length === 0 || grados.includes(p.grado)) &&
        (!soloStattrak || p.stattrak) &&
        p.precio >= precioMin && (precioMax === 0 || p.precio <= precioMax) &&
        (busqueda === "" || (p.nombre + " " + (p.descripcion ?? "")).toLowerCase().includes(busqueda))
    );
    return [...lista].sort((a, b) => {
      if (orden === "precio-asc") return a.precio - b.precio;
      if (orden === "precio-desc") return b.precio - a.precio;
      if (orden === "nombre") return a.nombre.localeCompare(b.nombre, "es");
      return a.id - b.id;
    });
  }, [productos, tipos, grados, soloStattrak, precioMin, precioMax, texto, orden,categoria]);

  function limpiar() {
    setParams({});
    setTipos([]);
    setGrados([]);
    setSoloStattrak(false);
    setPrecioMin(0);
    setPrecioMax(0);
    setTexto("");
  }

  if (cargando.productos) return <p className="container py-4">Cargando catálogo…</p>;
  return (
    <div className="container py-4">
      <Migas items={[{ texto: "Inicio", a: "/" }, { texto: "Catálogo" }]} />
      <h1 className="mb-4">Catálogo de skins</h1>
      <div className="row g-4">
        <div className="col-lg-3">
          <form className="filtros" aria-label="Filtros del catálogo" onSubmit={(e) => e.preventDefault()}>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="h6 mb-0">Filtros</h2>
              <button type="button" className="btn btn-sm btn-link p-0" onClick={limpiar}>
                Limpiar
              </button>
            </div>
            <fieldset className="mb-3">
              <legend>Tipo de arma</legend>
              {TIPOS.map((t) => (
                <div className="form-check" key={t.valor}>
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id={`tipo-${t.valor}`}
                    checked={tipos.includes(t.valor)}
                    onChange={() => setTipos((prev) => alternar(prev, t.valor))}
                  />
                  <label className="form-check-label" htmlFor={`tipo-${t.valor}`}>
                    {t.etiqueta}
                  </label>
                </div>
              ))}
            </fieldset>
            <fieldset className="mb-3">
              <legend>Grado de rareza</legend>
              {GRADOS.map((g) => (
                <div className="form-check" key={g.valor}>
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id={`grado-${g.valor}`}
                    checked={grados.includes(g.valor)}
                    onChange={() => setGrados((prev) => alternar(prev, g.valor))}
                  />
                  <label className="form-check-label" htmlFor={`grado-${g.valor}`}>
                    {g.etiqueta}
                  </label>
                </div>
              ))}
            </fieldset>
            <div className="form-check mb-3">
              <input
                className="form-check-input"
                type="checkbox"
                id="solo-stattrak"
                checked={soloStattrak}
                onChange={(e) => setSoloStattrak(e.target.checked)}
              />
              <label className="form-check-label" htmlFor="solo-stattrak">
                Solo StatTrak™
              </label>
            </div>
            <div className="mb-1">
              <label>Precio mínimo (CLP)<input className="form-control mb-3" type="number" min="0" value={precioMin || ""} onChange={e => setPrecioMin(Number(e.target.value))} /></label>
              <label htmlFor="precio-max" className="form-label">
                Precio máximo
              </label>
              <input
                type="range"
                className="form-range"
                id="precio-max"
                min={0}
                max={4000000}
                step={10000}
                value={precioMax}
                onChange={(e) => setPrecioMax(Number(e.target.value))}
              />
              <output htmlFor="precio-max" className="small text-body-secondary">
                {precioMax === 0 ? "Sin límite" : `Hasta ${formatoCLP(precioMax)}`}
              </output>
            </div>
          </form>
          <aside className="mt-4">
            <h2 className="h6">¿Buscas algo específico?</h2>
            <p className="small text-body-secondary">
              Escribe el nombre del arma y el patrón en el buscador, por ejemplo "AK-47" o "Karambit".
            </p>
          </aside>
        </div>

        <div className="col-lg-9">
          <div className="d-flex flex-wrap gap-2 justify-content-between align-items-center mb-3">
            <label className="visually-hidden" htmlFor="buscar-catalogo">
              Buscar dentro del catálogo
            </label>
            <input
              type="search"
              className="form-control"
              style={{ maxWidth: "320px" }}
              id="buscar-catalogo"
              placeholder="Buscar por nombre..."
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
            />
            <div className="d-flex align-items-center gap-2">
              <span className="small text-body-secondary" aria-live="polite">
                {visibles.length} resultado{visibles.length === 1 ? "" : "s"}
              </span>
              <label className="visually-hidden" htmlFor="orden-catalogo">
                Ordenar
              </label>
              <select
                className="form-select form-select-sm"
                id="orden-catalogo"
                style={{ width: "auto" }}
                value={orden}
                onChange={(e) => setOrden(e.target.value as Orden)}
              >
                <option value="relevancia">Más relevantes</option>
                <option value="precio-asc">Precio: menor a mayor</option>
                <option value="precio-desc">Precio: mayor a menor</option>
                <option value="nombre">Nombre A-Z</option>
              </select>
            </div>
          </div>
          {visibles.length === 0 ? (
            <p className="alert alert-secondary">No encontramos skins con esos filtros. Prueba quitando alguno.</p>
          ) : (
            <ListaProductos productos={visibles} columnas="row-cols-1 row-cols-sm-2 row-cols-xl-3" />
          )}
        </div>
      </div>
    </div>
  );
}
