import { Wallet, TrendingDown, TrendingUp, CreditCard } from 'lucide-react'
import type { ComponentType } from 'react'
import type { FinancialSummary } from '../types'
import { formatCurrency } from '../utils/format'

function MetricTile({
  title,
  amount,
  icon: Icon,
  iconBg,
  iconColor,
}: {
  title: string
  amount: number
  icon: ComponentType<{ size?: number; strokeWidth?: number }>
  iconBg: string
  iconColor: string
}) {
  return (
    <div className="metric-tile">
      <div className="icon-box" style={{ background: iconBg, color: iconColor }}>
        <Icon size={18} strokeWidth={2.4} />
      </div>
      <div>
        <p className="metric-title">{title}</p>
        <p className="metric-value">{formatCurrency(amount)}</p>
      </div>
    </div>
  )
}

export default function FinancialSummaryCard({ summary }: { summary: FinancialSummary }) {
  const spentPct =
    summary.budget > 0 ? Math.min(1, Math.max(0, summary.totalSpent / summary.budget)) : 0
  const collectionPct =
    summary.totalExpectedCollection > 0
      ? Math.min(1, Math.max(0, summary.totalCollected / summary.totalExpectedCollection))
      : 0
  const balanceColor = summary.netBalance >= 0 ? '#1f9e5c' : '#e5484d'
  const balanceBg = summary.netBalance >= 0 ? 'linear-gradient(155deg, #ffffff, #d6f7e6)' : 'linear-gradient(155deg, #ffffff, #ffe1e1)'

  return (
    <div className="card">
      <p className="card-title">Resumo Financeiro da Festa</p>

      <div className="grid-2" style={{ marginBottom: 10 }}>
        <MetricTile
          title="Orçado"
          amount={summary.budget}
          icon={Wallet}
          iconBg="linear-gradient(155deg, #ffffff, #dce8ff)"
          iconColor="#3b6fe0"
        />
        <MetricTile
          title="Gasto"
          amount={summary.totalSpent}
          icon={TrendingDown}
          iconBg="linear-gradient(155deg, #ffffff, #ffe1e1)"
          iconColor="#e5484d"
        />
      </div>
      <div className="grid-2">
        <MetricTile
          title="Arrecadado"
          amount={summary.totalCollected}
          icon={TrendingUp}
          iconBg="linear-gradient(155deg, #ffffff, #d6f7e6)"
          iconColor="#1f9e5c"
        />
        <MetricTile
          title="Saldo Atual"
          amount={summary.netBalance}
          icon={CreditCard}
          iconBg={balanceBg}
          iconColor={balanceColor}
        />
      </div>

      <div style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem' }}>
          <span style={{ color: 'var(--on-surface-variant)', fontWeight: 600 }}>Utilização do Orçamento</span>
          <strong style={{ color: spentPct >= 1 ? 'var(--danger)' : 'var(--primary)' }}>
            {Math.round(spentPct * 100)}% ({formatCurrency(summary.totalSpent)} /{' '}
            {formatCurrency(summary.budget)})
          </strong>
        </div>
        <div className="progress-track" style={{ marginTop: 6 }}>
          <div
            className="progress-fill"
            style={{
              width: `${spentPct * 100}%`,
              background:
                spentPct >= 1
                  ? 'linear-gradient(90deg, #ff8a8a, var(--danger))'
                  : 'linear-gradient(90deg, var(--primary-2), var(--primary))',
            }}
          />
        </div>
      </div>

      <div style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem' }}>
          <span style={{ color: 'var(--on-surface-variant)', fontWeight: 600 }}>Progresso da Arrecadação</span>
          <strong style={{ color: '#1f9e5c' }}>
            {Math.round(collectionPct * 100)}% ({formatCurrency(summary.totalCollected)} /{' '}
            {formatCurrency(summary.totalExpectedCollection)})
          </strong>
        </div>
        <div className="progress-track" style={{ marginTop: 6 }}>
          <div
            className="progress-fill"
            style={{ width: `${collectionPct * 100}%`, background: 'linear-gradient(90deg, #6ee7a3, #1f9e5c)' }}
          />
        </div>
      </div>
    </div>
  )
}
