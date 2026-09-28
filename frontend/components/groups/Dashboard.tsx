import { CreateGroupCard } from "@/components/groups/CreateGroupCard";
import { JoinGroupCard } from "@/components/groups/JoinGroupCard";
import { LeaderGroupsSection } from "@/components/groups/LeaderGroupsSection";
import { MemberGroupsSection } from "@/components/groups/MemberGroupsSection";
import type { Feedback } from "@/hooks/useFeedback";
import type { UseResearchGroups } from "@/hooks/useResearchGroups";
import type { ResearchGroup } from "@/types";

interface DashboardProps {
  groups: UseResearchGroups;
  feedback: Feedback;
  onManageGroup: (group: ResearchGroup) => void;
}

export function Dashboard({
  groups,
  feedback,
  onManageGroup,
}: DashboardProps) {
  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <CreateGroupCard groups={groups} feedback={feedback} />
        <JoinGroupCard groups={groups} feedback={feedback} />
      </div>

      <MemberGroupsSection groups={groups.memberGroups} />

      <LeaderGroupsSection
        groups={groups.leaderGroups}
        generatedTokens={groups.generatedTokens}
        onGenerateInvite={groups.generateInvite}
        onManageGroup={onManageGroup}
      />
    </div>
  );
}
