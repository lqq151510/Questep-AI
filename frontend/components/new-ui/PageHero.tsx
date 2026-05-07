import type { ReactNode } from "react";

type PageHeroProps = {
  title: string;
  description: string;
  kicker?: string;
  actions?: ReactNode;
};

export function PageHero({ title, description, kicker, actions }: PageHeroProps) {
  return (
    <section className="page-hero">
      {kicker ? <p className="hero-kicker">{kicker}</p> : null}
      <h1>{title}</h1>
      <p className="hero-description">{description}</p>
      {actions ? <div className="hero-actions">{actions}</div> : null}
    </section>
  );
}
