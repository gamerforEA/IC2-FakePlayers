package ic2.core;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ModUtils;
import ic2.api.event.ExplosionEvent;
import ic2.api.tile.ExplosionWhitelist;
import ic2.core.item.armor.ItemArmorHazmat;
import ic2.core.util.ItemStackWrapper;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
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
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.Map.Entry;

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
	private final double maxDistance;
	private final int areaSize;
	private final int areaX;
	private final int areaZ;
	private final DamageSource damageSource;
	private final List<EntityDamage> entitiesInRange;
	private final long[][] destroyedBlockPositions;
	private ChunkCache chunkCache;
	private static final double dropPowerLimit = 8.0D;
	private static final double damageAtDropPowerLimit = 32.0D;
	private static final double accelerationAtDropPowerLimit = 0.7D;
	private static final double motionLimit = 60.0D;
	private static final int secondaryRayCount = 5;
	private static final int bitSetElementSize = 2;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake;
	public boolean denyBlockBreak;
	// TODO gamerforEA code end

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop)
	{
		this(world, entity, x, y, z, power, drop, Type.Normal);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop, Type type)
	{
		this(world, entity, x, y, z, power, drop, type, null, 0);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop, Type type, EntityLivingBase igniter, int radiationRange)
	{
		super(world, entity, x, y, z, power);
		this.ExplosionRNG = new Random();
		this.entitiesInRange = new ArrayList();
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
		this.maxDistance = this.power / 0.4D;
		int maxDistanceInt = (int) Math.ceil(this.maxDistance);
		this.areaSize = maxDistanceInt * 2;
		this.areaX = Util.roundToNegInf(this.explosionX) - maxDistanceInt;
		this.areaZ = Util.roundToNegInf(this.explosionZ) - maxDistanceInt;
		if (this.isNuclear())
			this.damageSource = IC2DamageSource.getNukeSource(this);
		else
			this.damageSource = DamageSource.setExplosionSource(this);

		this.destroyedBlockPositions = new long[this.mapHeight][];

		// TODO gamerforEA code start
		this.fake = ModUtils.NEXUS_FACTORY.wrapFake(world);
		if (entity instanceof EntityPlayer)
			this.fake.setRealPlayer((EntityPlayer) entity);
		else if (igniter instanceof EntityPlayer)
			this.fake.setRealPlayer((EntityPlayer) igniter);
		// TODO gamerforEA code end
	}

	public void doExplosion()
	{
		if (this.power > 0.0F)
		{
			// TODO gamerforEA code start
			if (!EventConfig.explosionEnabled)
				return;
			// TODO gamerforEA code end

			ExplosionEvent event = new ExplosionEvent(this.worldObj, this.exploder, this.explosionX, this.explosionY, this.explosionZ, this.power, this.igniter, this.radiationRange, this.maxDistance);
			if (!MinecraftForge.EVENT_BUS.post(event))
			{
				this.chunkCache = new ChunkCache(this.worldObj, (int) this.explosionX - this.areaSize / 2, (int) this.explosionY - this.areaSize / 2, (int) this.explosionZ - this.areaSize / 2, (int) this.explosionX + this.areaSize / 2, (int) this.explosionY + this.areaSize / 2, (int) this.explosionZ + this.areaSize / 2, 0);

				for (Entity entity : (Iterable<Entity>) this.worldObj.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(this.explosionX - this.maxDistance, this.explosionY - this.maxDistance, this.explosionZ - this.maxDistance, this.explosionX + this.maxDistance, this.explosionY + this.maxDistance, this.explosionZ + this.maxDistance)))
				{
					if (entity instanceof EntityLivingBase || entity instanceof EntityItem)
					{
						int distance = (int) (Util.square(entity.posX - this.explosionX) + Util.square(entity.posY - this.explosionY) + Util.square(entity.posZ - this.explosionZ));
						double health = getEntityHealth(entity);
						this.entitiesInRange.add(new EntityDamage(entity, distance, health));
					}
				}

				boolean entitiesAreInRange = !this.entitiesInRange.isEmpty();
				if (entitiesAreInRange)
					this.entitiesInRange.sort(Comparator.comparingInt(a -> a.distance));

				int steps = (int) Math.ceil(3.141592653589793D / Math.atan(1.0D / this.maxDistance));

				for (int phi_n = 0; phi_n < 2 * steps; ++phi_n)
				{
					for (int theta_n = 0; theta_n < steps; ++theta_n)
					{
						double phi = 6.283185307179586D / steps * phi_n;
						double theta = 3.141592653589793D / steps * theta_n;
						this.shootRay(this.explosionX, this.explosionY, this.explosionZ, phi, theta, this.power, entitiesAreInRange && phi_n % 8 == 0 && theta_n % 8 == 0);
					}
				}

				for (EntityDamage entry : this.entitiesInRange)
				{
					Entity entity = entry.entity;

					// TODO gamerforEA code start
					if (EventConfig.explosionEvent && this.fake.cantDamage(entity))
						continue;
					// TODO gamerforEA code end

					entity.attackEntityFrom(this.damageSource, (float) entry.damage);
					if (entity instanceof EntityPlayer)
					{
						EntityPlayer entityPlayer = (EntityPlayer) entity;
						if (this.isNuclear() && this.igniter != null && entityPlayer == this.igniter && entityPlayer.getHealth() <= 0.0F)
							IC2.achievements.issueAchievement(entityPlayer, "dieFromOwnNuke");
					}

					double motionSq = Util.square(entry.motionX) + Util.square(entity.motionY) + Util.square(entity.motionZ);
					double reduction = motionSq > 3600.0D ? Math.sqrt(3600.0D / motionSq) : 1.0D;
					entity.motionX += entry.motionX * reduction;
					entity.motionY += entry.motionY * reduction;
					entity.motionZ += entry.motionZ * reduction;
				}

				if (this.isNuclear() && this.radiationRange >= 1)
					for (EntityLiving entity : (Iterable<EntityLiving>) this.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(this.explosionX - this.radiationRange, this.explosionY - this.radiationRange, this.explosionZ - this.radiationRange, this.explosionX + this.radiationRange, this.explosionY + this.radiationRange, this.explosionZ + this.radiationRange)))
					{
						if (!ItemArmorHazmat.hasCompleteHazmat(entity))
						{
							double distance = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ);
							int hungerLength = (int) (120.0D * (this.radiationRange - distance));
							int poisonLength = (int) (80.0D * (this.radiationRange / 3 - distance));
							if (hungerLength >= 0)
								entity.addPotionEffect(new PotionEffect(Potion.hunger.id, hungerLength, 0));

							if (poisonLength >= 0)
								IC2Potion.radiation.applyTo(entity, poisonLength, 0);
						}
					}

				IC2.network.get().initiateExplosionEffect(this.worldObj, this.explosionX, this.explosionY, this.explosionZ);
				Random rng = this.worldObj.rand;
				boolean doDrops = this.worldObj.getGameRules().getGameRuleBooleanValue("doTileDrops");
				Map<XZposition, Map<ItemStackWrapper, DropData>> blocksToDrop = new HashMap();
				this.worldObj.playSoundEffect(this.explosionX, this.explosionY, this.explosionZ, "random.explode", 4.0F, (1.0F + (rng.nextFloat() - rng.nextFloat()) * 0.2F) * 0.7F);

				for (int y = 0; y < this.destroyedBlockPositions.length; ++y)
				{
					long[] bitSet = this.destroyedBlockPositions[y];
					if (bitSet != null)
					{
						int index = -2;

						while ((index = nextSetIndex(index + 2, bitSet, 2)) != -1)
						{
							int realIndex = index / 2;
							int z = realIndex / this.areaSize;
							int x = realIndex - z * this.areaSize;
							x = x + this.areaX;
							z = z + this.areaZ;

							// TODO gamerforEA code start
							if (this.denyBlockBreak)
								continue;
							if (EventConfig.explosionRegionOnly && !EventUtils.isInPrivate(this.worldObj, x, y, z))
								continue;
							if (EventConfig.explosionEvent && this.fake.cantBreak(x, y, z))
								continue;
							// TODO gamerforEA code end

							Block block = this.chunkCache.getBlock(x, y, z);
							if (this.power < 20.0F)
							{
								double effectX = x + rng.nextFloat();
								double effectY = y + rng.nextFloat();
								double effectZ = z + rng.nextFloat();
								double d3 = effectX - this.explosionX;
								double d4 = effectY - this.explosionY;
								double d5 = effectZ - this.explosionZ;
								double effectDistance = MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
								d3 = d3 / effectDistance;
								d4 = d4 / effectDistance;
								d5 = d5 / effectDistance;
								double d7 = 0.5D / (effectDistance / this.power + 0.1D);
								d7 = d7 * (rng.nextFloat() * rng.nextFloat() + 0.3F);
								d3 = d3 * d7;
								d4 = d4 * d7;
								d5 = d5 * d7;
								this.worldObj.spawnParticle("explode", (effectX + this.explosionX) / 2.0D, (effectY + this.explosionY) / 2.0D, (effectZ + this.explosionZ) / 2.0D, d3, d4, d5);
								this.worldObj.spawnParticle("smoke", effectX, effectY, effectZ, d3, d4, d5);
							}

							if (doDrops && getAtIndex(index, bitSet, 2) == 1)
							{
								int meta = this.chunkCache.getBlockMetadata(x, y, z);

								for (ItemStack itemStack : block.getDrops(this.worldObj, x, y, z, meta, 0))
								{
									if (rng.nextFloat() <= this.explosionDropRate)
									{
										XZposition xZposition = new XZposition(x / 2, z / 2);
										if (!blocksToDrop.containsKey(xZposition))
											blocksToDrop.put(xZposition, new HashMap());

										Map<ItemStackWrapper, DropData> map = blocksToDrop.get(xZposition);
										ItemStackWrapper isw = new ItemStackWrapper(itemStack);
										if (!map.containsKey(isw))
											map.put(isw, new DropData(itemStack.stackSize, y));
										else
											map.put(isw, map.get(isw).add(itemStack.stackSize, y));
									}
								}
							}

							this.worldObj.setBlockToAir(x, y, z);
							block.onBlockDestroyedByExplosion(this.worldObj, x, y, z, this);
						}
					}
				}

				for (Entry<XZposition, Map<ItemStackWrapper, DropData>> entry : blocksToDrop.entrySet())
				{
					XZposition xZposition = entry.getKey();

					for (Entry<ItemStackWrapper, DropData> entry2 : entry.getValue().entrySet())
					{
						ItemStackWrapper isw = entry2.getKey();

						int stackSize;
						for (int count = entry2.getValue().n; count > 0; count -= stackSize)
						{
							stackSize = Math.min(count, 64);
							EntityItem entityitem = new EntityItem(this.worldObj, (xZposition.x + this.worldObj.rand.nextFloat()) * 2.0D, entry2.getValue().maxY + 0.5D, (xZposition.z + this.worldObj.rand.nextFloat()) * 2.0D, StackUtil.copyWithSize(isw.stack, stackSize));
							entityitem.delayBeforeCanPickup = 10;
							this.worldObj.spawnEntityInWorld(entityitem);
						}
					}
				}
			}
		}
	}

	public void destroy(int x, int y, int z, boolean noDrop)
	{
		this.destroyUnchecked(x, y, z, noDrop);
	}

	private void destroyUnchecked(int x, int y, int z, boolean noDrop)
	{
		int index = (z - this.areaZ) * this.areaSize + x - this.areaX;
		index = index * 2;
		long[] array = this.destroyedBlockPositions[y];
		if (array == null)
		{
			array = makeArray(Util.square(this.areaSize), 2);
			this.destroyedBlockPositions[y] = array;
		}

		if (noDrop)
			setAtIndex(index, array, 3);
		else
			setAtIndex(index, array, 1);
	}

	private void shootRay(double x, double y, double z, double phi, double theta, double power1, boolean killEntities)
	{
		/* TODO gamerforEA code optimize, old code:
		double deltaX = Math.sin(theta) * Math.cos(phi);
		double deltaY = Math.cos(theta);
		double deltaZ = Math.sin(theta) * Math.sin(phi); */
		float fPhi = (float) phi;
		float fTheta = (float) theta;
		float sinTheta = MathHelper.sin(fTheta);
		double deltaX = sinTheta * MathHelper.cos(fPhi);
		double deltaY = MathHelper.cos(fTheta);
		double deltaZ = sinTheta * MathHelper.sin(fPhi);
		// TODO gamerforEA code end

		int step = 0;

		while (true)
		{
			int blockY = Util.roundToNegInf(y);
			if (blockY < 0 || blockY >= this.mapHeight)
				break;

			int blockX = Util.roundToNegInf(x);
			int blockZ = Util.roundToNegInf(z);
			Block block = this.chunkCache.getBlock(blockX, blockY, blockZ);
			double absorption = this.getAbsorption(block, blockX, blockY, blockZ);
			if (absorption > 1000.0D && !ExplosionWhitelist.isBlockWhitelisted(block))
				absorption = 0.5D;
			else
			{
				if (absorption > power1)
					break;

				if (block == Blocks.stone || block != Blocks.air && !block.isAir(this.worldObj, blockX, blockY, blockZ))
					this.destroyUnchecked(blockX, blockY, blockZ, power1 > 8.0D);
			}

			if (killEntities && (step + 4) % 8 == 0 && !this.entitiesInRange.isEmpty() && power1 >= 0.25D)
				this.damageEntities(x, y, z, step, power1);

			if (absorption > 10.0D)
				for (int i = 0; i < 5; ++i)
				{
					this.shootRay(x, y, z, this.ExplosionRNG.nextDouble() * 2.0D * 3.141592653589793D, this.ExplosionRNG.nextDouble() * 3.141592653589793D, absorption * 0.4D, false);
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
		if (block != Blocks.air && !block.isAir(this.worldObj, x, y, z))
		{
			if ((block == Blocks.water || block == Blocks.flowing_water) && this.type != Type.Normal)
				++ret;
			else
			{
				double extra = (block.getExplosionResistance(this.exploder, this.worldObj, x, y, z, this.explosionX, this.explosionY, this.explosionZ) + 4.0F) * 0.3D;
				if (this.type != Type.Heat)
					ret += extra;
				else
					ret += extra * 6.0D;
			}

			return ret;
		}
		return ret;
	}

	private void damageEntities(double x, double y, double z, int step, double power)
	{
		int index;
		if (step != 4)
		{
			int distanceMin = Util.square(step - 5);
			int indexStart = 0;
			int indexEnd = this.entitiesInRange.size() - 1;

			do
			{
				index = (indexStart + indexEnd) / 2;
				int distance = this.entitiesInRange.get(index).distance;
				if (distance < distanceMin)
					indexStart = index + 1;
				else if (distance > distanceMin)
					indexEnd = index - 1;
				else
					indexEnd = index;

			}
			while (indexStart < indexEnd);
		}
		else
			index = 0;

		int distanceMax = Util.square(step + 5);

		for (int i = index; i < this.entitiesInRange.size(); ++i)
		{
			EntityDamage entry = this.entitiesInRange.get(i);
			if (entry.distance >= distanceMax)
				break;

			Entity entity = entry.entity;
			if (Util.square(entity.posX - x) + Util.square(entity.posY - y) + Util.square(entity.posZ - z) <= 25.0D)
			{
				// TODO gamerforEA code start
				if (EventConfig.explosionEvent && this.fake.cantDamage(entity))
					continue;
				// TODO gamerforEA code end

				double damage = 4.0D * power;
				entry.damage += damage;
				entry.health -= damage;
				double dx = entity.posX - this.explosionX;
				double dy = entity.posY - this.explosionY;
				double dz = entity.posZ - this.explosionZ;
				double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
				entry.motionX += dx / distance * 0.0875D * power;
				entry.motionY += dy / distance * 0.0875D * power;
				entry.motionZ += dz / distance * 0.0875D * power;
				if (entry.health <= 0.0D)
				{
					entity.attackEntityFrom(this.damageSource, (float) entry.damage);
					if (!entity.isEntityAlive())
					{
						this.entitiesInRange.remove(i);
						--i;
					}
				}
			}
		}

	}

	@Override
	public EntityLivingBase getExplosivePlacedBy()
	{
		return this.igniter;
	}

	private boolean isNuclear()
	{
		return this.type == Type.Nuclear;
	}

	private static double getEntityHealth(Entity entity)
	{
		return entity instanceof EntityItem ? 5.0D : Double.POSITIVE_INFINITY;
	}

	private static long[] makeArray(int size, int step)
	{
		return new long[(size * step + 8 - step) / 8];
	}

	private static int nextSetIndex(int start, long[] array, int step)
	{
		int offset = start % 8;

		for (int i = start / 8; i < array.length; ++i)
		{
			long aval = array[i];

			for (int j = offset; j < 8; j += step)
			{
				int val = (int) (aval >> j & (1 << step) - 1);
				if (val != 0)
					return i * 8 + j;
			}

			offset = 0;
		}

		return -1;
	}

	private static int getAtIndex(int index, long[] array, int step)
	{
		return (int) (array[index / 8] >>> index % 8 & (1 << step) - 1);
	}

	private static void setAtIndex(int index, long[] array, int value)
	{
		array[index / 8] |= value << index % 8;
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
				this.maxY = y;

			return this;
		}
	}

	private static class EntityDamage
	{
		final Entity entity;
		final int distance;
		double health;
		double damage;
		double motionX;
		double motionY;
		double motionZ;

		EntityDamage(Entity entity, int distance, double health)
		{
			this.entity = entity;
			this.distance = distance;
			this.health = health;
		}
	}

	public enum Type
	{
		Normal,
		Heat,
		Nuclear
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
				return false;
			XZposition xZposition = (XZposition) obj;
			return xZposition.x == this.x && xZposition.z == this.z;
		}

		@Override
		public int hashCode()
		{
			return this.x * 31 ^ this.z;
		}
	}
}
