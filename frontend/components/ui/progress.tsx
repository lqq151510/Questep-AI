import { cn } from "@/lib/utils";

type ProgressProps = {
  value: number;
  label?: string;
  tone?: "teal" | "amber" | "indigo" | "coral";
  size?: "default" | "small";
};

export function Progress({ value, label, tone, size = "default" }: ProgressProps) {
  return (
    <div className={cn("progress-track", size === "small" && "small")} aria-label={label}>
      <span className={tone} style={{ width: `${Math.max(0, Math.min(value, 100))}%` }} />
    </div>
  );
}
