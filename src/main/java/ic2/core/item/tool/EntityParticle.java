package ic2.core.item.tool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.FakePlayerUtils;

import cpw.mods.fml.common.registry.IThrowableEntity;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.util.Quaternion;
import ic2.core.util.Vector3;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class EntityParticle extends Entity implements IThrowableEntity
{
	private double coreSize;
	private double influenceSize;
	private int lifeTime;
	private Entity owner;
	private Vector3[] radialTestVectors;

	public EntityParticle(World world)
	{
		super(world);
		this.noClip = true;
		this.lifeTime = 6000;
	}

	public EntityParticle(World world, EntityLivingBase owner, float speed, double coreSize, double influenceSize)
	{
		this(world);
		this.coreSize = coreSize;
		this.influenceSize = influenceSize;
		this.owner = owner;
		this.setPosition(owner.posX, owner.posY + (double) owner.getEyeHeight(), owner.posZ);
		Vector3 motion = new Vector3(owner.getLookVec());
		Vector3 ortho = motion.copy().cross(Vector3.UP).scaleTo(influenceSize);
		double stepAngle = Math.atan(0.5D / influenceSize) * 2.0D;
		int steps = (int) Math.ceil(6.283185307179586D / stepAngle);
		Quaternion q = (new Quaternion()).setFromAxisAngle(motion, stepAngle);
		this.radialTestVectors = new Vector3[steps];
		this.radialTestVectors[0] = ortho.copy();

		for (int i = 1; i < steps; ++i)
		{
			q.rotate(ortho);
			this.radialTestVectors[i] = ortho.copy();
		}

		motion.scale((double) speed);
		this.motionX = motion.x;
		this.motionY = motion.y;
		this.motionZ = motion.z;
	}

	protected void entityInit()
	{
	}

	protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
	{
	}

	protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
	{
	}

	public Entity getThrower()
	{
		return this.owner;
	}

	public void setThrower(Entity entity)
	{
		this.owner = entity;
	}

	public void onUpdate()
	{
		// TODO gamerforEA code start
		if (!EventConfig.plasmaEnabled)
		{
			this.setDead();
			return;
		}
		// TODO gamerforEA code end
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		Vector3 start = new Vector3(this.prevPosX, this.prevPosY, this.prevPosZ);
		Vector3 end = new Vector3(this.posX, this.posY, this.posZ);
		MovingObjectPosition hit = this.worldObj.rayTraceBlocks(start.toVec3(), end.toVec3(), true);
		if (hit != null)
		{
			end.set(hit.hitVec);
			this.posX = hit.hitVec.xCoord;
			this.posY = hit.hitVec.yCoord;
			this.posZ = hit.hitVec.zCoord;
		}

		List entitiesToCheck = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(this.prevPosX, this.prevPosY, this.prevPosZ, this.posX, this.posY, this.posZ).expand(this.influenceSize, this.influenceSize, this.influenceSize));
		ArrayList entitiesInfluences = new ArrayList();
		double minDistanceSq = start.distanceSquared(end);
		Iterator maxInfluenceDistance = entitiesToCheck.iterator();

		MovingObjectPosition len;
		while (maxInfluenceDistance.hasNext())
		{
			Entity entity = (Entity) maxInfluenceDistance.next();
			if (entity != this.owner && entity.canBeCollidedWith())
			{
				MovingObjectPosition vForward = entity.boundingBox.expand(this.influenceSize, this.influenceSize, this.influenceSize).calculateIntercept(start.toVec3(), end.toVec3());
				if (vForward != null)
				{
					entitiesInfluences.add(vForward);
					len = entity.boundingBox.expand(this.coreSize, this.coreSize, this.coreSize).calculateIntercept(start.toVec3(), end.toVec3());
					if (len != null)
					{
						double distanceSq = start.distanceSquared(len.hitVec);
						if (distanceSq < minDistanceSq)
						{
							hit = len;
							minDistanceSq = distanceSq;
						}
					}
				}
			}
		}

		double var18 = Math.sqrt(minDistanceSq) + this.influenceSize;
		Iterator var19 = entitiesInfluences.iterator();

		while (var19.hasNext())
		{
			len = (MovingObjectPosition) var19.next();
			if (start.distance(len.hitVec) <= var18)
			{
				this.onInfluence(len);
			}
		}

		if (this.radialTestVectors != null)
		{
			Vector3 var20 = end.copy().sub(start);
			double var21 = var20.length();
			var20.scale(1.0D / var21);
			Vector3 origin = new Vector3(start);
			Vector3 tmp = new Vector3();

			for (int d = 0; (double) d < var21; ++d)
			{
				for (int i = 0; i < this.radialTestVectors.length; ++i)
				{
					origin.copy(tmp).add(this.radialTestVectors[i]);
					MovingObjectPosition influence = this.worldObj.rayTraceBlocks(origin.toVec3(), tmp.toVec3(), true);
					if (influence != null)
					{
						this.onInfluence(influence);
					}
				}

				origin.add(var20);
			}
		}

		if (hit != null)
		{
			this.onImpact(hit);
			this.setDead();
		}
		else
		{
			--this.lifeTime;
			if (this.lifeTime <= 0)
			{
				this.setDead();
			}
		}
	}

	protected void onImpact(MovingObjectPosition hit)
	{
		if (IC2.platform.isSimulating())
		{
			// TODO gamerforEA code clear: System.out.println("hit " + hit.typeOfHit + " " + hit.hitVec + " sim=" + IC2.platform.isSimulating());
			ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, this.owner, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 18.0F, 0.95F, ExplosionIC2.Type.Heat);
			explosion.doExplosion();
		}
	}

	protected void onInfluence(MovingObjectPosition hit)
	{
		if (IC2.platform.isSimulating())
		{
			// TODO gamerforEA code clear: System.out.println("influenced " + hit.typeOfHit + " " + hit.hitVec + " sim=" + IC2.platform.isSimulating());
			if (hit.typeOfHit == MovingObjectType.BLOCK && IC2.platform.isSimulating())
			{
				Block block = this.worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
				if (block != Blocks.water && block != Blocks.flowing_water)
				{
					ItemStack existing = new ItemStack(block, 1, this.worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ));
					ItemStack smelted = FurnaceRecipes.smelting().getSmeltingResult(existing);
					if (smelted != null && smelted.getItem() instanceof ItemBlock)
					{
						// TODO gamerforEA code start
						if (EventConfig.plasmaEvent && FakePlayerUtils.isInPrivate(this.worldObj, hit.blockX, hit.blockY, hit.blockZ)) return;
						// TODO gamerforEA code end
						this.worldObj.setBlock(hit.blockX, hit.blockY, hit.blockZ, ((ItemBlock) smelted.getItem()).field_150939_a, smelted.getItemDamage(), 3);
					}
					else
					{
						ForgeDirection side = ForgeDirection.VALID_DIRECTIONS[hit.sideHit];
						if (block.isFlammable(this.worldObj, hit.blockX, hit.blockY, hit.blockZ, side))
						{
							int x = hit.blockX - side.offsetX;
							int y = hit.blockY - side.offsetY;
							int z = hit.blockZ - side.offsetZ;
							// TODO gamerforEA code start
							if (EventConfig.plasmaEvent && FakePlayerUtils.isInPrivate(this.worldObj, x, y, z)) return;
							// TODO gamerforEA code end
							this.worldObj.setBlock(x, y, z, Blocks.fire);
						}
					}
				}
				else
				{
					// TODO gamerforEA code start
					if (EventConfig.plasmaEvent && FakePlayerUtils.isInPrivate(this.worldObj, hit.blockX, hit.blockY, hit.blockZ)) return;
					// TODO gamerforEA code end
					this.worldObj.setBlockToAir(hit.blockX, hit.blockY, hit.blockZ);
				}
			}
		}
	}
}