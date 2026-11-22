package com.replaymod.replay.mixin;

import com.replaymod.extras.advancedscreenshots.AdvancedScreenshots;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Em 1.8.9 não existe dispatchKeypresses — o screenshot é processado dentro de runTick.
@Mixin(value = {Minecraft.class}, remap = false)
public abstract class MixinKeyboardListener {
   @Inject(
      method = {"func_71407_l"},
      at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ScreenShotHelper;saveScreenshot(Ljava/io/File;IILnet/minecraft/client/shader/Framebuffer;)Lnet/minecraft/util/IChatComponent;",
            remap = false
         )},
      cancellable = true,
      remap = false
   )
   private void takeScreenshot(CallbackInfo ci) {
      if(ReplayModReplay.instance != null && ReplayModReplay.instance.getReplayHandler() != null) {
         AdvancedScreenshots.take();
         ci.cancel();
      }
   }
}
