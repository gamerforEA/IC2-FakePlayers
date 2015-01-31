package ic2.core.item.tool;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.IThrowableEntity;

public class EntityParticle extends Entity implements IThrowableEntity
{
	/* TODO gamerforEA code clear:
	private double coreSize;
	private double influenceSize;
	private int lifeTime;
	private Vector3[] radialTestVectors; */
	private Entity owner;

	public EntityParticle(World world)
	{
		super(world);
		/* TODO gamerforEA code clear:
		this.noClip = true;
		this.lifeTime = 6000; */
	}

	public EntityParticle(World world, EntityLivingBase owner, float speed, double coreSize, double influenceSize)
	{
		this(world);
		this.owner = owner;
		/* TODO gamerforEA code clear:
		this.coreSize = coreSize;
		this.influenceSize = influenceSize;
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
		this.motionZ = motion.z; */
	}

	@Override
	protected void entityInit()
	{
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
	{
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
	{
	}

	@Override
	public Entity getThrower()
	{
		return this.owner;
	}

	@Override
	public void setThrower(Entity entity)
	{
		this.owner = entity;
	}

	@Override
	public void onUpdate()
	{
		/* TODO gamerforEA code replace:
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

		List<Entity> entitiesToCheck = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(this.prevPosX, this.prevPosY, this.prevPosZ, this.posX, this.posY, this.posZ).expand(this.influenceSize, this.influenceSize, this.influenceSize));
		List<MovingObjectPosition> entitiesInfluences = new ArrayList();
		double minDistanceSq = start.distanceSquared(end);

		MovingObjectPosition len;
		for (Entity entity : entitiesToCheck)
		{
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

		double d = Math.sqrt(minDistanceSq) + this.influenceSize;

		for (MovingObjectPosition mop : entitiesInfluences)
		{
			len = mop;
			if (start.distance(len.hitVec) <= d)
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

			for (int i = 0; (double) i < var21; ++i)
			{
				for (int j = 0; j < this.radialTestVectors.length; ++j)
				{
					origin.copy(tmp).add(this.radialTestVectors[j]);
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
		} */
		this.setDead();
		// TODO gamerforEA code end
	}

	protected void onImpact(MovingObjectPosition hit)
	{
		/* TODO gamerforEA code clear:
		if (IC2.platform.isSimulating())
		{
			System.out.println("hit " + hit.typeOfHit + " " + hit.hitVec + " sim=" + IC2.platform.isSimulating());
			if (hit.typeOfHit == MovingObjectType.BLOCK && IC2.platform.isSimulating())
			{
			}

			ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, this.owner, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 18.0F, 0.95F, ExplosionIC2.Type.Heat);
			explosion.doExplosion();
		} */
	}

	protected void onInfluence(MovingObjectPosition hit)
	{
		/* TODO gamerforEA code clear:
		if (IC2.platform.isSimulating())
		{
			System.out.println("influenced " + hit.typeOfHit + " " + hit.hitVec + " sim=" + IC2.platform.isSimulating());
			if (hit.typeOfHit == MovingObjectType.BLOCK && IC2.platform.isSimulating())
			{
				Block block = this.worldObj.getBlock(hit.blockX, hit.blockY, hit.blockZ);
				if (block != Blocks.water && block != Blocks.flowing_water)
				{
					ItemStack existing = new ItemStack(block, 1, this.worldObj.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ));
					ItemStack smelted = FurnaceRecipes.smelting().getSmeltingResult(existing);
					if (smelted != null && smelted.getItem() instanceof ItemBlock)
					{
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
							this.worldObj.setBlock(x, y, z, Blocks.fire);
						}
					}
				}
				else
				{
					this.worldObj.setBlockToAir(hit.blockX, hit.blockY, hit.blockZ);
				}
			}
		} */
	}
}