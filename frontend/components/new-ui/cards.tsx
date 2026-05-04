import type { ReactNode } from "react";
import Link from "next/link";

type FeatureCardProps = {
  href: string;
  icon: ReactNode;
  title: string;
  description: string;
  tag: string;
};

export function FeatureCard({ href, icon, title, description, tag }: FeatureCardProps) {
  return (
    <Link href={href} className="feature-card">
      <span className="feature-icon">{icon}</span>
      <h3>{title}</h3>
      <p>{description}</p>
      <span className="feature-tag">{tag}</span>
    </Link>
  );
}

type MetricCardProps = {
  label: string;
  value: ReactNode;
  hint?: string;
};

export function MetricCard({ label, value, hint }: MetricCardProps) {
  return (
    <article className="metric-card">
      <p className="metric-label">{label}</p>
      <p className="metric-value">{value}</p>
      {hint ? <p className="metric-hint">{hint}</p> : null}
    </article>
  );
}
