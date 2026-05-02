import type { ButtonHTMLAttributes } from "react";

import { cn } from "@/lib/utils";

type ButtonVariant = "primary" | "ghost" | "icon" | "filter";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  selected?: boolean;
  size?: "default" | "small";
};

const variantClassName: Record<ButtonVariant, string> = {
  primary: "primary-button",
  ghost: "ghost-button",
  icon: "icon-button",
  filter: "filter-pill"
};

export function Button({
  className,
  variant = "ghost",
  selected = false,
  size = "default",
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(variantClassName[variant], selected && "selected", size === "small" && "small", className)}
      type={type}
      {...props}
    />
  );
}
