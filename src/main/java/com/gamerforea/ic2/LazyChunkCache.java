package com.gamerforea.ic2;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.util.ForgeDirection;

public final class LazyChunkCache implements IBlockAccess
{
	private final World world;
	private final int minChunkX;
	private final int minChunkZ;
	private final boolean forceLoadChunks;
	private final Chunk[][] chunkArray;
	private final boolean[][] visitedArray;

	public LazyChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
	{
		this(world, minX, minY, minZ, maxX, maxY, maxZ, true);
	}

	public LazyChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean forceLoadChunks)
	{
		this(world, minX, minY, minZ, maxX, maxY, maxZ, 0, forceLoadChunks);
	}

	public LazyChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int expandDistance)
	{
		this(world, minX, minY, minZ, maxX, maxY, maxZ, expandDistance, true);
	}

	public LazyChunkCache(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int expandDistance, boolean forceLoadChunks)
	{
		this.world = world;
		this.minChunkX = minX - expandDistance >> 4;
		this.minChunkZ = minZ - expandDistance >> 4;
		this.forceLoadChunks = forceLoadChunks;
		int maxChunkX = maxX + expandDistance >> 4;
		int maxChunkZ = maxZ + expandDistance >> 4;
		this.chunkArray = new Chunk[maxChunkX - this.minChunkX + 1][maxChunkZ - this.minChunkZ + 1];
		this.visitedArray = new boolean[maxChunkX - this.minChunkX + 1][maxChunkZ - this.minChunkZ + 1];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean extendedLevelsInChunkCache()
	{
		// TODO Implement this
		return false;
	}

	@Override
	public Block getBlock(int x, int y, int z)
	{
		Block block = Blocks.air;

		if (isValidY(y))
		{
			Chunk chunk = this.getChunkFromBlockCoords(x, z);
			if (chunk != null)
				block = chunk.getBlock(x & 15, y, z & 15);
		}

		return block;
	}

	@Override
	public TileEntity getTileEntity(int x, int y, int z)
	{
		if (isValidY(y))
		{
			Chunk chunk = this.getChunkFromBlockCoords(x, z);
			if (chunk != null)
				return chunk.func_150806_e(x & 15, y, z & 15);
		}

		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minBlockBrightness)
	{
		int skyBrightness = this.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z);
		int blockBrightness = this.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z);

		if (blockBrightness < minBlockBrightness)
			blockBrightness = minBlockBrightness;

		return skyBrightness << 20 | blockBrightness << 4;
	}

	@Override
	public int getBlockMetadata(int x, int y, int z)
	{
		if (isValidY(y))
		{
			Chunk chunk = this.getChunkFromBlockCoords(x, z);
			if (chunk != null)
				return chunk.getBlockMetadata(x & 15, y, z & 15);
		}

		return 0;
	}

	@Override
	public int isBlockProvidingPowerTo(int x, int y, int z, int p_72879_4_)
	{
		return this.getBlock(x, y, z).isProvidingStrongPower(this, x, y, z, p_72879_4_);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BiomeGenBase getBiomeGenForCoords(int x, int z)
	{
		return this.world.getBiomeGenForCoords(x, z);
	}

	@Override
	public boolean isAirBlock(int x, int y, int z)
	{
		return this.getBlock(x, y, z).isAir(this, x, y, z);
	}

	@SideOnly(Side.CLIENT)
	public int getSkyBlockTypeBrightness(EnumSkyBlock skyBlock, int x, int y, int z)
	{
		if (skyBlock == EnumSkyBlock.Sky && this.world.provider.hasNoSky)
			return 0;

		Chunk chunk = this.getChunkFromBlockCoords(x, z);
		if (chunk != null)
		{
			y = MathHelper.clamp_int(y, 0, 255);
			if (this.getBlock(x, y, z).getUseNeighborBrightness())
			{
				int brightness = this.getSpecialBlockBrightness(skyBlock, x, y + 1, z);
				int eastBrightness = this.getSpecialBlockBrightness(skyBlock, x + 1, y, z);
				int westBrightness = this.getSpecialBlockBrightness(skyBlock, x - 1, y, z);
				int southBrightness = this.getSpecialBlockBrightness(skyBlock, x, y, z + 1);
				int northBrightness = this.getSpecialBlockBrightness(skyBlock, x, y, z - 1);

				if (eastBrightness > brightness)
					brightness = eastBrightness;
				if (westBrightness > brightness)
					brightness = westBrightness;
				if (southBrightness > brightness)
					brightness = southBrightness;
				if (northBrightness > brightness)
					brightness = northBrightness;

				return brightness;
			}

			return chunk.getSavedLightValue(skyBlock, x & 15, y, z & 15);
		}

		return skyBlock.defaultLightValue;
	}

	@SideOnly(Side.CLIENT)
	public int getSpecialBlockBrightness(EnumSkyBlock skyBlock, int x, int y, int z)
	{
		y = MathHelper.clamp_int(y, 0, 255);
		Chunk chunk = this.getChunkFromBlockCoords(x, z);
		return chunk == null ? skyBlock.defaultLightValue : chunk.getSavedLightValue(skyBlock, x & 15, y, z & 15);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getHeight()
	{
		return 256;
	}

	@Override
	public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default)
	{
		return isValidXZ(x, z) ? this.getBlock(x, y, z).isSideSolid(this, x, y, z, side) : _default;
	}

	public Chunk getChunkFromBlockCoords(int x, int z)
	{
		return isValidXZ(x, z) ? this.getChunkFromChunkCoords(x >> 4, z >> 4) : null;
	}

	public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ)
	{
		int xIndex = chunkX - this.minChunkX;
		if (xIndex >= 0 && xIndex < this.chunkArray.length)
		{
			int zIndex = chunkZ - this.minChunkZ;
			if (zIndex >= 0)
			{
				Chunk[] chunksLine = this.chunkArray[xIndex];
				if (zIndex < chunksLine.length)
				{
					boolean[] visitedLine = this.visitedArray[xIndex];
					if (!visitedLine[zIndex])
					{
						visitedLine[zIndex] = true;

						if (this.forceLoadChunks)
							chunksLine[zIndex] = this.world.getChunkFromChunkCoords(chunkX, chunkZ);
						else
						{
							IChunkProvider chunkProvider = this.world.getChunkProvider();
							if (chunkProvider.chunkExists(chunkX, chunkZ))
								chunksLine[zIndex] = chunkProvider.provideChunk(chunkX, chunkZ);
						}
					}

					return chunksLine[zIndex];
				}
			}
		}

		return null;
	}

	private static boolean isValidXZ(int x, int z)
	{
		return x >= -30000000 && z >= -30000000 && x < 30000000 && z <= 30000000;
	}

	private static boolean isValidY(int y)
	{
		return y >= 0 && y < 256;
	}
}
