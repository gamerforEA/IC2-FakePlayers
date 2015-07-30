package ic2.core.block.machine.tileentity;

import java.util.List;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.FakePlayerUtils;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.block.TileEntityBlock;
import ic2.core.item.armor.ItemArmorHazmat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityTesla extends TileEntityBlock implements IEnergySink
{
	public double energy = 0.0D;
	public int ticker = 0;
	public int maxEnergy = 10000;
	public boolean addedToEnergyNet = false;

	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);

		try
		{
			this.energy = nbttagcompound.getDouble("energy");
		}
		catch (Exception var3)
		{
			this.energy = (double) nbttagcompound.getShort("energy");
		}
	}

	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setDouble("energy", this.energy);
	}

	public void onLoaded()
	{
		super.onLoaded();
		if (IC2.platform.isSimulating())
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			this.addedToEnergyNet = true;
		}
	}

	public void onUnloaded()
	{
		if (IC2.platform.isSimulating() && this.addedToEnergyNet)
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			this.addedToEnergyNet = false;
		}

		super.onUnloaded();
	}

	public boolean enableUpdateEntity()
	{
		return IC2.platform.isSimulating();
	}

	public void updateEntity()
	{
		super.updateEntity();
		if (IC2.platform.isSimulating() && this.redstoned())
		{
			if (this.energy >= (double) getCost())
			{
				int damage = (int) this.energy / getCost();
				--this.energy;
				if (this.ticker++ % 32 == 0 && this.shock(damage))
				{
					this.energy = 0.0D;
				}
			}
		}
	}

	public boolean shock(int damage)
	{
		boolean shock = false;
		List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox((double) (this.xCoord - 4), (double) (this.yCoord - 4), (double) (this.zCoord - 4), (double) (this.xCoord + 5), (double) (this.yCoord + 5), (double) (this.zCoord + 5)));

		for (int l = 0; l < entities.size(); ++l)
		{
			EntityLivingBase entity = entities.get(l);
			if (!ItemArmorHazmat.hasCompleteHazmat(entity))
			{
				// TODO gamerforEA code start
				if (EventConfig.teslaEvent && FakePlayerUtils.cantDamage(this.getOwnerFake(), entity)) continue;
				// TODO gamerforEA code end
				shock = true;
				entity.attackEntityFrom(IC2DamageSource.electricity, (float) damage);

				for (int i = 0; i < damage; ++i)
				{
					this.worldObj.spawnParticle("reddust", entity.posX + (double) this.worldObj.rand.nextFloat(), entity.posY + (double) (this.worldObj.rand.nextFloat() * 2.0F), entity.posZ + (double) this.worldObj.rand.nextFloat(), 0.0D, 0.0D, 1.0D);
				}
			}
		}

		return shock;
	}

	public boolean redstoned()
	{
		return this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord) || this.worldObj.isBlockIndirectlyGettingPowered(this.xCoord, this.yCoord, this.zCoord);
	}

	public static int getCost()
	{
		return 400;
	}

	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction)
	{
		return true;
	}

	public double getDemandedEnergy()
	{
		return (double) this.maxEnergy - this.energy;
	}

	public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage)
	{
		if (this.energy >= (double) this.maxEnergy)
		{
			return amount;
		}
		else
		{
			this.energy += amount;
			return 0.0D;
		}
	}

	public int getSinkTier()
	{
		return 2;
	}
}