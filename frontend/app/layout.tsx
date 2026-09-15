import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "SIGLab - Gestão de Grupos de Pesquisa",
  description: "Sistema simples e direto para gerenciamento de grupos de pesquisa",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR" className={inter.variable}>
      <body className="min-h-screen bg-[#F6F4F3] text-[#333333] font-sans antialiased flex flex-col">
        {children}
      </body>
    </html>
  );
}
