import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Calendar,
  Check,
  ChevronRight,
  ListFilter,
  PackagePlus,
  ShoppingBag,
  Pencil,
  Save,
  Trash2,
  PartyPopper,
  Plus,
} from 'lucide-react'
import type { PartyStore } from '../store/usePartyStore'
import {
  FALLBACK_CATEGORY_COLOR,
  FALLBACK_CATEGORY_LABEL,
  type CategoryEntity,
  type ExpenseEntity,
} from '../types'
import { formatCurrency, formatDate } from '../utils/format'
import BudgetVsSpentChart from '../components/BudgetVsSpentChart'
import AccordionSection from '../components/AccordionSection'

function findCategory(categories: CategoryEntity[], categoryId: string): CategoryEntity {
  return (
    categories.find((c) => c.id === categoryId) ?? {
      id: categoryId,
      name: FALLBACK_CATEGORY_LABEL,
      color: FALLBACK_CATEGORY_COLOR,
    }
  )
}

export default function Expenses({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const {
    activeEvent,
    expenses,
    categories,
    addExpense,
    updateExpense,
    toggleExpensePurchased,
    toggleExpensePaid,
    deleteExpense,
  } = store

  const [categoryFilter, setCategoryFilter] = useState<string | null>(null)
  const [showAdd, setShowAdd] = useState(false)
  const [editing, setEditing] = useState<ExpenseEntity | null>(null)
  const [detailExpenseId, setDetailExpenseId] = useState<string | null>(null)

  const filtered = useMemo(
    () => (categoryFilter ? expenses.filter((e) => e.category === categoryFilter) : expenses),
    [expenses, categoryFilter],
  )

  const detailExpense = expenses.find((e) => e.id === detailExpenseId) ?? null

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
        <button className="icon-btn" onClick={() => setShowAdd(true)} disabled={categories.length === 0}>
          <PackagePlus size={19} strokeWidth={2.2} />
        </button>
      </div>

      <div className="app-content">
        <div className="page">
          <BudgetVsSpentChart budget={activeEvent.budget} expenses={expenses} categories={categories} />

          {categories.length === 0 ? (
            <div className="empty-state">
              <p>
                Nenhuma categoria cadastrada. Crie categorias de gastos em Configurações antes de adicionar itens.
              </p>
            </div>
          ) : (
            <>
              <AccordionSection
                title="Filtrar por Categoria"
                icon={<ListFilter size={17} strokeWidth={2.3} color="var(--primary)" />}
                defaultExpanded={false}
              >
                <div className="chip-row">
                  <button className={`chip ${categoryFilter === null ? 'selected' : ''}`} onClick={() => setCategoryFilter(null)}>
                    Todas
                  </button>
                  {categories.map((cat) => (
                    <button
                      key={cat.id}
                      className={`chip ${categoryFilter === cat.id ? 'selected' : ''}`}
                      onClick={() => setCategoryFilter(cat.id)}
                    >
                      {cat.name}
                    </button>
                  ))}
                </div>
              </AccordionSection>

              {filtered.length === 0 ? (
                <div className="empty-state">
                  <span className="emoji">
                    <ShoppingBag size={28} strokeWidth={2} />
                  </span>
                  <p>Nenhum item de gasto cadastrado nesta categoria.</p>
                </div>
              ) : (
                filtered.map((exp) => (
                  <div
                    key={exp.id}
                    className="card"
                    style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 10, padding: 14, cursor: 'pointer' }}
                    onClick={() => setDetailExpenseId(exp.id)}
                  >
                    {exp.notes && <span className="pulse-dot" />}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <strong style={{ fontSize: '0.9rem', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {exp.title}
                      </strong>
                      <div style={{ display: 'flex', gap: 6, marginTop: 4 }}>
                        <span className={`badge ${exp.isPurchased ? 'badge-success' : 'badge-warning'}`}>
                          {exp.isPurchased ? 'Comprado' : 'A Comprar'}
                        </span>
                        <span className={`badge ${exp.isPaid ? 'badge-success' : 'badge-danger'}`}>
                          {exp.isPaid ? 'Pago' : 'A Pagar'}
                        </span>
                      </div>
                    </div>
                    <strong style={{ flexShrink: 0 }}>{formatCurrency(exp.amount)}</strong>
                    <ChevronRight size={18} strokeWidth={2.2} color="var(--on-surface-variant)" style={{ flexShrink: 0 }} />
                  </div>
                ))
              )}
            </>
          )}
        </div>
      </div>

      <button className="fab" onClick={() => setShowAdd(true)} disabled={categories.length === 0}>
        <Plus size={24} strokeWidth={2.6} />
      </button>

      {(showAdd || editing) && categories.length > 0 && (
        <ExpenseDialog
          expense={editing}
          categories={categories}
          onClose={() => {
            setShowAdd(false)
            setEditing(null)
          }}
          onSave={(data) => {
            if (editing) {
              updateExpense({ ...editing, ...data })
            } else {
              addExpense({ ...data, eventId: activeEvent.id, dateAddedMillis: Date.now(), notes: '' })
            }
            setShowAdd(false)
            setEditing(null)
          }}
        />
      )}

      {detailExpense && (
        <ExpenseDetailDialog
          expense={detailExpense}
          category={findCategory(categories, detailExpense.category)}
          onClose={() => setDetailExpenseId(null)}
          onTogglePurchased={(checked) => toggleExpensePurchased(detailExpense.id, checked)}
          onTogglePaid={(checked) => toggleExpensePaid(detailExpense.id, checked)}
          onSaveNotes={(notes) => updateExpense({ ...detailExpense, notes })}
          onEdit={() => {
            setEditing(detailExpense)
            setDetailExpenseId(null)
          }}
          onDelete={() => {
            deleteExpense(detailExpense)
            setDetailExpenseId(null)
          }}
        />
      )}
    </>
  )
}

