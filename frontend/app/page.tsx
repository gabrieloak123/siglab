"use client";

import { useState } from "react";
import { ConferencePanel } from "@/components/groups/ConferencePanel";
import { ConferenceSearch } from "@/components/groups/ConferenceSearch";
import { Dashboard } from "@/components/groups/Dashboard";
import { PublicGroupsSection } from "@/components/groups/PublicGroupsSection";
import { SocialPanel } from "@/components/groups/SocialPanel";
import { AppFooter } from "@/components/layout/AppFooter";
import { AppHeader } from "@/components/layout/AppHeader";
import { AuthModal } from "@/components/modals/AuthModal";
import { ProfileModal } from "@/components/modals/ProfileModal";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { useAuth, type ProfilePayload } from "@/hooks/useAuth";
import { useFeedback } from "@/hooks/useFeedback";
import { useResearchGroups } from "@/hooks/useResearchGroups";

export default function Home() {
  const feedback = useFeedback();
  const auth = useAuth(feedback);
  const groups = useResearchGroups(auth.user, feedback);
  const [authOpen, setAuthOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const openAuth = () => { feedback.clear(); setAuthOpen(true); };
  const openProfile = async () => { feedback.clear(); await auth.refreshUser(); setProfileOpen(true); };
  const updateProfile = async (payload: ProfilePayload) => {
    const updated = await auth.updateProfile(payload);
    if (updated) { setProfileOpen(false); await groups.refresh(); }
    return updated;
  };
  const deleteProfile = async (password: string) => {
    if (!(await auth.deleteProfile(password))) return false;
    setProfileOpen(false); auth.logout(); await groups.refresh(); return true;
  };
  return <div className="min-h-screen flex flex-col bg-[#F6F4F3] text-[#333333]">
    <AppHeader user={auth.user} onOpenAuth={openAuth} onOpenProfile={openProfile} onLogout={auth.logout} />
    <main className="flex-1 max-w-5xl w-full mx-auto p-6 animate-fade">
      <AlertBanner message={feedback.error} variant="error" />
      <AlertBanner message={feedback.success} variant="success" />
      {auth.user && (auth.user.interest_areas?.length ?? 0) !== 3 &&
        <div className="bg-amber-50 border border-amber-300 rounded p-4 mb-6 text-sm">
          Complete três áreas de interesse em <button className="font-bold underline" onClick={() => void openProfile()}>Gerenciar Perfil</button> para receber sugestões.
        </div>}
      {auth.user ? <><Dashboard groups={groups} feedback={feedback} /><SocialPanel /><ConferencePanel /><ConferenceSearch /></> : <PublicGroupsSection groups={groups.allGroups} />}
    </main>
    <AuthModal open={authOpen} feedback={feedback} onClose={() => setAuthOpen(false)}
      onLogin={async (email, password) => { const ok = await auth.login(email, password); if (ok) setAuthOpen(false); return ok; }}
      onRegister={async payload => { const ok = await auth.register(payload); if (ok) setAuthOpen(false); return ok; }}
      onGoogle={async (credential, authorId, areas) => { const ok = await auth.googleLogin(credential, authorId, areas); if (ok) setAuthOpen(false); return ok; }} />
    {profileOpen && auth.user && <ProfileModal user={auth.user} feedback={feedback} onClose={() => setProfileOpen(false)} onUpdateProfile={updateProfile} onDeleteProfile={deleteProfile} />}
    <AppFooter />
  </div>;
}
