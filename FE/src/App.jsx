import { useEffect, useState } from 'react'
import { api } from './api.js'
import Fotocamera from './Fotocamera.jsx'
import './App.css'

// Lingue installate con "brew install tesseract-lang": il codice a tre lettere
// e' quello che Tesseract si aspetta.
const LINGUE = [
  { codice: 'ita', etichetta: 'Italiano' },
  { codice: 'eng', etichetta: 'Inglese' },
  { codice: 'fra', etichetta: 'Francese' },
  { codice: 'deu', etichetta: 'Tedesco' },
]

function App() {
  const [file, setFile] = useState(null)
  const [lingua, setLingua] = useState('ita')
  const [testo, setTesto] = useState('')
  const [documenti, setDocumenti] = useState([])
  const [errore, setErrore] = useState('')
  const [inCorso, setInCorso] = useState(false)
  const [fotocameraAperta, setFotocameraAperta] = useState(false)

  // Lista iniziale dei documenti gia' salvati.
  useEffect(() => {
    caricaDocumenti()
  }, [])

  async function caricaDocumenti() {
    try {
      setDocumenti(await api('/api/document/all'))
    } catch (e) {
      setErrore(e.message)
    }
  }

  // Il corpo della richiesta e' identico per /scan e /upload: entrambi vogliono
  // il file e la lingua come form. Una funzione sola evita di ripeterlo.
  function costruisciForm() {
    const form = new FormData()
    form.append('file', file)
    form.append('lingua', lingua)
    return form
  }

  async function inviaA(percorso) {
    if (!file) {
      setErrore('Scegli prima un file')
      return null
    }

    setErrore('')
    setInCorso(true)
    try {
      return await api(percorso, { method: 'POST', body: costruisciForm() })
    } catch (e) {
      setErrore(e.message)
      return null
    } finally {
      // finally: il pulsante torna attivo anche quando la chiamata fallisce,
      // altrimenti al primo errore la pagina resterebbe bloccata.
      setInCorso(false)
    }
  }

  // Lo scatto della webcam e' un File come quello scelto dal disco: da qui in
  // poi il flusso e' identico, cambia solo da dove arriva l'immagine.
  function usaScatto(scatto) {
    setFile(scatto)
    setFotocameraAperta(false)
    setTesto('')
    setErrore('')
  }

  // Anteprima: legge il testo senza salvare niente.
  async function scansiona() {
    const risultato = await inviaA('/api/document/scan')
    if (risultato) {
      setTesto(risultato.text)
    }
  }

  // Salvataggio: file sul filesystem, documento sul database.
  async function salva() {
    const documento = await inviaA('/api/document/upload')
    if (documento) {
      setTesto(documento.text)
      // Aggiorniamo la lista dal server invece di aggiungere la riga a mano:
      // cosi' si vedono i valori veri salvati (id e createdAt li decide il BE).
      caricaDocumenti()
    }
  }

  return (
    <main>
      <h1>Scansione documenti (OCR)</h1>

      <section>
        <input
          type="file"
          accept="image/*,application/pdf"
          onChange={(evento) => setFile(evento.target.files[0])}
        />

        <button onClick={() => setFotocameraAperta(true)} disabled={fotocameraAperta}>
          Usa webcam
        </button>

        <select value={lingua} onChange={(evento) => setLingua(evento.target.value)}>
          {LINGUE.map((voce) => (
            <option key={voce.codice} value={voce.codice}>
              {voce.etichetta}
            </option>
          ))}
        </select>

        <button onClick={scansiona} disabled={inCorso}>
          Estrai testo
        </button>
        <button onClick={salva} disabled={inCorso}>
          Salva documento
        </button>

        {/* Il nome serve soprattutto dopo uno scatto: l'input file resta vuoto,
            quindi senza questa riga non si vedrebbe quale immagine e' pronta. */}
        {file && <p>File pronto: {file.name}</p>}
      </section>

      {fotocameraAperta && (
        <Fotocamera onScatto={usaScatto} onChiudi={() => setFotocameraAperta(false)} />
      )}

      {inCorso && <p>Elaborazione in corso...</p>}
      {errore && <p className="errore">{errore}</p>}

      {testo && (
        <section>
          <h2>Testo estratto</h2>
          {/* pre conserva a capo e spaziatura del testo cosi' come li rende l'OCR */}
          <pre>{testo}</pre>
        </section>
      )}

      <section>
        <h2>Documenti salvati ({documenti.length})</h2>
        <ul>
          {documenti.map((documento) => (
            <li key={documento.id}>
              <strong>{documento.titolo}</strong> — {documento.peso} byte —{' '}
              {new Date(documento.createdAt).toLocaleString('it-IT')}
            </li>
          ))}
        </ul>
      </section>
    </main>
  )
}

export default App
