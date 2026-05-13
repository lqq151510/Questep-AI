"use client";

import { Component } from "react";
import type { ReactNode } from "react";

type ErrorBoundaryProps = {
  children: ReactNode;
  fallback?: ReactNode;
};

type ErrorBoundaryState = {
  hasError: boolean;
  error: Error | null;
};

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      return (
        <div className="flex min-h-[240px] items-center justify-center rounded-xl border border-[var(--red)]/30 bg-[var(--red-soft)] p-6">
          <div className="text-center">
            <p className="text-lg font-semibold text-[var(--red)]">页面出现异常</p>
            <p className="mt-2 text-sm text-[var(--ink)]/80">
              {this.state.error?.message || "发生了未预期错误，请稍后重试"}
            </p>
            <button
              className="btn btn-ghost mt-4"
              onClick={() => this.setState({ hasError: false, error: null })}
            >
              重试
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
