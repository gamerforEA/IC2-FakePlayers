package ic2.core.crop;

import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.FastUtils;
import com.gamerforea.ic2.ModUtils;
import ic2.api.crops.BaseSeed;
import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropTile;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.item.ItemCropSeed;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.StatCollector;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

public class TileEntityCrop extends TileEntity implements INetworkDataProvider, INetworkUpdateListener, ICropTile
{
	public byte humidity = -1;
	public byte nutrients = -1;
	public byte airQuality = -1;
	private static final boolean debug = false;
	private CropCard crop = null;
	public int size = 0;
	public int statGrowth = 0;
	public int statGain = 0;
	public int statResistance = 0;
	public int scanLevel = 0;
	public NBTTagCompound customData = new NBTTagCompound();
	public int nutrientStorage = 0;
	public int waterStorage = 0;
	public int exStorage = 0;
	public int growthPoints = 0;
	public boolean upgraded = false;
	public char ticker;
	public boolean dirty;
	public static int tickRate = 256;
	public int weedlevel;
	public int Infestedlevel;

	public TileEntityCrop()
	{
		this.ticker = (char) IC2.random.nextInt(tickRate);
		this.dirty = true;
		this.weedlevel = 0;
		this.Infestedlevel = 0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		if (nbt.hasKey("cropOwner") && nbt.hasKey("cropName"))
			this.crop = Crops.instance.getCropCard(nbt.getString("cropOwner"), nbt.getString("cropName"));
		else if (nbt.hasKey("cropid"))
			this.crop = IC2Crops.getCropFromId(nbt.getShort("cropid"));

		this.size = nbt.getByte("size");
		this.statGrowth = nbt.getByte("statGrowth");
		this.statGain = nbt.getByte("statGain");
		this.statResistance = nbt.getByte("statResistance");
		if (nbt.hasKey("data0"))
			for (int x = 0; x < 16; ++x)
			{
				this.customData.setShort("legacy" + x, nbt.getShort("data" + x));
			}
		else if (nbt.hasKey("customData"))
			this.customData = nbt.getCompoundTag("customData");

		this.growthPoints = nbt.getInteger("growthPoints");
		this.nutrientStorage = nbt.getInteger("nutrientStorage");
		this.waterStorage = nbt.getInteger("waterStorage");
		this.exStorage = nbt.getInteger("exStorage");
		this.upgraded = nbt.getBoolean("upgraded");
		this.scanLevel = nbt.getByte("scanLevel");
		this.weedlevel = nbt.getInteger("weedlevel");
		this.Infestedlevel = nbt.getInteger("Infestedlevel");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		if (this.crop != null)
		{
			nbt.setString("cropOwner", this.crop.owner());
			nbt.setString("cropName", this.crop.name());
		}

		nbt.setByte("size", (byte) this.size);
		nbt.setByte("statGrowth", (byte) this.statGrowth);
		nbt.setByte("statGain", (byte) this.statGain);
		nbt.setByte("statResistance", (byte) this.statResistance);
		nbt.setTag("customData", this.customData);
		nbt.setInteger("growthPoints", this.growthPoints);
		nbt.setInteger("nutrientStorage", this.nutrientStorage);
		nbt.setInteger("waterStorage", this.waterStorage);
		nbt.setInteger("exStorage", this.exStorage);
		nbt.setBoolean("upgraded", this.upgraded);
		nbt.setByte("scanLevel", (byte) this.scanLevel);
		nbt.setInteger("weedlevel", this.weedlevel);
		nbt.setInteger("Infestedlevel", this.Infestedlevel);
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		++this.ticker;
		if (this.ticker % tickRate == 0)
			this.tick();

		if (this.dirty)
		{
			this.dirty = false;
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.worldObj.updateLightByType(EnumSkyBlock.Block, this.xCoord, this.yCoord, this.zCoord);
			if (IC2.platform.isSimulating())
				for (String field : this.getNetworkedFields())
				{
					IC2.network.get().updateTileEntityField(this, field);
				}
		}

	}

