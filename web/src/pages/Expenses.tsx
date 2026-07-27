import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, PackagePlus, ShoppingBag, Pencil, Trash2, PartyPopper, Plus } from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import {
  EXPENSE_CATEGORIES,
  EXPENSE_CATEGORY_COLOR,
  EXPENSE_CATEGORY_LABEL,
  type ExpenseCategory,
  type ExpenseEntity,
} from '../types'
import { formatCurrency } from '../utils/format'
import BudgetVsSpentChart from '../components/BudgetVsSpentChart'

export default function Expenses({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { activeEvent, expenses, addExpense, updateExpense, toggleExpensePurchased, deleteExpense } = store

  const [categoryFilter, setCategoryFilter] = useState<ExpenseCategory | null>(null)
  const [showAdd, setShowAdd] = useState(false)
  const [editing, setEditing] = useState<ExpenseEntity | null>(null)

  const filtered = useMemo(
    () => (categoryFilter ? expenses.filter((e) => e.category === categoryFilter) : expenses),
    [expenses, categoryFilter],
  )

  if (!activeEvent) {
    return (
      <>
        <div className="top-bar">
          <button className="icon-btn" onClick={() => navigate('/')}>
            <ArrowLeft size={19} strokeWidth={2.3} />
          </button>
          <h1>Controle de Gastos</h1>
        </div>
        <div className="empty-state">
          <span className="emoji">
            <PartyPopper size={30} strokeWidth={2} />
          </span>
          <p>Crie um evento primeiro para cadastrar gastos.</p>
        </div>
      </>
    )
  }

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate('/')}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1 style={{ flex: 1 }}>Controle de Gastos</h1>
        <button className="icon-btn" onClick={() => setShowAdd(true)}>
          <PackagePlus size={19} strokeWidth={2.2} />
        </button>
      </div>

      <div className="app-content">
        <div className="page">
          <BudgetVsSpentChart budget={activeEvent.budget} expenses={expenses} />

          <div className="chip-row">
            <button className={`chip ${categoryFilter === null ? 'selected' : ''}`} onClick={() => setCategoryFilter(null)}>
              Todas
            </button>
            {EXPENSE_CATEGORIES.map((cat) => (
              <button
                key={cat}
                className={`chip ${categoryFilter === cat ? 'selected' : ''}`}
                onClick={() => setCategoryFilter(cat)}
              >
                {EXPENSE_CATEGORY_LABEL[cat]}
              </button>
            ))}
          </div>

          {filtered.length === 0 ? (
            <div className="empty-state">
              <span className="emoji">
                <ShoppingBag size={28} strokeWidth={2} />
              </span>
              <p>Nenhum item de gasto cadastrado nesta categoria.</p>
            </div>
          ) : (
            filtered.map((exp) => (
              <div key={exp.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 10, padding: 14 }}>
                <input
                  type="checkbox"
                  checked={exp.isPurchased}
                  onChange={(e) => toggleExpensePurchased(exp.id, e.target.checked)}
                  style={{ width: 20, height: 20, flexShrink: 0 }}
                />
                <div style={{ flex: 1 }}>
                  <strong style={{ fontSize: '0.88rem' }}>{exp.title}</strong>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                    <span
                      className="badge"
                      style={{
                        background: EXPENSE_CATEGORY_COLOR[exp.category] + '33',
                        color: EXPENSE_CATEGORY_COLOR[exp.category],
                      }}
                    >
                      {EXPENSE_CATEGORY_LABEL[exp.category]}
                    </span>
                    <span style={{ fontSize: '0.72rem', color: exp.isPurchased ? '#2E7D32' : '#E65100' }}>
                      {exp.isPurchased ? 'Comprado' : 'A Comprar'}
                    </span>
                  </div>
                </div>
                <strong>{formatCurrency(exp.amount)}</strong>
                <div style={{ display: 'flex', gap: 2 }}>
                  <button className="icon-btn" onClick={() => setEditing(exp)}>
                    <Pencil size={16} strokeWidth={2.2} />
                  </button>
                  <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => deleteExpense(exp)}>
                    <Trash2 size={16} strokeWidth={2.2} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <button className="fab" onClick={() => setShowAdd(true)}>
        <Plus size={24} strokeWidth={2.6} />
      </button>

      {(showAdd || editing) && (
        <ExpenseDialog
          expense={editing}
          onClose={() => {
            setShowAdd(false)
            setEditing(null)
          }}
          onSave={(data) => {
            if (editing) {
              updateExpense({ ...editing, ...data })
            } else {
              addExpense({ ...data, eventId: activeEvent.id, dateAddedMillis: Date.now() })
            }
            setShowAdd(false)
            setEditing(null)
          }}
        />
      )}
    </>
  )
}

function ExpenseDialog({
  expense,
  onClose,
  onSave,
}: {
  expense: ExpenseEntity | null
  onClose: () => void
  onSave: (data: { title: string; amount: number; category: ExpenseCategory; isPurchased: boolean }) => void
}) {
  const [title, setTitle] = useState(expense?.title ?? '')
  const [amountText, setAmountText] = useState(expense?.amount?.toString() ?? '')
  const [category, setCategory] = useState<ExpenseCategory>(expense?.category ?? 'FOOD')
  const [isPurchased, setIsPurchased] = useState(expense?.isPurchased ?? false)

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>{expense ? 'Editar Item de Gasto' : 'Novo Item de Gasto'}</h2>
        <div className="field">
          <label className="field-label">Descrição do Item</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Ex: Salgadinhos, Refrigerantes, Decoração"
          />
        </div>
        <div className="field">
          <label className="field-label">Valor Estimado/Pago (R$)</label>
          <input
            type="number"
            value={amountText}
            onChange={(e) => setAmountText(e.target.value)}
            placeholder="Ex: 150.00"
          />
        </div>
        <label className="field-label">Categoria</label>
        <div style={{ marginBottom: 12 }}>
          {EXPENSE_CATEGORIES.map((cat) => (
            <label key={cat} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
              <input
                type="radio"
                checked={category === cat}
                onChange={() => setCategory(cat)}
                style={{ width: 'auto' }}
              />
              {EXPENSE_CATEGORY_LABEL[cat]}
            </label>
          ))}
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <input
            type="checkbox"
            checked={isPurchased}
            onChange={(e) => setIsPurchased(e.target.checked)}
            style={{ width: 'auto' }}
          />
          Já comprado / pago
        </label>
        <div className="dialog-actions">
          <button className="btn btn-outline" onClick={onClose}>
            Cancelar
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              if (title.trim()) {
                onSave({
                  title: title.trim(),
                  amount: parseFloat(amountText.replace(',', '.')) || 0,
                  category,
                  isPurchased,
                })
              }
            }}
          >
            Salvar
          </button>
        </div>
      </div>
    </div>
  )
}
