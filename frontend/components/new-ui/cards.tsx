"use client";

import { motion } from "framer-motion";
import { type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { useEffect, useState, useRef } from "react";

/* ============================================
   Feature Card
   ============================================ */

interface FeatureCardProps {
  icon: LucideIcon;
  title: string;
  description: string;
  tag?: string;
  href?: string;
  onClick?: () => void;
  className?: string;
  index?: number;
}

export function FeatureCard({
  icon: Icon,
  title,
  description,
  tag,
  href,
  onClick,
  className,
  index = 0,
}: FeatureCardProps) {
  const Wrapper = href ? motion.a : motion.button;
  const wrapperProps = href
    ? { href, className: cn("feature-card text-left", className) }
    : {
        type: "button" as const,
        onClick,
        className: cn("feature-card text-left", className),
      };

  return (
    <Wrapper
      {...wrapperProps}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.08 }}
      whileHover={{ y: -4 }}
    >
      <span className="feature-icon">
        <Icon size={20} strokeWidth={1.8} />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {tag && <span className="feature-tag">{tag}</span>}
    </Wrapper>
  );
}

/* ============================================
   Metric Card
   ============================================ */

interface MetricCardProps {
  label: string;
  value: number | string;
  hint?: string;
  className?: string;
  index?: number;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
}

export function MetricCard({
  label,
  value,
  hint,
  className,
  index = 0,
  trend,
  trendValue,
}: MetricCardProps) {
  return (
    <motion.div
      className={cn("metric-card", className)}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.06 }}
      whileHover={{ y: -3 }}
    >
      <p className="metric-label">{label}</p>
      <p className="metric-value">{value}</p>
      {hint && <p className="metric-hint">{hint}</p>}
      {trend && trendValue && (
        <div
          className={cn(
            "mt-2 flex items-center gap-1 text-xs font-medium",
            trend === "up" && "text-[var(--green)]",
            trend === "down" && "text-[var(--red)]",
            trend === "neutral" && "text-[var(--muted)]"
          )}
        >
          {trend === "up" && "↑"}
          {trend === "down" && "↓"}
          {trend === "neutral" && "→"}
          {trendValue}
        </div>
      )}
    </motion.div>
  );
}

/* ============================================
   Animated Counter
   ============================================ */

export function AnimatedCounter({
  target,
  duration = 1200,
  suffix = "",
}: {
  target: number;
  duration?: number;
  suffix?: string;
}) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  const hasAnimated = useRef(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !hasAnimated.current) {
            hasAnimated.current = true;
            const startTime = performance.now();
            const animate = (currentTime: number) => {
              const elapsed = currentTime - startTime;
              const progress = Math.min(elapsed / duration, 1);
              const eased = 1 - Math.pow(1 - progress, 3);
              setCount(Math.round(eased * target));
              if (progress < 1) {
                requestAnimationFrame(animate);
              }
            };
            requestAnimationFrame(animate);
          }
        });
      },
      { threshold: 0.3 }
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [target, duration]);

  return (
    <span ref={ref}>
      {count}
      {suffix}
    </span>
  );
}
