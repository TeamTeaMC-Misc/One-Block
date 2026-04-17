package xueluoanping.oneblock.Item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import xueluoanping.oneblock.ModContents;
import xueluoanping.oneblock.handler.Levelhandler;

public class FantasyBraceletItem extends Item {
    public FantasyBraceletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            ItemStack itemstack = player.getItemInHand(hand);

            BlockHitResult hitresult = getPlayerPOVHitResult(serverLevel, player, ClipContext.Fluid.NONE);
            if (hitresult.getType() == HitResult.Type.MISS) {
                return InteractionResult.PASS;
            } else {
                if (hitresult.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hitresult.getBlockPos();
                    var save = Levelhandler.getSaveData(serverLevel);
                    if (itemstack.getDamageValue() != 0) {
                        var oldPos = save.remove(pos);
                        if (oldPos != null) {
                            if (!player.isCreative())
                                itemstack.setDamageValue(0);
                            level.removeBlock(pos, false);
                        }

                    } else {
                        if (!player.isCreative())
                            itemstack.setDamageValue(1);
                        // save.remove(pos);
                        // save.update(pos, save.getOrDefault(pos));
                        // level.removeBlock(pos, false);
                        level.setBlockAndUpdate(pos, ModContents.one_stone.get().defaultBlockState());
                    }

                    // if (!player.isCreative())
                    //     player.setItemInHand(hand, itemstack);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.PASS;
                }
            }

        }
        return super.use(level, player, hand);
    }



    // @Override
    // public String getDescriptionId(ItemStack stack) {
    //     if (stack.getDamageValue() == 1)
    //         return super.getDescriptionId(stack) + "_1";
    //     return super.getDescriptionId(stack);
    // }
}
