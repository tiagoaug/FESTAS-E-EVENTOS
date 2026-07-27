import { useState, type ReactNode } from 'react'
import { ChevronDown } from 'lucide-react'

interface AccordionSectionProps {
  title: string
  icon?: ReactNode
  headerAction?: ReactNode
  defaultExpanded?: boolean
  children: ReactNode
}

export default function AccordionSection({
  title,
  icon,
  headerAction,
  defaultExpanded = true,
  children,
}: AccordionSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded)

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: 16,
            background: 'transparent',
            border: 'none',
            cursor: 'pointer',
            textAlign: 'left',
          }}
        >
          {icon}
          <span style={{ flex: 1, fontWeight: 800, fontSize: '1rem' }}>{title}</span>
          <ChevronDown
            size={18}
            strokeWidth={2.4}
            style={{ transition: 'transform 0.2s ease', transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)' }}
          />
        </button>
        {headerAction && (
          <div style={{ paddingRight: 12 }} onClick={(e) => e.stopPropagation()}>
            {headerAction}
          </div>
        )}
      </div>
      {expanded && <div style={{ padding: '0 16px 16px' }}>{children}</div>}
    </div>
  )
}
