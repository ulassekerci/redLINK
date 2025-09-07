import { motion, animate, useMotionValue, useTransform } from 'motion/react'
import { useUIStore } from '../../store/ui'
import { useEffect } from 'react'

export const OuterRing = ({ value, max, left }: { value?: number; max?: number; left?: boolean }) => {
  const angle = useMotionValue(0)

  useEffect(() => {
    const limitedValue = value && max ? (value > max ? max : value) / 3 : 0
    const proportionateValue = (limitedValue * 135) / (max || 1)
    animate(angle, proportionateValue, { type: 'spring', stiffness: 150, damping: 20 })
  }, [value, max, angle])

  const { aston } = useUIStore()

  const color1 = 'rgb(225, 29, 72)'
  const color2 = 'rgb(245, 49, 92)'
  const bgColor = 'rgba(159, 18, 57, 0.5)'

  const astonRightGradient = useTransform(angle, (a) => {
    return `
      conic-gradient(
        from 0.25deg,
        ${color1} 0deg,
        ${color2} ${a}deg,
        ${bgColor} ${a}deg,
        ${bgColor} 45deg,
        transparent 45deg,
        transparent 50deg,
        ${bgColor} 50deg,
        ${bgColor} 169deg,
        transparent 169deg,
        transparent 192deg,
        ${bgColor} 192deg,
        ${bgColor} 270deg,
        transparent 270deg
      )`
  })

  const astonLeftGradient = useTransform(angle, (a) => {
    return `
      conic-gradient(
        from 45deg,
        ${bgColor} 0deg,
        ${bgColor} ${45 - a}deg,
        ${color2} ${45 - a}deg,
        ${color1} 45deg,
        transparent 45deg,
        transparent 135deg,
        ${bgColor} 135deg,
        ${bgColor} 218deg,
        transparent 218deg,
        transparent 232deg,
        ${bgColor} 232deg,
        ${bgColor} 355deg,
        transparent 355deg
      )`
  })

  const getGradient = () => {
    if (aston) return left ? astonLeftGradient : astonRightGradient
    else return 'transparent'
  }

  return (
    <>
      <motion.div
        className='absolute inset-0 rounded-full scale-[99%]'
        style={{
          background: getGradient(),
          transform: `rotate(${left ? 45 : 225}deg)`,
        }}
      />
      <div className='absolute inset-0 rounded-full bg-slate-950 scale-[97.5%]' />
      {aston && left && (
        <>
          <GaugeLetters letter='T' location={-47.75} />
          <GaugeLetters letter='E' location={-45.1} />
          <GaugeLetters letter='K' location={-42.25} />
        </>
      )}
      {aston && !left && (
        <>
          <GaugeLetters letter='r' location={38.25} />
          <GaugeLetters letter='e' location={40.25} />
          <GaugeLetters letter='d' location={42.75} />
          <GaugeLetters letter='L' location={45.25} />
          <GaugeLetters letter='I' location={47} />
          <GaugeLetters letter='N' location={49.25} />
          <GaugeLetters letter='K' location={52.25} />
        </>
      )}
    </>
  )
}

const GaugeLetters = ({ letter, location }: { letter: string; location: number }) => {
  return (
    <div
      className='absolute text-center -inset-[10px]'
      style={{
        rotate: `${location}deg`,
      }}
    >
      <p className='inline-block text-rose-100/60 text-sm font-bold'>{letter}</p>
    </div>
  )
}
