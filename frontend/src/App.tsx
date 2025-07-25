import { Routes, Route } from 'react-router-dom';
import bg from './assets/bg.svg';
import logo from './assets/logo.svg';
import main from './assets/main_logo.svg';
import './App.css';

import Login from './components/Login';
import Dashboard from './components/Dashboard';
import ProtectedRoute from "./components/ProtectedRoute.tsx";
import type { JSX } from "react";

function App(): JSX.Element {
    return (
        <div
            className="relative min-h-screen w-full bg-cover bg-no-repeat bg-center flex items-center justify-center"
            style={{ backgroundImage: `url(${bg})` }}
        >
            {/* Top-right floating logo */}
            <div className="absolute top-4 right-4 w-[140px] h-[140px] opacity-90">
                <img src={logo} alt="Logo background" className="w-full h-full opacity-80" />
                <img src={main} alt="Main logo overlay"
                     className="absolute top-1/2 left-1/2 w-[110px] h-[110px] transform -translate-x-1/2 -translate-y-1/2 opacity-90" />
            </div>

            {/* Page content based on route */}
            <div className="z-10">
                <Routes>
                    <Route path="/" element={<Login />} />
                    <Route path="/dashboard"
                           element={
                               <ProtectedRoute>
                                   <Dashboard />
                               </ProtectedRoute>
                           } />
                </Routes>
            </div>
        </div>
    );
}

export default App;
