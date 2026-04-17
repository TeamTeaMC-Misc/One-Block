package xueluoanping.oneblock.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import xueluoanping.oneblock.ModConstants;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.util.RegisterFinderUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StageData {

    private String name;
    private int count;
    private List<BlockEntry> list;
    private String end_gift;
    private Identifier resourceLocation;
    private String target;
    private List<String> mods;

    private boolean disable_message;
    private int add_count;
    // bedrock set before a stage start it means we need time to prepare for a new stage
    // if you want to set
    private int bedrock_time;

    private boolean disable_addition;


    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    public JsonElement toJson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJsonTree(this);
    }

    public String getResName() {
        return String.format("%s.%s", resourceLocation.getNamespace(), resourceLocation.getPath().replace("/", "."));
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<BlockEntry> getList() {
        return list;
    }

    public void setList(List<BlockEntry> list) {
        this.list = list;
    }

    public String getEnd_gift() {
        return end_gift;
    }

    public void setEnd_gift(String end_gift) {
        this.end_gift = end_gift;
    }

    public Identifier getIdentifier() {
        return resourceLocation;
    }

    public void setIdentifier(Identifier resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public List<String> getMods() {
        return mods;
    }

    public void setMods(List<String> mods) {
        this.mods = mods;
    }

    public boolean isDisable_message() {
        return disable_message;
    }

    public void setDisable_message(boolean disable_message) {
        this.disable_message = disable_message;
    }

    public int getAdd_count() {
        return add_count;
    }

    public void setAdd_count(int add_count) {
        this.add_count = add_count;
    }

    public int getBedrock_time() {
        return bedrock_time;
    }

    public void setBedrock_time(int bedrock_time) {
        this.bedrock_time = bedrock_time;
    }

    public boolean isDisable_addition() {
        return disable_addition;
    }

    public void setDisable_addition(boolean disable_addition) {
        this.disable_addition = disable_addition;
    }

    public BlockEntry selectRandomByWeight(RandomSource random, StageProgress nowProgress, boolean reachRemain, int localCount) {
        var blockEntryStream = this.list;

        String fix_uid = nowProgress.checkPrecedence(localCount);

        var need_fix = blockEntryStream.stream().filter(blockEntry -> Objects.equals(blockEntry.getGlobalId(), fix_uid)).findFirst();
        if (need_fix.isPresent()) {
            return need_fix.get();
        }

        if (reachRemain)
            blockEntryStream = blockEntryStream.stream()
                    .filter(blockEntry -> nowProgress.indexOfRemain(blockEntry.getType(), blockEntry.getGlobalId()) >= 0).collect(Collectors.toList());

        int totalWeight = blockEntryStream
                .stream()
                .filter(blockEntry -> nowProgress.checkQuota(blockEntry.getType(), blockEntry.getGlobalId()))
                .mapToInt(BlockEntry::getWeight).sum();

        if (totalWeight < 0 || (totalWeight == 0 && !reachRemain)) {
            OneBlock.error("Found error in", this.getResName(), reachRemain, localCount, totalWeight, nowProgress, this.list);
            // Todo: fix the problem
            blockEntryStream = this.list;
            totalWeight = blockEntryStream
                    .stream()
                    .filter(blockEntry -> nowProgress.checkQuota(blockEntry.getType(), blockEntry.getGlobalId()))
                    .mapToInt(BlockEntry::getWeight).sum();
            // throw new IllegalArgumentException( OneBlock.getStr("Found error in", this.getResName(),reachRemain,localCount, totalWeight, nowProgress,this.list));
        } else if (totalWeight == 0 && !blockEntryStream.isEmpty()) {
            return blockEntryStream.get(0);
        }

        int randomNumber = random.nextInt(totalWeight) + 1;

        int cumulativeWeight = 0;
        for (BlockEntry entry : blockEntryStream) {
            cumulativeWeight += entry.getWeight();
            if (randomNumber <= cumulativeWeight) {
                return entry;
            }
        }
        OneBlock.error("Found error in", this.getResName(), totalWeight, nowProgress.remainCounter);
        return new BlockEntry("block", "minecraft:barrier");
    }

    public static class BlockEntry {
        private String type;
        private String id;
        // set it if you need to
        private String uid;
        private int weight;
        // loot_table was set just for
        private String loot_table;
        // just for mob
        private int count;

        // now it doesn't work, if you want to set a block with NBT it's recommended to use a template file
        private String blockstates;
        private String nbt;

        // force to get, ignore when 0
        // we could set it less than 0 but then transfer it into a positive number will help our work
        private int precedence_start;
        private int precedence;
        private int precedence_end;

        // not use it with fix
        private int min_times;
        private int max_times;
        private int times;

        private List<BlockEntry> preprocessing;
        private int offset_x;
        private int offset_y;
        private int offset_z;

        // usually for preprocessing
        private float chance;

        // from
        private Identifier from;

        @Override
        public String toString() {
            return "BlockEntry{" +
                    "blockstates='" + blockstates + '\'' +
                    ", type='" + type + '\'' +
                    ", id='" + id + '\'' +
                    ", uid='" + uid + '\'' +
                    ", weight=" + weight +
                    ", loot_table='" + loot_table + '\'' +
                    ", count=" + count +
                    ", nbt='" + nbt + '\'' +
                    ", precedence_start=" + precedence_start +
                    ", precedence=" + precedence +
                    ", precedence_end=" + precedence_end +
                    ", min_times=" + min_times +
                    ", max_times=" + max_times +
                    ", times=" + times +
                    ", preprocessing=" + preprocessing +
                    ", offset_x=" + offset_x +
                    ", offset_y=" + offset_y +
                    ", offset_z=" + offset_z +
                    ", chance=" + chance +
                    ", from=" + from +
                    '}';
        }

        public String getGlobalId() {
            if (this.getUid() != null)
                return this.getUid();
            return toString();
        }

        public float getRealChance() {
            if (chance == 0) {
                this.chance = 1.0f;
            }
            return chance;
        }

        public BlockEntry(String type, String id) {
            this.type = type;
            this.id = id;
            this.weight = 0;
            this.count = 0;
            // Gson default set 0 so we could ignore it if it less than 1
            this.min_times = 0;
            this.max_times = 0;
            // not offset
            this.offset_x = 0;
            this.offset_y = 0;
            this.offset_z = 0;
        }

        public String getLoot_table() {
            return loot_table;
        }

        public void setLoot_table(String loot_table) {
            this.loot_table = loot_table;
        }

        public int getMax_times() {
            return max_times;
        }

        public void setMax_times(int max_times) {
            this.max_times = max_times;
        }

        public int getMin_times() {
            return min_times;
        }

        public void setMin_times(int min_times) {
            this.min_times = min_times;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String getBlockstates() {
            return blockstates;
        }

        public void setBlockstates(String blockstates) {
            this.blockstates = blockstates;
        }

        public String getNbt() {
            return nbt;
        }

        public void setNbt(String nbt) {
            this.nbt = nbt;
        }

        public int getOffset_x() {
            return offset_x;
        }

        public void setOffset_x(int offset_x) {
            this.offset_x = offset_x;
        }

        public int getOffset_y() {
            return offset_y;
        }

        public void setOffset_y(int offset_y) {
            this.offset_y = offset_y;
        }

        public int getOffset_z() {
            return offset_z;
        }

        public void setOffset_z(int offset_z) {
            this.offset_z = offset_z;
        }

        public List<BlockEntry> getPreprocessing() {
            return preprocessing;
        }

        public void setPreprocessing(List<BlockEntry> preprocessing) {
            this.preprocessing = preprocessing;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public int getPrecedence_start() {
            return precedence_start;
        }

        public void setPrecedence_start(int precedence_start) {
            this.precedence_start = precedence_start;
        }

        public int getPrecedence_end() {
            return precedence_end;
        }

        public void setPrecedence_end(int precedence_end) {
            this.precedence_end = precedence_end;
        }

        public int getPrecedence() {
            return precedence;
        }

        public void setPrecedence(int precedence) {
            this.precedence = precedence;
        }

        public int getTimes() {
            return times;
        }

        public void setTimes(int times) {
            this.times = times;
        }

        public float getChance() {
            return chance;
        }

        public void setChance(float chance) {
            this.chance = chance;
        }

        public Identifier getFrom() {
            return from;
        }

        public void setFrom(Identifier from) {
            this.from = from;
        }

        public boolean isValid(ServerLevel access) {
            if (getPreprocessing() != null) {
                for (BlockEntry blockEntry : getPreprocessing()) {
                    boolean isChildValid = blockEntry.isValid(access);
                    if (!isChildValid) {
                        return false;
                    }
                }
            }

            switch (getType()) {
                case ModConstants.TYPE_BLOCK -> {
                    return RegisterFinderUtil.getBlockKey(RegisterFinderUtil.getBlock(getId())).toString().equals(getId());
                }
                case ModConstants.TYPE_GIFT, ModConstants.TYPE_COMMAND, ModConstants.TYPE_ARCHAEOLOGY,
                     ModConstants.TYPE_STRUCTURE, ModConstants.TYPE_CONFIGURED_FEATURE -> {
                    return true;
                }
                case ModConstants.TYPE_MOB -> {
                    return RegisterFinderUtil.getEntityKey(RegisterFinderUtil.getEntity(getId())).toString().equals(getId());
                }
                case ModConstants.TYPE_SOUND -> {
                    return RegisterFinderUtil.getSoundKey(RegisterFinderUtil.getSound(getId())).toString().equals(getId());
                }
                case ModConstants.TYPE_TEMPLATE -> {
                    return RegisterFinderUtil.checkTemplateKey(access, getId());
                }
                default -> {
                    return false;
                }
            }
        }


    }
}