	@Override
	public List<String> getNetworkedFields()
	{
		List<String> ret = new ArrayList<>(4);
		ret.add("crop");
		ret.add("size");
		ret.add("upgraded");
		ret.add("customData");
		ret.add("weedlevel");
		ret.add("Infestedlevel");
		return ret;
	}

	public void tick()
	{
		if (IC2.platform.isSimulating())
		{
			if (this.ticker % (tickRate << 2) == 0)
				this.humidity = this.updateHumidity();

			if ((this.ticker + tickRate) % (tickRate << 2) == 0)
				this.nutrients = this.updateNutrients();

			if ((this.ticker + tickRate * 2) % (tickRate << 2) == 0)
				this.airQuality = this.updateAirQuality();

			if (this.crop == null)
			{
				if (!this.upgraded || !this.attemptCrossing())
				{
					if (IC2.random.nextInt(100) != 0 || this.hasEx())
					{
						if (this.exStorage > 0 && IC2.random.nextInt(10) == 0)
							--this.exStorage;

						return;
					}

					this.reset();
					this.crop = IC2Crops.weed;
					this.size = 1;
				}

				assert this.crop != null;
			}

			this.crop.tick(this);
			if (this.crop.canGrow(this))
			{
				this.growthPoints += this.calcGrowthRate();
				if (this.crop == null)
					return;

				if (this.growthPoints >= this.crop.growthDuration(this))
				{
					this.growthPoints = 0;
					++this.size;
					this.dirty = true;
				}
			}

			if (this.nutrientStorage > 0)
				--this.nutrientStorage;

			if (this.waterStorage > 0)
				--this.waterStorage;

			if (this.crop.isWeed(this) && IC2.random.nextInt(50) - this.statGrowth <= 2)
				this.generateWeed();

		}
	}

	public void generateWeed()
	{
		int x = this.xCoord;
		int y = this.yCoord;
		int z = this.zCoord;
		switch (IC2.random.nextInt(4))
		{
			case 0:
				++x;
			case 1:
				--x;
			case 2:
				++z;
			case 3:
				--z;
		}

		if (this.worldObj.getTileEntity(x, y, z) instanceof TileEntityCrop)
		{
			TileEntityCrop teCrop = (TileEntityCrop) this.worldObj.getTileEntity(x, y, z);
			CropCard neighborCrop = teCrop.getCrop();
			if (neighborCrop == null || !neighborCrop.isWeed(teCrop) && IC2.random.nextInt(32) >= teCrop.statResistance && !teCrop.hasEx())
			{
				int newGrowth = Math.max(this.statGrowth, teCrop.statGrowth);
				if (newGrowth < 31 && IC2.random.nextBoolean())
					++newGrowth;

				teCrop.reset();
				teCrop.crop = IC2Crops.weed;
				teCrop.size = 1;
				teCrop.statGrowth = (byte) newGrowth;
			}
		}
		else if (this.worldObj.isAirBlock(x, y, z))
		{
			Block block = this.worldObj.getBlock(x, y - 1, z);
			if (block == Blocks.dirt || block == Blocks.grass || block == Blocks.farmland)
			{
				this.worldObj.setBlock(x, y - 1, z, Blocks.grass, 0, 7);
				this.worldObj.setBlock(x, y, z, Blocks.tallgrass, 1, 7);
			}
		}

	}

	public boolean hasEx()
	{
		if (this.exStorage > 0)
		{
			this.exStorage -= 5;
			return true;
		}
		return false;
	}

