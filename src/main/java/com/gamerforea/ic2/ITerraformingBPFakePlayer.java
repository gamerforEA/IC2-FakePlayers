package com.gamerforea.ic2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public interface ITerraformingBPFakePlayer
{
	/**
	 * Perform the terraforming operation.
	 *
	 * @param world  world to terraform
	 * @param x      X position to terraform
	 * @param z      Z position to terraform
	 * @param yCoord Y position of the terraformer
	 * @param player Terraformer owner
	 * @return Whether the operation was successful and the terraformer should
	 * consume energy.
	 */
	boolean terraform(World world, int x, int z, int yCoord, EntityPlayer player);
}