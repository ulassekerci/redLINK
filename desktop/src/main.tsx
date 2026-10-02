import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './tailwind.css'
import { BrowserRouter, Route, Routes } from 'react-router'
import { HomeScreen } from './routes/Home'
import { MapScreen } from './routes/Map'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Root } from './components/Root'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path='/' element={<Root />}>
          <Route index element={<HomeScreen />} />
          <Route path='map' element={<MapScreen />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </StrictMode>
)