	public boolean attemptCrossing()
	{
		if (IC2.random.nextInt(3) != 0)
			return false;
		List<TileEntityCrop> cropTes = new ArrayList<>(4);
		this.askCropJoinCross(this.xCoord - 1, this.yCoord, this.zCoord, cropTes);
		this.askCropJoinCross(this.xCoord + 1, this.yCoord, this.zCoord, cropTes);
		this.askCropJoinCross(this.xCoord, this.yCoord, this.zCoord - 1, cropTes);
		this.askCropJoinCross(this.xCoord, this.yCoord, this.zCoord + 1, cropTes);
		if (cropTes.size() < 2)
			return false;
		CropCard[] crops = Crops.instance.getCrops().toArray(new CropCard[0]);
		if (crops.length == 0)
			return false;
		int[] ratios = new int[crops.length];
		int total = 0;

		for (int i = 0; i < ratios.length; ++i)
		{
			CropCard crop = crops[i];
			if (crop.canGrow(this))
				for (TileEntityCrop te : cropTes)
				{
					total += this.calculateRatioFor(crop, te.getCrop());
				}

			ratios[i] = total;
		}

		int search = IC2.random.nextInt(total);
		int min = 0;
		int max = ratios.length - 1;

		while (min < max)
		{
			int cur = (min + max) / 2;
			int value = ratios[cur];
			if (search < value)
				max = cur;
			else
				min = cur + 1;
		}

		assert min == max;

		assert min >= 0 && min < ratios.length;

		assert ratios[min] > search;

		assert min == 0 || ratios[min - 1] <= search;

		this.upgraded = false;
		this.crop = crops[min];
		this.dirty = true;
		this.size = 1;
		this.statGrowth = 0;
		this.statResistance = 0;
		this.statGain = 0;

		for (TileEntityCrop te : cropTes)
		{
			this.statGrowth += te.statGrowth;
			this.statResistance += te.statResistance;
			this.statGain += te.statGain;
		}

		int count = cropTes.size();
		this.statGrowth /= count;
		this.statResistance /= count;
		this.statGain /= count;
		this.statGrowth += IC2.random.nextInt(1 + 2 * count) - count;
		this.statGain += IC2.random.nextInt(1 + 2 * count) - count;
		this.statResistance += IC2.random.nextInt(1 + 2 * count) - count;
		this.statGrowth = Util.limit(this.statGrowth, 0, 31);
		this.statGain = Util.limit(this.statGain, 0, 31);
		this.statResistance = Util.limit(this.statResistance, 0, 31);
		return true;
	}

	public int calculateRatioFor(CropCard newCrop, CropCard oldCrop)
	{
		if (newCrop == oldCrop)
			return 500;
		int value = 0;

		for (int i = 0; i < 5; ++i)
		{
			int delta = Math.abs(newCrop.stat(i) - oldCrop.stat(i));
			value += -delta + 2;
		}

		for (String attributeNew : newCrop.attributes())
		{
			for (String attributeOld : oldCrop.attributes())
			{
				if (attributeNew.equalsIgnoreCase(attributeOld))
					value += 5;
			}
		}

		int diff = newCrop.tier() - oldCrop.tier();
		if (diff > 1)
			value -= 2 * diff;

		if (diff < -3)
			value -= -diff;

		return Math.max(value, 0);
	}

	public void askCropJoinCross(int x, int y, int z, List<TileEntityCrop> crops)
	{
		TileEntity te = this.worldObj.getTileEntity(x, y, z);
		if (te instanceof TileEntityCrop)
		{
			TileEntityCrop sideCrop = (TileEntityCrop) te;
			CropCard neighborCrop = sideCrop.getCrop();
			if (neighborCrop != null)
				if (neighborCrop.canGrow(this) && neighborCrop.canCross(sideCrop))
				{
					int base = 4;
					if (sideCrop.statGrowth >= 16)
						++base;

					if (sideCrop.statGrowth >= 30)
						++base;

					if (sideCrop.statResistance >= 28)
						base += 27 - sideCrop.statResistance;

					if (base >= IC2.random.nextInt(20))
						crops.add(sideCrop);

				}
		}
	}

	public boolean leftClick(EntityPlayer player)
	{
		if (this.crop == null)
		{
			if (this.upgraded)
			{
				this.upgraded = false;
				this.dirty = true;
				if (IC2.platform.isSimulating())
					StackUtil.dropAsEntity(this.worldObj, this.xCoord, this.yCoord, this.zCoord, new ItemStack(Ic2Items.crop.getItem()));

				return true;
			}
			return false;
		}
		return this.crop.leftclick(this, player);
	}

