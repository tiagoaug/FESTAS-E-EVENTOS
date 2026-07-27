import { useRef, useState } from 'react'
import { Check, CheckCheck, Copy, Eye, ImageDown, Settings2, Share2, Timer, Users, X } from 'lucide-react'
import { calculateParticipantTarget } from '../store/usePartyStore'
import { FALLBACK_CATEGORY_COLOR, FALLBACK_CATEGORY_LABEL } from '../types'
import type { CategoryEntity, EventEntity, ExpenseEntity, FinancialSummary, ParticipantEntity } from '../types'
import { formatCurrency } from '../utils/format'
import {
  buildShareSummaryText,
  daysUntilEvent,
  DEFAULT_SHARE_OPTIONS,
  FULL_PARTY_DATA_OPTIONS,
  groupParticipants,
  splitExpensesByPayment,
  splitParticipantsByAttendance,
  splitParticipantsByPayment,
} from '../utils/shareSummary'
import type { ShareOptions } from '../utils/shareSummary'
import { captureElementAsJpgBlob, shareOrDownloadJpg } from '../utils/exportImage'
import AccordionSection from './AccordionSection'

interface ShareSummaryDialogProps {
  event: EventEntity
  participants: ParticipantEntity[]
  expenses: ExpenseEntity[]
  categories: CategoryEntity[]
  summary: FinancialSummary
  onClose: () => void
}

const SHARE_OPTIONS_STORAGE_KEY = 'shareOptions'

function loadShareOptions(): ShareOptions {
  try {
    const raw = localStorage.getItem(SHARE_OPTIONS_STORAGE_KEY)
    if (!raw) return DEFAULT_SHARE_OPTIONS
    return { ...DEFAULT_SHARE_OPTIONS, ...(JSON.parse(raw) as Partial<ShareOptions>) }
  } catch {
    return DEFAULT_SHARE_OPTIONS
  }
}

function ListSection({ title, count, children }: { title: string; count: number; children: React.ReactNode }) {
  return (
    <div style={{ marginBottom: 14 }}>
      <p style={{ fontWeight: 700, fontSize: '0.82rem', margin: '0 0 6px' }}>
        {title} ({count})
      </p>
      <div
        style={{
          maxHeight: 140,
          overflowY: 'auto',
          background: 'var(--surface-variant)',
          borderRadius: 12,
          padding: count > 0 ? '8px 12px' : '12px',
        }}
      >
        {children}
      </div>
    </div>
  )
}

function CheckboxRow({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
}) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', fontSize: '0.85rem' }}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        style={{ width: 'auto' }}
      />
      {label}
    </label>
  )
}

