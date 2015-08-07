package ic2.core.item.tool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.FakePlayerUtils;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;

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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

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
	public GameProfile ownerProfile;
	private FakePlayer ownerFake;

	public FakePlayer getOwnerFake()
	{
		if (this.ownerFake != null)
			return this.ownerFake;
		else if (this.ownerProfile != null)
			return this.ownerFake = FakePlayerUtils.create(this.worldObj, this.ownerProfile);
		else
			return FakePlayerUtils.getModFake(this.worldObj);
	}
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
		double sinYaw = Math.sin(yaw);
		double cosYaw = Math.cos(yaw);
		double cosPitch = Math.cos(pitch);
		double x = entityliving.posX - cosYaw * 0.16D;
		double z = entityliving.posZ - sinYaw * 0.16D;
		double startMotionX = -sinYaw * cosPitch;
		double startMotionY = -Math.sin(pitch);
		double startMotionZ = cosYaw * cosPitch;
		this.setPosition(x, y, z);
		this.setLaserHeading(startMotionX, startMotionY, startMotionZ, 1.0D);
		this.range = range;
		this.power = power;
		this.blockBreaks = blockBreaks;
		this.explosive = explosive;
		// TODO gamerforEA code start
		if (entityliving instanceof EntityPlayer)
			this.ownerProfile = ((EntityPlayer) entityliving).getGameProfile();
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
		if (IC2.platform.isSimulating() && (this.range < 1.0F || this.power <= 0.0F || this.blockBreaks <= 0))
		{
			if (this.explosive)
				this.explode();

			this.setDead();
		}
		else
		{
			++this.ticksInAir;
			Vec3 oldPosition = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			Vec3 newPosition = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition mop = this.worldObj.func_147447_a(oldPosition, newPosition, false, true, false);
			oldPosition = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			if (mop != null)
				newPosition = Vec3.createVectorHelper(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
			else
				newPosition = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			Entity entity = null;
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double d = 0.0D;

			float resis;
			for (int tEvent = 0; tEvent < list.size(); ++tEvent)
			{
				Entity entity1 = (Entity) list.get(tEvent);
				if (entity1.canBeCollidedWith() && (entity1 != this.owner || this.ticksInAir >= 5))
				{
					resis = 0.3F;
					AxisAlignedBB axis = entity1.boundingBox.expand(resis, resis, resis);
					MovingObjectPosition mop1 = axis.calculateIntercept(oldPosition, newPosition);
					if (mop1 != null)
					{
						double distance = oldPosition.distanceTo(mop1.hitVec);
						if (distance < d || d == 0.0D)
						{
							entity = entity1;
							d = distance;
						}
					}
				}
			}

			if (entity != null)
				mop = new MovingObjectPosition(entity);

			if (mop != null && IC2.platform.isSimulating())
			{
				if (this.explosive)
				{
					this.explode();
					this.setDead();
					return;
				}

				if (mop.entityHit != null)
				{
					LaserEvent.LaserHitsEntityEvent event = new LaserEvent.LaserHitsEntityEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, mop.entityHit);
					MinecraftForge.EVENT_BUS.post(event);
					if (this.takeDataFromEvent(event))
					{
						int powerI = (int) this.power;
						if (powerI > 0)
							/* TODO gamerforEA code replace, old code:
							if (entity != null)
							{
								entity.setFire(powerI * (this.smelt ? 2 : 1));
							}

							if (event.hitentity.attackEntityFrom((new EntityDamageSourceIndirect("arrow", this, this.owner)).setProjectile(), (float) powerI) && this.owner instanceof EntityPlayer && (event.hitentity instanceof EntityDragon && ((EntityDragon) event.hitentity).getHealth() <= 0.0F || event.hitentity instanceof EntityDragonPart && ((EntityDragonPart) event.hitentity).entityDragonObj instanceof EntityDragon && ((EntityLivingBase) ((EntityDragonPart) event.hitentity).entityDragonObj).getHealth() <= 0.0F))
							{
								IC2.achievements.issueAchievement((EntityPlayer) this.owner, "killDragonMiningLaser");
							} */
							if (EventConfig.laserEvent && FakePlayerUtils.cantDamage(this.getOwnerFake(), event.hitentity))
							{
								this.setDead();
								return;
							}
							else
							// TODO gamerforEA code end
							if (event.hitentity.attackEntityFrom(new EntityDamageSourceIndirect("arrow", this, this.owner).setProjectile(), powerI) && this.owner instanceof EntityPlayer && (event.hitentity instanceof EntityDragon && ((EntityDragon) event.hitentity).getHealth() <= 0.0F || event.hitentity instanceof EntityDragonPart && ((EntityDragonPart) event.hitentity).entityDragonObj instanceof EntityDragon && ((EntityLivingBase) ((EntityDragonPart) event.hitentity).entityDragonObj).getHealth() <= 0.0F))
								IC2.achievements.issueAchievement((EntityPlayer) this.owner, "killDragonMiningLaser");

						this.setDead();
					}
				}
				else
				{
					LaserEvent.LaserHitsBlockEvent event = new LaserEvent.LaserHitsBlockEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit, 0.9F, true, true);
					MinecraftForge.EVENT_BUS.post(event);
					// TODO gamerforEA code start
					if (EventConfig.laserEvent && FakePlayerUtils.cantBreak(event.x, event.y, event.z, this.getOwnerFake()))
					{
						this.setDead();
						return;
					}
					else
					// TODO gamerforEA code end
					if (this.takeDataFromEvent(event))
					{
						Block block = this.worldObj.getBlock(event.x, event.y, event.z);
						if (block != null && block != Blocks.glass && block != Blocks.glass_pane && !StackUtil.equals(block, Ic2Items.reinforcedGlass))
							if (!this.canMine(block))
								this.setDead();
							else if (IC2.platform.isSimulating())
							{
								resis = 0.0F;
								resis = block.getExplosionResistance(this, this.worldObj, event.x, event.y, event.z, this.posX, this.posY, this.posZ) + 0.3F;
								this.power -= resis / 10.0F;
								if (this.power >= 0.0F)
								{
									if (block.getMaterial() != Material.tnt && block.getMaterial() != MaterialIC2TNT.instance)
									{
										if (this.smelt)
											if (block.getMaterial() == Material.wood)
												event.dropBlock = false;
											else
											{
												Iterator var27 = block.getDrops(this.worldObj, event.x, event.y, event.z, this.worldObj.getBlockMetadata(event.x, event.y, event.z), 0).iterator();

												while (var27.hasNext())
												{
													ItemStack var28 = (ItemStack) var27.next();
													ItemStack var29 = FurnaceRecipes.smelting().getSmeltingResult(var28);
													if (var29 != null)
													{
														Block newBlock = StackUtil.getBlock(var29);
														if (newBlock != null && newBlock != block)
														{
															event.removeBlock = false;
															event.dropBlock = false;
															this.worldObj.setBlock(event.x, event.y, event.z, newBlock, var29.getItemDamage(), 7);
														}
														else
														{
															event.dropBlock = false;
															float var6 = 0.7F;
															double var7 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
															double var9 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
															double var11 = this.worldObj.rand.nextFloat() * var6 + (1.0F - var6) * 0.5D;
															EntityItem var13 = new EntityItem(this.worldObj, event.x + var7, event.y + var9, event.z + var11, var29.copy());
															var13.delayBeforeCanPickup = 10;
															this.worldObj.spawnEntityInWorld(var13);
														}

														this.power = 0.0F;
													}
												}
											}
									}
									else
										block.onBlockDestroyedByExplosion(this.worldObj, event.x, event.y, event.z, new Explosion(this.worldObj, this, event.x, event.y, event.z, 1.0F));

									if (event.removeBlock)
									{
										if (event.dropBlock)
											block.dropBlockAsItemWithChance(this.worldObj, event.x, event.y, event.z, this.worldObj.getBlockMetadata(event.x, event.y, event.z), event.dropChance, 0);

										this.worldObj.setBlockToAir(event.x, event.y, event.z);
										if (this.worldObj.rand.nextInt(10) == 0 && block.getMaterial().getCanBurn())
											this.worldObj.setBlock(event.x, event.y, event.z, Blocks.fire, 0, 7);
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
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt)
	{
		// TODO gamerforEA code start
		if (this.ownerProfile != null)
		{
			nbt.setString("ownerUUID", this.ownerProfile.getId().toString());
			nbt.setString("ownerName", this.ownerProfile.getName());
		}
		// TODO gamerforEA code end
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt)
	{
		// TODO gamerforEA code start
		String uuid = nbt.getString("ownerUUID");
		if (!Strings.isNullOrEmpty(uuid))
		{
			String name = nbt.getString("ownerName");
			if (!Strings.isNullOrEmpty(name))
				this.ownerProfile = new GameProfile(UUID.fromString(uuid), name);
		}
		// TODO gamerforEA code end
	}

	@Override
	public float getShadowSize()
	{
		return 0.0F;
	}

	public boolean takeDataFromEvent(LaserEvent event)
	{
		this.owner = event.owner;
		this.range = event.range;
		this.power = event.power;
		this.blockBreaks = event.blockBreaks;
		this.explosive = event.explosive;
		this.smelt = event.smelt;
		if (event.isCanceled())
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
			LaserEvent.LaserExplodesEvent event = new LaserEvent.LaserExplodesEvent(this.worldObj, this, this.owner, this.range, this.power, this.blockBreaks, this.explosive, this.smelt, 5.0F, 0.85F, 0.55F);
			MinecraftForge.EVENT_BUS.post(event);
			if (this.takeDataFromEvent(event))
			{
				ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, (Entity) null, this.posX, this.posY, this.posZ, event.explosionpower, event.explosiondroprate);
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