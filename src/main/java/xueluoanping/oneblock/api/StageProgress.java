package xueluoanping.oneblock.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class StageProgress {
    public static final String KEY_NAME = "name";
    public static final String KEY_ID = "id";
    public static final String KEY_TYPE = "type";
    public static final String KEY_COUNT = "count";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    // public static final String KEY_NAME="name";

    public StageProgress(String name, int counter) {
        this(name, counter, 0, new ListTag(), new ListTag(), new ListTag());
    }

    public StageProgress(String name, int counter, int bedrockLastTime, ListTag remainCounter, ListTag quotaCounter, ListTag precedenceCounter) {
        this.name = name;
        this.counter = counter;
        this.bedrockLastTime = bedrockLastTime;
        this.remainCounter = remainCounter;
        this.quotaCounter = quotaCounter;
        this.precedenceCounter = precedenceCounter;
    }


    public StageProgress() {
        this("", 0);
    }

    public String name;
    public int counter;
    public int bedrockLastTime;
    public ListTag remainCounter;
    public ListTag quotaCounter;
    public ListTag precedenceCounter;
    private static final int TAG_REMAIN = 1;
    private static final int TAG_QUOTA = 2;
    private static final int TAG_precedence_LOCK = 3;

    public int getRemainAmount() {
        int amount = 0;
        for (int i = 0; i < this.remainCounter.size(); i++) {
            var tag = this.remainCounter.getCompoundOrEmpty(i);
            amount += tag.getIntOr(KEY_COUNT, 0);
        }
        return amount;
    }

    public void addListTag(int tagType, String type, String id, int count) {
        var tag = new CompoundTag();
        tag.putString(KEY_TYPE, type);
        tag.putString(KEY_ID, id);
        tag.putInt(KEY_COUNT, count);
        if (tagType == TAG_REMAIN)
            this.remainCounter.add(tag);
        else if (tagType == TAG_QUOTA) {
            this.quotaCounter.add(tag);
        }
    }

    public void addRemain(String type, String id, int count) {
        addListTag(TAG_REMAIN, type, id, count);
    }

    public void addQuota(String type, String id, int count) {
        addListTag(TAG_QUOTA, type, id, count);
    }

    public void addPrecedence(String type, String id, int start, int end) {
        var tag = new CompoundTag();
        tag.putString(KEY_TYPE, type);
        tag.putString(KEY_ID, id);
        tag.putInt(KEY_START, start);
        tag.putInt(KEY_END, end);
        this.precedenceCounter.add(tag);
    }

    public int updateListTag(int tagType, String type, String id) {
        var listTag = tagType == TAG_REMAIN ? this.remainCounter : this.quotaCounter;
        int removeIndex = -1;
        for (int i = 0; i < listTag.size(); i++) {
            var tag = listTag.getCompoundOrEmpty(i);
            if (tag.getStringOr(KEY_TYPE, "").equals(type)
                    && tag.getStringOr(KEY_ID, "").equals(id)) {
                int count = tag.getIntOr(KEY_COUNT, 0) - 1;
                if (count > 0) {
                    tag.putInt(KEY_COUNT, tag.getIntOr(KEY_COUNT, 0) - 1);
                } else {
                    removeIndex = i;
                }
                break;
            }
        }
        return removeIndex;
    }

    public void updateRemain(String type, String id) {
        int removeIndex = updateListTag(TAG_REMAIN, type, id);
        // it's not safe to delete anything in a cycle
        if (removeIndex >= 0) this.remainCounter.remove(removeIndex);
    }

    public void updateQuota(String type, String id) {
        int toZeroIndex = updateListTag(TAG_REMAIN, type, id);
        // just record the entry count 0
    }

    public int indexOfListTag(int tagType, String type, String id) {
        var listTag = tagType == TAG_REMAIN ? this.remainCounter : this.quotaCounter;
        int index = -1;
        for (int i = 0; i < listTag.size(); i++) {
            var tag = listTag.getCompoundOrEmpty(i);
            if (tag.getStringOr(KEY_TYPE, "").equals(type)
                    && tag.getStringOr(KEY_ID, "").equals(id)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public int indexOfRemain(String type, String id) {
        return indexOfListTag(TAG_REMAIN, type, id);
    }

    public int indexOfQuota(String type, String id) {
        return indexOfListTag(TAG_QUOTA, type, id);
    }

    public boolean checkQuota(String type, String id) {
        int index = indexOfQuota(type, id);
        boolean result = true;
        if (index >= 0) {
            result = this.quotaCounter.getCompoundOrEmpty(index).getIntOr(KEY_COUNT, 0) > 0;
        }
        return result;
    }

    public String checkPrecedence(int localCount) {
        String uid = null;
        for (int i = 0; i < precedenceCounter.size(); i++) {
            var tag = precedenceCounter.getCompoundOrEmpty(i);
            int start = tag.getIntOr(KEY_START, 0);
            start = start != 0 ? start : Integer.MIN_VALUE;
            int end = tag.getIntOr(KEY_END, 0);
            end = end != 0 ? end : Integer.MAX_VALUE;
            if (start <= localCount && localCount <= end) {
                uid = tag.getStringOr(KEY_ID, "");
                break;
            }
        }
        return uid;
    }

    public void cleanRemain() {
        this.remainCounter.clear();
    }

    public void cleanQuota() {
        this.quotaCounter.clear();
    }

    public void cleanPrecedence() {
        this.precedenceCounter.clear();
    }

    public void cleanLocalCounter() {
        this.cleanRemain();
        this.cleanQuota();
        this.cleanPrecedence();
    }

    public void updateBedrockLastTime() {
        this.bedrockLastTime--;
    }

    public void setBedrockLastTime(int tick) {
        this.bedrockLastTime = tick;
    }

    public void updateCounter() {
        this.counter++;
    }


    public void updateLocalCounter(String type, String id) {
        this.updateRemain(type, id);
        this.updateQuota(type, id);
    }

    @Override
    public String toString() {
        return "OneBlockProgress{" +
                "name='" + name + '\'' +
                ", counter=" + counter +
                ", bedrockLastTime=" + bedrockLastTime +
                ", remainCounter=" + remainCounter +
                ", quotaCounter=" + quotaCounter +
                ", precedenceCounter=" + precedenceCounter +
                '}';
    }
}
