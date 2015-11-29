package ic2.core.block.machine.tileentity;

import java.util.Random;

import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.ic2.EventConfig;

import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.comp.Energy;
import ic2.core.block.comp.Redstone;
import ic2.core.item.armor.ItemArmorHazmat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

public class TileEntityTesla extends TileEntityBlock
{
	protected final Redstone redstone = this.addComponent(new Redstone(this));
	protected final Energy energy = this.addComponent(Energy.asBasicSink(this, 10000.0D, 2));
	private int ticker = IC2.random.nextInt(32);

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		if (this.redstone.hasRedstoneInput())
			if (this.energy.useEnergy(1.0D) && this.ticker++ % 32 == 0)
			{
				int damage = (int) this.energy.getEnergy() / 400;
				if (damage > 0 && this.shock(damage))
					this.energy.useEnergy(damage * 400);
			}
	}

	protected boolean shock(int damage)
	{
		int r = 4;

		for (EntityLivingBase entity : (Iterable<EntityLivingBase>) this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(this.xCoord - 4, this.yCoord - 4, this.zCoord - 4, this.xCoord + 4 + 1, this.yCoord + 4 + 1, this.zCoord + 4 + 1)))
			// TODO gamerforEA add condition [2, 3]
			if (!ItemArmorHazmat.hasCompleteHazmat(entity) || EventConfig.teslaEvent && EventUtils.cantDamage(this.fake.getPlayer(), entity) || entity.attackEntityFrom(IC2DamageSource.electricity, damage))
			{
				if (this.worldObj instanceof WorldServer)
				{
					WorldServer world = (WorldServer) this.worldObj;
					Random rnd = world.rand;

					for (int i = 0; i < damage; ++i)
						world.func_147487_a("reddust", entity.posX + rnd.nextFloat() - 0.5D, entity.posY + rnd.nextFloat() * 2.0F - 1.0D, entity.posZ + rnd.nextFloat() - 0.5D, 0, 0.10000000149011612D, 0.10000000149011612D, 1.0D, 1.0D);
				}

				return true;
			}

		return false;
	}
}
