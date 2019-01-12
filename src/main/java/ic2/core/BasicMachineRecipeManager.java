package ic2.core;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ic2.api.recipe.*;
import ic2.core.init.MainConfig;
import ic2.core.util.LogCategory;
import ic2.core.util.StackUtil;
import ic2.core.util.Tuple;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;

import java.util.*;
import java.util.Map.Entry;

public class BasicMachineRecipeManager implements IMachineRecipeManagerExt
{
	private final Map<IRecipeInput, RecipeOutput> recipes = new HashMap();
	private final Map<Item, Map<Integer, Tuple.T2<IRecipeInput, RecipeOutput>>> recipeCache = new IdentityHashMap();
	private final List<Tuple.T2<IRecipeInput, RecipeOutput>> uncacheableRecipes = new ArrayList();
	private boolean oreRegisterEventSubscribed;

	@Override
	public void addRecipe(IRecipeInput input, NBTTagCompound metadata, ItemStack... outputs)
	{
		if (!this.addRecipe(input, metadata, false, outputs))
			this.displayError("ambiguous recipe: [" + input.getInputs() + " -> " + Arrays.asList(outputs) + "]");

	}

	@Override
	public boolean addRecipe(IRecipeInput input, NBTTagCompound metadata, boolean overwrite, ItemStack... outputs)
	{
		return this.addRecipe(input, new RecipeOutput(metadata, outputs), overwrite);
	}