export default function ShareSummaryDialog({
  event,
  participants,
  expenses,
  categories,
  summary,
  onClose,
}: ShareSummaryDialogProps) {
  const [copied, setCopied] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [sharing, setSharing] = useState(false)
  const [showLivePreview, setShowLivePreview] = useState(false)
  const [previewBlob, setPreviewBlob] = useState<Blob | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [options, setOptions] = useState<ShareOptions>(loadShareOptions)
  const exportRef = useRef<HTMLDivElement>(null)
  const { paid, pending } = splitParticipantsByPayment(event, participants)
  const { confirmed: confirmedGuests, awaiting: awaitingGuests } = splitParticipantsByAttendance(participants)
  const { paidExpenses, pendingExpenses } = splitExpensesByPayment(expenses)
  const groups = groupParticipants(participants, options.groupByFamily, options.alphabetical)
  const isOrganizerOnly = event.costShareMode === 'ORGANIZER_ONLY'
  const days = daysUntilEvent(event.eventDateMillis)

  const updateOptions = (patch: Partial<ShareOptions>) => {
    setOptions((prev) => {
      const next = { ...prev, ...patch }
      try {
        localStorage.setItem(SHARE_OPTIONS_STORAGE_KEY, JSON.stringify(next))
      } catch {
        // Best-effort persistence only.
      }
      return next
    })
  }

  const handleCopy = async () => {
    const text = buildShareSummaryText(event, participants, summary, options, expenses, categories)
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      alert('Não foi possível copiar automaticamente. Selecione e copie o texto manualmente.')
    }
  }

  const handlePrepareExport = async () => {
    if (!exportRef.current || exporting) return
    setExporting(true)
    try {
      const blob = await captureElementAsJpgBlob(exportRef.current)
      setPreviewBlob(blob)
      setPreviewUrl(URL.createObjectURL(blob))
    } catch {
      alert('Não foi possível gerar a prévia da imagem agora. Tente novamente.')
    } finally {
      setExporting(false)
    }
  }

  const closePreview = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
    setPreviewBlob(null)
    setPreviewUrl(null)
  }

  const handleConfirmExport = async () => {
    if (!previewBlob || sharing) return
    setSharing(true)
    try {
      await shareOrDownloadJpg(previewBlob, `${event.title.replace(/\s+/g, '_')}.jpg`, `Resumo: ${event.title}`)
      closePreview()
    } catch {
      alert('Não foi possível exportar a imagem agora. Tente novamente.')
    } finally {
      setSharing(false)
    }
  }

  const previewContent = (
    <>
      {options.showCountdown && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            background: 'linear-gradient(155deg, rgba(139,108,240,0.14), rgba(139,108,240,0.06))',
            borderRadius: 14,
            padding: '10px 14px',
            marginBottom: 14,
            fontSize: '0.82rem',
            fontWeight: 700,
            color: 'var(--primary)',
          }}
        >
          <Timer size={16} strokeWidth={2.4} />
          {days > 0
            ? `Faltam ${days} dia${days === 1 ? '' : 's'} para o evento`
            : days === 0
              ? 'É hoje!'
              : 'Evento já realizado'}
        </div>
      )}

      {options.showTotals && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 14 }}>
          <span className="badge" style={{ background: 'var(--surface-variant)' }}>
            {participants.length} Convidados
          </span>
          <span className="badge badge-success">
            {participants.filter((p) => p.confirmed).length} Confirmados
          </span>
          {!isOrganizerOnly && (
            <>
              <span className="badge badge-success">{paid.length} Pagaram</span>
              <span className="badge badge-danger">{pending.length} Faltam Pagar</span>
            </>
          )}
        </div>
      )}

      {options.showBudget && (
        <div className="grid-2" style={{ marginBottom: 14 }}>
          <div className="metric-tile">
            <div>
              <p className="metric-title">Orçamento</p>
              <p className="metric-value">{formatCurrency(summary.budget)}</p>
            </div>
          </div>
          <div className="metric-tile">
            <div>
              <p className="metric-title">Total Gasto</p>
              <p className="metric-value">{formatCurrency(summary.totalSpent)}</p>
            </div>
          </div>
        </div>
      )}

      {options.showGuestList && (
        <ListSection title="Convidados" count={participants.length}>
          {participants.length === 0 ? (
            <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
              Nenhum convidado cadastrado ainda.
            </span>
          ) : (
            groups.map((group) => (
              <div key={group.family ?? '__flat__'}>
                {group.family && (
                  <p
                    style={{
                      fontSize: '0.72rem',
                      fontWeight: 800,
                      color: 'var(--on-surface-variant)',
                      margin: '8px 0 2px',
                      textTransform: 'uppercase',
                      letterSpacing: 0.4,
                    }}
                  >
                    {group.family}
                  </p>
                )}
                {group.members.map((p) => (
                  <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0' }}>
                    {p.name}
                    {!group.family && p.familyGroup && (
                      <span style={{ color: 'var(--on-surface-variant)' }}> • {p.familyGroup}</span>
                    )}
                    {p.confirmed && <Check size={12} strokeWidth={3} style={{ marginLeft: 4, color: 'var(--success)' }} />}
                  </p>
                ))}
              </div>
            ))
          )}
        </ListSection>
      )}

      {options.showAttendanceList && (
        <>
          <ListSection title="Presença Confirmada" count={confirmedGuests.length}>
            {confirmedGuests.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Ninguém confirmou presença ainda.
              </span>
            ) : (
              confirmedGuests.map((p) => (
                <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--success)' }}>
                  {p.name}
                </p>
              ))
            )}
          </ListSection>

          <ListSection title="Aguardando Confirmação" count={awaitingGuests.length}>
            {awaitingGuests.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Todos já confirmaram presença!
              </span>
            ) : (
              awaitingGuests.map((p) => (
                <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--warning)' }}>
                  {p.name}
                </p>
              ))
            )}
          </ListSection>
        </>
      )}

      {!isOrganizerOnly && (
        <>
          <ListSection title="Já Pagaram" count={paid.length}>
            {paid.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Ninguém quitou o valor ainda.
              </span>
            ) : (
              paid.map((p) => (
                <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--success)' }}>
                  {p.name}: {formatCurrency(p.paidAmount)}
                </p>
              ))
            )}
          </ListSection>

          <ListSection title="Faltam Pagar" count={pending.length}>
            {pending.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Todo mundo está em dia!
              </span>
            ) : (
              pending.map((p) => {
                const target = calculateParticipantTarget(p, event, participants)
                const missing = Math.max(0, target - p.paidAmount)
                return (
                  <p key={p.id} style={{ fontSize: '0.8rem', margin: '4px 0', color: 'var(--danger)' }}>
                    {p.name}: falta {formatCurrency(missing)}
                  </p>
                )
              })
            )}
          </ListSection>
        </>
      )}

      {options.showExpenseItems && (
        <>
          <ListSection title="Itens Pagos" count={paidExpenses.length}>
            {paidExpenses.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Nenhum item pago ainda.
              </span>
            ) : (
              paidExpenses.map((e) => {
                const category = categories.find((c) => c.id === e.category)
                return (
                  <div
                    key={e.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 6,
                      fontSize: '0.8rem',
                      margin: '4px 0',
                    }}
                  >
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          background: category?.color ?? FALLBACK_CATEGORY_COLOR,
                          display: 'inline-block',
                          flexShrink: 0,
                        }}
                      />
                      {e.title}
                      <span style={{ color: 'var(--on-surface-variant)' }}>
                        ({category?.name ?? FALLBACK_CATEGORY_LABEL})
                      </span>
                    </span>
                    <strong style={{ color: 'var(--success)', flexShrink: 0 }}>{formatCurrency(e.amount)}</strong>
                  </div>
                )
              })
            )}
          </ListSection>

          <ListSection title="Itens a Pagar" count={pendingExpenses.length}>
            {pendingExpenses.length === 0 ? (
              <span style={{ fontSize: '0.78rem', color: 'var(--on-surface-variant)' }}>
                Nenhum item pendente!
              </span>
            ) : (
              pendingExpenses.map((e) => {
                const category = categories.find((c) => c.id === e.category)
                return (
                  <div
                    key={e.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 6,
                      fontSize: '0.8rem',
                      margin: '4px 0',
                    }}
                  >
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span
                        style={{
                          width: 8,
                          height: 8,
                          borderRadius: '50%',
                          background: category?.color ?? FALLBACK_CATEGORY_COLOR,
                          display: 'inline-block',
                          flexShrink: 0,
                        }}
                      />
                      {e.title}
                      <span style={{ color: 'var(--on-surface-variant)' }}>
                        ({category?.name ?? FALLBACK_CATEGORY_LABEL})
                      </span>
                    </span>
                    <strong style={{ color: 'var(--danger)', flexShrink: 0 }}>{formatCurrency(e.amount)}</strong>
                  </div>
                )
              })
            )}
          </ListSection>
        </>
      )}

      {options.notes.trim() && (
        <div style={{ marginBottom: 4 }}>
          <p style={{ fontWeight: 700, fontSize: '0.82rem', margin: '0 0 6px' }}>Observações</p>
          <div
            style={{
              background: 'var(--surface-variant)',
              borderRadius: 12,
              padding: '8px 12px',
              fontSize: '0.8rem',
              whiteSpace: 'pre-wrap',
            }}
          >
            {options.notes.trim()}
          </div>
        </div>
      )}
    </>
  )

  const pageContent = (
    <div
      style={{
        background: 'linear-gradient(165deg, #ffffff 0%, #f3f0ff 100%)',
        borderRadius: 22,
        padding: 18,
        border: '1px solid var(--outline-variant)',
        boxShadow: '0 16px 32px -16px rgba(90, 60, 160, 0.35)',
        color: '#1a1329',
      }}
    >
      <h3
        style={{
          margin: '0 0 4px',
          fontSize: '1.05rem',
          fontWeight: 800,
          color: '#1a1329',
          display: 'flex',
          alignItems: 'center',
          gap: 6,
        }}
      >
        🎉 {event.title}
      </h3>
      {event.location && (
        <p style={{ margin: '0 0 12px', fontSize: '0.78rem', color: '#6b6280' }}>{event.location}</p>
      )}
      {previewContent}
    </div>
  )

  return (
    <>
      <div className="overlay" onClick={onClose}>
        <div className="dialog" onClick={(e) => e.stopPropagation()}>
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Users size={18} strokeWidth={2.4} color="var(--primary)" />
            Centro de Compartilhamento
          </h2>

          <AccordionSection
            title="Configurações de Compartilhamento"
            icon={<Settings2 size={16} strokeWidth={2.4} color="var(--primary)" />}
            defaultExpanded={false}
          >
            <button
              type="button"
              className="btn btn-primary btn-block"
              style={{ marginBottom: 12 }}
              onClick={() => updateOptions(FULL_PARTY_DATA_OPTIONS)}
            >
              <CheckCheck size={16} strokeWidth={2.4} /> Compartilhar Dados Completos da Festa
            </button>

            <CheckboxRow
              checked={options.groupByFamily}
              onChange={(v) => updateOptions({ groupByFamily: v })}
              label="Separar convidados por família"
            />
            <CheckboxRow
              checked={options.alphabetical}
              onChange={(v) => updateOptions({ alphabetical: v })}
              label="Ordem alfabética"
            />
            <CheckboxRow
              checked={options.showTotals}
              onChange={(v) => updateOptions({ showTotals: v })}
              label="Mostrar totais (convidados, confirmados, pagos)"
            />
            <CheckboxRow
              checked={options.showGuestList}
              onChange={(v) => updateOptions({ showGuestList: v })}
              label="Mostrar lista de convidados"
            />
            <CheckboxRow
              checked={options.showAttendanceList}
              onChange={(v) => updateOptions({ showAttendanceList: v })}
              label="Mostrar lista de presença (confirmados / aguardando)"
            />
            <CheckboxRow
              checked={options.showBudget}
              onChange={(v) => updateOptions({ showBudget: v })}
              label="Mostrar orçamento e gastos"
            />
            <CheckboxRow
              checked={options.showExpenseItems}
              onChange={(v) => updateOptions({ showExpenseItems: v })}
              label="Mostrar itens pagos e itens a pagar"
            />
            <CheckboxRow
              checked={options.showCountdown}
              onChange={(v) => updateOptions({ showCountdown: v })}
              label="Mostrar contagem regressiva"
            />
            <div className="field" style={{ marginTop: 10, marginBottom: 0 }}>
              <label className="field-label">Observações</label>
              <textarea
                value={options.notes}
                onChange={(e) => updateOptions({ notes: e.target.value })}
                placeholder="Ex: Levar prato para compartilhar, traje esporte fino..."
                rows={3}
              />
            </div>
          </AccordionSection>

          <button
            type="button"
            className="btn btn-outline btn-block"
            style={{ marginTop: 14 }}
            onClick={() => setShowLivePreview(true)}
          >
            <Eye size={16} strokeWidth={2.3} /> Visualizar Prévia
          </button>

          {!showLivePreview && (
            <div style={{ position: 'absolute', left: -9999, top: -9999, width: 380 }} aria-hidden>
              <div ref={exportRef}>{pageContent}</div>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 14 }}>
            <button className="btn btn-outline btn-block" onClick={handleCopy}>
              {copied ? <Check size={16} strokeWidth={2.4} /> : <Copy size={16} strokeWidth={2.3} />}
              {copied ? 'Copiado!' : 'Copiar como Texto'}
            </button>
            <button className="btn btn-primary btn-block" onClick={handlePrepareExport} disabled={exporting}>
              <ImageDown size={16} strokeWidth={2.3} /> {exporting ? 'Gerando...' : 'Exportar JPG'}
            </button>
          </div>

          <div className="dialog-actions">
            <button className="btn btn-outline btn-block" onClick={onClose}>
              <X size={16} strokeWidth={2.3} /> Fechar
            </button>
          </div>
        </div>
      </div>

      {showLivePreview && (
        <div className="overlay" onClick={() => setShowLivePreview(false)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Eye size={18} strokeWidth={2.4} color="var(--primary)" />
              Prévia da Página
            </h2>

            <div ref={exportRef}>{pageContent}</div>

            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={() => setShowLivePreview(false)}>
                <X size={16} strokeWidth={2.3} /> Fechar Prévia
              </button>
              <button className="btn btn-primary" onClick={handlePrepareExport} disabled={exporting}>
                <ImageDown size={16} strokeWidth={2.3} /> {exporting ? 'Gerando...' : 'Exportar JPG'}
              </button>
            </div>
          </div>
        </div>
      )}

      {previewUrl && (
        <div className="overlay" onClick={closePreview}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Prévia do que será compartilhado</h2>
            <img
              src={previewUrl}
              alt="Prévia do resumo de compartilhamento"
              style={{ width: '100%', borderRadius: 16, border: '1px solid var(--outline-variant)', display: 'block' }}
            />
            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={closePreview}>
                <X size={16} strokeWidth={2.3} /> Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleConfirmExport} disabled={sharing}>
                <Share2 size={16} strokeWidth={2.3} /> {sharing ? 'Enviando...' : 'Compartilhar / Salvar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
