"use client";

import type { RemoteState } from "@/types/dashboard";

type LoadingOverlayProps = {
  state: RemoteState;
  loadingText?: string;
  errorText?: string;
};

export function LoadingOverlay({
  state,
  loadingText = "Loading...",
  errorText = "Connection lost. Please try again."
}: LoadingOverlayProps) {
  if (state === "idle" || state === "online") return null;

  if (state === "syncing") {
    return (
      <div className="flex items-center gap-2 rounded-lg bg-blue-500/10 px-3 py-2 text-sm text-blue-300">
        <svg
          className="h-4 w-4 animate-spin"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
        <span>{loadingText}</span>
      </div>
    );
  }

  if (state === "offline") {
    return (
      <div className="flex items-center gap-2 rounded-lg bg-red-500/10 px-3 py-2 text-sm text-red-300">
        <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <span>{errorText}</span>
      </div>
    );
  }

  return null;
}
