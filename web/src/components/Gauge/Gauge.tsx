import { useState, type ReactNode } from 'react'
import { GaugeMarkers } from './Markers'
import { GaugeNumbers } from './Numbers'
import { motion } from 'motion/react'

interface GaugeProps {
  value: number
  max: number
  unit: string
  step?: number
  children?: ReactNode
  outerRing?: { value: number; max: number }
}

export const Gauge = ({ value, max, unit, children, step = 10, outerRing }: GaugeProps) => {
  const [aston, setAston] = useState(false)

  return (
    <motion.div
      initial={{ scale: 0.25 }}
      animate={{ scale: 1 }}
      transition={{ type: 'spring', damping: 20, stiffness: 200 }}
      className='relative w-[420px] h-[420px] cursor-default select-none'
      onClick={() => setAston(!aston)}
    >
      {(aston || outerRing) && <OuterRing value={outerRing?.value} max={outerRing?.max} />}
      <GradientCircle value={value} max={max} />
      {aston && <InnerRing value={value} max={max} />}
      <NumberDisplay value={value} unit={unit} />
      <div className='flex flex-col absolute left-1/2 transform -translate-x-1/2 bottom-10'>{children}</div>
      {aston && <GaugeMarkers max={max} step={step / 10} />}
      {aston && <GaugeNumbers value={value} max={max} step={step} />}
    </motion.div>
  )
}

const NumberDisplay = ({ value, unit }: { value: number; unit: string }) => {
  return (
    <div className='flex flex-col absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2'>
      <span className='text-white text-8xl font-bold [font-variant-numeric:tabular-nums]'>{value}</span>
      <span className='text-white/75 text-lg font-medium text-center'>{unit}</span>
    </div>
  )
}

const GradientCircle = ({ value, max }: { value: number; max: number }) => {
  return (
    <>
      <div
        className='absolute inset-0 rounded-full scale-[93.7%]'
        style={{
          background: `conic-gradient(
              from 0deg,
              rgba(159, 18, 57, 0.5) 0deg,
              rgba(225, 29, 72, 1) ${(value * 270) / max + 90}deg,
              rgba(159, 18, 57, 0.5) ${(value * 270) / max + 90}deg
            )`,
          transform: `rotate(135deg)`,
        }}
      />
      <div className='absolute inset-0 rounded-full bg-gradient-to-tr from-[#12011e] to-[#1e0109] scale-[56%]' />
    </>
  )
}

const OuterRing = ({ value, max }: { value?: number; max?: number }) => {
  const limitedValue = value && max ? (value > max ? max : value) / 3 : 0
  return (
    <>
      <div
        className='absolute inset-0 rounded-full scale-[100%]'
        style={{
          background: `conic-gradient(
              from 0deg,
              rgba(225, 29, 72, 1) 0deg,
              rgba(225, 29, 72, 1) ${(limitedValue * 270) / (max || 1)}deg,
              rgba(159, 18, 57, 0.5) ${(limitedValue * 270) / (max || 1)}deg,
              rgba(159, 18, 57, 0.5) 90deg,
              transparent 90deg
            )`,
          transform: `rotate(225deg)`,
        }}
      />
      <div className='absolute inset-0 rounded-full bg-slate-950 scale-[98.5%]' />
    </>
  )
}

const InnerRing = ({ value, max }: { value: number; max: number }) => {
  const limitedValue = value > max ? max : value
  return (
    <>
      <div
        className='absolute inset-0 rounded-full scale-[52%]'
        style={{
          background: `conic-gradient(
              from 0deg,
              rgba(225, 29, 72, 1) 0deg,
              rgba(225, 29, 72, 1) ${(limitedValue * 270) / max}deg,
              rgba(159, 18, 57, 0.5) ${(limitedValue * 270) / max}deg
            )`,
          transform: `rotate(225deg)`,
        }}
      />
      <div className='absolute inset-0 rounded-full bg-gradient-to-tr from-[#12011e] to-[#1e0109] scale-[50%]' />
    </>
  )
}
