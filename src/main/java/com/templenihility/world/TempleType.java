package com.templenihility.world;

public enum TempleType {
    SHRINE(1, 8, 10, "神龛"),
    TEMPLE(2, 15, 25, "圣殿"),
    TEMPLE_COMPLEX(3, 30, 50, "圣殿群");

    private final int tier;
    private final int minSize;
    private final int maxSize;
    private final String chineseName;

    TempleType(int tier, int minSize, int maxSize, String chineseName) {
        this.tier = tier;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.chineseName = chineseName;
    }

    public int getTier() { return tier; }
    public int getMinSize() { return minSize; }
    public int getMaxSize() { return maxSize; }
    public String getChineseName() { return chineseName; }
}
