package ic2.core;

import ic2.api.tile.ExplosionWhitelist;
import ic2.core.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class ExplosionIC2 extends Explosion
{
	private final Random ExplosionRNG = new Random();
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
	private final List<EntityDamage> entitiesInRange = new ArrayList();
	private final long[][] destroyedBlockPositions;
	private ChunkCache chunkCache;
	private static final double dropPowerLimit = 8.0D;
	private static final double damageAtDropPowerLimit = 32.0D;
	private static final double accelerationAtDropPowerLimit = 0.7D;
	private static final double motionLimit = 60.0D;
	private static final int secondaryRayCount = 5;
	private static final int bitSetElementSize = 2;

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop)
	{
		this(world, entity, x, y, z, power, drop, Type.Normal);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power, float drop, Type type1)
	{
		this(world, entity, x, y, z, power, drop, type1, (EntityLivingBase) null, 0);
	}

	public ExplosionIC2(World world, Entity entity, double x, double y, double z, float power1, float drop, Type type, EntityLivingBase igniter, int radiationRange)
	{
		super(world, entity, x, y, z, power1);
		this.worldObj = world;
		this.mapHeight = IC2.getWorldHeight(world);
		this.exploder = entity;
		this.power = power1;
		this.explosionDropRate = drop;
		this.explosionX = x;
		this.explosionY = y;
		this.explosionZ = z;
		this.type = type;
		this.igniter = igniter;
		this.radiationRange = radiationRange;
		this.maxDistance = (double) this.power / 0.4D;
		int maxDistanceInt = (int) Math.ceil(this.maxDistance);
		this.areaSize = maxDistanceInt * 2;
		this.areaX = Util.roundToNegInf(this.explosionX) - maxDistanceInt;
		this.areaZ = Util.roundToNegInf(this.explosionZ) - maxDistanceInt;
		if (this.isNuclear())
		{
			this.damageSource = IC2DamageSource.getNukeSource(this);
		}
		else
		{
			this.damageSource = DamageSource.setExplosionSource(this);
		}

		this.destroyedBlockPositions = new long[this.mapHeight][];
	}

	public void doExplosion()
	{
		/* TODO gamerforEA code clear:
		if (this.power > 0.0F)
		{
			ExplosionEvent event = new ExplosionEvent(this.worldObj, this.exploder, this.explosionX, this.explosionY, this.explosionZ, (double) this.power, this.igniter, this.radiationRange, this.maxDistance);
			if (!MinecraftForge.EVENT_BUS.post(event))
			{
				this.chunkCache = new ChunkCache(this.worldObj, (int) this.explosionX - this.areaSize / 2, (int) this.explosionY - this.areaSize / 2, (int) this.explosionZ - this.areaSize / 2, (int) this.explosionX + this.areaSize / 2, (int) this.explosionY + this.areaSize / 2, (int) this.explosionZ + this.areaSize / 2, 0);
				List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity((Entity) null, AxisAlignedBB.getBoundingBox(this.explosionX - this.maxDistance, this.explosionY - this.maxDistance, this.explosionZ - this.maxDistance, this.explosionX + this.maxDistance, this.explosionY + this.maxDistance, this.explosionZ + this.maxDistance));

				for (Entity entity : entities)
				{
					if (entity instanceof EntityLivingBase || entity instanceof EntityItem)
					{
						int distance = (int) (Util.square(entity.posX - this.explosionX) + Util.square(entity.posY - this.explosionY) + Util.square(entity.posZ - this.explosionZ));
						this.entitiesInRange.add(new EntityDamage(entity, distance, getEntityHealth(entity)));
					}
				}

				boolean notEmpty = !this.entitiesInRange.isEmpty();
				if (notEmpty)
				{
					Collections.sort(this.entitiesInRange, new Comparator<EntityDamage>()
					{
						public int compare(EntityDamage a, EntityDamage b)
						{
							return a.distance - b.distance;
						}
					});
				}

				int ceil = (int) Math.ceil(Math.PI / Math.atan(1.0D / this.maxDistance));

				for (int i = 0; i < 2 * ceil; ++i)
				{
					for (int j = 0; j < ceil; ++j)
					{
						double d = Math.PI * 2D / (double) ceil * (double) i;
						double d1 = Math.PI / (double) ceil * (double) j;
						this.shootRay(this.explosionX, this.explosionY, this.explosionZ, d, d1, (double) this.power, notEmpty && i % 8 == 0 && j % 8 == 0);
					}
				}

				double motion;
				EntityDamage entityDamage;
				Entity entity;
				for (Iterator<EntityDamage> iterator = this.entitiesInRange.iterator(); iterator.hasNext(); entity.motionZ += entityDamage.motionZ * motion)
				{
					entityDamage = iterator.next();
					entity = entityDamage.entity;
					entity.attackEntityFrom(this.damageSource, (float) entityDamage.damage);
					if (entity instanceof EntityPlayer)
					{
						EntityPlayer player = (EntityPlayer) entity;
						if (this.isNuclear() && this.igniter != null && player == this.igniter && player.getHealth() <= 0.0F)
						{
							IC2.achievements.issueAchievement(player, "dieFromOwnNuke");
						}
					}

					double motion1 = Util.square(entityDamage.motionX) + Util.square(entity.motionY) + Util.square(entity.motionZ);
					motion = motion1 > 3600.0D ? Math.sqrt(3600.0D / motion1) : 1.0D;
					entity.motionX += entityDamage.motionX * motion;
					entity.motionY += entityDamage.motionY * motion;
				}

				if (this.isNuclear() && this.radiationRange >= 1)
				{
					Iterator<EntityLiving> iterator = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(this.explosionX - (double) this.radiationRange, this.explosionY - (double) this.radiationRange, this.explosionZ - (double) this.radiationRange, this.explosionX + (double) this.radiationRange, this.explosionY + (double) this.radiationRange, this.explosionZ + (double) this.radiationRange)).iterator();

					while (iterator.hasNext())
					{
						EntityLiving entityLiving = iterator.next();
						if (!ItemArmorHazmat.hasCompleteHazmat(entityLiving))
						{
							double d = entityLiving.getDistance(this.explosionX, this.explosionY, this.explosionZ);
							int range = (int) (120.0D * ((double) this.radiationRange - d));
							int duration = (int) (80.0D * ((double) (this.radiationRange / 3) - d));
							if (range >= 0)
							{
								entityLiving.addPotionEffect(new PotionEffect(Potion.hunger.id, range, 0));
							}

							if (duration >= 0)
							{
								IC2Potion.radiation.applyTo(entityLiving, duration, 0);
							}
						}
					}
				}

				((NetworkManager) IC2.network.get()).initiateExplosionEffect(this.worldObj, this.explosionX, this.explosionY, this.explosionZ);
				Random random = this.worldObj.rand;
				boolean doTileDropsRule = this.worldObj.getGameRules().getGameRuleBooleanValue("doTileDrops");
				HashMap<XZposition, Map<ItemStackWrapper, DropData>> map = new HashMap();

				for (int y = 0; y < this.destroyedBlockPositions.length; ++y)
				{
					long[] positions = this.destroyedBlockPositions[y];
					if (positions != null)
					{
						int j = -2;

						while ((j = nextSetIndex(j + 2, positions, 2)) != -1)
						{
							int i = j / 2;
							int z = i / this.areaSize;
							int x = i - z * this.areaSize;
							x += this.areaX;
							z += this.areaZ;
							Block block = this.chunkCache.getBlock(x, y, z);
							if (this.power < 20.0F)
							{
								double d = (double) ((float) x + random.nextFloat());
								double d1 = (double) ((float) y + random.nextFloat());
								double d2 = (double) ((float) z + random.nextFloat());
								double d3 = d - this.explosionX;
								double d4 = d1 - this.explosionY;
								double d5 = d2 - this.explosionZ;
								double d6 = (double) MathHelper.sqrt_double(d3 * d3 + d4 * d4 + d5 * d5);
								d3 /= d6;
								d4 /= d6;
								d5 /= d6;
								double d7 = 0.5D / (d6 / (double) this.power + 0.1D);
								d7 *= (double) (random.nextFloat() * random.nextFloat() + 0.3F);
								d3 *= d7;
								d4 *= d7;
								d5 *= d7;
								this.worldObj.spawnParticle("explode", (d + this.explosionX) / 2.0D, (d1 + this.explosionY) / 2.0D, (d2 + this.explosionZ) / 2.0D, d3, d4, d5);
								this.worldObj.spawnParticle("smoke", d, d1, d2, d3, d4, d5);
							}

							if (doTileDropsRule && getAtIndex(j, positions, 2) == 1)
							{
								int meta = this.chunkCache.getBlockMetadata(x, y, z);
								for (ItemStack stack : block.getDrops(this.worldObj, x, y, z, meta, 0))
								{
									if (random.nextFloat() <= this.explosionDropRate)
									{
										XZposition pos = new XZposition(x / 2, z / 2);
										if (!map.containsKey(pos))
										{
											map.put(pos, new HashMap());
										}

										Map<ItemStackWrapper, DropData> map1 = map.get(pos);
										ItemStackWrapper isw = new ItemStackWrapper(stack);
										if (!map1.containsKey(isw))
										{
											map1.put(isw, new DropData(stack.stackSize, y));
										}
										else
										{
											map1.put(isw, map1.get(isw).add(stack.stackSize, y));
										}
									}
								}
							}

							this.worldObj.setBlockToAir(x, y, z);
							block.onBlockDestroyedByExplosion(this.worldObj, x, y, z, this);
						}
					}
				}

				for (Entry<XZposition, Map<ItemStackWrapper, DropData>> entry : map.entrySet())
				{
					XZposition pos = entry.getKey();
					for (Entry<ItemStackWrapper, DropData> entry1 : entry.getValue().entrySet())
					{
						ItemStackWrapper isw = entry1.getKey();
						int i = 0;

						for (int j = entry1.getValue().n; j > 0; j -= i)
						{
							i = Math.min(j, 64);
							EntityItem item = new EntityItem(this.worldObj, (double) ((float) pos.x + this.worldObj.rand.nextFloat()) * 2.0D, (double) ((DropData) entry1.getValue()).maxY + 0.5D, (double) ((float) pos.z + this.worldObj.rand.nextFloat()) * 2.0D, StackUtil.copyWithSize(isw.stack, i));
							item.delayBeforeCanPickup = 10;
							this.worldObj.spawnEntityInWorld(item);
						}
					}
				}
			}
		} */
	}

	public void destroy(int x, int y, int z, boolean noDrop)
	{
		this.destroyUnchecked(x, y, z, noDrop);
	}

	private void destroyUnchecked(int x, int y, int z, boolean noDrop)
	{
		int index = (z - this.areaZ) * this.areaSize + (x - this.areaX);
		index *= 2;
		long[] array = this.destroyedBlockPositions[y];
		if (array == null)
		{
			array = makeArray(Util.square(this.areaSize), 2);
			this.destroyedBlockPositions[y] = array;
		}

		if (noDrop)
		{
			setAtIndex(index, array, 3);
		}
		else
		{
			setAtIndex(index, array, 1);
		}

	}

	private void shootRay(double x, double y, double z, double phi, double theta, double power, boolean killEntities)
	{
		double sinTheta = Math.sin(theta); // TODO gamerforEA improve performance - replace 2 calls Math.sin(D)
		double deltaX = sinTheta * Math.cos(phi);
		double deltaY = Math.cos(theta);
		double deltaZ = sinTheta * Math.sin(phi);
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
				if (absorption > power)
				{
					break;
				}

				if (block == Blocks.stone || block != Blocks.air && !block.isAir(this.worldObj, blockX, blockY, blockZ))
				{
					this.destroyUnchecked(blockX, blockY, blockZ, power > 8.0D);
				}
			}

			if (killEntities && (step + 4) % 8 == 0 && !this.entitiesInRange.isEmpty() && power >= 0.25D)
			{
				this.damageEntities(x, y, z, step, power);
			}

			if (absorption > 10.0D)
			{
				for (int i = 0; i < 5; ++i)
				{
					this.shootRay(x, y, z, this.ExplosionRNG.nextDouble() * 2.0D * Math.PI, this.ExplosionRNG.nextDouble() * Math.PI, absorption * 0.4D, false);
				}
			}

			power -= absorption;
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
		else
		{
			return ret;
		}
	}

	private void damageEntities(double x, double y, double z, int step, double power)
	{
		int index;
		int i;
		if (step != 4)
		{
			i = Util.square(step - 5);
			int j = 0;
			int k = this.entitiesInRange.size() - 1;

			do
			{
				index = (j + k) / 2;
				int damage = ((EntityDamage) this.entitiesInRange.get(index)).distance;
				if (damage < i)
				{
					j = index + 1;
				}
				else if (damage > i)
				{
					k = index - 1;
				}
				else
				{
					k = index;
				}
			}
			while (j < k);
		}
		else
		{
			index = 0;
		}

		int distanceMax = Util.square(step + 5);

		for (i = index; i < this.entitiesInRange.size(); ++i)
		{
			EntityDamage entityDamage = (EntityDamage) this.entitiesInRange.get(i);
			if (entityDamage.distance >= distanceMax)
			{
				break;
			}

			Entity entity = entityDamage.entity;
			if (Util.square(entity.posX - x) + Util.square(entity.posY - y) + Util.square(entity.posZ - z) <= 25.0D)
			{
				double power1 = 4.0D * power;
				entityDamage.damage += power1;
				entityDamage.health -= power1;
				double dx = entity.posX - this.explosionX;
				double dy = entity.posY - this.explosionY;
				double dz = entity.posZ - this.explosionZ;
				double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
				entityDamage.motionX += dx / distance * 0.0875D * power;
				entityDamage.motionY += dy / distance * 0.0875D * power;
				entityDamage.motionZ += dz / distance * 0.0875D * power;
				if (entityDamage.health <= 0.0D)
				{
					entity.attackEntityFrom(this.damageSource, (float) entityDamage.damage);
					if (!entity.isEntityAlive())
					{
						this.entitiesInRange.remove(i);
						--i;
					}
				}
			}
		}
	}

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
				int val = (int) (aval >> j & (long) ((1 << step) - 1));
				if (val != 0)
				{
					return i * 8 + j;
				}
			}

			offset = 0;
		}

		return -1;
	}

	private static int getAtIndex(int index, long[] array, int step)
	{
		return (int) (array[index / 8] >>> index % 8 & (long) ((1 << step) - 1));
	}

	private static void setAtIndex(int index, long[] array, int value)
	{
		array[index / 8] |= (long) (value << index % 8);
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

		public boolean equals(Object obj)
		{
			if (!(obj instanceof XZposition))
			{
				return false;
			}
			else
			{
				XZposition pos = (XZposition) obj;
				return pos.x == this.x && pos.z == this.z;
			}
		}

		public int hashCode()
		{
			return this.x * 31 ^ this.z;
		}
	}
}