function ExpenseDetailDialog({
  expense,
  category,
  onClose,
  onTogglePurchased,
  onTogglePaid,
  onSaveNotes,
  onEdit,
  onDelete,
}: {
  expense: ExpenseEntity
  category: CategoryEntity
  onClose: () => void
  onTogglePurchased: (checked: boolean) => void
  onTogglePaid: (checked: boolean) => void
  onSaveNotes: (notes: string) => void
  onEdit: () => void
  onDelete: () => void
}) {
  const [notes, setNotes] = useState(expense.notes)
  const [saved, setSaved] = useState(false)

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>{expense.title}</h2>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
          <span className="badge" style={{ background: category.color + '33', color: category.color }}>
            {category.name}
          </span>
          <span
            style={{
              fontSize: '0.75rem',
              color: 'var(--on-surface-variant)',
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <Calendar size={12} /> {formatDate(expense.dateAddedMillis)}
          </span>
        </div>

        <p style={{ fontSize: '1.4rem', fontWeight: 800, margin: '0 0 14px' }}>{formatCurrency(expense.amount)}</p>

        <label className="field-label">Status de Compra</label>
        <div className="chip-row" style={{ marginBottom: 14 }}>
          <button
            className={`chip ${!expense.isPurchased ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => onTogglePurchased(false)}
          >
            A Comprar
          </button>
          <button
            className={`chip ${expense.isPurchased ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => onTogglePurchased(true)}
          >
            Comprado
          </button>
        </div>

        <label className="field-label">Status de Pagamento</label>
        <div className="chip-row" style={{ marginBottom: 16 }}>
          <button
            className={`chip ${!expense.isPaid ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => onTogglePaid(false)}
          >
            A Pagar
          </button>
          <button
            className={`chip ${expense.isPaid ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => onTogglePaid(true)}
          >
            Pago
          </button>
        </div>

        <div className="field">
          <label className="field-label">Observação</label>
          <textarea
            rows={3}
            value={notes}
            onChange={(e) => {
              setNotes(e.target.value)
              setSaved(false)
            }}
            placeholder="Ex: comprar na promoção, aguardando orçamento do fornecedor..."
          />
        </div>
        <button
          className="btn btn-outline btn-block"
          style={{ marginBottom: 16 }}
          onClick={() => {
            onSaveNotes(notes)
            setSaved(true)
          }}
        >
          {saved ? <Check size={15} strokeWidth={2.4} /> : <Save size={15} strokeWidth={2.3} />}
          {saved ? 'Observação Salva' : 'Salvar Observação'}
        </button>

        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onEdit}>
            <Pencil size={15} strokeWidth={2.2} /> Editar
          </button>
          <button className="btn" style={{ flex: 1, background: 'var(--danger)', color: 'white' }} onClick={onDelete}>
            <Trash2 size={15} strokeWidth={2.2} /> Excluir
          </button>
        </div>

        <div className="dialog-actions">
          <button className="btn btn-outline btn-block" onClick={onClose}>
            Fechar
          </button>
        </div>
      </div>
    </div>
  )
}

function ExpenseDialog({
  expense,
  categories,
  onClose,
  onSave,
}: {
  expense: ExpenseEntity | null
  categories: CategoryEntity[]
  onClose: () => void
  onSave: (data: {
    title: string
    amount: number
    category: string
    isPurchased: boolean
    isPaid: boolean
  }) => void
}) {
  const [title, setTitle] = useState(expense?.title ?? '')
  const [amountText, setAmountText] = useState(expense?.amount?.toString() ?? '')
  const [category, setCategory] = useState<string>(expense?.category ?? categories[0].id)
  const [isPurchased, setIsPurchased] = useState(expense?.isPurchased ?? false)
  const [isPaid, setIsPaid] = useState(expense?.isPaid ?? false)

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
          {categories.map((cat) => (
            <label key={cat.id} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
              <input
                type="radio"
                checked={category === cat.id}
                onChange={() => setCategory(cat.id)}
                style={{ width: 'auto' }}
              />
              <span
                style={{
                  width: 10,
                  height: 10,
                  borderRadius: '50%',
                  background: cat.color,
                  display: 'inline-block',
                }}
              />
              {cat.name}
            </label>
          ))}
        </div>
        <label className="field-label">Status de Compra</label>
        <div className="chip-row" style={{ marginBottom: 14 }}>
          <button
            type="button"
            className={`chip ${!isPurchased ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setIsPurchased(false)}
          >
            A Comprar
          </button>
          <button
            type="button"
            className={`chip ${isPurchased ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setIsPurchased(true)}
          >
            Comprado
          </button>
        </div>
        <label className="field-label">Status de Pagamento</label>
        <div className="chip-row" style={{ marginBottom: 16 }}>
          <button
            type="button"
            className={`chip ${!isPaid ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setIsPaid(false)}
          >
            A Pagar
          </button>
          <button
            type="button"
            className={`chip ${isPaid ? 'selected' : ''}`}
            style={{ flex: 1, textAlign: 'center' }}
            onClick={() => setIsPaid(true)}
          >
            Pago
          </button>
        </div>
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
                  isPaid,
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