	@Override
	public boolean pick(boolean manual)
	{
		if (this.crop == null)
			return false;
		boolean bonus = this.harvest(false);
		float firstchance = this.crop.dropSeedChance(this);

		for (int i = 0; i < this.statResistance; ++i)
		{
			firstchance *= 1.1F;
		}

		int drop = 0;
		if (bonus)
		{
			if (IC2.random.nextFloat() <= (firstchance + 1.0F) * 0.8F)
				++drop;

			float chance = this.crop.dropSeedChance(this) + (float) this.statGrowth / 100.0F;
			if (!manual)
				chance *= 0.8F;

			for (int i = 23; i < this.statGain; ++i)
			{
				chance *= 0.95F;
			}

			if (IC2.random.nextFloat() <= chance)
				++drop;
		}
		else if (IC2.random.nextFloat() <= firstchance * 1.5F)
			++drop;

		ItemStack[] re = new ItemStack[drop];

		for (int i = 0; i < drop; ++i)
		{
			re[i] = this.crop.getSeeds(this);
		}

		this.reset();
		if (IC2.platform.isSimulating() && re.length > 0)
			for (ItemStack stack : re)
			{
				if (stack.getItem() != Ic2Items.cropSeed.getItem())
					stack.stackTagCompound = null;

				StackUtil.dropAsEntity(this.worldObj, this.xCoord, this.yCoord, this.zCoord, stack);
			}

		return true;
	}

	public boolean rightClick(EntityPlayer player)
	{
		ItemStack current = player.getCurrentEquippedItem();
		boolean creative = player.capabilities.isCreativeMode;
		if (current != null)
		{
			if (this.crop == null)
			{
				if (current.getItem() == Ic2Items.crop.getItem() && !this.upgraded)
				{
					if (!creative)
					{
						--current.stackSize;
						if (current.stackSize <= 0)
							player.inventory.mainInventory[player.inventory.currentItem] = null;
					}

					this.upgraded = true;
					this.dirty = true;
					return true;
				}

				if (this.applyBaseSeed(player))
					return true;
			}

			if (current.getItem() == Items.water_bucket || current.getItem() == Ic2Items.waterCell.getItem())
			{
				if (this.waterStorage < 10)
				{
					this.waterStorage = 10;
					return true;
				}

				return current.getItem() == Items.water_bucket;
			}

			if (current.getItem() == Items.wheat_seeds)
			{
				if (this.nutrientStorage <= 50)
				{
					this.nutrientStorage += 25;
					--current.stackSize;
					if (current.stackSize <= 0)
						player.inventory.mainInventory[player.inventory.currentItem] = null;

					return true;
				}

				return false;
			}

			if (current.getItem() == Items.dye && current.getItemDamage() == 15 || current.getItem() == Ic2Items.fertilizer.getItem())
			{
				if (this.applyFertilizer(true))
				{
					if (creative)
						return true;
					--current.stackSize;
					if (current.stackSize <= 0)
						player.inventory.mainInventory[player.inventory.currentItem] = null;

					return true;
				}
				return false;
			}

			if (current.getItem() == Ic2Items.hydratingCell.getItem())
			{
				if (this.applyHydration(true, current, player))
				{
					if (current.stackSize <= 0)
						player.inventory.mainInventory[player.inventory.currentItem] = null;

					return true;
				}

				return false;
			}

			if (current.getItem() == Ic2Items.weedEx.getItem() && this.applyWeedEx(true))
			{
				current.damageItem(1, player);
				if (current.stackSize <= 0)
					player.inventory.mainInventory[player.inventory.currentItem] = null;

				return true;
			}
		}

		if (this.crop == null)
			return false;
		return this.crop.rightclick(this, player);
	}

