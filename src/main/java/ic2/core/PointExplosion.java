package ic2.core;

import ic2.core.util.Util;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class PointExplosion extends Explosion
{
	private final World world;
	private final float dropRate;
	private final int entityDamage;

	public PointExplosion(World world1, Entity entity, double x, double y, double z, float power, float dropRate1, int entityDamage1)
	{
		super(world1, entity, x, y, z, power);
		this.world = world1;
		this.dropRate = dropRate1;
		this.entityDamage = entityDamage1;
	}

	public void doExplosionA()
	{
		// TODO gamerforEA code start
		if (this.world != null) return;
		// TODO gamerforEA code end

		for (int entitiesInRange = Util.roundToNegInf(this.explosionX) - 1; entitiesInRange <= Util.roundToNegInf(this.explosionX) + 1; ++entitiesInRange)
		{
			for (int i = Util.roundToNegInf(this.explosionY) - 1; i <= Util.roundToNegInf(this.explosionY) + 1; ++i)
			{
				for (int entity = Util.roundToNegInf(this.explosionZ) - 1; entity <= Util.roundToNegInf(this.explosionZ) + 1; ++entity)
				{
					Block block = this.world.getBlock(entitiesInRange, i, entity);
					if (block.getExplosionResistance(this.exploder, this.world, entitiesInRange, i, entity, this.explosionX, this.explosionY, this.explosionZ) < this.explosionSize * 10.0F)
					{
						this.affectedBlockPositions.add(new ChunkPosition(entitiesInRange, i, entity));
					}
				}
			}
		}

		for (Entity entity : (List<Entity>) this.world.getEntitiesWithinAABBExcludingEntity(this.exploder, AxisAlignedBB.getBoundingBox(this.explosionX - 2.0D, this.explosionY - 2.0D, this.explosionZ - 2.0D, this.explosionX + 2.0D, this.explosionY + 2.0D, this.explosionZ + 2.0D)))
		{
			entity.attackEntityFrom(DamageSource.setExplosionSource(this), (float) this.entityDamage);
		}

		this.explosionSize = 1.0F / this.dropRate;
	}
}