import { twMerge } from 'tailwind-merge'

function App() {
  return (
    <>
      <div>
        <p className={twMerge('text-green-600')}>tailwind test</p>
      </div>
    </>
  )
}

export default App