	public boolean applyBaseSeed(EntityPlayer player)
	{
		ItemStack current = player.getCurrentEquippedItem();
		BaseSeed seed = Crops.instance.getBaseSeed(current);
		if (seed != null)
		{
			if (current.stackSize < seed.stackSize)
				return false;

			if (this.tryPlantIn(seed.crop, seed.size, seed.statGrowth, seed.statGain, seed.statResistance, 1))
			{
				if (player.capabilities.isCreativeMode)
					return true;

				if (current.getItem().hasContainerItem(current))
				{
					if (current.stackSize > 1)
						return false;

					player.inventory.mainInventory[player.inventory.currentItem] = current.getItem().getContainerItem(current);
				}
				else
				{
					current.stackSize -= seed.stackSize;
					if (current.stackSize <= 0)
						player.inventory.mainInventory[player.inventory.currentItem] = null;
				}

				return true;
			}
		}

		return false;
	}

	public boolean tryPlantIn(CropCard crop, int si, int statGr, int statGa, int statRe, int scan)
	{
		if (crop != null && crop != IC2Crops.weed && !this.upgraded)
		{
			if (!crop.canGrow(this))
				return false;
			this.reset();
			this.crop = crop;
			this.size = (byte) si;
			this.statGrowth = (byte) statGr;
			this.statGain = (byte) statGa;
			this.statResistance = (byte) statRe;
			this.scanLevel = (byte) scan;
			return true;
		}
		return false;
	}

	public boolean applyFertilizer(boolean manual)
	{
		if (this.nutrientStorage >= 100)
			return false;
		this.nutrientStorage += manual ? 100 : 90;
		return true;
	}

	public boolean applyHydration(TileEntityCropmatron cropmatron)
	{
		if (this.waterStorage >= 200)
			return false;
		int apply = 200 - this.waterStorage;
		FluidStack drain = cropmatron.getFluidTank().drain(apply, true);
		if (drain != null)
		{
			this.waterStorage += drain.amount;
			return true;
		}
		return false;
	}

	public boolean applyHydration(boolean manual, ItemStack itemStack, EntityPlayer player)
	{
		if ((manual || this.waterStorage < 180) && this.waterStorage < 200)
		{
			int apply = manual ? 200 - this.waterStorage : 180 - this.waterStorage;
			apply = Math.min(apply, itemStack.getMaxDamage() - itemStack.getItemDamage());
			if (!player.capabilities.isCreativeMode && itemStack.attemptDamageItem(apply, IC2.random))
				player.inventory.mainInventory[player.inventory.currentItem] = Ic2Items.cell;

			this.waterStorage += apply;
			return true;
		}
		return false;
	}

	public boolean applyWeedEx(boolean manual)
	{
		if ((this.exStorage < 100 || !manual) && this.exStorage < 150)
		{
			this.exStorage += 50;
			boolean triggerDecline;
			if (manual)
				triggerDecline = this.worldObj.rand.nextInt(5) == 0;
			else
				triggerDecline = this.worldObj.rand.nextInt(3) == 0;

			if (this.crop != null && this.crop.isWeed(this) && this.exStorage >= 75 && triggerDecline)
				switch (this.worldObj.rand.nextInt(5))
				{
					case 0:
						if (this.statGrowth > 0)
							--this.statGrowth;
					case 1:
						if (this.statGain > 0)
							--this.statGain;
					default:
						if (this.statResistance > 0)
							--this.statResistance;
				}

			return true;
		}
		return false;
	}

	@Override
	public ItemStack[] harvest_automated(boolean optimal)
	{
		if (this.crop == null)
			return null;
		if (!this.crop.canBeHarvested(this))
			return null;
		if (optimal && this.size != this.crop.getOptimalHavestSize(this))
			return null;
		double chance = (double) this.crop.dropGainChance();
		chance = chance * Math.pow(1.03D, (double) this.statGain);
		int dropCount = (int) Math.max(0L, Math.round(IC2.random.nextGaussian() * chance * 0.6827D + chance));
		ItemStack[] ret = new ItemStack[dropCount];

		for (int i = 0; i < dropCount; ++i)
		{
			ret[i] = this.crop.getGain(this);
			if (ret[i] != null && IC2.random.nextInt(100) <= this.statGain)
				++ret[i].stackSize;
		}

		this.size = this.crop.getSizeAfterHarvest(this);
		this.dirty = true;
		return ret;
	}

