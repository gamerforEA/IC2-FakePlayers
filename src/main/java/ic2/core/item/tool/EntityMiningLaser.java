package ic2.core.item.tool;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ModUtils;
import cpw.mods.fml.common.registry.IThrowableEntity;
import ic2.api.event.LaserEvent;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.MaterialIC2TNT;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityMiningLaser extends Entity implements IThrowableEntity
{
	public float range;
	public float power;
	public int blockBreaks;
	public boolean explosive;
	public static Set<Block> unmineableBlocks = new HashSet(Arrays.asList(Blocks.brick_block, Blocks.obsidian, Blocks.lava, Blocks.flowing_lava, Blocks.water, Blocks.flowing_water, Blocks.bedrock, StackUtil.getBlock(Ic2Items.reinforcedStone), StackUtil.getBlock(Ic2Items.reinforcedDoorBlock)));
	public static final double laserSpeed = 1.0D;
	public EntityLivingBase owner;
	public boolean headingSet;
	public boolean smelt;
	private int ticksInAir;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public EntityMiningLaser(World world)
	{
		super(world);
		this.range = 0.0F;
		this.power = 0.0F;
		this.blockBreaks = 0;
		this.explosive = false;
		this.headingSet = false;
		this.smelt = false;
		this.ticksInAir = 0;
		this.setSize(0.8F, 0.8F);
		this.yOffset = 0.0F;
	}

	public EntityMiningLaser(World world, EntityLivingBase entityliving, float range, float power, int blockBreaks, boolean explosive)
	{
		this(world, entityliving, range, power, blockBreaks, explosive, entityliving.rotationYaw, entityliving.rotationPitch);
	}

	public EntityMiningLaser(World world, EntityLivingBase entityliving, float range, float power, int blockBreaks, boolean explosive, boolean smelt)
	{
		this(world, entityliving, range, power, blockBreaks, explosive, entityliving.rotationYaw, entityliving.rotationPitch);
		this.smelt = smelt;
	}

	public EntityMiningLaser(World world, EntityLivingBase entityliving, float range, float power, int blockBreaks, boolean explosive, double yawDeg, double pitchDeg)
	{
		this(world, entityliving, range, power, blockBreaks, explosive, yawDeg, pitchDeg, entityliving.posY + entityliving.getEyeHeight() - 0.1D);
	}

	public EntityMiningLaser(World world, EntityLivingBase entityliving, float range, float power, int blockBreaks, boolean explosive, double yawDeg, double pitchDeg, double y)
	{
		super(world);
		this.range = 0.0F;
		this.power = 0.0F;
		this.blockBreaks = 0;
		this.explosive = false;
		this.headingSet = false;
		this.smelt = false;
		this.ticksInAir = 0;
		this.owner = entityliving;
		this.setSize(0.8F, 0.8F);
		this.yOffset = 0.0F;
		double yaw = Math.toRadians(yawDeg);
		double pitch = Math.toRadians(pitchDeg);
		double x = entityliving.posX - Math.cos(yaw) * 0.16D;
		double z = entityliving.posZ - Math.sin(yaw) * 0.16D;
		double startMotionX = -Math.sin(yaw) * Math.cos(pitch);
		double startMotionY = -Math.sin(pitch);
		double startMotionZ = Math.cos(yaw) * Math.cos(pitch);
		this.setPosition(x, y, z);
		this.setLaserHeading(startMotionX, startMotionY, startMotionZ, 1.0D);
		this.range = range;
		this.power = power;
		this.blockBreaks = blockBreaks;
		this.explosive = explosive;

		// TODO gamerforEA code start
		this.fake.setRealPlayer(entityliving);
		// TODO gamerforEA code end
	}

	@Override
	protected void entityInit()
	{
	}

	public void setLaserHeading(double motionX, double motionY, double motionZ, double speed)
	{
		double currentSpeed = MathHelper.sqrt_double(motionX * motionX + motionY * motionY + motionZ * motionZ);
		this.motionX = motionX / currentSpeed * speed;
		this.motionY = motionY / currentSpeed * speed;
		this.motionZ = motionZ / currentSpeed * speed;
		this.prevRotationYaw = this.rotationYaw = (float) Math.toDegrees(Math.atan2(motionX, motionZ));
		this.prevRotationPitch = this.rotationPitch = (float) Math.toDegrees(Math.atan2(motionY, MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ)));
		this.headingSet = true;
	}

	@Override
	public void setVelocity(double motionX, double motionY, double motionZ)
	{
		this.setLaserHeading(motionX, motionY, motionZ, 1.0D);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (!IC2.platform.isSimulating() || this.range >= 1.0F && this.power > 0.0F && this.blockBreaks > 0)
		{
			++this.ticksInAir;
			Vec3 oldPosition = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			Vec3 newPosition = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition movingobjectposition = this.worldObj.func_147447_a(oldPosition, newPosition, false, true, false);
			oldPosition = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			if (movingobjectposition != null)
				newPosition = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
			else
				newPosition = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			Entity entity = null;
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double d = 0.0D;

			for (int l = 0; l < list.size(); ++l)
			{
				Entity entity1 = (Entity) list.get(l);
				if (entity1.canBeCollidedWith() && (entity1 != this.owner || this.ticksInAir >= 5))
				{
					float f4 = 0.3F;
					AxisAlignedBB axisalignedbb1 = entity1.boundingBox.expand(f4, f4, f4);
					MovingObjectPosition movingobjectposition1 = axisalignedbb1.calculateIntercept(oldPosition, newPosition);
					if (movingobjectposition1 != null)
					{
						double d1 = oldPosition.distanceTo(movingobjectposition1.hitVec);
						if (d1 < d || d == 0.0D)
						{
							entity = entity1;
							d = d1;
						}
					}
				}
			}

			if (entity != null)
				movingobjectposition = new MovingObjectPosition(entity);

			if (movingobjectposition != null && IC2.platform.isSimulating())
			{
				if (this.explosive)
				{
					this.explode();
					this.setDead();
					return;
				}

				if (movingobjectposition.entityHit != null)
				{
					LaserEvent.LaserHitsEntityEvent tEvent = new LaserEvent.LaserHitsEntityEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, movingobjectposition.entityHit);
					MinecraftForge.EVENT_BUS.post(tEvent);
					if (this.takeDataFromEvent(tEvent))
					{
						int damage = (int) this.power;
						if (damage > 0)
							/* TODO gamerforEA code replace, old code:
							if (entity != null)
							{
								entity.setFire(damage * (this.smelt ? 2 : 1));
							} */
							if (EventConfig.laserEvent && this.fake.cantDamage(tEvent.hitentity))
							{
								this.setDead();
								return;
							}
							else
								// TODO gamerforEA code end
								if (tEvent.hitentity.attackEntityFrom(new EntityDamageSourceIndirect("arrow", this, this.owner).setProjectile(), damage) && this.owner instanceof EntityPlayer && (tEvent.hitentity instanceof EntityDragon && ((EntityDragon) tEvent.hitentity).getHealth() <= 0.0F || tEvent.hitentity instanceof EntityDragonPart && ((EntityDragonPart) tEvent.hitentity).entityDragonObj instanceof EntityDragon && ((EntityLivingBase) ((EntityDragonPart) tEvent.hitentity).entityDragonObj).getHealth() <= 0.0F))
									IC2.achievements.issueAchievement((EntityPlayer) this.owner, "killDragonMiningLaser");

						this.setDead();
					}
				}
				else
				{
					LaserEvent.LaserHitsBlockEvent tEvent = new LaserEvent.LaserHitsBlockEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, movingobjectposition.blockX, movingobjectposition.blockY, movingobjectposition.blockZ, movingobjectposition.sideHit, 0.9F, true, true);
					MinecraftForge.EVENT_BUS.post(tEvent);

					// TODO gamerforEA code start
					if (tEvent.y > EventConfig.laserMaxBreakY || !EventConfig.laserBreakBlock || EventConfig.laserEvent && this.fake.cantBreak(tEvent.x, tEvent.y, tEvent.z))
					{
						this.setDead();
						return;
					}
					else
						// TODO gamerforEA code end
						if (this.takeDataFromEvent(tEvent))
						{
							Block tBlock = this.worldObj.getBlock(tEvent.x, tEvent.y, tEvent.z);
							if (tBlock != null && tBlock != Blocks.glass && tBlock != Blocks.glass_pane && !StackUtil.equals(tBlock, Ic2Items.reinforcedGlass))
								if (!this.canMine(tBlock))
									this.setDead();
								else if (IC2.platform.isSimulating())
								{
									float resis = 0.0F;
									resis = tBlock.getExplosionResistance(this, this.worldObj, tEvent.x, tEvent.y, tEvent.z, this.posX, this.posY, this.posZ) + 0.3F;
									this.power -= resis / 10.0F;
									if (this.power >= 0.0F)
									{
										if (tBlock.getMaterial() != Material.tnt && tBlock.getMaterial() != MaterialIC2TNT.instance)
										{
											if (this.smelt)
												if (tBlock.getMaterial() == Material.wood)
													tEvent.dropBlock = false;
												else
													for (ItemStack isa : tBlock.getDrops(this.worldObj, tEvent.x, tEvent.y, tEvent.z, this.worldObj.getBlockMetadata(tEvent.x, tEvent.y, tEvent.z), 0))
													{
														ItemStack is = FurnaceRecipes.smelting().getSmeltingResult(isa);
														if (is != null)
														{
															Block newBlock = StackUtil.getBlock(is);
															if (newBlock != null && newBlock != tBlock)
															{
																tEvent.removeBlock = false;
																tEvent.dropBlock = false;
																this.worldObj.setBlock(tEvent.x, tEvent.y, tEvent.z, newBlock, is.getItemDamage(), 7);
															}
															else
															{
																tEvent.dropBlock = false;
																float var6 = 0.7F;
																double var7 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
																double var9 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
																double var11 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
																EntityItem var13 = new EntityItem(this.worldObj, tEvent.x + var7, tEvent.y + var9, tEvent.z + var11, is.copy());
																var13.delayBeforeCanPickup = 10;
																this.worldObj.spawnEntityInWorld(var13);
															}

															this.power = 0.0F;
														}
													}
										}
										else
											tBlock.onBlockDestroyedByExplosion(this.worldObj, tEvent.x, tEvent.y, tEvent.z, new Explosion(this.worldObj, this, tEvent.x, tEvent.y, tEvent.z, 1.0F));

										if (tEvent.removeBlock)
										{
											if (tEvent.dropBlock)
												tBlock.dropBlockAsItemWithChance(this.worldObj, tEvent.x, tEvent.y, tEvent.z, this.worldObj.getBlockMetadata(tEvent.x, tEvent.y, tEvent.z), tEvent.dropChance, 0);

											this.worldObj.setBlockToAir(tEvent.x, tEvent.y, tEvent.z);
											if (this.worldObj.rand.nextInt(10) == 0 && tBlock.getMaterial().getCanBurn())
												this.worldObj.setBlock(tEvent.x, tEvent.y, tEvent.z, Blocks.fire, 0, 7);
										}

										--this.blockBreaks;
									}
								}
						}
				}
			}
			else
				this.power -= 0.5F;

			this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			this.range = (float) (this.range - Math.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ));
			if (this.isInWater())
				this.setDead();

		}
		else
		{
			if (this.explosive)
				this.explode();

			this.setDead();
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbttagcompound)
	{
		// TODO gamerforEA code start
		this.fake.writeToNBT(nbttagcompound);
		// TODO gamerforEA code end
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbttagcompound)
	{
		// TODO gamerforEA code start
		this.fake.readFromNBT(nbttagcompound);
		// TODO gamerforEA code end
	}

	@Override
	public float getShadowSize()
	{
		return 0.0F;
	}

	public boolean takeDataFromEvent(LaserEvent aEvent)
	{
		this.owner = aEvent.owner;
		this.range = aEvent.range;
		this.power = aEvent.power;
		this.blockBreaks = aEvent.blockBreaks;
		this.explosive = aEvent.explosive;
		this.smelt = aEvent.smelt;
		if (aEvent.isCanceled())
		{
			this.setDead();
			return false;
		}
		else
			return true;
	}

	public void explode()
	{
		if (IC2.platform.isSimulating())
		{
			LaserEvent.LaserExplodesEvent tEvent = new LaserEvent.LaserExplodesEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, 5.0F, 0.85F, 0.55F);
			MinecraftForge.EVENT_BUS.post(tEvent);
			if (this.takeDataFromEvent(tEvent))
			{
				ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, null, this.posX, this.posY, this.posZ, tEvent.explosionpower, tEvent.explosiondroprate);

				// TODO gamerforEA code start
				explosion.fake.setParent(this.fake);
				explosion.denyBlockBreak = !EventConfig.laserBreakBlock;
				// TODO gamerforEA code end

				explosion.doExplosion();
			}
		}

	}

	public boolean canMine(Block block)
	{
		return !unmineableBlocks.contains(block);
	}

	@Override
	public Entity getThrower()
	{
		return this.owner;
	}

	@Override
	public void setThrower(Entity entity)
	{
		if (entity instanceof EntityLivingBase)
			this.owner = (EntityLivingBase) entity;

	}
}
