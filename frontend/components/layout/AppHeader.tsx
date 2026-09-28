import type { User } from "@/types";

interface AppHeaderProps {
  user: User | null;
  onOpenAuth: () => void;
  onOpenProfile: () => void;
  onLogout: () => void;
}

export function AppHeader({
  user,
  onOpenAuth,
  onOpenProfile,
  onLogout,
}: AppHeaderProps) {
  return (
    <header className="bg-[#333333] text-[#F6F4F3] py-4 px-6 shadow-sm flex justify-between items-center">
      <div>
        <h1 className="text-xl font-bold tracking-tight">SIGLab</h1>
        <p className="text-xs text-gray-300">Sistema de Grupos de Pesquisa</p>
      </div>

      {user ? (
        <div className="flex items-center gap-3">
          <span className="text-sm font-medium">
            Logado como: <strong>{user.name}</strong> ({user.email})
          </span>
          <button onClick={onOpenProfile} className="btn-dark text-sm">
            Gerenciar Perfil
          </button>
          <button onClick={onLogout} className="btn-rust text-sm">
            Sair
          </button>
        </div>
      ) : (
        <button onClick={onOpenAuth} className="btn-green text-sm">
          Cadastrar / Login
        </button>
      )}
    </header>
  );
}
