import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { TiendaProvider } from "./context/TiendaContext";
import { AuthProvider } from "./context/AuthContext";
import { CarritoProvider } from "./context/CarritoContext";
import { ToastProvider } from "./context/ToastContext";
import "bootstrap/dist/css/bootstrap.min.css";
import "./estilos.css";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <TiendaProvider>
          <CarritoProvider>
            <ToastProvider>
              <App />
            </ToastProvider>
          </CarritoProvider>
        </TiendaProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
