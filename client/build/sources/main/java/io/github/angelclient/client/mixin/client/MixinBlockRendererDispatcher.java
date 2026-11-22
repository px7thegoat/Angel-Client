package io.github.angelclient.client.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.angelclient.client.mod.impl.PerformanceMod;

import java.util.HashSet;
import java.util.Set;

@Mixin(BlockRendererDispatcher.class)
public class MixinBlockRendererDispatcher {

    // Lista otimizada de blocos que serão ocultados
    private static final Set<Block> FOLIAGE_BLOCKS = new HashSet<>();

    static {
        FOLIAGE_BLOCKS.add(Blocks.tallgrass);
        FOLIAGE_BLOCKS.add(Blocks.double_plant);
        FOLIAGE_BLOCKS.add(Blocks.yellow_flower);
        FOLIAGE_BLOCKS.add(Blocks.red_flower);
        FOLIAGE_BLOCKS.add(Blocks.deadbush);
        FOLIAGE_BLOCKS.add(Blocks.waterlily);
        FOLIAGE_BLOCKS.add(Blocks.vine);
        FOLIAGE_BLOCKS.add(Blocks.wheat);
        FOLIAGE_BLOCKS.add(Blocks.carrots);
        FOLIAGE_BLOCKS.add(Blocks.potatoes);
        FOLIAGE_BLOCKS.add(Blocks.pumpkin_stem);
        FOLIAGE_BLOCKS.add(Blocks.melon_stem);
        FOLIAGE_BLOCKS.add(Blocks.nether_wart);
        FOLIAGE_BLOCKS.add(Blocks.reeds);
        FOLIAGE_BLOCKS.add(Blocks.sapling);
        FOLIAGE_BLOCKS.add(Blocks.red_mushroom);
        FOLIAGE_BLOCKS.add(Blocks.brown_mushroom);
        FOLIAGE_BLOCKS.add(Blocks.cactus);
    }
    
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    public void onRenderBlock(IBlockState state, BlockPos pos, IBlockAccess world, 
                              WorldRenderer renderer, CallbackInfoReturnable<Boolean> cir) {
        
        Block block = state.getBlock();

        // Verifica se a opção no client está ativada
        if (PerformanceMod.instance != null) {
            // Opções originais (noFoliage)
            if (PerformanceMod.instance.noFoliage && FOLIAGE_BLOCKS.contains(block)) {
                cir.setReturnValue(false);
                return;
            }

            // Opções Badlion Visuais
            if (PerformanceMod.instance.isEnabled()) {
                if (PerformanceMod.instance.hideTallGrass && block instanceof net.minecraft.block.BlockTallGrass) {
                    cir.setReturnValue(false);
                } else if (PerformanceMod.instance.hideDoubleTallGrass && block instanceof net.minecraft.block.BlockDoublePlant && ((net.minecraft.block.BlockDoublePlant) block).getVariant(world, pos) == net.minecraft.block.BlockDoublePlant.EnumPlantType.GRASS) {
                    cir.setReturnValue(false);
                } else if (PerformanceMod.instance.hideFlowers && (block instanceof net.minecraft.block.BlockFlower || block instanceof net.minecraft.block.BlockMushroom)) {
                    cir.setReturnValue(false);
                } else if (PerformanceMod.instance.hideDoubleTallFlowers && block instanceof net.minecraft.block.BlockDoublePlant && ((net.minecraft.block.BlockDoublePlant) block).getVariant(world, pos) != net.minecraft.block.BlockDoublePlant.EnumPlantType.GRASS && ((net.minecraft.block.BlockDoublePlant) block).getVariant(world, pos) != net.minecraft.block.BlockDoublePlant.EnumPlantType.FERN) {
                    cir.setReturnValue(false);
                } else if (PerformanceMod.instance.hideFences && (block instanceof net.minecraft.block.BlockFence || block instanceof net.minecraft.block.BlockWall)) {
                    cir.setReturnValue(false);
                } else if (PerformanceMod.instance.hideFenceGates && block instanceof net.minecraft.block.BlockFenceGate) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}