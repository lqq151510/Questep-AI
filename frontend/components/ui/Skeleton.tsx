"use client";

import { cn } from "@/lib/utils";

interface SkeletonProps {
  className?: string;
  variant?: "text" | "card" | "circle" | "rect";
  lines?: number;
}

export function Skeleton({ className, variant = "text", lines = 1 }: SkeletonProps) {
  if (variant === "text" && lines > 1) {
    return (
      <div className={cn("grid gap-2", className)}>
        {Array.from({ length: lines }).map((_, i) => (
          <div
            key={i}
            className="skeleton h-4"
            style={{ width: `${Math.random() * 30 + 70}%` }}
          />
        ))}
      </div>
    );
  }

  const variantStyles = {
    text: "h-4 w-full",
    card: "h-32 w-full rounded-xl",
    circle: "h-10 w-10 rounded-full",
    rect: "h-20 w-full rounded-lg",
  };

  return <div className={cn("skeleton", variantStyles[variant], className)} />;
}

export function MetricCardSkeleton() {
  return (
    <div className="metric-card">
      <div className="skeleton h-3 w-20" />
      <div className="skeleton mt-3 h-8 w-16" />
      <div className="skeleton mt-2 h-3 w-28" />
    </div>
  );
}

export function FeatureCardSkeleton() {
  return (
    <div className="feature-card">
      <div className="skeleton h-11 w-11 rounded-xl" />
      <div className="skeleton mt-4 h-5 w-32" />
      <div className="skeleton mt-2 h-3 w-full" />
      <div className="skeleton mt-1 h-3 w-3/4" />
    </div>
  );
}

export function ListCardSkeleton() {
  return (
    <div className="list-card">
      <div className="skeleton h-3 w-24" />
      <div className="skeleton mt-2 h-5 w-3/4" />
      <div className="skeleton mt-2 h-3 w-1/2" />
    </div>
  );
}
