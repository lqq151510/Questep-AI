"use client";

import { AlertTriangle, Loader2 } from "lucide-react";

type PageLoadingStateProps = {
  text?: string;
};

type PageErrorStateProps = {
  title?: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function PageLoadingState({ text = "加载中..." }: PageLoadingStateProps) {
  return (
    <div className="panel">
      <div className="flex items-center justify-center gap-2 py-8 text-[var(--muted)]">
        <Loader2 size={18} className="animate-spin text-[var(--blue)]" />
        <span>{text}</span>
      </div>
    </div>
  );
}

export function PageErrorState({
  title = "加载失败",
  message,
  actionLabel = "重试",
  onAction
}: PageErrorStateProps) {
  return (
    <div className="panel">
      <div className="rounded-xl border border-[var(--red)]/30 bg-[var(--red-soft)] p-4">
        <div className="mb-2 flex items-center gap-2 text-[var(--red)]">
          <AlertTriangle size={16} />
          <span className="font-semibold">{title}</span>
        </div>
        <p className="text-sm text-[var(--ink)]/85">{message}</p>
        {onAction && (
          <button type="button" className="btn btn-ghost mt-3" onClick={onAction}>
            {actionLabel}
          </button>
        )}
      </div>
    </div>
  );
}

export function InlineErrorState({ message }: { message: string }) {
  return (
    <div className="mb-4 rounded-lg border border-[var(--red)]/30 bg-[var(--red-soft)] p-3 text-sm text-[var(--red)]">
      {message}
    </div>
  );
}
