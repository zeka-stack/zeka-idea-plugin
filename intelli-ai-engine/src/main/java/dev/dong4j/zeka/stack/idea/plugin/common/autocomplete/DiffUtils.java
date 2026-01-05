package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class DiffUtils {
    @NotNull
    static List<DiffGroup> computeDiffGroups(@NotNull String oldText, @NotNull String newText) {
        int n = oldText.length();
        int m = newText.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (oldText.charAt(i) == newText.charAt(j)) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        List<DiffGroup> groups = new ArrayList<>();
        StringBuilder additions = new StringBuilder();
        StringBuilder deletions = new StringBuilder();
        int indexInOld = 0;
        Integer groupIndex = null;
        int i = 0;
        int j = 0;
        while (i < n || j < m) {
            if (i < n && j < m && oldText.charAt(i) == newText.charAt(j)) {
                flushGroup(groups, additions, deletions, groupIndex);
                additions.setLength(0);
                deletions.setLength(0);
                groupIndex = null;
                i++;
                j++;
                indexInOld++;
            } else if (j < m && (i == n || dp[i][j + 1] >= dp[i + 1][j])) {
                if (groupIndex == null) {
                    groupIndex = indexInOld;
                }
                additions.append(newText.charAt(j));
                j++;
            } else if (i < n) {
                if (groupIndex == null) {
                    groupIndex = indexInOld;
                }
                deletions.append(oldText.charAt(i));
                i++;
                indexInOld++;
            }
        }
        flushGroup(groups, additions, deletions, groupIndex);
        return groups;
    }

    private static void flushGroup(@NotNull List<DiffGroup> groups,
                                   @NotNull StringBuilder additions,
                                   @NotNull StringBuilder deletions,
                                   Integer groupIndex) {
        if (groupIndex == null) {
            return;
        }
        groups.add(new DiffGroup(additions.toString(), deletions.toString(), groupIndex));
    }
}