	@Override
	public boolean harvest(boolean manual)
	{
		ItemStack[] drops = this.harvest_automated(false);
		if (drops == null)
			return false;
		if (IC2.platform.isSimulating() && drops.length > 0)
			for (ItemStack drop : drops)
			{
				StackUtil.dropAsEntity(this.worldObj, this.xCoord, this.yCoord, this.zCoord, drop);
			}

		return true;
	}

	public void onNeighbourChange()
	{
		if (this.crop != null)
			this.crop.onNeighbourChange(this);
	}

	public int emitRedstone()
	{
		return this.crop == null ? 0 : this.crop.emitRedstone(this);
	}

	public void onBlockDestroyed()
	{
		if (this.crop != null)
			this.crop.onBlockDestroyed(this);
	}

	public int getEmittedLight()
	{
		return this.crop == null ? 0 : this.crop.getEmittedLight(this);
	}

	@Override
	public byte getHumidity()
	{
		if (this.humidity == -1)
			this.humidity = this.updateHumidity();

		return this.humidity;
	}

	@Override
	public byte getNutrients()
	{
		if (this.nutrients == -1)
			this.nutrients = this.updateNutrients();

		return this.nutrients;
	}

	@Override
	public byte getAirQuality()
	{
		if (this.airQuality == -1)
			this.airQuality = this.updateAirQuality();

		return this.airQuality;
	}

	public byte updateHumidity()
	{
		int value = Crops.instance.getHumidityBiomeBonus(this.worldObj.getBiomeGenForCoords(this.xCoord, this.zCoord));
		if (this.worldObj.getBlockMetadata(this.xCoord, this.yCoord - 1, this.zCoord) >= 7)
			value += 2;

		if (this.waterStorage >= 5)
			value += 2;

		value = value + (this.waterStorage + 24) / 25;
		return (byte) value;
	}

	public byte updateNutrients()
	{
		int value = Crops.instance.getNutrientBiomeBonus(this.worldObj.getBiomeGenForCoords(this.xCoord, this.zCoord));

		for (int i = 2; i < 5 && this.worldObj.getBlock(this.xCoord, this.yCoord - i, this.zCoord) == Blocks.dirt; ++i)
		{
			++value;
		}

		value = value + (this.nutrientStorage + 19) / 20;
		return (byte) value;
	}

	public byte updateAirQuality()
	{
		int value = 0;
		int height = (this.yCoord - 64) / 15;
		if (height > 4)
			height = 4;

		if (height < 0)
			height = 0;

		value = value + height;
		int fresh = 9;

		for (int x = this.xCoord - 1; x <= this.xCoord + 1 && fresh > 0; ++x)
		{
			for (int z = this.zCoord - 1; z <= this.zCoord + 1 && fresh > 0; ++z)
			{
				if (this.worldObj.isBlockNormalCubeDefault(x, this.yCoord, z, false) || this.worldObj.getTileEntity(x, this.yCoord, z) instanceof TileEntityCrop)
					--fresh;
			}
		}

		value = value + fresh / 2;
		if (this.worldObj.canBlockSeeTheSky(this.xCoord, this.yCoord + 1, this.zCoord))
			value += 2;

		return (byte) value;
	}

	public int updateMultiCulture()
	{
		Set<CropCard> crops = new HashSet<>();

		for (int x = -1; x < 1; ++x)
		{
			for (int z = -1; z < 1; ++z)
			{
				TileEntity te = this.worldObj.getTileEntity(x + this.xCoord, this.yCoord, z + this.zCoord);
				if (te instanceof TileEntityCrop)
				{
					CropCard neighborCrop = ((TileEntityCrop) te).getCrop();
					if (neighborCrop != null)
						crops.add(neighborCrop);
				}
			}
		}

		return crops.size() - 1;
	}

