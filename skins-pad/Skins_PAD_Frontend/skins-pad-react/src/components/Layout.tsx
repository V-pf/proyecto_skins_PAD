import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";

export default function Layout() {
  const { pathname, hash } = useLocation();

  // al cambiar de página volvemos arriba (o bajamos al #ancla si la hay)
  useEffect(() => {
    if (hash) {
      document.getElementById(hash.slice(1))?.scrollIntoView();
    } else {
      window.scrollTo(0, 0);
    }
  }, [pathname, hash]);

  return (
    <>
      <Header />
      <main id="contenido">
        <Outlet />
      </main>
      <Footer />
    </>
  );
}
