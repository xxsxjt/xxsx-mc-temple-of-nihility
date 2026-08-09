package com.templenihility.transport;

import java.util.List;
import java.util.Locale;

public final class TransportPolicy {
    public enum ResourceType {
        ITEMS,
        FLUIDS,
        ENERGY;

        public static ResourceType byId(int id) {
            ResourceType[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public enum ResourceMode {
        ALL,
        ITEMS,
        FLUIDS,
        ENERGY;

        public static ResourceMode byId(int id) {
            ResourceMode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public enum Priority {
        BALANCED,
        ITEMS_FIRST,
        FLUIDS_FIRST,
        ENERGY_FIRST;

        public static Priority byId(int id) {
            Priority[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public enum RedstoneMode {
        IGNORE,
        REQUIRE_POWER,
        REQUIRE_NO_POWER;

        public static RedstoneMode byId(int id) {
            RedstoneMode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public enum FilterMode {
        OFF,
        WHITELIST,
        BLACKLIST;

        public static FilterMode byId(int id) {
            FilterMode[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    public enum BlockedReason {
        IDLE,
        RUNNING,
        REDSTONE_CONTROL,
        INVALID_DIRECTION,
        SOURCE_UNLOADED,
        TARGET_UNLOADED,
        FILTER_EMPTY,
        FILTER_REJECTED,
        SOURCE_EMPTY,
        SOURCE_BLOCKED,
        SOURCE_CAPABILITY_MISSING,
        TARGET_CAPABILITY_MISSING,
        SAME_HANDLER,
        TARGET_BLOCKED;

        public static BlockedReason byId(int id) {
            BlockedReason[] values = values();
            return values[Math.floorMod(id, values.length)];
        }

        public String translationKey() {
            return "screen.templenihility.transport.reason." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static boolean allowsRedstone(RedstoneMode mode, boolean powered) {
        return switch (mode) {
            case IGNORE -> true;
            case REQUIRE_POWER -> powered;
            case REQUIRE_NO_POWER -> !powered;
        };
    }

    public static boolean allowsItem(FilterMode mode, boolean hasEntries, boolean matches) {
        return switch (mode) {
            case OFF -> true;
            case WHITELIST -> hasEntries && matches;
            case BLACKLIST -> !hasEntries || !matches;
        };
    }

    public static List<ResourceType> orderedResources(ResourceMode mode, Priority priority, int rotation) {
        return switch (mode) {
            case ITEMS -> List.of(ResourceType.ITEMS);
            case FLUIDS -> List.of(ResourceType.FLUIDS);
            case ENERGY -> List.of(ResourceType.ENERGY);
            case ALL -> switch (priority) {
                case ITEMS_FIRST -> List.of(ResourceType.ITEMS, ResourceType.FLUIDS, ResourceType.ENERGY);
                case FLUIDS_FIRST -> List.of(ResourceType.FLUIDS, ResourceType.ENERGY, ResourceType.ITEMS);
                case ENERGY_FIRST -> List.of(ResourceType.ENERGY, ResourceType.ITEMS, ResourceType.FLUIDS);
                case BALANCED -> switch (Math.floorMod(rotation, 3)) {
                    case 1 -> List.of(ResourceType.FLUIDS, ResourceType.ENERGY, ResourceType.ITEMS);
                    case 2 -> List.of(ResourceType.ENERGY, ResourceType.ITEMS, ResourceType.FLUIDS);
                    default -> List.of(ResourceType.ITEMS, ResourceType.FLUIDS, ResourceType.ENERGY);
                };
            };
        };
    }

    public static int nextDistinctDirectionId(int currentId, int forbiddenId, int directionCount) {
        if (directionCount < 2) {
            throw new IllegalArgumentException("At least two directions are required");
        }
        int current = Math.floorMod(currentId, directionCount);
        int forbidden = Math.floorMod(forbiddenId, directionCount);
        for (int offset = 1; offset <= directionCount; offset++) {
            int candidate = (current + offset) % directionCount;
            if (candidate != forbidden) {
                return candidate;
            }
        }
        return current;
    }

    public static BlockedReason chooseDiagnostic(List<BlockedReason> reasons) {
        BlockedReason selected = BlockedReason.IDLE;
        int selectedRank = -1;
        for (BlockedReason reason : reasons) {
            int rank = diagnosticRank(reason);
            if (rank > selectedRank) {
                selected = reason;
                selectedRank = rank;
            }
        }
        return selected;
    }

    private static int diagnosticRank(BlockedReason reason) {
        return switch (reason) {
            case FILTER_EMPTY -> 90;
            case FILTER_REJECTED -> 85;
            case TARGET_BLOCKED, SAME_HANDLER -> 80;
            case SOURCE_BLOCKED -> 75;
            case SOURCE_EMPTY -> 70;
            case TARGET_CAPABILITY_MISSING -> 60;
            case SOURCE_CAPABILITY_MISSING -> 50;
            case SOURCE_UNLOADED, TARGET_UNLOADED, INVALID_DIRECTION, REDSTONE_CONTROL -> 40;
            case IDLE -> 0;
            case RUNNING -> 100;
        };
    }

    private TransportPolicy() {
    }
}
