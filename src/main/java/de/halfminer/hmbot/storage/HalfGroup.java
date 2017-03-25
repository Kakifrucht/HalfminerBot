package de.halfminer.hmbot.storage;

import java.util.Set;

/**
 * Groups are determined by a users talk power and contain a set of permissions.
 */
class HalfGroup {

    private final int talkPower;
    private final Set<String> permissions;

    HalfGroup(int talkPower, Set<String> permissions) {
        this.talkPower = talkPower;
        this.permissions = permissions;
    }

    int getTalkPower() {
        return talkPower;
    }

    boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    void addPermissions(HalfGroup mergeWith) {
        permissions.addAll(mergeWith.getPermissions());
    }

    private Set<String> getPermissions() {
        return permissions;
    }
}
