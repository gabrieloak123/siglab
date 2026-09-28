interface AlertBannerProps {
  message: string;
  variant: "error" | "success";
  size?: "sm" | "md";
}

const VARIANT_STYLES = {
  error: "bg-[#BF3100]",
  success: "bg-[#3E885B]",
};

const SIZE_STYLES = {
  sm: "mb-4 p-3 rounded text-xs font-medium",
  md: "mb-6 p-4 rounded font-medium text-sm",
};

export function AlertBanner({
  message,
  variant,
  size = "md",
}: AlertBannerProps) {
  if (!message) return null;

  return (
    <div
      className={`${VARIANT_STYLES[variant]} ${SIZE_STYLES[size]} text-white`}
    >
      {message}
    </div>
  );
}
