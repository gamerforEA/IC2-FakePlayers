package ic2.core.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gamerforea.ic2.EventConfig;

import ic2.api.recipe.IScrapboxManager;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.Ic2Items;
import ic2.core.init.InternalName;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class ItemScrapbox extends ItemIC2
{
	public ItemScrapbox(InternalName internalName)
	{
		super(internalName);
		BlockDispenser.dispenseBehaviorRegistry.putObject(this, new BehaviorScrapboxDispense());
	}

	public static void init()
	{
		Recipes.scrapboxDrops = new ItemScrapbox.ScrapboxRecipeManager();
		if (IC2.suddenlyHoes)
			addDrop(Items.wooden_hoe, 9001.0F);
		else
			addDrop(Items.wooden_hoe, 5.01F);

		addDrop(Blocks.dirt, 5.0F);
		addDrop(Items.stick, 4.0F);
		addDrop(Blocks.grass, 3.0F);
		addDrop(Blocks.gravel, 3.0F);
		addDrop(Blocks.netherrack, 2.0F);
		addDrop(Items.rotten_flesh, 2.0F);
		addDrop(Items.apple, 1.5F);
		addDrop(Items.bread, 1.5F);
		addDrop(Ic2Items.filledTinCan.getItem(), 1.5F);
		addDrop(Items.wooden_sword);
		addDrop(Items.wooden_shovel);
		addDrop(Items.wooden_pickaxe);
		addDrop(Blocks.soul_sand);
		addDrop(Items.sign);
		addDrop(Items.leather);
		addDrop(Items.feather);
		addDrop(Items.bone);
		addDrop(Items.cooked_porkchop, 0.9F);
		addDrop(Items.cooked_beef, 0.9F);
		addDrop(Blocks.pumpkin, 0.9F);
		addDrop(Items.cooked_chicken, 0.9F);
		addDrop(Items.minecart, 0.01F);
		addDrop(Items.redstone, 0.9F);
		addDrop(Ic2Items.rubber.getItem(), 0.8F);
		addDrop(Items.glowstone_dust, 0.8F);
		addDrop(Ic2Items.coalDust, 0.8F);
		addDrop(Ic2Items.copperDust, 0.8F);
		addDrop(Ic2Items.tinDust, 0.8F);
		addDrop(Ic2Items.suBattery.getItem(), 0.7F);
		addDrop(Ic2Items.ironDust, 0.7F);
		addDrop(Ic2Items.goldDust, 0.7F);
		addDrop(Items.slime_ball, 0.6F);
		addDrop(Blocks.iron_ore, 0.5F);
		addDrop(Items.golden_helmet, 0.01F);
		addDrop(Blocks.gold_ore, 0.5F);
		addDrop(Items.cake, 0.5F);
		addDrop(Items.diamond, 0.1F);
		addDrop(Items.emerald, 0.05F);
		addDrop(Items.ender_pearl, 0.08F);
		addDrop(Items.blaze_rod, 0.04F);
		addDrop(Items.egg, 0.8F);
		if (Ic2Items.copperOre != null)
			addDrop(Ic2Items.copperOre.getItem(), 0.7F);
		else
		{
			List<ItemStack> ores = OreDictionary.getOres("oreCopper");
			if (!ores.isEmpty())
				addDrop(ores.get(0).copy(), 0.7F);
		}

		if (Ic2Items.tinOre != null)
			addDrop(Ic2Items.tinOre.getItem(), 0.7F);
		else
		{
			List<ItemStack> ores = OreDictionary.getOres("oreTin");
			if (!ores.isEmpty())
				addDrop(ores.get(0).copy(), 0.7F);
		}

	}

	public static void addDrop(Item item)
	{
		addDrop(new ItemStack(item), 1.0F);
	}

	public static void addDrop(Item item, float chance)
	{
		addDrop(new ItemStack(item), chance);
	}

	public static void addDrop(Block block)
	{
		addDrop(new ItemStack(block), 1.0F);
	}

	public static void addDrop(Block block, float chance)
	{
		addDrop(new ItemStack(block), chance);
	}

	public static void addDrop(ItemStack item)
	{
		addDrop(item, 1.0F);
	}

	public static void addDrop(ItemStack item, float chance)
	{
		Recipes.scrapboxDrops.addDrop(item, chance);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
	{
		// TODO gamerforEA code start
		if (!EventConfig.scrapboxDropEnabled)
			return itemstack;
		// TODO gamerforEA code end

		if (IC2.platform.isSimulating())
		{
			ItemStack itemStack = Recipes.scrapboxDrops.getDrop(itemstack, !entityplayer.capabilities.isCreativeMode);
			if (itemStack != null)
				entityplayer.dropPlayerItemWithRandomChoice(itemStack, false);
		}

		return itemstack;
	}

	static class Drop
	{
		ItemStack item;
		Float originalChance;
		float upperChanceBound;
		static float topChance;

		Drop(ItemStack item1, float chance)
		{
			this.item = item1;
			this.originalChance = Float.valueOf(chance);
			this.upperChanceBound = topChance += chance;
		}
	}

	static class ScrapboxRecipeManager implements IScrapboxManager
	{
		private final List<ItemScrapbox.Drop> drops = new ArrayList();

		@Override
		public void addDrop(ItemStack drop, float rawChance)
		{
			this.drops.add(new ItemScrapbox.Drop(drop, rawChance));
		}

		@Override
		public ItemStack getDrop(ItemStack input, boolean adjustInput)
		{
			if (this.drops.isEmpty())
				return null;
			else
			{
				if (adjustInput)
					--input.stackSize;

				float chance = IC2.random.nextFloat() * ItemScrapbox.Drop.topChance;
				int low = 0;
				int high = this.drops.size() - 1;

				while (low < high)
				{
					int mid = (high + low) / 2;
					if (chance < this.drops.get(mid).upperChanceBound)
						high = mid;
					else
						low = mid + 1;
				}

				return this.drops.get(low).item.copy();
			}
		}

		@Override
		public Map<ItemStack, Float> getDrops()
		{
			Map<ItemStack, Float> ret = new HashMap(this.drops.size());

			for (ItemScrapbox.Drop drop : this.drops)
				ret.put(drop.item, Float.valueOf(drop.originalChance.floatValue() / ItemScrapbox.Drop.topChance));

			return ret;
		}
	}
}
