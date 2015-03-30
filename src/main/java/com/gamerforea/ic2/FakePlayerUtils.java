package com.gamerforea.ic2;

import java.lang.ref.WeakReference;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.gamerforea.wgew.cauldron.event.CauldronBlockBreakEvent;
import com.gamerforea.wgew.cauldron.event.CauldronEntityDamageByEntityEvent;
import com.mojang.authlib.GameProfile;

public final class FakePlayerUtils
{
	private static WeakReference<FakePlayer> player = new WeakReference<FakePlayer>(null);

	private static WeakReference<FakePlayer> createNewPlayer(World world)
	{
		return new WeakReference<FakePlayer>(createFakePlayer(UUID.fromString("6c788982-d6ca-11e4-b9d6-1681e6b88ec1"), "[IC2]", world));
	}

	public static final FakePlayer getPlayer(World world)
	{
		if (player.get() == null) player = createNewPlayer(world);
		else player.get().worldObj = world;

		return player.get();
	}

	public static FakePlayer createFakePlayer(UUID uuid, String name, World world)
	{
		return FakePlayerFactory.get((WorldServer) world, new GameProfile(uuid, name));
	}

	public static org.bukkit.event.block.BlockBreakEvent callBlockBreakEvent(int x, int y, int z, EntityPlayer player)
	{
		CauldronBlockBreakEvent event = new CauldronBlockBreakEvent(player, x, y, z);
		Bukkit.getServer().getPluginManager().callEvent(event);
		return event.getBukkitEvent();
	}

	public static org.bukkit.event.entity.EntityDamageByEntityEvent callEntityDamageByEntityEvent(Entity damager, Entity damagee, DamageCause cause, double damage)
	{
		CauldronEntityDamageByEntityEvent event = new CauldronEntityDamageByEntityEvent(damager, damagee, cause, damage);
		Bukkit.getServer().getPluginManager().callEvent(event);
		return event.getBukkitEvent();
	}
}