package game.card;

public final class TargetSpec {
    private final boolean allowFriendly;
    private final boolean allowEnemy;
    private final boolean allowUnit;
    private final boolean allowAvatar;
    private final boolean allowTile;
    private final boolean requiresEmptyTile;
    private final int range; // -1 => global
    private final boolean teleportLike;

    public TargetSpec(
            boolean allowFriendly,
            boolean allowEnemy,
            boolean allowUnit,
            boolean allowAvatar,
            boolean allowTile,
            boolean requiresEmptyTile,
            int range,
            boolean teleportLike
    ) {
        this.allowFriendly = allowFriendly;
        this.allowEnemy = allowEnemy;
        this.allowUnit = allowUnit;
        this.allowAvatar = allowAvatar;
        this.allowTile = allowTile;
        this.requiresEmptyTile = requiresEmptyTile;
        this.range = range;
        this.teleportLike = teleportLike;
    }

    public boolean isAllowFriendly() { return allowFriendly; }
    public boolean isAllowEnemy() { return allowEnemy; }
    public boolean isAllowUnit() { return allowUnit; }
    public boolean isAllowAvatar() { return allowAvatar; }
    public boolean isAllowTile() { return allowTile; }
    public boolean isRequiresEmptyTile() { return requiresEmptyTile; }
    public int getRange() { return range; }
    public boolean teleportLike() { return teleportLike; }

    public static TargetSpec none() {
        return new TargetSpec(false, false, false, false, false, false, -1, false);
    }

    public static TargetSpec friendlyUnitAny() {
        return new TargetSpec(true, false, true, true, false, false, -1, false);
    }

    public static TargetSpec friendlyUnitNonAvatar() {
        return new TargetSpec(true, false, true, false, false, false, -1, false);
    }

    public static TargetSpec enemyUnitAny() {
        return new TargetSpec(false, true, true, true, false, false, -1, false);
    }

    public static TargetSpec enemyUnitNonAvatar() {
        return new TargetSpec(false, true, true, false, false, false, -1, false);
    }

    public static TargetSpec emptyTileGlobal() {
        return new TargetSpec(false, false, false, false, true, true, -1, false);
    }

    public static TargetSpec portalStepTarget() {
        return new TargetSpec(true, false, true, false, true, true, -1, true);
    }

    public boolean unitOnly() {
        return allowUnit && !allowTile && !teleportLike;
    }

    public boolean isEmptyTileGlobal() {
        return allowTile && requiresEmptyTile && range == -1 && !teleportLike;
    }

    public boolean friendlyOnly() {
        return allowFriendly && !allowEnemy;
    }

    public boolean enemyOnly() {
        return allowEnemy && !allowFriendly;
    }

    public boolean canTargetAvatar() {
        return allowAvatar;
    }

    public boolean isNoTarget() {
        return !allowUnit && !allowTile && !teleportLike;
    }
}