	public void addIfNotPresent(CropCard crop, LinkedList<CropCard> crops)
	{
		for (int i = 0; i < crops.size(); ++i)
		{
			if (crop == crops.get(i))
				return;
		}

		crops.add(crop);
	}

	public int calcGrowthRate()
	{
		if (this.crop == null)
			return 0;
		int base = 3 + IC2.random.nextInt(7) + this.statGrowth;
		int need = (this.crop.tier() - 1) * 4 + this.statGrowth + this.statGain + this.statResistance;
		if (need < 0)
			need = 0;

		int have = this.crop.weightInfluences(this, (float) this.getHumidity(), (float) this.getNutrients(), (float) this.getAirQuality()) * 5;
		if (have >= need)
			base = base * (100 + have - need) / 100;
		else
		{
			int neg = (need - have) * 4;
			if (neg > 100 && IC2.random.nextInt(32) > this.statResistance)
			{
				this.reset();
				base = 0;
			}
			else
			{
				base = base * (100 - neg) / 100;
				if (base < 0)
					base = 0;
			}
		}

		return base;
	}

	// TODO gamerforEA code start
	public void calcTrampling()
	{
		this.calcTrampling(null);
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add Entity parameter
	public void calcTrampling(Entity entity)
	{
		if (IC2.platform.isSimulating() && IC2.random.nextInt(100) == 0 && IC2.random.nextInt(40) > this.statResistance)
		{
			// TODO gamerforEA code start
			EntityPlayer player = entity instanceof EntityPlayer ? FastUtils.getFake(this.worldObj, ((EntityPlayer) entity).getGameProfile()) : ModUtils.NEXUS_FACTORY.getFake(this.worldObj);
			if (EventUtils.cantBreak(player, this.xCoord, this.yCoord - 1, this.zCoord))
				return;
			// TODO gamerforEA code end

			this.reset();
			this.worldObj.setBlock(this.xCoord, this.yCoord - 1, this.zCoord, Blocks.dirt, 0, 7);
		}
	}

	public void onEntityCollision(Entity entity)
	{
		if (this.crop != null)
			if (this.crop.onEntityCollision(this, entity))
				// TODO gamerforEA add Entity parameter
				this.calcTrampling(entity);
	}

	@Override
	public void reset()
	{
		this.crop = null;
		this.size = 0;
		this.customData = new NBTTagCompound();
		this.dirty = true;
		this.statGain = 0;
		this.statResistance = 0;
		this.statGrowth = 0;
		this.nutrients = -1;
		this.airQuality = -1;
		this.humidity = -1;
		this.growthPoints = 0;
		this.upgraded = false;
		this.scanLevel = 0;
	}

	@Override
	public void updateState()
	{
		this.dirty = true;
	}

	public String getScanned()
	{
		if (this.crop == null)
			return null;
		if (this.scanLevel <= 0)
			return null;
		String name = StatCollector.translateToLocal(this.crop.displayName());
		return this.scanLevel >= 4 ? String.format("%s - Gr: %d Ga: %d Re: %d S: %d/%d", name, this.statGrowth, this.statGain, this.statResistance, this.size, this.crop.maxSize()) : String.format("%s - Size: %d/%d", name, this.size, this.crop.maxSize());
	}

	@Override
	public boolean isBlockBelow(Block reqBlock)
	{
		if (this.crop == null)
			return false;
		for (int i = 1; i < this.crop.getrootslength(this); ++i)
		{
			Block block = this.worldObj.getBlock(this.xCoord, this.yCoord - i, this.zCoord);
			if (block.isAir(this.worldObj, this.xCoord, this.yCoord - i, this.zCoord))
				return false;

			if (block == reqBlock)
				return true;
		}

		return false;
	}

	@Override
	public boolean isBlockBelow(String oreDictionaryName)
	{
		if (this.crop == null)
			return false;
		for (int i = 1; i < this.crop.getrootslength(this); ++i)
		{
			Block block = this.worldObj.getBlock(this.xCoord, this.yCoord - i, this.zCoord);
			int metaData = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord - i, this.zCoord);
			if (block.isAir(this.worldObj, this.xCoord, this.yCoord - i, this.zCoord))
				return false;

			for (int aux = 0; aux < OreDictionary.getOres(oreDictionaryName).size(); ++aux)
			{
				ItemStack itemStack = OreDictionary.getOres(oreDictionaryName).get(aux);
				if (itemStack.getItem() == Item.getItemFromBlock(block) && itemStack.getItemDamage() == metaData)
					return true;
			}
		}

		return false;
	}

