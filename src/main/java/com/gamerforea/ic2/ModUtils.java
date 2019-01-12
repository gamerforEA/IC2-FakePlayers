package com.gamerforea.ic2;

import com.gamerforea.eventhelper.nexus.ModNexus;
import com.gamerforea.eventhelper.nexus.ModNexusFactory;
import com.gamerforea.eventhelper.nexus.NexusUtils;
import com.gamerforea.eventhelper.util.EventUtils;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;

@ModNexus(name = "IC2", uuid = "6c788982-d6ca-11e4-b9d6-1681e6b88ec1")
public final class ModUtils
{
	public static final ModNexusFactory NEXUS_FACTORY = NexusUtils.getFactory();

	public static boolean cantBreakOrNotInPrivate(@Nonnull EntityPlayer player, int x, int y, int z)
	{
		return EventConfig.terraRegionOnly && !EventUtils.isInPrivate(player.worldObj, x, y, z) || EventUtils.cantBreak(player, x, y, z);
	}
}