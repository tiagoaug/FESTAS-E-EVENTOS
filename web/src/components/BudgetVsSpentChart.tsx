import { useState } from 'react'
import { ChevronDown, PieChart } from 'lucide-react'
import { FALLBACK_CATEGORY_COLOR, FALLBACK_CATEGORY_LABEL } from '../types'
import type { CategoryEntity, ExpenseEntity } from '../types'
import { formatCurrency } from '../utils/format'

export default function BudgetVsSpentChart({
  budget,
  expenses,
  categories,
}: {
  budget: number
  expenses: ExpenseEntity[]
  categories: CategoryEntity[]
}) {
  const [showBreakdown, setShowBreakdown] = useState(true)
  const totalSpent = expenses.reduce((sum, e) => sum + e.amount, 0)
  const remainingBudget = Math.max(0, budget - totalSpent)
  const totalPaid = expenses.filter((e) => e.isPaid).reduce((sum, e) => sum + e.amount, 0)
  const totalToPay = expenses.filter((e) => !e.isPaid).reduce((sum, e) => sum + e.amount, 0)

  const categoryIds = Array.from(new Set(expenses.map((e) => e.category)))
  const byCategory = categoryIds
    .map((categoryId) => {
      const category = categories.find((c) => c.id === categoryId) ?? {
        id: categoryId,
        name: FALLBACK_CATEGORY_LABEL,
        color: FALLBACK_CATEGORY_COLOR,
      }
      const total = expenses.filter((e) => e.category === categoryId).reduce((sum, e) => sum + e.amount, 0)
      return { category, total }
    })
    .filter((c) => c.total > 0)

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

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginTop: 14,
          paddingTop: 14,
          borderTop: '1px solid var(--outline-variant)',
        }}
      >
        <div style={{ flex: 1 }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: 0, fontWeight: 600 }}>
            Valores Pagos
          </p>
          <p style={{ fontWeight: 800, margin: 0, color: 'var(--success)' }}>{formatCurrency(totalPaid)}</p>
        </div>
        <div style={{ width: 1, height: 30, background: 'var(--outline-variant)' }} />
        <div style={{ flex: 1, textAlign: 'right' }}>
          <p style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)', margin: 0, fontWeight: 600 }}>
            Valores a Pagar
          </p>
          <p style={{ fontWeight: 800, margin: 0, color: 'var(--danger)' }}>{formatCurrency(totalToPay)}</p>
        </div>
      </div>

      <button
        type="button"
        onClick={() => setShowBreakdown((v) => !v)}
        style={{
          width: '100%',
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
          padding: 0,
          fontWeight: 800,
          fontSize: '0.85rem',
          margin: '20px 0 10px',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          color: 'var(--on-surface)',
        }}
      >
        <PieChart size={16} strokeWidth={2.4} color="var(--primary)" />
        <span style={{ flex: 1, textAlign: 'left' }}>Gastos por Categoria</span>
        <ChevronDown
          size={16}
          strokeWidth={2.4}
          style={{ transition: 'transform 0.2s ease', transform: showBreakdown ? 'rotate(180deg)' : 'rotate(0deg)' }}
        />
      </button>

      {!showBreakdown ? null : byCategory.length === 0 ? (
        <p style={{ fontSize: '0.82rem', color: 'var(--on-surface-variant)' }}>
          Nenhum gasto cadastrado ainda.
        </p>
      ) : (
        byCategory.map(({ category, total }) => {
          const pct = totalSpent > 0 ? total / totalSpent : 0
          return (
            <div key={category.id} style={{ marginBottom: 10 }}>
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
                      background: category.color,
                      display: 'inline-block',
                      boxShadow: `0 0 0 3px ${category.color}33`,
                    }}
                  />
                  <span style={{ fontWeight: 600 }}>{category.name}</span>
                </span>
                <strong>
                  {formatCurrency(total)} ({Math.round(pct * 100)}%)
                </strong>
              </div>
              <div className="progress-track" style={{ marginTop: 4 }}>
                <div className="progress-fill" style={{ width: `${pct * 100}%`, background: category.color }} />
              </div>
            </div>
          )
        })
      )}
    </div>
  )
}
