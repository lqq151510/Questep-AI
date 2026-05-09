"use client";

import { type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export function EmptyState({ icon: Icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div className={cn("flex flex-col items-center justify-center py-16 px-4 text-center", className)}>
      <div className="relative">
        <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-[var(--blue)] to-[var(--cyan)] opacity-10 blur-xl" />
        <div className="relative flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--blue-soft)] to-[var(--cyan-soft)] text-[var(--blue)]">
          <Icon size={28} strokeWidth={1.5} />
        </div>
      </div>
      <h3 className="mt-5 text-base font-semibold text-[var(--ink)]">{title}</h3>
      {description && (
        <p className="mt-2 max-w-xs text-sm text-[var(--muted)]">{description}</p>
      )}
      {action && (
        <button
          type="button"
          onClick={action.onClick}
          className="btn btn-accent mt-5"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
