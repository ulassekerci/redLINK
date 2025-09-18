import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './tailwind.css'
import { BrowserRouter, Route, Routes } from 'react-router'
import { HomeScreen } from './routes/Home'
import { MapScreen } from './routes/Map'
import 'maplibre-gl/dist/maplibre-gl.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path='/' element={<HomeScreen />} />
        <Route path='/map' element={<MapScreen />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>
)
