package ic2.core.item.tool;

import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.event.LaserEvent;
import ic2.api.item.ElectricItem;
import ic2.api.network.INetworkItemEventListener;
import ic2.core.IC2;
import ic2.core.audio.PositionSpec;
import ic2.core.init.InternalName;
import ic2.core.util.StackUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.LinkedList;
import java.util.List;

public class ItemToolMiningLaser extends ItemElectricTool implements INetworkItemEventListener
{
	private static final int EventShotMining = 0;
	private static final int EventShotLowFocus = 1;
	private static final int EventShotLongRange = 2;
	private static final int EventShotHorizontal = 3;
	private static final int EventShotSuperHeat = 4;
	private static final int EventShotScatter = 5;
	private static final int EventShotExplosive = 6;

	public ItemToolMiningLaser(InternalName internalName)
	{
		super(internalName, 100);
		this.maxCharge = 300000;
		this.transferLimit = 512;
		this.tier = 3;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		super.addInformation(stack, player, list, par4);
		NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(stack);
		String mode;
		switch (nbtData.getInteger("laserSetting"))
		{
			case 0:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.mining");
				break;
			case 1:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.lowFocus");
				break;
			case 2:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.longRange");
				break;
			case 3:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.horizontal");
				break;
			case 4:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.superHeat");
				break;
			case 5:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.scatter");
				break;
			case 6:
				mode = StatCollector.translateToLocal("ic2.tooltip.mode.explosive");
				break;
			default:
				return;
		}

		list.add(StatCollector.translateToLocalFormatted("ic2.tooltip.mode", mode));
	}

