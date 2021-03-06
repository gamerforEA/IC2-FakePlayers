package ic2.core.block.kineticgenerator.tileentity;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.LazyChunkCache;
import com.google.common.base.Preconditions;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.energy.tile.IKineticSource;
import ic2.api.item.IKineticRotor;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.WorldData;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableClass;
import ic2.core.block.invslot.InvSlotConsumableKineticRotor;
import ic2.core.block.kineticgenerator.container.ContainerWindKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuiWindKineticGenerator;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.Util;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.BitSet;
import java.util.List;
import java.util.Vector;

public class TileEntityWindKineticGenerator extends TileEntityInventory implements IKineticSource, IHasGui
{
	public final InvSlotConsumableClass rotorSlot = new InvSlotConsumableKineticRotor(this, "rotorslot", 0, InvSlot.Access.IO, 1, InvSlot.InvSide.ANY, IKineticRotor.GearboxType.WIND);
	private double windStrength;
	private int obstructedCrossSection;
	private int crossSection;
	private int updateTicker = IC2.random.nextInt(this.getTickRate());
	private float rotationSpeed;
	private static final double efficiencyRollOffExponent = 2.0D;
	private static final int nominalRotationPeriod = 500;
	public static final float outputModifier = 10.0F * ConfigUtil.getFloat(MainConfig.get(), "balance/energy/kineticgenerator/wind");

	// TODO gamerforEA code start
	private int ticksSkipped;

