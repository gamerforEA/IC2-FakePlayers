package ic2.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class ExplosionIC2 extends Explosion
{
	/* TODO gamerforEA code clear:
	private final Random ExplosionRNG = new Random();
	private final World worldObj;
	private final int mapHeight;
	private final float power;
	private final float explosionDropRate;
	private final Type type;
	private final int radiationRange;
	private final List<Entry<Integer, Entity>> entitiesInRange = new ArrayList();
	private final Map<ChunkPosition, Boolean> destroyedBlockPositions = new HashMap();
	private ChunkCache chunkCache;
	private static final double dropPowerLimit = 8.0D;
	private static final double damageAtDropPowerLimit = 32.0D;
	private static final double accelerationAtDropPowerLimit = 0.7D;
	private static final double motionLimit = 60.0D;
	private static final int secondaryRayCount = 5; */
	private final EntityLivingBase igniter;

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
		/* TODO gamerforEA code clear:
		this.worldObj = world;
		this.mapHeight = IC2.getWorldHeight(world);
		this.exploder = entity;
		this.power = power;
		this.explosionDropRate = drop;
		this.explosionX = x;
		this.explosionY = y;
		this.explosionZ = z;
		this.type = type;
		this.radiationRange = radiationRange; */
		this.igniter = igniter;
	}

	public void doExplosion()
	{
		/* TODO gamerforEA code clear:
		if (this.power > 0.0F)
		{
			double maxDistance = (double) this.power / 0.4D;
			ExplosionEvent event = new ExplosionEvent(this.worldObj, this.exploder, this.explosionX, this.explosionY, this.explosionZ, (double) this.power, this.igniter, this.radiationRange, maxDistance);
			if (!MinecraftForge.EVENT_BUS.post(event))
			{
				int maxDistanceInt = (int) Math.ceil(maxDistance);
				this.chunkCache = new ChunkCache(this.worldObj, (int) this.explosionX - maxDistanceInt, (int) this.explosionY - maxDistanceInt, (int) this.explosionZ - maxDistanceInt, (int) this.explosionX + maxDistanceInt, (int) this.explosionY + maxDistanceInt, (int) this.explosionZ + maxDistanceInt, 0);
				List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(null, AxisAlignedBB.getBoundingBox(this.explosionX - maxDistance, this.explosionY - maxDistance, this.explosionZ - maxDistance, this.explosionX + maxDistance, this.explosionY + maxDistance, this.explosionZ + maxDistance));

				for (Entity entity : entities)
				{
					if (entity instanceof EntityLivingBase || entity instanceof EntityItem)
					{
						this.entitiesInRange.add(new SimpleEntry((int) ((entity.posX - this.explosionX) * (entity.posX - this.explosionX) + (entity.posY - this.explosionY) * (entity.posY - this.explosionY) + (entity.posZ - this.explosionZ) * (entity.posZ - this.explosionZ)), entity));
					}
				}

				if (!this.entitiesInRange.isEmpty())
				{
					Collections.sort(this.entitiesInRange, new Comparator<Entry<Integer, Entity>>()
					{
						public int compare(Entry<Integer, Entity> a, Entry<Integer, Entity> b)
						{
							return a.getKey() - b.getKey();
						}
					});
				}

				int ceilDistance = (int) Math.ceil(3.141592653589793D / Math.atan(1.0D / maxDistance));

				for (int i = 0; i < 2 * ceilDistance; ++i)
				{
					for (int j = 0; j < ceilDistance; ++j)
					{
						double phi = 6.283185307179586D / (double) ceilDistance * (double) i;
						double theta = 3.141592653589793D / (double) ceilDistance * (double) j;
						this.shootRay(this.explosionX, this.explosionY, this.explosionZ, phi, theta, (double) this.power, !this.entitiesInRange.isEmpty() && i % 8 == 0 && j % 8 == 0);
					}
				}

				for (Entry<Integer, Entity> entry : this.entitiesInRange)
				{
					Entity entity = (Entity) entry.getValue();
					double d = entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ;
					if (d > 3600.0D)
					{
						double motion = Math.sqrt(3600.0D / d);
						entity.motionX *= motion;
						entity.motionY *= motion;
						entity.motionZ *= motion;
					}
				}

				if (this.isNuclear() && this.radiationRange >= 1)
				{
					List<EntityLiving> list = this.worldObj.getEntitiesWithinAABB(EntityLiving.class, AxisAlignedBB.getBoundingBox(this.explosionX - (double) this.radiationRange, this.explosionY - (double) this.radiationRange, this.explosionZ - (double) this.radiationRange, this.explosionX + (double) this.radiationRange, this.explosionY + (double) this.radiationRange, this.explosionZ + (double) this.radiationRange));

					for (EntityLiving entity : list)
					{
						if (!ItemArmorHazmat.hasCompleteHazmat(entity))
						{
							double distance = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ);
							int duration = (int) (120.0D * ((double) this.radiationRange - distance));
							if ((duration = (int) (120.0D * ((double) this.radiationRange - distance))) >= 0)
							{
								entity.addPotionEffect(new PotionEffect(Potion.hunger.id, duration, 0));
							}

							if ((duration = (int) (80.0D * ((double) (this.radiationRange / 3) - distance))) >= 0)
							{
								IC2Potion.radiation.applyTo(entity, duration, 0);
							}
						}
					}
				}

				IC2.network.get().initiateExplosionEffect(this.worldObj, this.explosionX, this.explosionY, this.explosionZ);
				Map<XZposition, Map<ItemStackWrapper, DropData>> posMap = new HashMap();

				for (Entry<ChunkPosition, Boolean> entry : this.destroyedBlockPositions.entrySet())
				{
					int x = entry.getKey().chunkPosX;
					int y = entry.getKey().chunkPosY;
					int z = entry.getKey().chunkPosZ;
					Block block = this.chunkCache.getBlock(x, y, z);
					if (entry.getValue())
					{
						double effectX = (double) ((float) x + this.worldObj.rand.nextFloat());
						double effectY = (double) ((float) y + this.worldObj.rand.nextFloat());
						double effectZ = (double) ((float) z + this.worldObj.rand.nextFloat());
						double velX = effectX - this.explosionX;
						double velY = effectY - this.explosionY;
						double velZ = effectZ - this.explosionZ;
						double effectDistance = (double) MathHelper.sqrt_double(velX * velX + velY * velY + velZ * velZ);
						velX /= effectDistance;
						velY /= effectDistance;
						velZ /= effectDistance;
						double effectDistance1 = 0.5D / (effectDistance / (double) this.power + 0.1D);
						effectDistance1 *= (double) (this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F);
						velX *= effectDistance1;
						velY *= effectDistance1;
						velZ *= effectDistance1;
						this.worldObj.spawnParticle("explode", (effectX + this.explosionX) / 2.0D, (effectY + this.explosionY) / 2.0D, (effectZ + this.explosionZ) / 2.0D, velX, velY, velZ);
						this.worldObj.spawnParticle("smoke", effectX, effectY, effectZ, velX, velY, velZ);
						int meta = this.worldObj.getBlockMetadata(x, y, z);

						for (ItemStack itemStack : block.getDrops(this.worldObj, x, y, z, meta, 0))
						{
							if (this.worldObj.rand.nextFloat() <= this.explosionDropRate)
							{
								XZposition pos = new XZposition(x / 2, z / 2);
								if (!posMap.containsKey(pos))
								{
									posMap.put(pos, new HashMap());
								}

								Map<ItemStackWrapper, DropData> map = posMap.get(pos);
								ItemStackWrapper isw = new ItemStackWrapper(itemStack);
								if (!map.containsKey(isw))
								{
									map.put(isw, new DropData(itemStack.stackSize, y));
								}
								else
								{
									map.put(isw, map.get(isw).add(itemStack.stackSize, y));
								}
							}
						}
					}

					this.worldObj.setBlockToAir(x, y, z);
					block.onBlockDestroyedByExplosion(this.worldObj, x, y, z, this);
				}

				for (Entry<XZposition, Map<ItemStackWrapper, DropData>> entry : posMap.entrySet())
				{
					XZposition pos = (XZposition) entry.getKey();
					for (Entry<ItemStackWrapper, DropData> entry1 : entry.getValue().entrySet())
					{
						ItemStackWrapper isw = entry1.getKey();

						int stackSize;
						for (int i = entry1.getValue().n; i > 0; i -= stackSize)
						{
							stackSize = Math.min(i, 64);
							EntityItem item = new EntityItem(this.worldObj, (double) ((float) pos.x + this.worldObj.rand.nextFloat()) * 2.0D, (double) ((DropData) entry1.getValue()).maxY + 0.5D, (double) ((float) pos.z + this.worldObj.rand.nextFloat()) * 2.0D, StackUtil.copyWithSize(isw.stack, stackSize));
							item.delayBeforeCanPickup = 10;
							this.worldObj.spawnEntityInWorld(item);
						}
					}
				}
			}
		} */
	}

	public void destroy(int x, int y, int z)
	{
		/* TODO gamerforEA code clear:
		ChunkPosition position = new ChunkPosition(x, y, z);
		if (!this.destroyedBlockPositions.containsKey(position))
		{
			this.destroyedBlockPositions.put(position, true);
		} */
	}

	/* TODO gamerforEA code clear:
	private void shootRay(double x, double y, double z, double phi, double theta, double power, boolean killEntities)
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
				if (absorption > power)
				{
					break;
				}

				if (!block.isAir(this.worldObj, blockX, blockY, blockZ))
				{
					ChunkPosition pos = new ChunkPosition(blockX, blockY, blockZ);
					if (power > 8.0D)
					{
						this.destroyedBlockPositions.put(pos, false);
					}
					else if (!this.destroyedBlockPositions.containsKey(pos))
					{
						this.destroyedBlockPositions.put(pos, true);
					}
				}
			}

			int i;
			if (killEntities && (step + 4) % 8 == 0 && !this.entitiesInRange.isEmpty() && power >= 0.25D)
			{
				int i1;
				if (step != 4)
				{
					i1 = step * step - 25;
					int entity = 0;
					int dx = this.entitiesInRange.size() - 1;

					do
					{
						i = (entity + dx) / 2;
						int distance = ((Integer) ((Entry) this.entitiesInRange.get(i)).getKey()).intValue();
						if (distance < i1)
						{
							entity = i + 1;
						}
						else if (distance > i1)
						{
							dx = i - 1;
						}
						else
						{
							dx = i;
						}
					}
					while (entity < dx);
				}
				else
				{
					i = 0;
				}

				int distanceMax = step * step + 25;

				for (i1 = i; i1 < this.entitiesInRange.size() && this.entitiesInRange.get(i).getKey() < distanceMax; ++i1)
				{
					Entity entity = this.entitiesInRange.get(i).getValue();
					if ((entity.posX - x) * (entity.posX - x) + (entity.posY - y) * (entity.posY - y) + (entity.posZ - z) * (entity.posZ - z) <= 25.0D)
					{
						entity.attackEntityFrom(this.getDamageSource(), (float) ((int) (32.0D * power / 8.0D)));
						if (entity instanceof EntityPlayer)
						{
							EntityPlayer player = (EntityPlayer) entity;
							if (this.isNuclear() && this.igniter != null && player == this.igniter && player.getHealth() <= 0.0F)
							{
								IC2.achievements.issueAchievement(player, "dieFromOwnNuke");
							}
						}

						double dx = entity.posX - this.explosionX;
						double dy = entity.posY - this.explosionY;
						double dz = entity.posZ - this.explosionZ;
						double distance1 = Math.sqrt(dx * dx + dy * dy + dz * dz);
						entity.motionX += dx / distance1 * 0.7D * power / 8.0D;
						entity.motionY += dy / distance1 * 0.7D * power / 8.0D;
						entity.motionZ += dz / distance1 * 0.7D * power / 8.0D;
						if (!entity.isEntityAlive())
						{
							this.entitiesInRange.remove(i1);
							--i1;
						}
					}
				}
			}

			if (absorption > 10.0D)
			{
				for (i = 0; i < 5; ++i)
				{
					this.shootRay(x, y, z, this.ExplosionRNG.nextDouble() * 2.0D * 3.141592653589793D, this.ExplosionRNG.nextDouble() * 3.141592653589793D, absorption * 0.4D, false);
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
	} */

	@Override
	public EntityLivingBase getExplosivePlacedBy()
	{
		return this.igniter;
	}

	/* TODO gamerforEA code clear:
	private DamageSource getDamageSource()
	{
		return this.isNuclear() ? IC2DamageSource.setNukeSource(this) : DamageSource.setExplosionSource(this);
	}

	private boolean isNuclear()
	{
		return this.type == Type.Nuclear;
	} */

	public static enum Type
	{
		Normal, Heat, Nuclear;
	}

	/* TODO gamerforEA code clear:
	private static class DropData
	{
		int n;
		int maxY;

		DropData(int n, int y)
		{
			this.n = n;
			this.maxY = y;
		}

		public DropData add(int n, int y)
		{
			this.n += n;
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

		XZposition(int x, int z)
		{
			this.x = x;
			this.z = z;
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
	} */
}