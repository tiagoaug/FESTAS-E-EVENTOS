import { useState } from 'react'

export default function MiniCalculator({
  onClose,
  onUse,
}: {
  onClose: () => void
  onUse: (value: number) => void
}) {
  const [display, setDisplay] = useState('0')
  const [stored, setStored] = useState<number | null>(null)
  const [pendingOp, setPendingOp] = useState<'+' | '-' | '×' | '÷' | null>(null)
  const [overwrite, setOverwrite] = useState(true)

  const inputDigit = (d: string) => {
    setDisplay((prev) => (overwrite || prev === '0' ? d : prev + d))
    setOverwrite(false)
  }

  const inputDot = () => {
    setDisplay((prev) => (overwrite ? '0.' : prev.includes('.') ? prev : `${prev}.`))
    setOverwrite(false)
  }

  const clearAll = () => {
    setDisplay('0')
    setStored(null)
    setPendingOp(null)
    setOverwrite(true)
  }

  const backspace = () => {
    setDisplay((prev) => (prev.length > 1 ? prev.slice(0, -1) : '0'))
  }

  const compute = (a: number, b: number, op: '+' | '-' | '×' | '÷') => {
    switch (op) {
      case '+':
        return a + b
      case '-':
        return a - b
      case '×':
        return a * b
      case '÷':
        return b === 0 ? 0 : a / b
    }
  }

  const chooseOp = (op: '+' | '-' | '×' | '÷') => {
    const current = parseFloat(display) || 0
    if (stored !== null && pendingOp) {
      const result = compute(stored, current, pendingOp)
      setStored(result)
      setDisplay(String(result))
    } else {
      setStored(current)
    }
    setPendingOp(op)
    setOverwrite(true)
  }

  const equals = () => {
    const current = parseFloat(display) || 0
    if (stored !== null && pendingOp) {
      const result = compute(stored, current, pendingOp)
      setDisplay(String(result))
      setStored(null)
      setPendingOp(null)
      setOverwrite(true)
    }
  }

  const keyStyle = { padding: '14px 0', fontSize: '1.05rem', fontWeight: 700 }

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 300 }}>
        <h2>Calculadora</h2>
        <div
          style={{
            background: 'var(--surface-variant)',
            borderRadius: 12,
            padding: '14px 16px',
            textAlign: 'right',
            fontSize: '1.6rem',
            fontWeight: 800,
            marginBottom: 12,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {display}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 16 }}>
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={clearAll}>
            C
          </button>
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={backspace}>
            ⌫
          </button>
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={() => chooseOp('÷')}>
            ÷
          </button>
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={() => chooseOp('×')}>
            ×
          </button>
          {['7', '8', '9'].map((d) => (
            <button key={d} type="button" className="btn btn-outline" style={keyStyle} onClick={() => inputDigit(d)}>
              {d}
            </button>
          ))}
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={() => chooseOp('-')}>
            -
          </button>
          {['4', '5', '6'].map((d) => (
            <button key={d} type="button" className="btn btn-outline" style={keyStyle} onClick={() => inputDigit(d)}>
              {d}
            </button>
          ))}
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={() => chooseOp('+')}>
            +
          </button>
          {['1', '2', '3'].map((d) => (
            <button key={d} type="button" className="btn btn-outline" style={keyStyle} onClick={() => inputDigit(d)}>
              {d}
            </button>
          ))}
          <button
            type="button"
            className="btn btn-primary"
            style={{ ...keyStyle, gridRow: 'span 2' }}
            onClick={equals}
          >
            =
          </button>
          <button
            type="button"
            className="btn btn-outline"
            style={{ ...keyStyle, gridColumn: 'span 2' }}
            onClick={() => inputDigit('0')}
          >
            0
          </button>
          <button type="button" className="btn btn-outline" style={keyStyle} onClick={inputDot}>
            ,
          </button>
        </div>
        <div className="dialog-actions">
          <button className="btn btn-outline" onClick={onClose}>
            Fechar
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              onUse(parseFloat(display) || 0)
              onClose()
            }}
          >
            Usar Resultado
          </button>
        </div>
      </div>
    </div>
  )
}
