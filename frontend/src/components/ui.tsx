import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react';

export function Button({
  variant = 'primary',
  className = '',
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
}) {
  return (
    <button className={`btn btn--${variant} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export function Input({
  label,
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label?: string }) {
  return (
    <label className={`field ${className}`.trim()}>
      {label && <span className="field__label">{label}</span>}
      <input className="field__input" {...props} />
    </label>
  );
}

export function Select({
  label,
  className = '',
  children,
  ...props
}: React.SelectHTMLAttributes<HTMLSelectElement> & { label?: string }) {
  return (
    <label className={`field ${className}`.trim()}>
      {label && <span className="field__label">{label}</span>}
      <select className="field__input" {...props}>
        {children}
      </select>
    </label>
  );
}

export function Card({
  title,
  subtitle,
  action,
  children,
  className = '',
}: {
  title?: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={['card', className].filter(Boolean).join(' ')}>
      {(title || action) && (
        <header className="card__header">
          <div>
            {title && <h2 className="card__title">{title}</h2>}
            {subtitle && <p className="card__subtitle">{subtitle}</p>}
          </div>
          {action}
        </header>
      )}
      {children}
    </section>
  );
}

export function EmptyState({ icon, title, description }: {
  icon: ReactNode;
  title: string;
  description: string;
}) {
  return (
    <div className="empty">
      <div className="empty__icon">{icon}</div>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}

export function Spinner() {
  return <div className="spinner" aria-label="Cargando" role="status" />;
}

export function Alert({ variant = 'error', children }: {
  variant?: 'error' | 'success' | 'info';
  children: ReactNode;
}) {
  return <div className={`alert alert--${variant}`}>{children}</div>;
}

export function Badge({
  variant = 'default',
  children,
}: {
  variant?: 'default' | 'success';
  children: ReactNode;
}) {
  return <span className={`badge badge--${variant}`}>{children}</span>;
}
