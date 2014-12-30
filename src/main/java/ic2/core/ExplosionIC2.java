package ic2.core;

import ic2.api.tile.ExplosionWhitelist;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.network.NetworkManager;
import ic2.core.util.ItemStackWrapper;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class ExplosionIC2 extends Explosion
{
	private final Random ExplosionRNG;
	private final World worldObj;
	private final int mapHeight;
	private final float power;
	private final float explosionDropRate;
	private final Type type;
	private final int radiationRange;
	private final EntityLivingBase igniter;
	private final List<Entry<Integer, Entity>> entitiesInRange;
	private final Map<ChunkPosition, Boolean> destroyedBlockPositions;
	private ChunkCache chunkCache;
	private static final double dropPowerLimit = 8.0D;
	private static final double damageAtDropPowerLimit = 32.0D;
	private static final double accelerationAtDropPowerLimit = 0.7D;
	private static final double motionLimit = 60.0D;
	private static final int secondaryRayCount = 5;

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop)
	{
		this(world, entity, x, y, z, power, drop, Type.Normal);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop, Type type)
	{
		this(world, entity, x, y, z, power, drop, type, (EntityLivingBase) null, 0);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop, Type type, EntityLivingBase igniter, int radiationRange)
	{
		super(world, entity, x, y, z, power);
		this.ExplosionRNG = new Random();
		this.entitiesInRange = new ArrayList();
		this.destroyedBlockPositions = new HashMap();
		this.worldObj = world;
		this.mapHeight = IC2.getWorldHeight(world);
		this.exploder = entity;
		this.power = power;
		this.explosionDropRate = drop;
		this.explosionX = x;
		this.explosionY = y;
		this.explosionZ = z;
		this.type = type;
		this.igniter = igniter;
		this.radiationRange = radiationRange;
	}

	public void doExplosion()
	{
		// TODO gamerforEA code start
		if (this.worldObj != null) return;
		// TODO gamerforEA code end
		if (this.power > 0.0F)
		{
			double maxDistance = (double) this.power / 0.4D;
			int maxDistanceInt = (int) Math.ceil(maxDistance);
			this.chunkCache = new ChunkCache(this.worldObj, (int) this.explosionX - maxDistanceInt, (int) this.explosionY - maxDistanceInt, (int) this.explosionZ - maxDistanceInt, (int) this.explosionX + maxDistanceInt, (int) this.explosionY + maxDistanceInt, (int) this.explosionZ + maxDistanceInt, 0);

			for (Entity entity : (List<Entity>) this.worldObj.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(this.explosionX - maxDistance, this.explosionY - maxDistance, this.explosionZ - maxDistance, this.explosionX + maxDistance, this.explosionY + maxDistance, this.explosionZ + maxDistance)))
			{
				if (entity instanceof EntityLivingBase || entity instanceof EntityItem)
				{
					this.entitiesInRange.add(new SimpleEntry(Integer.valueOf((int) ((entity.posX - this.explosionX) * (entity.posX - this.explosionX) + (entity.posY - this.explosionY) * (entity.posY - this.explosionY) + (entity.posZ - this.explosionZ) * (entity.posZ - this.explosionZ))), entity));
				}
			}

			boolean empty = !this.entitiesInRange.isEmpty();
			if (!this.entitiesInRange.isEmpty())
			{
				Collections.sort(this.entitiesInRange, new Comparator<Entry<Integer, Entity>>()
				{
					@Override
					public int compare(Entry<Integer, Entity> a, Entry<Integer, Entity> b)
					{
						return a.getKey() - b.getKey();
					}
				});
			}

			int distance = (int) Math.ceil(3.141592653589793D / Math.atan(1.0D / maxDistance));

			double d;
			for (int blocksToDrop = 0; blocksToDrop < 2 * distance; ++blocksToDrop)
			{
				for (int i = 0; i < distance; ++i)
				{
					d = 6.283185307179586D / (double) distance * (double) blocksToDrop;
					double d1 = 3.141592653589793D / (double) distance * (double) i;
					this.shootRay(this.explosionX, this.explosionY, this.explosionZ, d, d1, (double) this.power, empty && blocksToDrop % 8 == 0 && i % 8 == 0);
				}
			}

			for (Entry<Integer, Entity> entry : this.entitiesInRange)
			{
				Entity entity = entry.getValue();
				double pos = entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ;
				if (pos > 3600.0D)
				{
					double d1 = Math.sqrt(3600.0D / pos);
					entity.motionX *= d1;
					entity.motionY *= d1;
					entity.motionZ *= d1;
				}
			}

			int y;
			int z;
			if (this.isNuclear() && this.radiationRange >= 1)
			{
				for (EntityLiving entity : (List<EntityLiving>) this.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(this.explosionX - (double) this.radiationRange, this.explosionY - (double) this.radiationRange, this.explosionZ - (double) this.radiationRange, this.explosionX + (double) this.radiationRange, this.explosionY + (double) this.radiationRange, this.explosionZ + (double) this.radiationRange)))
				{
					if (!ItemArmorHazmat.hasCompleteHazmat(entity))
					{
						d = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ);
						y = (int) (120.0D * ((double) this.radiationRange - d));
						z = (int) (80.0D * ((double) (this.radiationRange / 3) - d));
						if (y >= 0)
						{
							entity.addPotionEffect(new PotionEffect(Potion.hunger.id, y, 0));
						}

						if (z >= 0)
						{
							IC2Potion.radiation.applyTo(entity, z, 0);
						}
					}
				}
			}

			((NetworkManager) IC2.network.get()).initiateExplosionEffect(this.worldObj, this.explosionX, this.explosionY, this.explosionZ);
			Map<XZposition, Map<ItemStackWrapper, DropData>> dropMap = new HashMap();

			for (Entry<ChunkPosition, Boolean> entry : this.destroyedBlockPositions.entrySet())
			{
				int x = ((ChunkPosition) entry.getKey()).chunkPosX;
				y = ((ChunkPosition) entry.getKey()).chunkPosY;
				z = ((ChunkPosition) entry.getKey()).chunkPosZ;
				Block block = this.chunkCache.getBlock(x, y, z);
				if ((Boolean) entry.getValue())
				{
					double count = (double) ((float) x + this.worldObj.rand.nextFloat());
					double entityitem = (double) ((float) y + this.worldObj.rand.nextFloat());
					double effectZ = (double) ((float) z + this.worldObj.rand.nextFloat());
					double d3 = count - this.explosionX;
					double d4 = entityitem - this.explosionY;
					double d5 = effectZ - this.explosionZ;
					double effectDistance = (double) MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
					d3 /= effectDistance;
					d4 /= effectDistance;
					d5 /= effectDistance;
					double d7 = 0.5D / (effectDistance / (double) this.power + 0.1D);
					d7 *= (double) (this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F);
					d3 *= d7;
					d4 *= d7;
					d5 *= d7;
					this.worldObj.spawnParticle("explode", (count + this.explosionX) / 2.0D, (entityitem + this.explosionY) / 2.0D, (effectZ + this.explosionZ) / 2.0D, d3, d4, d5);
					this.worldObj.spawnParticle("smoke", count, entityitem, effectZ, d3, d4, d5);
					int meta = this.worldObj.getBlockMetadata(x, y, z);
					Iterator<ItemStack> iterator = block.getDrops(this.worldObj, x, y, z, meta, 0).iterator();

					while (iterator.hasNext())
					{
						ItemStack itemStack = iterator.next();
						if (this.worldObj.rand.nextFloat() <= this.explosionDropRate)
						{
							XZposition xZposition = new XZposition(x / 2, z / 2);
							if (!dropMap.containsKey(xZposition))
							{
								dropMap.put(xZposition, new HashMap());
							}

							Map<ItemStackWrapper, DropData> map = dropMap.get(xZposition);
							ItemStackWrapper isw = new ItemStackWrapper(itemStack);
							if (!map.containsKey(isw))
							{
								map.put(isw, new DropData(itemStack.stackSize, y));
							}
							else
							{
								map.put(isw, (map.get(isw)).add(itemStack.stackSize, y));
							}
						}
					}
				}

				this.worldObj.setBlockToAir(x, y, z);
				block.onBlockDestroyedByExplosion(this.worldObj, x, y, z, this);
			}

			for (Entry<XZposition, Map<ItemStackWrapper, DropData>> entry : dropMap.entrySet())
			{
				XZposition pos = entry.getKey();

				for (Entry<ItemStackWrapper, DropData> entry1 : entry.getValue().entrySet())
				{
					ItemStackWrapper isw = (ItemStackWrapper) entry1.getKey();

					int stackSize;
					for (int i = ((DropData) entry1.getValue()).n; i > 0; i -= stackSize)
					{
						stackSize = Math.min(i, 64);
						EntityItem item = new EntityItem(this.worldObj, (double) ((float) pos.x + this.worldObj.rand.nextFloat()) * 2.0D, (double) ((DropData) entry1.getValue()).maxY + 0.5D, (double) ((float) pos.z + this.worldObj.rand.nextFloat()) * 2.0D, StackUtil.copyWithSize(isw.stack, stackSize));
						item.delayBeforeCanPickup = 10;
						this.worldObj.spawnEntityInWorld(item);
					}
				}
			}
		}
	}

	public void destroy(int x, int y, int z)
	{
		// TODO gamerforEA code start
		if (this.worldObj != null) return;
		// TODO gamerforEA code end
		ChunkPosition position = new ChunkPosition(x, y, z);
		if (!this.destroyedBlockPositions.containsKey(position))
		{
			this.destroyedBlockPositions.put(position, Boolean.valueOf(true));
		}
	}

	private void shootRay(double x, double y, double z, double phi, double theta, double power1, boolean killEntities)
	{
		double deltaX = Math.sin(theta) * Math.cos(phi);
		double deltaY = Math.cos(theta);
		double deltaZ = Math.sin(theta) * Math.sin(phi);
		int step = 0;

		while (true)
		{
			int blockY = Util.roundToNegInf(y);
			if (blockY < 0 || blockY >= this.mapHeight)
			{
				break;
			}

			int blockX = Util.roundToNegInf(x);
			int blockZ = Util.roundToNegInf(z);
			Block block = this.chunkCache.getBlock(blockX, blockY, blockZ);
			double absorption = this.getAbsorption(block, blockX, blockY, blockZ);
			if (absorption > 1000.0D && !ExplosionWhitelist.isBlockWhitelisted(block))
			{
				absorption = 0.5D;
			}
			else
			{
				if (absorption > power1)
				{
					break;
				}

				if (!block.isAir(this.worldObj, blockX, blockY, blockZ))
				{
					ChunkPosition i = new ChunkPosition(blockX, blockY, blockZ);
					if (power1 > 8.0D)
					{
						this.destroyedBlockPositions.put(i, Boolean.valueOf(false));
					}
					else if (!this.destroyedBlockPositions.containsKey(i))
					{
						this.destroyedBlockPositions.put(i, Boolean.valueOf(true));
					}
				}
			}

			int var39;
			if (killEntities && (step + 4) % 8 == 0 && !this.entitiesInRange.isEmpty() && power1 >= 0.25D)
			{
				int i1;
				if (step != 4)
				{
					i1 = step * step - 25;
					int entity = 0;
					int dx = this.entitiesInRange.size() - 1;

					do
					{
						var39 = (entity + dx) / 2;
						int distance = ((Integer) ((Entry) this.entitiesInRange.get(var39)).getKey()).intValue();
						if (distance < i1)
						{
							entity = var39 + 1;
						}
						else if (distance > i1)
						{
							dx = var39 - 1;
						}
						else
						{
							dx = var39;
						}
					}
					while (entity < dx);
				}
				else
				{
					var39 = 0;
				}

				int distanceMax = step * step + 25;

				for (i1 = var39; i1 < this.entitiesInRange.size() && ((Integer) ((Entry) this.entitiesInRange.get(var39)).getKey()).intValue() < distanceMax; ++i1)
				{
					Entity var40 = (Entity) ((Entry) this.entitiesInRange.get(var39)).getValue();
					if ((var40.posX - x) * (var40.posX - x) + (var40.posY - y) * (var40.posY - y) + (var40.posZ - z) * (var40.posZ - z) <= 25.0D)
					{
						var40.attackEntityFrom(this.getDamageSource(), (float) ((int) (32.0D * power1 / 8.0D)));
						if (var40 instanceof EntityPlayer)
						{
							EntityPlayer var41 = (EntityPlayer) var40;
							if (this.isNuclear() && this.igniter != null && var41 == this.igniter && var41.getHealth() <= 0.0F)
							{
								IC2.achievements.issueAchievement(var41, "dieFromOwnNuke");
							}
						}

						double var42 = var40.posX - this.explosionX;
						double dy = var40.posY - this.explosionY;
						double dz = var40.posZ - this.explosionZ;
						double distance1 = Math.sqrt(var42 * var42 + dy * dy + dz * dz);
						var40.motionX += var42 / distance1 * 0.7D * power1 / 8.0D;
						var40.motionY += dy / distance1 * 0.7D * power1 / 8.0D;
						var40.motionZ += dz / distance1 * 0.7D * power1 / 8.0D;
						if (!var40.isEntityAlive())
						{
							this.entitiesInRange.remove(i1);
							--i1;
						}
					}
				}
			}

			if (absorption > 10.0D)
			{
				for (var39 = 0; var39 < 5; ++var39)
				{
					this.shootRay(x, y, z, this.ExplosionRNG.nextDouble() * 2.0D * 3.141592653589793D, this.ExplosionRNG.nextDouble() * 3.141592653589793D, absorption * 0.4D, false);
				}
			}

			power1 -= absorption;
			x += deltaX;
			y += deltaY;
			z += deltaZ;
			++step;
		}

	}

	private double getAbsorption(Block block, int x, int y, int z)
	{
		double ret = 0.5D;
		if (block.isAir(this.worldObj, x, y, z))
		{
			return ret;
		}
		else
		{
			if ((block == Blocks.water || block == Blocks.flowing_water) && this.type != Type.Normal)
			{
				++ret;
			}
			else
			{
				double extra = (double) (block.getExplosionResistance(this.exploder, this.worldObj, x, y, z, this.explosionX, this.explosionY, this.explosionZ) + 4.0F) * 0.3D;
				if (this.type != Type.Heat)
				{
					ret += extra;
				}
				else
				{
					ret += extra * 6.0D;
				}
			}

			return ret;
		}
	}

	@Override
	public EntityLivingBase getExplosivePlacedBy()
	{
		return this.igniter;
	}

	private DamageSource getDamageSource()
	{
		return this.isNuclear() ? IC2DamageSource.setNukeSource(this) : DamageSource.setExplosionSource(this);
	}

	private boolean isNuclear()
	{
		return this.type == Type.Nuclear;
	}

	public static enum Type
	{
		Normal, Heat, Nuclear;
	}

	private static class DropData
	{
		int n;
		int maxY;

		DropData(int n1, int y)
		{
			this.n = n1;
			this.maxY = y;
		}

		public DropData add(int n1, int y)
		{
			this.n += n1;
			if (y > this.maxY)
			{
				this.maxY = y;
			}

			return this;
		}
	}

	private static class XZposition
	{
		int x;
		int z;

		XZposition(int x1, int z1)
		{
			this.x = x1;
			this.z = z1;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof XZposition))
			{
				return false;
			}
			else
			{
				XZposition xZposition = (XZposition) obj;
				return xZposition.x == this.x && xZposition.z == this.z;
			}
		}

		@Override
		public int hashCode()
		{
			return this.x * 31 ^ this.z;
		}
	}
}