"use client";

import { useEffect, useMemo, useRef, useState } from "react";

type AnimatedCounterProps = {
  value: number;
  durationMs?: number;
  suffix?: string;
};

function cubicOut(x: number) {
  return 1 - Math.pow(1 - x, 3);
}

export function AnimatedCounter({ value, durationMs = 1400, suffix = "" }: AnimatedCounterProps) {
  const [display, setDisplay] = useState(0);
  const [visible, setVisible] = useState(false);
  const ref = useRef<HTMLSpanElement | null>(null);

  useEffect(() => {
    const node = ref.current;
    if (!node) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setVisible(true);
            observer.disconnect();
            break;
          }
        }
      },
      { threshold: 0.35 }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!visible) {
      return;
    }
    let raf = 0;
    const start = performance.now();

    const tick = (time: number) => {
      const progress = Math.min((time - start) / durationMs, 1);
      const nextValue = Math.round(value * cubicOut(progress));
      setDisplay(nextValue);
      if (progress < 1) {
        raf = requestAnimationFrame(tick);
      }
    };

    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [durationMs, value, visible]);

  const formatted = useMemo(() => display.toLocaleString("zh-CN"), [display]);
  return (
    <span ref={ref}>
      {formatted}
      {suffix}
    </span>
  );
}
