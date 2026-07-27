import { NavLink } from 'react-router-dom'
import { Home, Users, ShoppingCart, Wallet, MessageCircle } from 'lucide-react'

const items = [
  { to: '/', icon: Home, label: 'Início' },
  { to: '/participants', icon: Users, label: 'Convidados' },
  { to: '/expenses', icon: ShoppingCart, label: 'Gastos' },
  { to: '/payments', icon: Wallet, label: 'Receber' },
  { to: '/invitations', icon: MessageCircle, label: 'Convites' },
]

export default function BottomNav() {
  return (
    <nav className="bottom-nav">
      {items.map((item) => {
        const Icon = item.icon
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) => (isActive ? 'active' : '')}
          >
            <span className="nav-icon">
              <Icon size={20} strokeWidth={2.3} />
            </span>
            <span>{item.label}</span>
          </NavLink>
        )
      })}
    </nav>
  )
}
