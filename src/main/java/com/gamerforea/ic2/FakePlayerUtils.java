package com.gamerforea.ic2;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.gamerforea.wgew.cauldron.event.CauldronBlockBreakEvent;
import com.gamerforea.wgew.cauldron.event.CauldronEntityDamageByEntityEvent;
import com.gamerforea.wgew.cauldron.event.CauldronIsInPrivateEvent;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class FakePlayerUtils
{
	private static WeakReference<FakePlayer> player = new WeakReference<FakePlayer>(null);

	public static final FakePlayer getModFake(World world)
	{
		if (player.get() == null)
		{
			GameProfile profile = new GameProfile(UUID.fromString("6c788982-d6ca-11e4-b9d6-1681e6b88ec1"), "[IC2]");
			player = new WeakReference<FakePlayer>(create(world, profile));
		}
		else player.get().worldObj = world;

		return player.get();
	}

	public static FakePlayer create(World world, GameProfile profile)
	{
		return FakePlayerFactory.get((WorldServer) world, profile);
	}

	public static boolean cantBreak(int x, int y, int z, EntityPlayer player)
	{
		try
		{
			CauldronBlockBreakEvent event = new CauldronBlockBreakEvent(player, x, y, z);
			Bukkit.getServer().getPluginManager().callEvent(event);
			return event.getBukkitEvent().isCancelled();
		}
		catch (Throwable throwable)
		{
			GameProfile profile = player.getGameProfile();
			System.err.println(String.format("Failed call CauldronBlockBreakEvent [Name: %s, UUID: %s, X: %d, Y: %d, Z: %d]", profile.getName(), profile.getId().toString(), x, y, z));
			return true;
		}
	}

	public static boolean cantDamage(Entity damager, Entity damagee)
	{
		try
		{
			CauldronEntityDamageByEntityEvent event = new CauldronEntityDamageByEntityEvent(damager, damagee, DamageCause.ENTITY_ATTACK, 1D);
			Bukkit.getServer().getPluginManager().callEvent(event);
			return event.getBukkitEvent().isCancelled();
		}
		catch (Throwable throwable)
		{
			System.err.println(String.format("Failed call CauldronEntityDamageByEntityEvent [Damager UUID: %s, Damagee UUID: %s]", damager.getUniqueID().toString(), damagee.getUniqueID().toString()));
			return true;
		}
	}

	public static boolean isInPrivate(World world, int x, int y, int z)
	{
		try
		{
			CauldronIsInPrivateEvent event = new CauldronIsInPrivateEvent(world, x, y, z);
			Bukkit.getServer().getPluginManager().callEvent(event);
			return event.isInPrivate;
		}
		catch (Throwable throwable)
		{
			System.err.println(String.format("Failed call CauldronIsInPrivateEvent [World: %s, X: %d, Y: %d, Z: %d]", world.getWorldInfo().getWorldName(), x, y, z));
			return true;
		}
	}
}