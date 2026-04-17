package xueluoanping.oneblock.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import xueluoanping.oneblock.OneBlock;
import xueluoanping.oneblock.handler.Levelhandler;
import xueluoanping.oneblock.util.ClientUtils;


public class BlockOne extends Block {

    public BlockOne(Properties properties) {
        super(properties);
    }

    // Add all the properties here, or may cause a null point exception.
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> blockBlockStateBuilder) {
        super.createBlockStateDefinition(blockBlockStateBuilder);
    }

    @Override
    public RenderShape getRenderShape(BlockState p_149645_1_) {
        return RenderShape.INVISIBLE;
    }

    // listen neighbour
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }
    // @Override
    // public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    //     return new BlockEntityOne( pos, state);
    // }

    // trigger whenever on place
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState state1, boolean p_60570_) {
        super.onPlace(state, level, pos, state1, p_60570_);

        if (level instanceof ServerLevel serverLevel) {
            // it may be clear but be cautious on remove method here so we have a fallon to help it
            serverLevel.scheduleTick(pos, this, 1);
        }
    }

    // do work, wait randomtick if not trigger onplace
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        super.tick(state, level, pos, randomSource);
        // if (level instanceof ServerLevel serverLevel)
        {
            OneBlock.logger(pos, "Loading a stage");
            var save = Levelhandler.getSaveData(level);
            save.remove(pos);
            save.update(pos, save.getOrDefault(pos));
            level.removeBlock(pos, false);
            ClientUtils.playFireWorkParticles(level, pos);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        if (level instanceof ServerLevel serverLevel)
            this.tick(state, serverLevel, pos, level.getRandom());
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos pos, RandomSource randomSource) {
        super.animateTick(blockState, level, pos, randomSource);
        // var vec=new Vec3(pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5);
        // ParticleUtils.spawnParticleOnFace(level,pos,Direction.DOWN,ParticleTypes.FIREWORK,vec,0.1F)

    }


}
