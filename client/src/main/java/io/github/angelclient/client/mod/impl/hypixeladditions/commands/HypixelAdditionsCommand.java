package io.github.angelclient.client.mod.impl.hypixeladditions.commands;

import io.github.angelclient.client.mod.impl.hypixeladditions.HypixelAdditionsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;

public abstract class HypixelAdditionsCommand extends CommandBase {

	protected final HypixelAdditionsMod mod;
	protected final Minecraft mc = Minecraft.getMinecraft();
	
	public HypixelAdditionsCommand(io.github.angelclient.client.mod.impl.hypixeladditions.HypixelAdditionsMod mod) { this.mod=mod; }

}