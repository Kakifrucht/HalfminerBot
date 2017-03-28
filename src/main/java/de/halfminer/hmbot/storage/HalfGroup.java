package de.halfminer.hmbot.storage;

import java.util.Set;

/**
 * Groups are determined by a users talk power and contain a set of permissions.
 */
class HalfGroup {

    private final String name;
    private final int talkPower;
    private final Set<String> permissions;

    HalfGroup(String name, int talkPower, Set<String> permissions) {
        this.name = name;
        this.talkPower = talkPower;
        this.permissions = permissions;
    }

    String getName() {
        return name;
    }

    int getTalkPower() {
        return talkPower;
    }

    boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    void addPermissions(HalfGroup mergeWith) {
        permissions.addAll(mergeWith.permissions);
    }

    Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return name + ": " + talkPower + ", Permissions: " + permissions;
    }
}
