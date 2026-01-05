package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

interface TriggerRule {
    boolean check(TriggerContext context);

    default boolean ignoreWhenNotPass() {
        return false;
    }

    default boolean passAllWhenPassOne() {
        return false;
    }
}
