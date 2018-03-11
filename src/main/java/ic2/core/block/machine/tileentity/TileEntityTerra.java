package ic2.core.block.machine.tileentity;

import com.gamerforea.ic2.EventConfig;
import com.gamerforea.ic2.ITerraformingBPFakePlayer;
import com.gamerforea.ic2.ModUtils;
import ic2.api.item.ITerraformingBP;
import ic2.core.IC2;
import ic2.core.audio.AudioSource;
import ic2.core.audio.PositionSpec;
import ic2.core.block.invslot.InvSlotConsumableClass;
import ic2.core.util.StackUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityTerra extends TileEntityElectricMachine
{
	public int failedAttempts = 0;
	public int lastX = -1;
	public int lastY = -1;
	public int lastZ = -1;
	public AudioSource audioSource;
	public int inactiveTicks = 0;
	public final InvSlotConsumableClass tfbpSlot = new InvSlotConsumableClass(this, "tfbp", 0, 1, ITerraformingBP.class);

	public TileEntityTerra()
	{
		super(100000, 3, -1);
	}

	@Override
	public String getInventoryName()
	{
		return "Terraformer";
	}

	@Override
	protected void updateEntityServer()
	{
		super.updateEntityServer();
		boolean newActive = false;
		if (!this.tfbpSlot.isEmpty())
		{
			ITerraformingBP tfbp = (ITerraformingBP) this.tfbpSlot.get().getItem();
			if (this.energy >= tfbp.getConsume())
			{
				newActive = true;
				int x = this.xCoord;
				int z = this.zCoord;
				int range = 1;
				if (this.lastY > -1)
				{
					range = tfbp.getRange() / 10;
					x = this.lastX - this.worldObj.rand.nextInt(range + 1) + this.worldObj.rand.nextInt(range + 1);
					z = this.lastZ - this.worldObj.rand.nextInt(range + 1) + this.worldObj.rand.nextInt(range + 1);
				}
				else
				{
					if (this.failedAttempts > 4)
						this.failedAttempts = 4;

					range = tfbp.getRange() * (this.failedAttempts + 1) / 5;
					x = x - this.worldObj.rand.nextInt(range + 1) + this.worldObj.rand.nextInt(range + 1);
					z = z - this.worldObj.rand.nextInt(range + 1) + this.worldObj.rand.nextInt(range + 1);
				}

				// TODO gamerforEA code replace, old code: if (tfbp.terraform(this.worldObj, x, z, this.yCoord))
				if (tfbp instanceof ITerraformingBPFakePlayer ? ((ITerraformingBPFakePlayer) tfbp).terraform(this.worldObj, x, z, this.yCoord, this.fake.get()) : tfbp.terraform(this.worldObj, x, z, this.yCoord))
				// TODO gamerforEA code end
				{
					this.energy -= tfbp.getConsume();
					this.failedAttempts = 0;
					this.lastX = x;
					this.lastZ = z;
					this.lastY = this.yCoord;
				}
				else
				{
					this.energy -= tfbp.getConsume() / 10;
					++this.failedAttempts;
					this.lastY = -1;
				}
			}
		}

		if (newActive)
		{
			this.inactiveTicks = 0;
			this.setActive(true);
		}
		else if (!newActive && this.getActive() && this.inactiveTicks++ > 30)
			this.setActive(false);

	}

	@Override
	public void onUnloaded()
	{
		if (IC2.platform.isRendering() && this.audioSource != null)
		{
			IC2.audioManager.removeSources(this);
			this.audioSource = null;
		}

		super.onUnloaded();
	}

	@Override
	public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage)
	{
		if (this.energy >= this.maxEnergy)
			return amount;
		else
		{
			this.energy += amount;
			return 0.0D;
		}
	}

	public boolean ejectBlueprint()
	{
		if (this.tfbpSlot.isEmpty())
			return false;
		else
		{
			if (IC2.platform.isSimulating())
			{
				StackUtil.dropAsEntity(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.tfbpSlot.get());
				this.tfbpSlot.clear();
			}

			return true;
		}
	}

	public void insertBlueprint(ItemStack tfbp)
	{
		this.ejectBlueprint();
		this.tfbpSlot.put(tfbp);
	}

	public static int getFirstSolidBlockFrom(World world, int x, int z, int y)
	{
		while (y > 0)
		{
			if (world.isBlockNormalCubeDefault(x, y, z, false))
				return y;

			--y;
		}

		return -1;
	}

	public static int getFirstBlockFrom(World world, int x, int z, int y)
	{
		while (y > 0)
		{
			if (!world.isAirBlock(x, y, z))
				return y;

			--y;
		}

		return -1;
	}

	// TODO gamerforEA code start
	public static boolean switchGround(World world, Block from, Block to, int x, int y, int z, boolean upwards)
	{
		return switchGround(world, from, to, x, y, z, upwards, ModUtils.getModFake(world));
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add EntityPlayer
	public static boolean switchGround(World world, Block from, Block to, int x, int y, int z, boolean upwards, EntityPlayer player)
	{
		if (upwards)
		{
			++y;
			int saveY = y;

			while (true)
			{
				Block block = world.getBlock(x, y - 1, z);
				if (block != from)
					if (saveY == y)
						return false;
					else
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
							return false;
						// TODO gamerforEA code end

						world.setBlock(x, y, z, to, 0, 7);
						return true;
					}

				--y;
			}
		}
		else
			while (true)
			{
				Block block = world.getBlock(x, y, z);
				if (block != to)
				{
					block = world.getBlock(x, y, z);
					if (block != from)
						return false;
					else
					{
						// TODO gamerforEA code start
						if (EventConfig.terraEvent && ModUtils.cantBreakOrNotInPrivate(player, x, y, z))
							return false;
						// TODO gamerforEA code end

						world.setBlock(x, y, z, to, 0, 7);
						return true;
					}
				}

				--y;
			}
	}

	public static BiomeGenBase getBiomeAt(World world, int x, int z)
	{
		return world.getChunkFromBlockCoords(x, z).getBiomeGenForWorldCoords(x & 15, z & 15, world.getWorldChunkManager());
	}

	public static void setBiomeAt(World world, int x, int z, BiomeGenBase biome)
	{
		Chunk chunk = world.getChunkFromBlockCoords(x, z);
		byte[] array = chunk.getBiomeArray();
		array[(z & 15) << 4 | x & 15] = (byte) (biome.biomeID & 255);
		chunk.setBiomeArray(array);
	}

	@Override
	public void onNetworkUpdate(String field)
	{
		if (field.equals("active") && this.prevActive != this.getActive())
		{
			if (this.audioSource == null)
				this.audioSource = IC2.audioManager.createSource(this, PositionSpec.Center, "Terraformers/TerraformerGenericloop.ogg", true, false, IC2.audioManager.getDefaultVolume());

			if (this.getActive())
			{
				if (this.audioSource != null)
					this.audioSource.play();
			}
			else if (this.audioSource != null)
				this.audioSource.stop();
		}

		super.onNetworkUpdate(field);
	}
}