	@Override
	public List<String> getHudInfo(ItemStack itemStack)
	{
		List<String> info = new LinkedList();
		NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemStack);
		String mode = StatCollector.translateToLocal(getModeString(nbtData.getInteger("laserSetting")));
		info.addAll(super.getHudInfo(itemStack));
		info.add(StatCollector.translateToLocalFormatted("ic2.tooltip.mode", mode));
		return info;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (!IC2.platform.isSimulating())
			return stack;
		else
		{
			NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(stack);
			int laserSetting = nbtData.getInteger("laserSetting");
			if (IC2.keyboard.isModeSwitchKeyDown(player))
			{
				laserSetting = (laserSetting + 1) % 7;
				nbtData.setInteger("laserSetting", laserSetting);
				IC2.platform.messagePlayer(player, "ic2.tooltip.mode", getModeString(laserSetting));
			}
			else
			{
				// TODO gamerforEA code replace, old code: int consume = new int[] { 1250, 100, 5000, 0, 2500, 10000, 5000 }[laserSetting];
				int consume;
				switch (laserSetting)
				{
					case 0:
						consume = EventConfig.laserMining;
						break;
					case 1:
						consume = EventConfig.laserLowFocus;
						break;
					case 2:
						consume = EventConfig.laserLongRange;
						break;
					case 4:
						consume = EventConfig.laserSuperHeat;
						break;
					case 5:
						consume = EventConfig.laserScatter;
						break;
					case 6:
						consume = EventConfig.laserExplosive;
						break;
					default:
						consume = 0;
						break;
				}
				// TODO gamerforEA code end

				if (!ElectricItem.manager.use(stack, consume, player))
					return stack;

				switch (laserSetting)
				{
					case 0:
						if (this.shootLaser(world, player, stack, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false))
							IC2.network.get().initiateItemEvent(player, stack, 0, true);
						break;
					case 1:
						if (this.shootLaser(world, player, stack, 4.0F, 5.0F, 1, false, false))
							IC2.network.get().initiateItemEvent(player, stack, 1, true);
						break;
					case 2:
						if (this.shootLaser(world, player, stack, Float.POSITIVE_INFINITY, 20.0F, Integer.MAX_VALUE, false, false))
							IC2.network.get().initiateItemEvent(player, stack, 2, true);
					case 3:
					default:
						break;
					case 4:
						if (this.shootLaser(world, player, stack, Float.POSITIVE_INFINITY, 8.0F, Integer.MAX_VALUE, false, true))
							IC2.network.get().initiateItemEvent(player, stack, 4, true);
						break;
					case 5:
						// TODO gamerforEA code start
						if (!EventConfig.laserscatterEnabled)
							break;
						// TODO gamerforEA code end

						for (int x = -2; x <= 2; ++x)
						{
							for (int y = -2; y <= 2; ++y)
							{
								this.shootLaser(world, player, stack, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, false, false, player.rotationYaw + 20.0F * x, player.rotationPitch + 20.0F * y);
							}
						}

						IC2.network.get().initiateItemEvent(player, stack, 5, true);
						break;
					case 6:
						if (this.shootLaser(world, player, stack, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, true, false))
							IC2.network.get().initiateItemEvent(player, stack, 6, true);
				}
			}

			return super.onItemRightClick(stack, world, player);
		}
	}

	@Override
	public boolean onItemUseFirst(ItemStack itemstack, EntityPlayer entityPlayer, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
	{
		if (!IC2.platform.isSimulating())
			return false;
		else
		{
			NBTTagCompound nbtData = StackUtil.getOrCreateNbtData(itemstack);
			if (!IC2.keyboard.isModeSwitchKeyDown(entityPlayer) && nbtData.getInteger("laserSetting") == 3)
				if (Math.abs(entityPlayer.posY + entityPlayer.getEyeHeight() - 0.1D - (y + 0.5D)) < 1.5D)
				{
					// TODO gamerforEA code replace, old value: 3000.0D
					if (ElectricItem.manager.use(itemstack, EventConfig.laserHorizontal, entityPlayer) && this.shootLaser(world, entityPlayer, itemstack, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false, entityPlayer.rotationYaw, 0.0D, y + 0.5D))
						IC2.network.get().initiateItemEvent(entityPlayer, itemstack, 3, true);
				}
				else
					IC2.platform.messagePlayer(entityPlayer, "Mining laser aiming angle too steep");

			return false;
		}
	}

	public boolean shootLaser(World world, EntityLivingBase entityliving, ItemStack laseritem, float range, float power, int blockBreaks, boolean explosive, boolean smelt)
	{
		return this.shootLaser(world, entityliving, laseritem, range, power, blockBreaks, explosive, smelt, entityliving.rotationYaw, entityliving.rotationPitch);
	}

	public boolean shootLaser(World world, EntityLivingBase entityliving, ItemStack laseritem, float range, float power, int blockBreaks, boolean explosive, boolean smelt, double yawDeg, double pitchDeg)
	{
		return this.shootLaser(world, entityliving, laseritem, range, power, blockBreaks, explosive, smelt, yawDeg, pitchDeg, entityliving.posY + entityliving.getEyeHeight() - 0.1D);
	}

	public boolean shootLaser(World world, EntityLivingBase entityliving, ItemStack laseritem, float range, float power, int blockBreaks, boolean explosive, boolean smelt, double yawDeg, double pitchDeg, double y)
	{
		EntityMiningLaser tLaser = new EntityMiningLaser(world, entityliving, range, power, blockBreaks, explosive, yawDeg, pitchDeg, y);
		LaserEvent.LaserShootEvent tEvent = new LaserEvent.LaserShootEvent(world, tLaser, entityliving, range, power, blockBreaks, explosive, smelt, laseritem);
		MinecraftForge.EVENT_BUS.post(tEvent);
		if (tLaser.takeDataFromEvent(tEvent))
		{
			world.spawnEntityInWorld(tLaser);
			return true;
		}
		else
			return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack stack)
	{
		return EnumRarity.uncommon;
	}

	@Override
	public void onNetworkEvent(ItemStack stack, EntityPlayer player, int event)
	{
		switch (event)
		{
			case 0:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 1:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserLowFocus.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 2:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserLongRange.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 3:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 4:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaser.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 5:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserScatter.ogg", true, IC2.audioManager.getDefaultVolume());
				break;
			case 6:
				IC2.audioManager.playOnce(player, PositionSpec.Hand, "Tools/MiningLaser/MiningLaserExplosive.ogg", true, IC2.audioManager.getDefaultVolume());
		}

	}

	private static String getModeString(int mode)
	{
		switch (mode)
		{
			case 0:
				return "ic2.tooltip.mode.mining";
			case 1:
				return "ic2.tooltip.mode.lowFocus";
			case 2:
				return "ic2.tooltip.mode.longRange";
			case 3:
				return "ic2.tooltip.mode.horizontal";
			case 4:
				return "ic2.tooltip.mode.superHeat";
			case 5:
				return "ic2.tooltip.mode.scatter";
			case 6:
				return "ic2.tooltip.mode.explosive";
			default:
				assert false;

				return "";
		}
	}
}
