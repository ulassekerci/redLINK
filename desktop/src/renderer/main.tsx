import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@fontsource-variable/inter'
import './tailwind.css'
import { HashRouter, Route, Routes } from 'react-router'
import { HomeScreen } from './routes/Home'
import { MapScreen } from './routes/Map'
import { SettingsScreen } from './routes/Settings'
import 'maplibre-gl/dist/maplibre-gl.css'
import { Root } from './components/Root'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HashRouter>
      <Routes>
        <Route path='/' element={<Root />}>
          <Route index element={<HomeScreen />} />
          <Route path='map' element={<MapScreen />} />
          <Route path='settings' element={<SettingsScreen />} />
        </Route>
      </Routes>
    </HashRouter>
  </StrictMode>
)
