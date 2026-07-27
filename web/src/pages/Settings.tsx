import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, Check, MessageCircle, Palette, Pencil, Plus, Tag, Trash2, Type } from 'lucide-react'
import { FONTS, THEMES, useSettings } from '../context/settingsContextValue'
import type { PartyStore } from '../store/usePartyStore'
import { CATEGORY_COLOR_SWATCHES, type CategoryEntity } from '../types'
import AccordionSection from '../components/AccordionSection'

export default function Settings({ store }: { store: PartyStore }) {
  const navigate = useNavigate()
  const { theme, setTheme, font, setFont, useWhatsApp, setUseWhatsApp } = useSettings()
  const { categories, addCategory, updateCategory, deleteCategory } = store

  const [showCategoryDialog, setShowCategoryDialog] = useState(false)
  const [editingCategory, setEditingCategory] = useState<CategoryEntity | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CategoryEntity | null>(null)

  return (
    <>
      <div className="top-bar">
        <button className="icon-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={19} strokeWidth={2.3} />
        </button>
        <h1>Configurações</h1>
      </div>

      <div className="app-content">
        <div className="page">
          <AccordionSection
            title="Preferências Gerais"
            icon={<MessageCircle size={17} strokeWidth={2.3} color="var(--whatsapp)" />}
          >
            <label style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 0' }}>
              <input
                type="checkbox"
                checked={useWhatsApp}
                onChange={(e) => setUseWhatsApp(e.target.checked)}
                style={{ width: 'auto' }}
              />
              <span>
                <strong style={{ display: 'block', fontSize: '0.88rem' }}>Usar WhatsApp</strong>
                <span style={{ fontSize: '0.75rem', color: 'var(--on-surface-variant)' }}>
                  Mostra os botões e atalhos de WhatsApp pelo app. Se desmarcado, eles ficam ocultos e o espaço é
                  reduzido.
                </span>
              </span>
            </label>
          </AccordionSection>

          <AccordionSection title="Tema de Cores" icon={<Palette size={17} strokeWidth={2.3} color="var(--primary)" />}>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(2, 1fr)',
                gap: 10,
              }}
            >
              {THEMES.map((t) => {
                const selected = theme === t.id
                return (
                  <button
                    key={t.id}
                    onClick={() => setTheme(t.id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      padding: '12px 14px',
                      borderRadius: 16,
                      border: selected ? `1.5px solid ${t.swatch}` : '1px solid var(--outline-variant)',
                      background: selected ? `${t.swatch}1a` : 'rgba(255,255,255,0.5)',
                      cursor: 'pointer',
                      textAlign: 'left',
                    }}
                  >
                    <span
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        background: t.swatch,
                        flexShrink: 0,
                        boxShadow: `0 3px 8px -2px ${t.swatch}88`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      {selected && <Check size={15} strokeWidth={3} color="white" />}
                    </span>
                    <span style={{ fontSize: '0.85rem', fontWeight: 700 }}>{t.label}</span>
                  </button>
                )
              })}
            </div>
          </AccordionSection>

          <AccordionSection title="Fonte do Aplicativo" icon={<Type size={17} strokeWidth={2.3} color="var(--primary)" />}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {FONTS.map((f) => {
                const selected = font === f.id
                return (
                  <button
                    key={f.id}
                    onClick={() => setFont(f.id)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '12px 14px',
                      borderRadius: 14,
                      border: selected ? '1.5px solid var(--primary)' : '1px solid var(--outline-variant)',
                      background: selected
                        ? 'linear-gradient(155deg, rgba(139,108,240,0.14), rgba(139,108,240,0.06))'
                        : 'rgba(255,255,255,0.5)',
                      cursor: 'pointer',
                      textAlign: 'left',
                    }}
                  >
                    <span style={{ fontFamily: f.stack }}>
                      <span style={{ fontSize: '1.1rem', fontWeight: 700, display: 'block' }}>
                        Aa Festas & Eventos
                      </span>
                      <span style={{ fontSize: '0.72rem', color: 'var(--on-surface-variant)' }}>{f.label}</span>
                    </span>
                    {selected && (
                      <span
                        style={{
                          width: 22,
                          height: 22,
                          borderRadius: '50%',
                          background: 'var(--primary)',
                          color: 'white',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          flexShrink: 0,
                        }}
                      >
                        <Check size={13} strokeWidth={3} />
                      </span>
                    )}
                  </button>
                )
              })}
            </div>
          </AccordionSection>

          <AccordionSection
            title="Categorias de Gastos"
            icon={<Tag size={17} strokeWidth={2.3} color="var(--primary)" />}
            headerAction={
              <button className="icon-btn" onClick={() => setShowCategoryDialog(true)}>
                <Plus size={18} strokeWidth={2.3} />
              </button>
            }
          >
            {categories.length === 0 ? (
              <p style={{ fontSize: '0.8rem', color: 'var(--on-surface-variant)' }}>
                Nenhuma categoria cadastrada ainda.
              </p>
            ) : (
              categories.map((cat) => (
                <div key={cat.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0' }}>
                  <span
                    style={{ width: 20, height: 20, borderRadius: '50%', background: cat.color, flexShrink: 0 }}
                  />
                  <span style={{ flex: 1, fontSize: '0.85rem', fontWeight: 600 }}>{cat.name}</span>
                  <button className="icon-btn" onClick={() => setEditingCategory(cat)}>
                    <Pencil size={15} strokeWidth={2.2} />
                  </button>
                  <button className="icon-btn" style={{ color: 'var(--danger)' }} onClick={() => setDeleteTarget(cat)}>
                    <Trash2 size={15} strokeWidth={2.2} />
                  </button>
                </div>
              ))
            )}
          </AccordionSection>
        </div>
      </div>

      {(showCategoryDialog || editingCategory) && (
        <CategoryDialog
          category={editingCategory}
          onClose={() => {
            setShowCategoryDialog(false)
            setEditingCategory(null)
          }}
          onSave={(data) => {
            if (editingCategory) {
              updateCategory({ ...editingCategory, ...data })
            } else {
              addCategory(data)
            }
            setShowCategoryDialog(false)
            setEditingCategory(null)
          }}
        />
      )}

      {deleteTarget && (
        <div className="overlay" onClick={() => setDeleteTarget(null)}>
          <div className="dialog" onClick={(e) => e.stopPropagation()}>
            <h2>Excluir Categoria?</h2>
            <p>
              Tem certeza que deseja excluir '{deleteTarget.name}'? Gastos já cadastrados nela continuam existindo,
              apenas sem uma categoria válida.
            </p>
            <div className="dialog-actions">
              <button className="btn btn-outline" onClick={() => setDeleteTarget(null)}>
                Cancelar
              </button>
              <button
                className="btn"
                style={{ background: 'var(--danger)', color: 'white' }}
                onClick={() => {
                  deleteCategory(deleteTarget)
                  setDeleteTarget(null)
                }}
              >
                Excluir
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function CategoryDialog({
  category,
  onClose,
  onSave,
}: {
  category: CategoryEntity | null
  onClose: () => void
  onSave: (data: { name: string; color: string }) => void
}) {
  const [name, setName] = useState(category?.name ?? '')
  const [color, setColor] = useState(category?.color ?? CATEGORY_COLOR_SWATCHES[0])

  return (
    <div className="overlay" onClick={onClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h2>{category ? 'Editar Categoria' : 'Nova Categoria'}</h2>
        <div className="field">
          <label className="field-label">Nome da Categoria</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Ex: Fotografia, Lembrancinhas"
          />
        </div>
        <label className="field-label">Cor</label>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 16 }}>
          {CATEGORY_COLOR_SWATCHES.map((swatch) => (
            <button
              key={swatch}
              onClick={() => setColor(swatch)}
              style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                background: swatch,
                border: color === swatch ? '3px solid var(--on-surface)' : '2px solid transparent',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {color === swatch && <Check size={14} strokeWidth={3} color="white" />}
            </button>
          ))}
        </div>
        <div className="dialog-actions">
          <button className="btn btn-outline" onClick={onClose}>
            Cancelar
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              if (name.trim()) onSave({ name: name.trim(), color })
            }}
          >
            Salvar
          </button>
        </div>
      </div>
    </div>
  )
}
