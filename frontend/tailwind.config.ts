import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{js,ts,jsx,tsx,mdx}", "./components/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        background: "var(--background)",
        canvas: "var(--canvas)",
        surface: {
          DEFAULT: "var(--surface)",
          soft: "var(--surface-soft)",
          blue: "var(--surface-blue)"
        },
        ink: "var(--ink)",
        muted: "var(--muted)",
        subtle: "var(--subtle)",
        border: {
          DEFAULT: "var(--border)",
          strong: "var(--border-strong)"
        },
        primary: {
          DEFAULT: "var(--blue)",
          soft: "var(--blue-soft)"
        },
        success: {
          DEFAULT: "var(--green)",
          soft: "var(--green-soft)"
        },
        warning: {
          DEFAULT: "var(--yellow)",
          soft: "var(--yellow-soft)"
        },
        danger: {
          DEFAULT: "var(--red)",
          soft: "var(--red-soft)"
        }
      },
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"]
      },
      borderRadius: {
        "2xl": "16px",
        "3xl": "20px",
        "4xl": "24px"
      },
      boxShadow: {
        surface: "var(--shadow-sm)",
        "surface-md": "var(--shadow-md)"
      }
    }
  },
  plugins: []
};

export default config;
