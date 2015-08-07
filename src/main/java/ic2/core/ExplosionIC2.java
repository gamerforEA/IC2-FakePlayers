package ic2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.FakePlayerUtils;

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

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power1, float drop)
	{
		this(world, entity, x, y, z, power1, drop, Type.Normal);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power1, float drop, Type type1)
	{
		this(world, entity, x, y, z, power1, drop, type1, (EntityLivingBase) null, 0);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power1, float drop, Type type1, EntityLivingBase igniter1, int radiationRange1)
	{
		super(world, entity, x, y, z, power1);
		this.ExplosionRNG = new Random();
		this.entitiesInRange = new ArrayList();
		this.worldObj = world;
		this.mapHeight = IC2.getWorldHeight(world);
		this.exploder = entity;
		this.power = power1;
		this.explosionDropRate = drop;
		this.explosionX = x;
		this.explosionY = y;
		this.explosionZ = z;
		this.type = type1;
		this.igniter = igniter1;
		this.radiationRange = radiationRange1;
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
				List entities = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity) null, AxisAlignedBB.getBoundingBox(this.explosionX - this.maxDistance, this.explosionY - this.maxDistance, this.explosionZ - this.maxDistance, this.explosionX + this.maxDistance, this.explosionY + this.maxDistance, this.explosionZ + this.maxDistance));
				Iterator entitiesAreInRange = entities.iterator();

				while (true)
				{
					Entity steps;
					int rng;
					do
					{
						if (!entitiesAreInRange.hasNext())
						{
							boolean var31 = !this.entitiesInRange.isEmpty();
							if (var31)
								Collections.sort(this.entitiesInRange, new Comparator<EntityDamage>()
								{
									@Override
									public int compare(EntityDamage a, EntityDamage b)
									{
										return a.distance - b.distance;
									}
								});

							int var32 = (int) Math.ceil(3.141592653589793D / Math.atan(1.0D / this.maxDistance));

							double blocksToDrop;
							for (rng = 0; rng < 2 * var32; ++rng)
								for (int var34 = 0; var34 < var32; ++var34)
								{
									blocksToDrop = 6.283185307179586D / var32 * rng;
									double entry = 3.141592653589793D / var32 * var34;
									this.shootRay(this.explosionX, this.explosionY, this.explosionZ, blocksToDrop, entry, this.power, var31 && rng % 8 == 0 && var34 % 8 == 0);
								}

							double xZposition;
							Iterator var33;
							EntityDamage var36;
							Entity var37;
							for (var33 = this.entitiesInRange.iterator(); var33.hasNext(); var37.motionZ += var36.motionZ * xZposition)
							{
								var36 = (EntityDamage) var33.next();
								var37 = var36.entity;
								var37.attackEntityFrom(this.damageSource, (float) var36.damage);
								if (var37 instanceof EntityPlayer)
								{
									EntityPlayer i$ = (EntityPlayer) var37;
									if (this.isNuclear() && this.igniter != null && i$ == this.igniter && i$.getHealth() <= 0.0F)
										IC2.achievements.issueAchievement(i$, "dieFromOwnNuke");
								}

								double var41 = Util.square(var36.motionX) + Util.square(var37.motionY) + Util.square(var37.motionZ);
								xZposition = var41 > 3600.0D ? Math.sqrt(3600.0D / var41) : 1.0D;
								var37.motionX += var36.motionX * xZposition;
								var37.motionY += var36.motionY * xZposition;
							}

							int var47;
							if (this.isNuclear() && this.radiationRange >= 1)
							{
								var33 = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(this.explosionX - this.radiationRange, this.explosionY - this.radiationRange, this.explosionZ - this.radiationRange, this.explosionX + this.radiationRange, this.explosionY + this.radiationRange, this.explosionZ + this.radiationRange)).iterator();

								while (var33.hasNext())
								{
									EntityLiving var38 = (EntityLiving) var33.next();
									if (!ItemArmorHazmat.hasCompleteHazmat(var38))
									{
										blocksToDrop = var38.getDistance(this.explosionX, this.explosionY, this.explosionZ);
										int var44 = (int) (120.0D * (this.radiationRange - blocksToDrop));
										var47 = (int) (80.0D * (this.radiationRange / 3 - blocksToDrop));
										if (var44 >= 0)
											var38.addPotionEffect(new PotionEffect(Potion.hunger.id, var44, 0));

										if (var47 >= 0)
											IC2Potion.radiation.applyTo(var38, var47, 0);
									}
								}
							}

							IC2.network.get().initiateExplosionEffect(this.worldObj, this.explosionX, this.explosionY, this.explosionZ);
							Random var35 = this.worldObj.rand;
							boolean var39 = this.worldObj.getGameRules().getGameRuleBooleanValue("doTileDrops");
							HashMap var40 = new HashMap();
							this.worldObj.playSoundEffect(this.explosionX, this.explosionY, this.explosionZ, "random.explode", 4.0F, (1.0F + (var35.nextFloat() - var35.nextFloat()) * 0.2F) * 0.7F);

							int var53;
							for (int var42 = 0; var42 < this.destroyedBlockPositions.length; ++var42)
							{
								long[] var45 = this.destroyedBlockPositions[var42];
								if (var45 != null)
								{
									var47 = -2;

									while ((var47 = nextSetIndex(var47 + 2, var45, 2)) != -1)
									{
										int i$1 = var47 / 2;
										int entry2 = i$1 / this.areaSize;
										int isw = i$1 - entry2 * this.areaSize;
										isw += this.areaX;
										entry2 += this.areaZ;
										// TODO gamerforEA code start
										if (EventConfig.explosionEvent && FakePlayerUtils.isInPrivate(this.worldObj, isw, var42, entry2))
											continue;
										// TODO gamerforEA code end
										Block count = this.chunkCache.getBlock(isw, var42, entry2);
										if (this.power < 20.0F)
										{
											double stackSize = isw + var35.nextFloat();
											double itemStack = var42 + var35.nextFloat();
											double map = entry2 + var35.nextFloat();
											double d3 = stackSize - this.explosionX;
											double d4 = itemStack - this.explosionY;
											double d5 = map - this.explosionZ;
											double effectDistance = MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
											d3 /= effectDistance;
											d4 /= effectDistance;
											d5 /= effectDistance;
											double d7 = 0.5D / (effectDistance / this.power + 0.1D);
											d7 *= var35.nextFloat() * var35.nextFloat() + 0.3F;
											d3 *= d7;
											d4 *= d7;
											d5 *= d7;
											this.worldObj.spawnParticle("explode", (stackSize + this.explosionX) / 2.0D, (itemStack + this.explosionY) / 2.0D, (map + this.explosionZ) / 2.0D, d3, d4, d5);
											this.worldObj.spawnParticle("smoke", stackSize, itemStack, map, d3, d4, d5);
										}

										if (var39 && getAtIndex(var47, var45, 2) == 1)
										{
											var53 = this.chunkCache.getBlockMetadata(isw, var42, entry2);
											Iterator entityitem = count.getDrops(this.worldObj, isw, var42, entry2, var53, 0).iterator();

											while (entityitem.hasNext())
											{
												ItemStack var55 = (ItemStack) entityitem.next();
												if (var35.nextFloat() <= this.explosionDropRate)
												{
													XZposition xZposition1 = new XZposition(isw / 2, entry2 / 2);
													if (!var40.containsKey(xZposition1))
														var40.put(xZposition1, new HashMap());

													Map var56 = (Map) var40.get(xZposition1);
													ItemStackWrapper isw1 = new ItemStackWrapper(var55);
													if (!var56.containsKey(isw1))
														var56.put(isw1, new DropData(var55.stackSize, var42));
													else
														var56.put(isw1, ((DropData) var56.get(isw1)).add(var55.stackSize, var42));
												}
											}
										}

										this.worldObj.setBlockToAir(isw, var42, entry2);
										count.onBlockDestroyedByExplosion(this.worldObj, isw, var42, entry2, this);
									}
								}
							}

							Iterator var43 = var40.entrySet().iterator();

							while (var43.hasNext())
							{
								Entry var46 = (Entry) var43.next();
								XZposition var48 = (XZposition) var46.getKey();
								Iterator var49 = ((Map) var46.getValue()).entrySet().iterator();

								while (var49.hasNext())
								{
									Entry var50 = (Entry) var49.next();
									ItemStackWrapper var51 = (ItemStackWrapper) var50.getKey();

									for (int var52 = ((DropData) var50.getValue()).n; var52 > 0; var52 -= var53)
									{
										var53 = Math.min(var52, 64);
										EntityItem var54 = new EntityItem(this.worldObj, (var48.x + this.worldObj.rand.nextFloat()) * 2.0D, ((DropData) var50.getValue()).maxY + 0.5D, (var48.z + this.worldObj.rand.nextFloat()) * 2.0D, StackUtil.copyWithSize(var51.stack, var53));
										var54.delayBeforeCanPickup = 10;
										this.worldObj.spawnEntityInWorld(var54);
									}
								}
							}

							return;
						}

						steps = (Entity) entitiesAreInRange.next();
					}
					while (!(steps instanceof EntityLivingBase) && !(steps instanceof EntityItem));

					rng = (int) (Util.square(steps.posX - this.explosionX) + Util.square(steps.posY - this.explosionY) + Util.square(steps.posZ - this.explosionZ));
					double doDrops = getEntityHealth(steps);
					this.entitiesInRange.add(new EntityDamage(steps, rng, doDrops));
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
		index *= 2;
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
		double sinTheta = Math.sin(theta);
		double deltaX = sinTheta * Math.cos(phi);
		double deltaY = Math.cos(theta);
		double deltaZ = sinTheta * Math.sin(phi); */
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
					this.shootRay(x, y, z, this.ExplosionRNG.nextDouble() * 2.0D * 3.141592653589793D, this.ExplosionRNG.nextDouble() * 3.141592653589793D, absorption * 0.4D, false);

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
		else
			return ret;
	}

	private void damageEntities(double x, double y, double z, int step, double power)
	{
		int index;
		int i;
		if (step != 4)
		{
			i = Util.square(step - 5);
			int entry = 0;
			int entity = this.entitiesInRange.size() - 1;

			do
			{
				index = (entry + entity) / 2;
				int damage = this.entitiesInRange.get(index).distance;
				if (damage < i)
					entry = index + 1;
				else if (damage > i)
					entity = index - 1;
				else
					entity = index;
			}
			while (entry < entity);
		}
		else
			index = 0;

		int distanceMax = Util.square(step + 5);

		for (i = index; i < this.entitiesInRange.size(); ++i)
		{
			EntityDamage var25 = this.entitiesInRange.get(i);
			if (var25.distance >= distanceMax)
				break;

			Entity var26 = var25.entity;
			if (Util.square(var26.posX - x) + Util.square(var26.posY - y) + Util.square(var26.posZ - z) <= 25.0D)
			{
				double var27 = 4.0D * power;
				var25.damage += var27;
				var25.health -= var27;
				double dx = var26.posX - this.explosionX;
				double dy = var26.posY - this.explosionY;
				double dz = var26.posZ - this.explosionZ;
				double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
				var25.motionX += dx / distance * 0.0875D * power;
				var25.motionY += dy / distance * 0.0875D * power;
				var25.motionZ += dz / distance * 0.0875D * power;
				if (var25.health <= 0.0D)
				{
					var26.attackEntityFrom(this.damageSource, (float) var25.damage);
					if (!var26.isEntityAlive())
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
				this.maxY = y;

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
				return false;
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