	@Override
	public ItemStack generateSeeds(CropCard crop, byte growth, byte gain, byte resis, byte scan)
	{
		return ItemCropSeed.generateItemStackFromValues(crop, growth, gain, resis, scan);
	}

	@Override
	public ItemStack generateSeeds(short plant, byte growth, byte gain, byte resis, byte scan)
	{
		return this.generateSeeds(IC2Crops.getCropFromId(plant), growth, gain, resis, scan);
	}

	@Override
	public void onNetworkUpdate(String field)
	{
		this.dirty = true;
	}

	@Override
	public CropCard getCrop()
	{
		return this.crop;
	}

	@Override
	public short getID()
	{
		return (short) Crops.instance.getIdFor(this.crop);
	}

	@Override
	public byte getSize()
	{
		return (byte) this.size;
	}

	@Override
	public byte getGrowth()
	{
		return (byte) this.statGrowth;
	}

	@Override
	public byte getGain()
	{
		return (byte) this.statGain;
	}

	@Override
	public byte getResistance()
	{
		return (byte) this.statResistance;
	}

	@Override
	public byte getScanLevel()
	{
		return (byte) this.scanLevel;
	}

	@Override
	public NBTTagCompound getCustomData()
	{
		return this.customData;
	}

	@Override
	public int getNutrientStorage()
	{
		return this.nutrientStorage;
	}

	@Override
	public int getHydrationStorage()
	{
		return this.waterStorage;
	}

	@Override
	public int getWeedExStorage()
	{
		return this.exStorage;
	}

	@Override
	public int getLightLevel()
	{
		return this.worldObj.getBlockLightValue(this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public void setCrop(CropCard cropCard)
	{
		this.crop = cropCard;
		this.dirty = true;
	}

	@Override
	public void setID(short id)
	{
		this.setCrop(IC2Crops.getCropFromId(id));
	}

	@Override
	public void setSize(byte size1)
	{
		this.size = size1;
		this.dirty = true;
	}

	@Override
	public void setGrowth(byte growth)
	{
		this.statGrowth = growth;
	}

	@Override
	public void setGain(byte gain)
	{
		this.statGain = gain;
	}

	@Override
	public void setResistance(byte resistance)
	{
		this.statResistance = resistance;
	}

	@Override
	public void setScanLevel(byte scanLevel1)
	{
		this.scanLevel = scanLevel1;
	}

	@Override
	public void setNutrientStorage(int nutrientStorage1)
	{
		this.nutrientStorage = nutrientStorage1;
	}

	@Override
	public void setHydrationStorage(int hydrationStorage)
	{
		this.waterStorage = hydrationStorage;
	}

	@Override
	public void setWeedExStorage(int weedExStorage)
	{
		this.exStorage = weedExStorage;
	}

	@Override
	public World getWorld()
	{
		return this.worldObj;
	}

	@Override
	public ChunkCoordinates getLocation()
	{
		return new ChunkCoordinates(this.xCoord, this.yCoord, this.zCoord);
	}

	public int getvisualweedlevel()
	{
		return this.weedlevel;
	}

	public int getvisualInfestedlevel()
	{
		return this.Infestedlevel < 10 ? 0 : this.Infestedlevel < 30 ? 1 : this.Infestedlevel < 50 ? 2 : this.Infestedlevel < 70 ? 3 : this.Infestedlevel < 90 ? 4 : 5;
	}
}
