"use client";

import { useState } from "react";

import { Dashboard } from "@/components/groups/Dashboard";
import { PublicGroupsSection } from "@/components/groups/PublicGroupsSection";
import { AppFooter } from "@/components/layout/AppFooter";
import { AppHeader } from "@/components/layout/AppHeader";
import { AuthModal } from "@/components/modals/AuthModal";
import { GroupModal } from "@/components/modals/GroupModal";
import { ProfileModal } from "@/components/modals/ProfileModal";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { useAuth, type ProfilePayload } from "@/hooks/useAuth";
import { useFeedback } from "@/hooks/useFeedback";
import { useResearchGroups, type GroupDraft } from "@/hooks/useResearchGroups";
import type { ResearchGroup } from "@/types";

export default function Home() {
  const feedback = useFeedback();
  const auth = useAuth(feedback);
  const groups = useResearchGroups(auth.user, feedback);

  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<ResearchGroup | null>(
    null
  );

  const openAuthModal = () => {
    feedback.clear();
    setIsAuthModalOpen(true);
  };

  const openProfileModal = async () => {
    feedback.clear();
    await auth.refreshUser();
    setIsProfileModalOpen(true);
  };

  const openGroupModal = (group: ResearchGroup) => {
    feedback.clear();
    setSelectedGroup(group);
    setIsGroupModalOpen(true);
  };

  const handleUpdateProfile = async (payload: ProfilePayload) => {
    const updatedUser = await auth.updateProfile(payload);
    if (!updatedUser) return null;

    setIsProfileModalOpen(false);
    await groups.refresh();
    return updatedUser;
  };

  const handleUpdateGroup = async (payload: GroupDraft) => {
    if (!selectedGroup) return false;

    const updated = await groups.updateGroup(selectedGroup, payload);
    if (updated) setIsGroupModalOpen(false);
    return updated;
  };

  const handleDeleteGroup = async (password: string) => {
    if (!selectedGroup) return false;

    const deleted = await groups.deleteGroup(selectedGroup, password);
    if (deleted) {
      setIsGroupModalOpen(false);
      setSelectedGroup(null);
    }
    return deleted;
  };

  const handleDeleteProfile = async (password: string) => {
    const deleted = await auth.deleteProfile(password);
    if (!deleted) return false;

    setIsProfileModalOpen(false);
    auth.logout();
    feedback.succeed("Sua conta de usuário foi excluída com sucesso.");
    await groups.refresh();
    return true;
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#F6F4F3] text-[#333333]">
      <AppHeader
        user={auth.user}
        onOpenAuth={openAuthModal}
        onOpenProfile={openProfileModal}
        onLogout={auth.logout}
      />

      <main className="flex-1 max-w-5xl w-full mx-auto p-6 animate-fade">
        <AlertBanner message={feedback.error} variant="error" />
        <AlertBanner message={feedback.success} variant="success" />

        {auth.user ? (
          <Dashboard
            groups={groups}
            feedback={feedback}
            onManageGroup={openGroupModal}
          />
        ) : (
          <PublicGroupsSection groups={groups.allGroups} />
        )}
      </main>

      <AuthModal
        open={isAuthModalOpen}
        feedback={feedback}
        onClose={() => setIsAuthModalOpen(false)}
        onLogin={async (email) => {
          const authenticated = await auth.login(email);
          if (authenticated) setIsAuthModalOpen(false);
          return authenticated;
        }}
        onRegister={async (payload) => {
          const registered = await auth.register(payload);
          if (registered) setIsAuthModalOpen(false);
          return registered;
        }}
      />

      {isProfileModalOpen && auth.user && (
        <ProfileModal
          user={auth.user}
          feedback={feedback}
          onClose={() => setIsProfileModalOpen(false)}
          onUpdateProfile={handleUpdateProfile}
          onDeleteProfile={handleDeleteProfile}
        />
      )}

      {isGroupModalOpen && selectedGroup && (
        <GroupModal
          group={selectedGroup}
          feedback={feedback}
          onClose={() => setIsGroupModalOpen(false)}
          onUpdateGroup={handleUpdateGroup}
          onDeleteGroup={handleDeleteGroup}
        />
      )}

      <AppFooter />
    </div>
  );
}
