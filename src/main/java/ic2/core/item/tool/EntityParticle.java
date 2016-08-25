package ic2.core.item.tool;

import java.util.ArrayList;
import java.util.List;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ModUtils;

import cpw.mods.fml.common.registry.IThrowableEntity;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.util.Quaternion;
import ic2.core.util.Vector3;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = new FakePlayerContainerEntity(ModUtils.profile, this);
	// TODO gamerforEA code end

	public EntityParticle(World world)
	{
		super(world);
		this.noClip = true;
		this.lifeTime = 6000;
	}

	public EntityParticle(World world, EntityLivingBase owner1, float speed, double coreSize1, double influenceSize1)
	{
		this(world);
		this.coreSize = coreSize1;
		this.influenceSize = influenceSize1;
		this.owner = owner1;
		this.setPosition(owner1.posX, owner1.posY + owner1.getEyeHeight(), owner1.posZ);
		Vector3 motion = new Vector3(owner1.getLookVec());
		Vector3 ortho = motion.copy().cross(Vector3.UP).scaleTo(influenceSize1);
		double stepAngle = Math.atan(0.5D / influenceSize1) * 2.0D;
		int steps = (int) Math.ceil(6.283185307179586D / stepAngle);
		Quaternion q = new Quaternion().setFromAxisAngle(motion, stepAngle);
		this.radialTestVectors = new Vector3[steps];
		this.radialTestVectors[0] = ortho.copy();

		for (int i = 1; i < steps; ++i)
		{
			q.rotate(ortho);
			this.radialTestVectors[i] = ortho.copy();
		}

		motion.scale(speed);
		this.motionX = motion.x;
		this.motionY = motion.y;
		this.motionZ = motion.z;

		// TODO gamerforEA code start
		if (owner1 instanceof EntityPlayer)
			this.fake.profile = ((EntityPlayer) owner1).getGameProfile();
		// TODO gamerforEA code end
	}

	@Override
	protected void entityInit()
	{
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound)
	{
		// TODO gamerforEA code start
		this.fake.readFromNBT(nbttagcompound);
		// TODO gamerforEA code end
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound)
	{
		// TODO gamerforEA code start
		this.fake.writeToNBT(nbttagcompound);
		// TODO gamerforEA code end
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

		List<Entity> entitiesToCheck = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, AxisAlignedBB.getBoundingBox(this.prevPosX, this.prevPosY, this.prevPosZ, this.posX, this.posY, this.posZ).expand(this.influenceSize, this.influenceSize, this.influenceSize));
		List<MovingObjectPosition> entitiesInfluences = new ArrayList();
		double minDistanceSq = start.distanceSquared(end);

		for (Entity entity : entitiesToCheck)
			if (entity != this.owner && entity.canBeCollidedWith())
			{
				MovingObjectPosition entityInfluence = entity.boundingBox.expand(this.influenceSize, this.influenceSize, this.influenceSize).calculateIntercept(start.toVec3(), end.toVec3());
				if (entityInfluence != null)
				{
					entitiesInfluences.add(entityInfluence);
					MovingObjectPosition entityHit = entity.boundingBox.expand(this.coreSize, this.coreSize, this.coreSize).calculateIntercept(start.toVec3(), end.toVec3());
					if (entityHit != null)
					{
						double distanceSq = start.distanceSquared(entityHit.hitVec);
						if (distanceSq < minDistanceSq)
						{
							hit = entityHit;
							minDistanceSq = distanceSq;
						}
					}
				}
			}

		double maxInfluenceDistance = Math.sqrt(minDistanceSq) + this.influenceSize;

		for (MovingObjectPosition entityInfluence : entitiesInfluences)
			if (start.distance(entityInfluence.hitVec) <= maxInfluenceDistance)
				this.onInfluence(entityInfluence);

		if (this.radialTestVectors != null)
		{
			Vector3 vForward = end.copy().sub(start);
			double len = vForward.length();
			vForward.scale(1.0D / len);
			Vector3 origin = new Vector3(start);
			Vector3 tmp = new Vector3();

			for (int d = 0; d < len; ++d)
			{
				for (Vector3 radialTestVector : this.radialTestVectors)
				{
					origin.copy(tmp).add(radialTestVector);
					MovingObjectPosition influence = this.worldObj.rayTraceBlocks(origin.toVec3(), tmp.toVec3(), true);
					if (influence != null)
						this.onInfluence(influence);
				}

				origin.add(vForward);
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
				this.setDead();
		}

	}

	protected void onImpact(MovingObjectPosition hit)
	{
		if (IC2.platform.isSimulating())
		{
			// TODO gamerforEA code clear: System.out.println("hit " + hit.typeOfHit + " " + hit.hitVec + " sim=" + IC2.platform.isSimulating());
			if (hit.typeOfHit == MovingObjectType.BLOCK && IC2.platform.isSimulating())
				;

			ExplosionIC2 explosion = new ExplosionIC2(this.worldObj, this.owner, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, 18.0F, 0.95F, ExplosionIC2.Type.Heat);

			// TODO gamerforEA code start
			explosion.fake.profile = this.fake.profile;
			// TODO gamerforEA code end

			explosion.doExplosion();
		}
	}

	protected void onInfluence(MovingObjectPosition hit)
	{
		if (IC2.platform.isSimulating())
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
			if (EventConfig.plasmaEvent && EventUtils.cantBreak(this.fake.getPlayer(), hit.blockX, hit.blockY, hit.blockZ))
			return;
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
			if (EventConfig.plasmaEvent && EventUtils.cantBreak(this.fake.getPlayer(), x, y, z))
			return;
			// TODO gamerforEA code end

			this.worldObj.setBlock(x, y, z, Blocks.fire);
			}
			}
			}
			else
			{
			// TODO gamerforEA code start
			if (EventConfig.plasmaEvent && EventUtils.cantBreak(this.fake.getPlayer(), hit.blockX, hit.blockY, hit.blockZ))
			return;
			// TODO gamerforEA code end

			this.worldObj.setBlockToAir(hit.blockX, hit.blockY, hit.blockZ);
			}
			}
	}
}
