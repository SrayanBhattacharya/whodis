import React from "react";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  isLoading?: boolean;
}

export function Button({
  variant = "primary",
  size = "md",
  isLoading = false,
  className = "",
  disabled,
  children,
  ...props
}: ButtonProps) {
  const baseStyles =
    "inline-flex items-center justify-center font-medium transition-all duration-150 focus-visible:outline-none disabled:opacity-40 disabled:cursor-not-allowed select-none cursor-pointer rounded-md";

  const sizeStyles = {
    sm: "text-xs px-3 py-1.5 h-8",
    md: "text-xs px-4 py-2 h-9",
    lg: "text-sm px-5 py-2.5 h-10",
  }[size];

  const variantStyles = {
    primary:
      "bg-white text-black hover:bg-[#eaeaea] active:bg-[#d0d0d0] font-semibold border border-white",
    secondary:
      "bg-[#0a0a0a] text-[#ededed] border border-[#222222] hover:border-[#444444] hover:bg-[#141414]",
    ghost:
      "text-[#888888] hover:text-[#ededed] hover:bg-[#141414]",
    danger:
      "bg-[#111111] text-red-400 border border-red-500/30 hover:border-red-500 hover:bg-red-950/20",
  }[variant];

  return (
    <button
      className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <span className="flex items-center gap-2">
          <svg
            className="animate-spin h-3.5 w-3.5 text-current"
            xmlns="http://www.w3.org/2000/svg"
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
            ></circle>
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v8H4z"
            ></path>
          </svg>
          <span className="font-mono text-xs">PROCESSING</span>
        </span>
      ) : (
        children
      )}
    </button>
  );
}
