import type { Producto } from "../types";
import { useTienda } from "../context/TiendaContext";
import { useOperacion } from "../lib/useOperacion";
import { solicitar } from "../services/http";
export default function Imagenes({ producto }: { producto: Producto }) {
  const { recargar } = useTienda(),
    { ejecutar, ocupado } = useOperacion();
  const ruta = "/productos/" + producto.id + "/imagenes";
  return (
    <div>
      {producto.imagenes?.map((i) => (
        <div key={i.id}>
          <img src={i.urlImagen} alt={producto.nombre} width="64" />
          <button
            disabled={ocupado}
            onClick={() =>
              void ejecutar(async () => {
                await solicitar(ruta + "/" + i.id, "DELETE");
                await recargar();
              })
            }
          >
            Quitar imagen
          </button>
        </div>
      ))}
      <label>
        Agregar imagen JPG o PNG (máx. 32 KB)
        <input
          type="file"
          accept="image/jpeg,image/png"
          disabled={ocupado}
          onChange={(e) => {
            const f = e.target.files?.[0];
            e.target.value = "";
            if (!f) return;
            void ejecutar(async () => {
              if (
                f.size > 32768 ||
                !["image/jpeg", "image/png"].includes(f.type)
              )
                throw Error("Usa JPG o PNG de hasta 32 KB.");
              const url = await new Promise<string>((res, rej) => {
                const r = new FileReader();
                r.onload = () => res(String(r.result));
                r.onerror = () => rej(Error("No se pudo leer la imagen"));
                r.readAsDataURL(f);
              });
              await solicitar(ruta, "POST", {
                urlImagen: url,
                esPrincipal: !producto.imagenes?.length,
              });
              await recargar();
            });
          }}
        />
      </label>
      <small className="d-block">
        Sin fotos propias se usa la ilustración del tipo de arma.
      </small>
    </div>
  );
}
