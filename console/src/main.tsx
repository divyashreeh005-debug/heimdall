import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "./index.css";
import App from "./App";
import Login from "./pages/Login";
import PrList from "./pages/PrList";
import PrDetail from "./pages/PrDetail";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<App />}>
          <Route path="login" element={<Login />} />
          <Route path="repos/:repoId" element={<PrList />} />
          <Route path="pull-requests/:prId" element={<PrDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
