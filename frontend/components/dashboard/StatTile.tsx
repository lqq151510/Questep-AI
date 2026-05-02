import type { LucideIcon } from "lucide-react";

type StatTileProps = {
  icon: LucideIcon;
  label: string;
  value: string;
  note: string;
};

export function StatTile({ icon: Icon, label, value, note }: StatTileProps) {
  return (
    <article className="stat-tile">
      <div className="stat-icon">
        <Icon size={19} />
      </div>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        <small>{note}</small>
      </div>
    </article>
  );
}
