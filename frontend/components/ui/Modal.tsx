import type { ReactNode } from "react";

interface ModalProps {
  children: ReactNode;
  scrollable?: boolean;
}

export function Modal({ children, scrollable = false }: ModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 flex justify-center items-center p-4 z-50 animate-fade">
      <div
        className={`bg-white max-w-md w-full rounded border border-gray-300 p-6 shadow-md${
          scrollable ? " max-h-[90vh] overflow-y-auto" : ""
        }`}
      >
        {children}
      </div>
    </div>
  );
}