	@Override
	public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput)
	{
		if (input == null)
			return null;
		Tuple.T2<IRecipeInput, RecipeOutput> data = this.getRecipe(input);
		if (data == null)
			return null;
		if (input.stackSize >= data.a.getAmount() && (!input.getItem().hasContainerItem(input) || input.stackSize == data.a.getAmount()))
		{
			if (adjustInput)
				if (input.getItem().hasContainerItem(input))
				{
					ItemStack container = input.getItem().getContainerItem(input);
					input.func_150996_a(container.getItem());
					input.stackSize = container.stackSize;
					input.setItemDamage(container.getItemDamage());
					input.stackTagCompound = container.stackTagCompound;
				}
				else
					input.stackSize -= data.a.getAmount();

			return data.b;
		}
		return null;
	}

	@Override
	public Map<IRecipeInput, RecipeOutput> getRecipes()
	{
		return new AbstractMap<IRecipeInput, RecipeOutput>()
		{
			@Override
			public Set<Entry<IRecipeInput, RecipeOutput>> entrySet()
			{
				return new AbstractSet<Entry<IRecipeInput, RecipeOutput>>()
				{
					@Override
					public Iterator<Entry<IRecipeInput, RecipeOutput>> iterator()
					{
						return new Iterator<Entry<IRecipeInput, RecipeOutput>>()
						{
							private final Iterator<Entry<IRecipeInput, RecipeOutput>> recipeIt;
							private IRecipeInput lastInput;

							{
								this.recipeIt = BasicMachineRecipeManager.this.recipes.entrySet().iterator();
							}

							@Override
							public boolean hasNext()
							{
								return this.recipeIt.hasNext();
							}

							@Override
							public Entry<IRecipeInput, RecipeOutput> next()
							{
								Entry<IRecipeInput, RecipeOutput> ret = this.recipeIt.next();
								this.lastInput = ret.getKey();
								return ret;
							}

							@Override
							public void remove()
							{
								this.recipeIt.remove();
								BasicMachineRecipeManager.this.removeCachedRecipes(this.lastInput);
							}
						};
					}

					@Override
					public int size()
					{
						return BasicMachineRecipeManager.this.recipes.size();
					}
				};
			}

			@Override
			public RecipeOutput put(IRecipeInput key, RecipeOutput value)
			{
				BasicMachineRecipeManager.this.addRecipe(key, value, true);
				return null;
			}
		};
	}

	@SubscribeEvent
	public void onOreRegister(OreRegisterEvent event)
	{
		List<Tuple.T2<IRecipeInput, RecipeOutput>> datas = new ArrayList();

		for (Entry<IRecipeInput, RecipeOutput> data : this.recipes.entrySet())
		{
			if (((IRecipeInput) data.getKey()).getClass() == RecipeInputOreDict.class)
			{
				RecipeInputOreDict recipe = (RecipeInputOreDict) data.getKey();
				if (recipe.input.equals(event.Name))
					datas.add(new Tuple.T2(data.getKey(), data.getValue()));
			}
		}

		for (Tuple.T2<IRecipeInput, RecipeOutput> data : datas)
		{
			this.addToCache(event.Ore, data);
		}

	}

	private Tuple.T2<IRecipeInput, RecipeOutput> getRecipe(ItemStack input)
	{
		Map<Integer, Tuple.T2<IRecipeInput, RecipeOutput>> metaMap = this.recipeCache.get(input.getItem());
		if (metaMap != null)
		{
			/* TODO gamerforEA code replace, old code:
			Tuple.T2<IRecipeInput, RecipeOutput> data = metaMap.get(32767);
			if (data != null)
				return data;

			int meta = input.getItemDamage();
			data = metaMap.get(meta);
			if (data != null)
				return data; */
			int meta = input.getItemDamage();
			Tuple.T2<IRecipeInput, RecipeOutput> data = metaMap.get(meta);
			if (data != null)
				return data;
			data = metaMap.get(32767);
			if (data != null)
				return data;
			// TODO gamerforEA code end
		}

		for (Tuple.T2<IRecipeInput, RecipeOutput> data : this.uncacheableRecipes)
		{
			if (data.a.matches(input))
				return data;
		}

		return null;
	}

	private boolean addRecipe(IRecipeInput input, RecipeOutput output, boolean overwrite)
	{
		if (input == null)
		{
			this.displayError("The recipe input is null");
			return false;
		}
		ListIterator<ItemStack> it = output.items.listIterator();

		while (it.hasNext())
		{
			ItemStack stack = it.next();
			if (stack == null)
			{
				this.displayError("An output ItemStack is null.");
				return false;
			}

			if (!StackUtil.check(stack))
			{
				this.displayError("The output ItemStack " + StackUtil.toStringSafe(stack) + " is invalid.");
				return false;
			}

			if (input.matches(stack) && (output.metadata == null || !output.metadata.hasKey("ignoreSameInputOutput")))
			{
				this.displayError("The output ItemStack " + stack.toString() + " is the same as the recipe input " + input + ".");
				return false;
			}

			it.set(stack.copy());
		}

		for (ItemStack is : input.getInputs())
		{
			Tuple.T2<IRecipeInput, RecipeOutput> data = this.getRecipe(is);
			if (data != null)
			{
				if (!overwrite)
					return false;

				do
				{
					this.recipes.remove(data.a);
					this.removeCachedRecipes(data.a);
					data = this.getRecipe(is);
				}
				while (data != null);
			}
		}

		this.recipes.put(input, output);
		this.addToCache(input, output);
		return true;
	}

	private void addToCache(IRecipeInput input, RecipeOutput output)
	{
		Tuple.T2<IRecipeInput, RecipeOutput> data = new Tuple.T2(input, output);
		List<ItemStack> stacks = this.getStacksFromRecipe(input);
		if (stacks != null)
		{
			for (ItemStack stack : stacks)
			{
				this.addToCache(stack, data);
			}

			if (input.getClass() == RecipeInputOreDict.class && !this.oreRegisterEventSubscribed)
			{
				MinecraftForge.EVENT_BUS.register(this);
				this.oreRegisterEventSubscribed = true;
			}
		}
		else
			this.uncacheableRecipes.add(data);

	}

	private void addToCache(ItemStack stack, Tuple.T2<IRecipeInput, RecipeOutput> data)
	{
		Item item = stack.getItem();
		Map<Integer, Tuple.T2<IRecipeInput, RecipeOutput>> metaMap = this.recipeCache.computeIfAbsent(item, k -> new HashMap<>());
		int meta = stack.getItemDamage();
		metaMap.put(meta, data);
	}

	private void removeCachedRecipes(IRecipeInput input)
	{
		List<ItemStack> stacks = this.getStacksFromRecipe(input);
		if (stacks != null)
			for (ItemStack stack : stacks)
			{
				Item item = stack.getItem();
				int meta = stack.getItemDamage();
				Map<Integer, Tuple.T2<IRecipeInput, RecipeOutput>> map = this.recipeCache.get(item);
				if (map == null)
					IC2.log.warn(LogCategory.Recipe, "Inconsistent recipe cache, the entry for the item " + item + "(" + stack + ") is missing.");
				else
				{
					map.remove(meta);
					if (map.isEmpty())
						this.recipeCache.remove(item);
				}
			}
		else
			this.uncacheableRecipes.removeIf(data -> data.a == input);

	}

	private List<ItemStack> getStacksFromRecipe(IRecipeInput recipe)
	{
		if (recipe.getClass() == RecipeInputItemStack.class)
			return recipe.getInputs();
		if (recipe.getClass() == RecipeInputOreDict.class)
		{
			Integer meta = ((RecipeInputOreDict) recipe).meta;
			if (meta == null)
				return recipe.getInputs();
			List<ItemStack> ret = new ArrayList<>(recipe.getInputs());
			ListIterator<ItemStack> it = ret.listIterator();

			while (it.hasNext())
			{
				ItemStack stack = it.next();
				if (stack.getItemDamage() != meta)
				{
					stack = stack.copy();
					stack.setItemDamage(meta);
					it.set(stack);
				}
			}

			return ret;
		}
		return null;
	}

	private void displayError(String msg)
	{
		if (MainConfig.ignoreInvalidRecipes)
			IC2.log.warn(LogCategory.Recipe, msg);
		else
			throw new RuntimeException(msg);
	}
}
