package de.halfminer.hmbot.storage;

import java.util.HashSet;
import java.util.Set;

/**
 * Groups are determined by a users talk power and contain a set of permissions.
 */
class HalfGroup {

    private final int talkPower;
    private final Set<String> permissions;
    private HalfGroup inheretedGroup;

    HalfGroup(int talkPower, Set<String> permissions) {
        this.talkPower = talkPower;
        this.permissions = permissions;
    }

    void setInheretedGroup(HalfGroup toInherit) {
        inheretedGroup = toInherit;
    }

    int getTalkPower() {
        return talkPower;
    }

    boolean hasPermission(String permission) {
        return permissions.contains(permission)
                || (inheretedGroup != null && inheretedGroup.hasPermission(permission));
    }

    void addPermissions(HalfGroup mergeWith) {
        permissions.addAll(mergeWith.permissions);
    }

    Set<String> getAllPermissions() {
        Set<String> allPerms = new HashSet<>(permissions);
        HalfGroup currentChild = this;
        while (currentChild.inheretedGroup != null) {
            currentChild = currentChild.inheretedGroup;
            allPerms.addAll(currentChild.permissions);
        }
        return allPerms;
    }
}
