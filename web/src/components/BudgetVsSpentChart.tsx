import { PieChart } from 'lucide-react'
import { EXPENSE_CATEGORIES, EXPENSE_CATEGORY_COLOR, EXPENSE_CATEGORY_LABEL } from '../types'
import type { ExpenseEntity } from '../types'
import { formatCurrency } from '../utils/format'

export default function BudgetVsSpentChart({
  budget,
  expenses,
}: {
  budget: number
  expenses: ExpenseEntity[]
}) {
  const totalSpent = expenses.reduce((sum, e) => sum + e.amount, 0)
  const remainingBudget = Math.max(0, budget - totalSpent)

  const byCategory = EXPENSE_CATEGORIES.map((cat) => {
    const total = expenses.filter((e) => e.category === cat).reduce((sum, e) => sum + e.amount, 0)
    return { cat, total }
  }).filter((c) => c.total > 0)

  return (
    <div className="card">
      <p className="card-title">Comparativo: Orçado vs. Gasto Real</p>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{ flex: 1 }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: 0, fontWeight: 600 }}>
            Orçamento Total
          </p>
          <p style={{ fontWeight: 800, color: '#3b6fe0', margin: 0 }}>{formatCurrency(budget)}</p>
        </div>
        <div style={{ width: 1, height: 30, background: 'var(--outline-variant)' }} />
        <div style={{ flex: 1, textAlign: 'center' }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: 0, fontWeight: 600 }}>
            Total Gasto
          </p>
          <p
            style={{
              fontWeight: 800,
              margin: 0,
              color: totalSpent > budget ? 'var(--danger)' : 'var(--success)',
            }}
          >
            {formatCurrency(totalSpent)}
          </p>
        </div>
        <div style={{ width: 1, height: 30, background: 'var(--outline-variant)' }} />
        <div style={{ flex: 1, textAlign: 'right' }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: 0, fontWeight: 600 }}>
            Saldo
          </p>
          <p
            style={{
              fontWeight: 800,
              margin: 0,
              color: budget >= totalSpent ? 'var(--success)' : 'var(--danger)',
            }}
          >
            {formatCurrency(remainingBudget)}
          </p>
        </div>
      </div>

      <p
        style={{
          fontWeight: 800,
          fontSize: '0.85rem',
          margin: '20px 0 10px',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        <PieChart size={16} strokeWidth={2.4} color="var(--primary)" />
        Gastos por Categoria
      </p>

      {byCategory.length === 0 ? (
        <p style={{ fontSize: '0.82rem', color: 'var(--on-surface-variant)' }}>
          Nenhum gasto cadastrado ainda.
        </p>
      ) : (
        byCategory.map(({ cat, total }) => {
          const pct = totalSpent > 0 ? total / totalSpent : 0
          const color = EXPENSE_CATEGORY_COLOR[cat]
          return (
            <div key={cat} style={{ marginBottom: 10 }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  fontSize: '0.8rem',
                }}
              >
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span
                    style={{
                      width: 10,
                      height: 10,
                      borderRadius: '50%',
                      background: color,
                      display: 'inline-block',
                      boxShadow: `0 0 0 3px ${color}33`,
                    }}
                  />
                  <span style={{ fontWeight: 600 }}>{EXPENSE_CATEGORY_LABEL[cat]}</span>
                </span>
                <strong>
                  {formatCurrency(total)} ({Math.round(pct * 100)}%)
                </strong>
              </div>
              <div className="progress-track" style={{ marginTop: 4 }}>
                <div className="progress-fill" style={{ width: `${pct * 100}%`, background: color }} />
              </div>
            </div>
          )
        })
      )}
    </div>
  )
}
