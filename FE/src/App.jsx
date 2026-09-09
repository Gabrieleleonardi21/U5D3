import { useEffect, useState } from 'react'
import { api } from './api.js'
import './App.css'

function App() {
  const [stato, setStato] = useState('Collegamento in corso...')

  // Chiamata di prova al back-end: serve a verificare che CORS sia configurato
  // correttamente. Se l'origine non fosse autorizzata, il browser bloccherebbe
  // la risposta e finiremmo nel catch.
  useEffect(() => {
    api('/api/ping')
      .then((dati) => setStato(`Back-end raggiunto: ${dati.messaggio} (${dati.orario})`))
      .catch((errore) => setStato(`Back-end non raggiungibile: ${errore.message}`))
  }, [])

  return (
    <main>
      <h1>U5D3</h1>
      <p>{stato}</p>
    </main>
  )
}

export default App