	private boolean tryTick()
	{
		int skipTicksAmount = EventConfig.skipWindGeneratorTicksAmount;
		if (skipTicksAmount <= 0)
			return true;

		this.ticksSkipped++;
		if (this.ticksSkipped > skipTicksAmount)
		{
			this.ticksSkipped = 0;
			return true;
		}

		return false;
	}
	// TODO gamerforEA code end

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		if (this.updateTicker++ % this.getTickRate() == 0)
		{
			// TODO gamerforEA code start
			if (!this.tryTick())
				return;
			// TODO gamerforEA code end

			boolean needsInvUpdate;
			if (!this.rotorSlot.isEmpty())
				// TODO gamerforEA code replace, old code:
				// if (this.checkSpace(1, true) == 0)
				if (this.checkSpace(1, true, 1) == 0)
				// TODO gamerforEA code end
				{
					if (!this.getActive())
						this.setActive(true);

					needsInvUpdate = true;
				}
				else
				{
					if (this.getActive())
						this.setActive(false);

					needsInvUpdate = true;
				}
			else
			{
				if (this.getActive())
					this.setActive(false);

				needsInvUpdate = true;
			}

			if (this.getActive())
			{
				this.crossSection = this.getRotorDiameter() / 2 * 2 * 2 + 1;
				this.crossSection *= this.crossSection;
				this.obstructedCrossSection = this.checkSpace(this.getRotorDiameter() * 3, false);
				if (this.obstructedCrossSection > 0 && this.obstructedCrossSection <= (this.getRotorDiameter() + 1) / 2)
					this.obstructedCrossSection = 0;

				if (this.obstructedCrossSection < 0)
				{
					this.windStrength = 0.0D;
					this.setRotationSpeed(0.0F);
				}
				else
				{
					this.windStrength = this.calcWindStrength();
					float speed = (float) Util.limit((this.windStrength - (double) this.getMinWindStrength()) / (double) this.getMaxWindStrength(), 0.0D, 2.0D);
					this.setRotationSpeed(speed);
					if (this.windStrength >= (double) this.getMinWindStrength())
					{
						/* TODO gamerforEA code replace, old code:
						if (this.windStrength <= (double) this.getMaxWindStrength())
							this.rotorSlot.damage(1, false);
						else
							this.rotorSlot.damage(4, false); */
						int damage = EventConfig.windRotorDamage;
						if (this.windStrength <= (double) this.getMaxWindStrength())
							this.rotorSlot.damage(damage, false);
						else
							this.rotorSlot.damage(damage * 4, false);
						// TODO gamerforEA code end
					}
				}
			}

			if (needsInvUpdate)
				this.markDirty();

		}
	}

	@Override
	public List<String> getNetworkedFields()
	{
		List<String> ret = new Vector(1);
		ret.add("rotationSpeed");
		ret.add("rotorSlot");
		ret.addAll(super.getNetworkedFields());
		return ret;
	}

	@Override
	public ContainerBase<TileEntityWindKineticGenerator> getGuiContainer(EntityPlayer entityPlayer)
	{
		return new ContainerWindKineticGenerator(entityPlayer, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer entityPlayer, boolean isAdmin)
	{
		return new GuiWindKineticGenerator(new ContainerWindKineticGenerator(entityPlayer, this));
	}

	public boolean facingMatchesDirection(ForgeDirection direction)
	{
		return direction.ordinal() == this.getFacing();
	}

	@Override
	public boolean wrenchCanSetFacing(EntityPlayer entityPlayer, int side)
	{
		return side != 0 && side != 1 && this.getFacing() != side;
	}

	@Override
	public void setFacing(short side)
	{
		super.setFacing(side);
	}

	public String getRotorhealth()
	{
		return !this.rotorSlot.isEmpty() ? StatCollector.translateToLocalFormatted("ic2.WindKineticGenerator.gui.rotorhealth", (int) (100.0F - (float) this.rotorSlot.get().getItemDamage() / (float) this.rotorSlot.get().getMaxDamage() * 100.0F)) : "";
	}

	@Override
	public int maxrequestkineticenergyTick(ForgeDirection directionFrom)
	{
		return this.getKuOutput();
	}

	@Override
	public int requestkineticenergy(ForgeDirection directionFrom, int requestkineticenergy)
	{
		return this.facingMatchesDirection(directionFrom.getOpposite()) ? Math.min(requestkineticenergy, this.getKuOutput()) : 0;
	}

	@Override
	public String getInventoryName()
	{
		return "Wind Kinetic Generator";
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer)
	{
	}

	@Override
	public boolean shouldRenderInPass(int pass)
	{
		return pass == 0;
	}

	// TODO gamerforEA code end
	private static final ThreadLocal<BitSet> VALID_HEIGHT_SET = new ThreadLocal<>();

	public int checkSpace(int length, boolean onlyrotor)
	{
		return this.checkSpace(length, onlyrotor, Integer.MAX_VALUE);
	}

	private static BitSet provideValidHeightSet(int size)
	{
		BitSet bitSet = VALID_HEIGHT_SET.get();
		if (bitSet != null && size <= bitSet.length())
		{
			bitSet.clear();
			return bitSet;
		}

		bitSet = new BitSet(size);
		VALID_HEIGHT_SET.set(bitSet);
		return bitSet;
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add maxOccupiedCount:int parameter
	public int checkSpace(int length, boolean onlyrotor, int maxOccupiedCount)
	{
		// TODO gamerforEA code start
		Preconditions.checkArgument(maxOccupiedCount > 0);
		// TODO gamerforEA code end

		int box = this.getRotorDiameter() / 2;
		int lentemp = 0;
		if (onlyrotor)
		{
			length = 1;
			lentemp = length + 1;
		}

		if (!onlyrotor)
			box *= 2;

		ForgeDirection fwdDir = ForgeDirection.VALID_DIRECTIONS[this.getFacing()];
		ForgeDirection rightDir = fwdDir.getRotation(ForgeDirection.DOWN);
		int xMaxDist = Math.abs(length * fwdDir.offsetX + box * rightDir.offsetX);
		int zMaxDist = Math.abs(length * fwdDir.offsetZ + box * rightDir.offsetZ);
		int occupiedCount = 0;

		// TODO gamerforEA code replace, old code:
		// ChunkCache chunkCache = new ChunkCache(this.worldObj, this.xCoord - xMaxDist, this.yCoord - box, this.zCoord - zMaxDist, this.xCoord + xMaxDist, this.yCoord + box, this.zCoord + zMaxDist, 0);
		IBlockAccess chunkCache;
		boolean optimize = EventConfig.optimizeWindGenerator;
		if (optimize)
			chunkCache = new LazyChunkCache(this.worldObj, this.xCoord - xMaxDist, this.yCoord - box, this.zCoord - zMaxDist, this.xCoord + xMaxDist, this.yCoord + box, this.zCoord + zMaxDist, false);
		else
			chunkCache = new ChunkCache(this.worldObj, this.xCoord - xMaxDist, this.yCoord - box, this.zCoord - zMaxDist, this.xCoord + xMaxDist, this.yCoord + box, this.zCoord + zMaxDist, 0);

		int minX = this.xCoord - xMaxDist;
		int maxX = this.xCoord + xMaxDist;
		int minZ = this.zCoord - zMaxDist;
		int maxZ = this.zCoord + zMaxDist;
		int xSize = maxX - minX + 1;
		int zSize = maxZ - minZ + 1;
		boolean optimizeByHeight = optimize && EventConfig.optimizeWindGeneratorByHeight && maxOccupiedCount > 1;
		BitSet validHeightSet = optimizeByHeight ? provideValidHeightSet(xSize * zSize) : null;
		if (validHeightSet != null)
		{
			int minY = this.yCoord - box;
			for (int right = -box; right <= box; ++right)
			{
				for (int fwd = lentemp - length; fwd <= length; ++fwd)
				{
					int x = this.xCoord + fwd * fwdDir.offsetX + right * rightDir.offsetX;
					int z = this.zCoord + fwd * fwdDir.offsetZ + right * rightDir.offsetZ;

					assert Math.abs(x - this.xCoord) <= xMaxDist;
					assert Math.abs(z - this.zCoord) <= zMaxDist;

					Chunk chunk = ((LazyChunkCache) chunkCache).getChunkFromBlockCoords(x, z);
					boolean isValidHeight = chunk != null && chunk.getHeightValue(x & 15, z & 15) >= minY;
					if (isValidHeight)
					{
						int localX = x - minX;
						int localZ = z - minZ;
						int index = localX * xSize + localZ;
						validHeightSet.set(index, true);
					}
				}
			}
		}
		// TODO gamerforEA code end

		for (int up = -box; up <= box; ++up)
		{
			int y = this.yCoord + up;

			// TODO gamerforEA code start
			if (y < 0 || y > 255)
				continue;
			// TODO gamerforEA code end

			for (int right = -box; right <= box; ++right)
			{
				boolean occupied = false;

				for (int fwd = lentemp - length; fwd <= length; ++fwd)
				{
					int x = this.xCoord + fwd * fwdDir.offsetX + right * rightDir.offsetX;
					int z = this.zCoord + fwd * fwdDir.offsetZ + right * rightDir.offsetZ;

					assert Math.abs(x - this.xCoord) <= xMaxDist;
					assert Math.abs(z - this.zCoord) <= zMaxDist;

					// TODO gamerforEA code start
					if (validHeightSet != null)
					{
						int localX = x - minX;
						int localZ = z - minZ;
						int index = localX * xSize + localZ;
						if (!validHeightSet.get(index))
							continue;
					}
					// TODO gamerforEA code end

					Block block = chunkCache.getBlock(x, y, z);
					if (!block.isAir(chunkCache, x, y, z))
					{
						occupied = true;

						// TODO gamerforEA code start
						if (onlyrotor)
							break;
						// TODO gamerforEA code end

						if ((up != 0 || right != 0 || fwd != 0) && chunkCache.getTileEntity(x, y, z) instanceof TileEntityWindKineticGenerator && !onlyrotor)
							return -1;
					}
				}

				if (occupied)
				{
					++occupiedCount;

					// TODO gamerforEA code start
					if (occupiedCount >= maxOccupiedCount)
						return occupiedCount;
					// TODO gamerforEA code end
				}
			}
		}

		return occupiedCount;
	}

	public boolean checkrotor()
	{
		return !this.rotorSlot.isEmpty();
	}

	public boolean rotorspace()
	{
		// TODO gamerforEA code replace, old code:
		// return this.checkSpace(1, true) == 0;
		return this.checkSpace(1, true, 1) == 0;
		// TODO gamerforEA code end
	}

	private void setRotationSpeed(float speed)
	{
		if (this.rotationSpeed != speed)
		{
			this.rotationSpeed = speed;
			IC2.network.get().updateTileEntityField(this, "rotationSpeed");
		}

	}

	public int getTickRate()
	{
		return 32;
	}

	public double calcWindStrength()
	{
		double windStr = WorldData.get(this.worldObj).windSim.getWindAt((double) this.yCoord);
		windStr = windStr * (1.0D - Math.pow((double) this.obstructedCrossSection / (double) this.crossSection, 2.0D));
		return Math.max(0.0D, windStr);
	}

	public float getAngle()
	{
		if (this.rotationSpeed > 0.0F)
		{
			long period = (long) (5.0E8F / this.rotationSpeed);
			return (float) (System.nanoTime() % period) / (float) period * 360.0F;
		}
		return 0.0F;
	}

	public float getefficiency()
	{
		ItemStack stack = this.rotorSlot.get();
		return stack != null && stack.getItem() instanceof IKineticRotor ? ((IKineticRotor) stack.getItem()).getEfficiency(stack) : 0.0F;
	}

	public int getMinWindStrength()
	{
		ItemStack stack = this.rotorSlot.get();
		return stack != null && stack.getItem() instanceof IKineticRotor ? ((IKineticRotor) stack.getItem()).getMinWindStrength(stack) : 0;
	}

	public int getMaxWindStrength()
	{
		ItemStack stack = this.rotorSlot.get();
		return stack != null && stack.getItem() instanceof IKineticRotor ? ((IKineticRotor) stack.getItem()).getMaxWindStrength(stack) : 0;
	}

	public int getRotorDiameter()
	{
		ItemStack stack = this.rotorSlot.get();
		return stack != null && stack.getItem() instanceof IKineticRotor ? ((IKineticRotor) stack.getItem()).getDiameter(stack) : 0;
	}

	public ResourceLocation getRotorRenderTexture()
	{
		ItemStack stack = this.rotorSlot.get();
		return stack != null && stack.getItem() instanceof IKineticRotor ? ((IKineticRotor) stack.getItem()).getRotorRenderTexture(stack) : new ResourceLocation(IC2.textureDomain, "textures/items/rotors/rotorWoodmodel.png");
	}

	public boolean guiisoverload()
	{
		return this.windStrength > (double) this.getMaxWindStrength();
	}

	public boolean guiisminWindStrength()
	{
		return this.windStrength >= (double) this.getMinWindStrength();
	}

	public int getKuOutput()
	{
		return this.windStrength >= (double) this.getMinWindStrength() && this.getActive() ? (int) (this.windStrength * (double) outputModifier * (double) this.getefficiency()) : 0;
	}

	public int getWindStrength()
	{
		return (int) this.windStrength;
	}

	@Override
	public void setActive(boolean active)
	{
		if (active != this.getActive())
			IC2.network.get().updateTileEntityField(this, "rotorSlot");

		super.setActive(active);
	}
}
