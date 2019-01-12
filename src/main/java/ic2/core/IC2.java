package ic2.core;

import com.gamerforea.eventhelper.config.ConfigUtils;
import com.gamerforea.ic2.EventConfig;
import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.energy.EnergyNet;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeInputItemStack;
import ic2.api.tile.ExplosionWhitelist;
import ic2.api.util.Keys;
import ic2.core.audio.AudioManager;
import ic2.core.block.*;
import ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator;
import ic2.core.block.machine.tileentity.*;
import ic2.core.command.CommandIc2;
import ic2.core.command.CommandTps;
import ic2.core.crop.IC2Crops;
import ic2.core.energy.EnergyNetGlobal;
import ic2.core.init.*;
import ic2.core.item.*;
import ic2.core.item.tfbp.ItemTFBPCultivation;
import ic2.core.item.tfbp.ItemTFBPFlatification;
import ic2.core.item.tool.EntityMiningLaser;
import ic2.core.item.tool.EntityParticle;
import ic2.core.network.NetworkManager;
import ic2.core.util.*;
import ic2.core.uu.UuIndex;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogDensity;
import net.minecraftforge.client.event.TextureStitchEvent.Post;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn;
import net.minecraftforge.event.world.ChunkWatchEvent.Watch;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;
import org.lwjgl.opengl.GL11;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@Mod(modid = "IC2",
	 name = "IndustrialCraft 2",
	 version = "2.2.827-experimental",
	 useMetadata = true,
	 certificateFingerprint = "de041f9f6187debbc77034a344134053277aa3b0",
	 dependencies = "required-after:Forge@[10.13.0.1200,)")
public class IC2 implements IWorldGenerator, IFuelHandler
{
	public static final String VERSION = "2.2.827-experimental";
	public static final String MODID = "IC2";
	private static IC2 instance;
	@SidedProxy(clientSide = "ic2.core.PlatformClient", serverSide = "ic2.core.Platform")
	public static Platform platform;
	public static SideGateway<NetworkManager> network;
	@SidedProxy(clientSide = "ic2.core.util.KeyboardClient", serverSide = "ic2.core.util.Keyboard")
	public static Keyboard keyboard;
	@SidedProxy(clientSide = "ic2.core.audio.AudioManagerClient", serverSide = "ic2.core.audio.AudioManager")
	public static AudioManager audioManager;
	public static Log log;
	public static IC2Achievements achievements;
	public static TickHandler tickHandler;
	public static int cableRenderId;
	public static int fenceRenderId;
	public static int miningPipeRenderId;
	public static int luminatorRenderId;
	public static int cropRenderId;
	public static Random random;
	public static final Map<IRecipeInput, Integer> valuableOres;
	public static boolean suddenlyHoes;
	public static boolean seasonal;
	public static boolean initialized;
	public static final CreativeTabIC2 tabIC2;
	public static final String textureDomain;
	public static final int setBlockNotify = 1;
	public static final int setBlockUpdate = 2;
	public static final int setBlockNoUpdateFromClient = 4;
	public final TickrateTracker tickrateTracker = new TickrateTracker();
	public final PriorityExecutor threadPool = new PriorityExecutor(Math.max(Runtime.getRuntime().availableProcessors(), 2));

	public IC2()
	{
		instance = this;
		Info.ic2ModInstance = this;
	}

	public static IC2 getInstance()
	{
		return instance;
	}

	@EventHandler
	public void load(FMLPreInitializationEvent event)
	{
		long startTime = System.nanoTime();
		log = new Log(event.getModLog());
		log.debug(LogCategory.General, "Starting pre-init.");
		MainConfig.load();

		// TODO gamerforEA code start
		ConfigUtils.readConfig(EventConfig.class);
		// TODO gamerforEA code end

		Localization.preInit(event.getSourceFile());
		tickHandler = new TickHandler();

		for (IRecipeInput input : ConfigUtil.asRecipeInputList(MainConfig.get(), "misc/additionalValuableOres"))
		{
			addValuableOre(input, 1);
		}

		audioManager.initialize();
		ElectricItem.manager = new GatewayElectricItemManager();
		ElectricItem.rawManager = new ElectricItemManager();
		ItemInfo itemInfo = new ItemInfo();
		Info.itemEnergy = itemInfo;
		Info.itemFuel = itemInfo;
		Keys.instance = keyboard;
		BlocksItems.init();
		Blocks.obsidian.setResistance(60.0F);
		Blocks.enchanting_table.setResistance(60.0F);
		Blocks.ender_chest.setResistance(60.0F);
		Blocks.anvil.setResistance(60.0F);
		Blocks.water.setResistance(30.0F);
		Blocks.flowing_water.setResistance(30.0F);
		Blocks.lava.setResistance(30.0F);
		ExplosionWhitelist.addWhitelistedBlock(Blocks.bedrock);
		FurnaceRecipes furnaceRecipes = FurnaceRecipes.smelting();
		if (Ic2Items.rubberWood != null)
		{
			furnaceRecipes.func_151394_a(Ic2Items.rubberWood, new ItemStack(Blocks.log, 1, 3), 0.1F);
		}

		if (Ic2Items.tinOre != null)
		{
			furnaceRecipes.func_151394_a(Ic2Items.tinOre, Ic2Items.tinIngot, 0.5F);
		}

		if (Ic2Items.copperOre != null)
		{
			furnaceRecipes.func_151394_a(Ic2Items.copperOre, Ic2Items.copperIngot, 0.5F);
		}

		if (Ic2Items.leadOre != null)
		{
			furnaceRecipes.func_151394_a(Ic2Items.leadOre, Ic2Items.leadIngot, 0.5F);
		}

		furnaceRecipes.func_151394_a(Ic2Items.ironDust, new ItemStack(Items.iron_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.goldDust, new ItemStack(Items.gold_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.tinDust, Ic2Items.tinIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.copperDust, Ic2Items.copperIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.leadDust, Ic2Items.leadIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.silverDust, Ic2Items.silverIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.hydratedCoalDust, Ic2Items.coalDust.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.bronzeDust, Ic2Items.bronzeIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.resin, Ic2Items.rubber.copy(), 0.3F);
		furnaceRecipes.func_151396_a(Ic2Items.mugCoffee.getItem(), new ItemStack(Ic2Items.mugCoffee.getItem(), 1, 1), 0.1F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedIronOre, new ItemStack(Items.iron_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedGoldOre, new ItemStack(Items.gold_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedCopperOre, Ic2Items.copperIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedTinOre, Ic2Items.tinIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedLeadOre, Ic2Items.leadIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.crushedSilverOre, Ic2Items.silverIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedIronOre, new ItemStack(Items.iron_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedGoldOre, new ItemStack(Items.gold_ingot, 1), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedCopperOre, Ic2Items.copperIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedTinOre, Ic2Items.tinIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedLeadOre, Ic2Items.leadIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.purifiedCrushedSilverOre, Ic2Items.silverIngot.copy(), 0.0F);
		furnaceRecipes.func_151394_a(Ic2Items.rawcrystalmemory, Ic2Items.crystalmemory.copy(), 0.0F);
		ItemScrapbox.init();
		ItemTFBPCultivation.init();
		ItemTFBPFlatification.init();
		TileEntityCanner.init();
		TileEntityCompressor.init();
		TileEntityExtractor.init();
		TileEntityMacerator.init();
		TileEntityRecycler.init();
		TileEntityCentrifuge.init();
		TileEntityMatter.init();
		TileEntityMetalFormer.init();
		TileEntitySemifluidGenerator.init();
		TileEntityOreWashing.init();
		TileEntityFluidHeatGenerator.init();
		TileEntityBlockCutter.init();
		TileEntityBlastFurnace.init();
		TileEntityLiquidHeatExchanger.init();
		EntityIC2Boat.init();
		StackUtil.getBlock(Ic2Items.reinforcedStone).setHarvestLevel("pickaxe", 2);
		StackUtil.getBlock(Ic2Items.reinforcedDoorBlock).setHarvestLevel("pickaxe", 2);
		StackUtil.getBlock(Ic2Items.insulatedCopperCableBlock).setHarvestLevel("axe", 0);
		StackUtil.getBlock(Ic2Items.constructionFoamWall).setHarvestLevel("pickaxe", 1);
		if (Ic2Items.copperOre != null)
		{
			StackUtil.getBlock(Ic2Items.copperOre).setHarvestLevel("pickaxe", 1);
		}

		if (Ic2Items.tinOre != null)
		{
			StackUtil.getBlock(Ic2Items.tinOre).setHarvestLevel("pickaxe", 1);
		}

		if (Ic2Items.uraniumOre != null)
		{
			StackUtil.getBlock(Ic2Items.uraniumOre).setHarvestLevel("pickaxe", 2);
		}

		if (Ic2Items.leadOre != null)
		{
			StackUtil.getBlock(Ic2Items.leadOre).setHarvestLevel("pickaxe", 1);
		}

		if (Ic2Items.rubberWood != null)
		{
			StackUtil.getBlock(Ic2Items.rubberWood).setHarvestLevel("axe", 0);
		}

		MinecraftForge.EVENT_BUS.register(this);
		RecipeSorter.register("ic2:shaped", AdvRecipe.class, Category.SHAPED, "after:minecraft:shapeless");
		RecipeSorter.register("ic2:shapeless", AdvShapelessRecipe.class, Category.SHAPELESS, "after:ic2:shaped");

		for (String oreName : OreDictionary.getOreNames())
		{
			for (ItemStack ore : OreDictionary.getOres(oreName))
			{
				this.registerOre(new OreRegisterEvent(oreName, ore));
			}
		}

		assert Ic2Items.bronzeIngot != null;

		assert Ic2Items.copperIngot != null;

		assert Ic2Items.tinIngot != null;

		assert Ic2Items.leadIngot != null;

		assert Ic2Items.rubber != null;

		if (Ic2Items.copperOre != null)
		{
			OreDictionary.registerOre("oreCopper", Ic2Items.copperOre.copy());
		}

		if (Ic2Items.tinOre != null)
		{
			OreDictionary.registerOre("oreTin", Ic2Items.tinOre.copy());
		}

		if (Ic2Items.uraniumOre != null)
		{
			OreDictionary.registerOre("oreUranium", Ic2Items.uraniumOre.copy());
		}

		if (Ic2Items.leadOre != null)
		{
			OreDictionary.registerOre("oreLead", Ic2Items.leadOre.copy());
		}

		if (Ic2Items.rubberLeaves != null)
		{
			ItemStack tStack = Ic2Items.rubberLeaves.copy();
			tStack.setItemDamage(32767);
			OreDictionary.registerOre("treeLeaves", tStack);
		}

		if (Ic2Items.rubberSapling != null)
		{
			ItemStack tStack = Ic2Items.rubberSapling.copy();
			tStack.setItemDamage(32767);
			OreDictionary.registerOre("treeSapling", tStack);
		}

		if (Ic2Items.rubberWood != null)
		{
			ItemStack tStack = Ic2Items.rubberWood.copy();
			tStack.setItemDamage(32767);
			OreDictionary.registerOre("woodRubber", tStack);
		}

		OreDictionary.registerOre("dustStone", Ic2Items.stoneDust.copy());
		OreDictionary.registerOre("dustBronze", Ic2Items.bronzeDust.copy());
		OreDictionary.registerOre("dustClay", Ic2Items.clayDust.copy());
		OreDictionary.registerOre("dustCoal", Ic2Items.coalDust.copy());
		OreDictionary.registerOre("dustCopper", Ic2Items.copperDust.copy());
		OreDictionary.registerOre("dustGold", Ic2Items.goldDust.copy());
		OreDictionary.registerOre("dustIron", Ic2Items.ironDust.copy());
		OreDictionary.registerOre("dustSilver", Ic2Items.silverDust.copy());
		OreDictionary.registerOre("dustTin", Ic2Items.tinDust.copy());
		OreDictionary.registerOre("dustLead", Ic2Items.leadDust.copy());
		OreDictionary.registerOre("dustObsidian", Ic2Items.obsidianDust.copy());
		OreDictionary.registerOre("dustLapis", Ic2Items.lapiDust.copy());
		OreDictionary.registerOre("dustSulfur", Ic2Items.sulfurDust.copy());
		OreDictionary.registerOre("dustLithium", Ic2Items.lithiumDust.copy());
		OreDictionary.registerOre("dustDiamond", Ic2Items.diamondDust.copy());
		OreDictionary.registerOre("dustSiliconDioxide", Ic2Items.silicondioxideDust.copy());
		OreDictionary.registerOre("dustHydratedCoal", Ic2Items.hydratedCoalDust.copy());
		OreDictionary.registerOre("dustAshes", Ic2Items.AshesDust.copy());
		OreDictionary.registerOre("dustTinyCopper", Ic2Items.smallCopperDust.copy());
		OreDictionary.registerOre("dustTinyGold", Ic2Items.smallGoldDust.copy());
		OreDictionary.registerOre("dustTinyIron", Ic2Items.smallIronDust.copy());
		OreDictionary.registerOre("dustTinySilver", Ic2Items.smallSilverDust.copy());
		OreDictionary.registerOre("dustTinyTin", Ic2Items.smallTinDust.copy());
		OreDictionary.registerOre("dustTinyLead", Ic2Items.smallLeadDust.copy());
		OreDictionary.registerOre("dustTinySulfur", Ic2Items.smallSulfurDust.copy());
		OreDictionary.registerOre("dustTinyLithium", Ic2Items.smallLithiumDust.copy());
		OreDictionary.registerOre("dustTinyBronze", Ic2Items.smallBronzeDust.copy());
		OreDictionary.registerOre("dustTinyLapis", Ic2Items.smallLapiDust.copy());
		OreDictionary.registerOre("dustTinyObsidian", Ic2Items.smallObsidianDust.copy());
		OreDictionary.registerOre("itemRubber", Ic2Items.rubber.copy());
		OreDictionary.registerOre("ingotBronze", Ic2Items.bronzeIngot.copy());
		OreDictionary.registerOre("ingotCopper", Ic2Items.copperIngot.copy());
		OreDictionary.registerOre("ingotSteel", Ic2Items.advIronIngot.copy());
		OreDictionary.registerOre("ingotLead", Ic2Items.leadIngot.copy());
		OreDictionary.registerOre("ingotTin", Ic2Items.tinIngot.copy());
		OreDictionary.registerOre("ingotSilver", Ic2Items.silverIngot.copy());
		OreDictionary.registerOre("plateIron", Ic2Items.plateiron.copy());
		OreDictionary.registerOre("plateGold", Ic2Items.plategold.copy());
		OreDictionary.registerOre("plateCopper", Ic2Items.platecopper.copy());
		OreDictionary.registerOre("plateTin", Ic2Items.platetin.copy());
		OreDictionary.registerOre("plateLead", Ic2Items.platelead.copy());
		OreDictionary.registerOre("plateLapis", Ic2Items.platelapi.copy());
		OreDictionary.registerOre("plateObsidian", Ic2Items.plateobsidian.copy());
		OreDictionary.registerOre("plateBronze", Ic2Items.platebronze.copy());
		OreDictionary.registerOre("plateSteel", Ic2Items.plateadviron.copy());
		OreDictionary.registerOre("plateDenseSteel", Ic2Items.denseplateadviron.copy());
		OreDictionary.registerOre("plateDenseIron", Ic2Items.denseplateiron.copy());
		OreDictionary.registerOre("plateDenseGold", Ic2Items.denseplategold.copy());
		OreDictionary.registerOre("plateDenseCopper", Ic2Items.denseplatecopper.copy());
		OreDictionary.registerOre("plateDenseTin", Ic2Items.denseplatetin.copy());
		OreDictionary.registerOre("plateDenseLead", Ic2Items.denseplatelead.copy());
		OreDictionary.registerOre("plateDenseLapis", Ic2Items.denseplatelapi.copy());
		OreDictionary.registerOre("plateDenseObsidian", Ic2Items.denseplateobsidian.copy());
		OreDictionary.registerOre("plateDenseBronze", Ic2Items.denseplatebronze.copy());
		OreDictionary.registerOre("crushedIron", Ic2Items.crushedIronOre.copy());
		OreDictionary.registerOre("crushedGold", Ic2Items.crushedGoldOre.copy());
		OreDictionary.registerOre("crushedSilver", Ic2Items.crushedSilverOre.copy());
		OreDictionary.registerOre("crushedLead", Ic2Items.crushedLeadOre.copy());
		OreDictionary.registerOre("crushedCopper", Ic2Items.crushedCopperOre.copy());
		OreDictionary.registerOre("crushedTin", Ic2Items.crushedTinOre.copy());
		OreDictionary.registerOre("crushedUranium", Ic2Items.crushedUraniumOre.copy());
		OreDictionary.registerOre("crushedPurifiedIron", Ic2Items.purifiedCrushedIronOre.copy());
		OreDictionary.registerOre("crushedPurifiedGold", Ic2Items.purifiedCrushedGoldOre.copy());
		OreDictionary.registerOre("crushedPurifiedSilver", Ic2Items.purifiedCrushedSilverOre.copy());
		OreDictionary.registerOre("crushedPurifiedLead", Ic2Items.purifiedCrushedLeadOre.copy());
		OreDictionary.registerOre("crushedPurifiedCopper", Ic2Items.purifiedCrushedCopperOre.copy());
		OreDictionary.registerOre("crushedPurifiedTin", Ic2Items.purifiedCrushedTinOre.copy());
		OreDictionary.registerOre("crushedPurifiedUranium", Ic2Items.purifiedCrushedUraniumOre.copy());
		OreDictionary.registerOre("blockBronze", Ic2Items.bronzeBlock.copy());
		OreDictionary.registerOre("blockCopper", Ic2Items.copperBlock.copy());
		OreDictionary.registerOre("blockTin", Ic2Items.tinBlock.copy());
		OreDictionary.registerOre("blockUranium", Ic2Items.uraniumBlock.copy());
		OreDictionary.registerOre("blockLead", Ic2Items.leadBlock.copy());
		OreDictionary.registerOre("blockSteel", Ic2Items.advironblock.copy());
		OreDictionary.registerOre("circuitBasic", Ic2Items.electronicCircuit.copy());
		OreDictionary.registerOre("circuitAdvanced", Ic2Items.advancedCircuit.copy());
		OreDictionary.registerOre("gemDiamond", Ic2Items.industrialDiamond.copy());
		OreDictionary.registerOre("gemDiamond", Items.diamond);
		OreDictionary.registerOre("craftingToolForgeHammer", new ItemStack(Ic2Items.ForgeHammer.getItem(), 1, 32767));
		OreDictionary.registerOre("craftingToolWireCutter", new ItemStack(Ic2Items.cutter.getItem(), 1, 32767));
		EnergyNet.instance = EnergyNetGlobal.initialize();
		IC2Crops.init();
		Info.DMG_ELECTRIC = IC2DamageSource.electricity;
		Info.DMG_NUKE_EXPLOSION = IC2DamageSource.nuke;
		Info.DMG_RADIATION = IC2DamageSource.radiation;
		IC2Potion.init();
		new IC2Loot();
		achievements = new IC2Achievements();
		EntityRegistry.registerModEntity(EntityMiningLaser.class, "MiningLaser", 0, this, 160, 5, true);
		EntityRegistry.registerModEntity(EntityDynamite.class, "Dynamite", 1, this, 160, 5, true);
		EntityRegistry.registerModEntity(EntityStickyDynamite.class, "StickyDynamite", 2, this, 160, 5, true);
		EntityRegistry.registerModEntity(EntityItnt.class, "Itnt", 3, this, 160, 5, true);
		EntityRegistry.registerModEntity(EntityNuke.class, "Nuke", 4, this, 160, 5, true);
		EntityRegistry.registerModEntity(EntityBoatCarbon.class, "BoatCarbon", 5, this, 80, 3, true);
		EntityRegistry.registerModEntity(EntityBoatRubber.class, "BoatRubber", 6, this, 80, 3, true);
		EntityRegistry.registerModEntity(EntityBoatElectric.class, "BoatElectric", 7, this, 80, 3, true);
		EntityRegistry.registerModEntity(EntityParticle.class, "Particle", 8, this, 160, 1, true);
		int d = Integer.parseInt(new SimpleDateFormat("Mdd").format(new Date()));
		suddenlyHoes = (double) d > Math.cbrt(6.4E7D) && (double) d < Math.cbrt(6.5939264E7D);
		seasonal = (double) d > Math.cbrt(1.089547389E9D) && (double) d < Math.cbrt(1.338273208E9D);
		GameRegistry.registerWorldGenerator(this, 0);
		GameRegistry.registerFuelHandler(this);
		MinecraftForge.EVENT_BUS.register(new IC2BucketHandler());
		initialized = true;
		log.debug(LogCategory.General, "Finished pre-init after %d ms.", Long.valueOf((System.nanoTime() - startTime) / 1000000L));
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		long startTime = System.nanoTime();
		log.debug(LogCategory.General, "Starting init.");
		Rezepte.loadRecipes();
		log.debug(LogCategory.General, "Finished init after %d ms.", Long.valueOf((System.nanoTime() - startTime) / 1000000L));
	}

	@EventHandler
	public void modsLoaded(FMLPostInitializationEvent event)
	{
		long startTime = System.nanoTime();
		log.debug(LogCategory.General, "Starting post-init.");
		if (!initialized)
		{
			platform.displayError("IndustrialCraft 2 has failed to initialize properly.");
		}

		Rezepte.loadFailedRecipes();
		Localization.postInit();
		if (loadSubModule("bcIntegration"))
		{
			log.debug(LogCategory.SubModule, "BuildCraft integration module loaded.");
		}

		List<IRecipeInput> purgedRecipes = new ArrayList();
		purgedRecipes.addAll(ConfigUtil.asRecipeInputList(MainConfig.get(), "recipes/purge"));
		if (ConfigUtil.getBool(MainConfig.get(), "balance/disableEnderChest"))
		{
			purgedRecipes.add(new RecipeInputItemStack(new ItemStack(Blocks.ender_chest)));
		}

		Iterator<IRecipe> it = CraftingManager.getInstance().getRecipeList().iterator();

		label148:
		while (it.hasNext())
		{
			IRecipe recipe = it.next();
			ItemStack output = recipe.getRecipeOutput();
			if (output != null)
			{
				Iterator found = purgedRecipes.iterator();

				while (true)
				{
					if (!found.hasNext())
					{
						continue label148;
					}

					IRecipeInput input = (IRecipeInput) found.next();
					if (input.matches(output))
					{
						break;
					}
				}

				it.remove();
			}
		}

		if (ConfigUtil.getBool(MainConfig.get(), "recipes/smeltToIc2Items"))
		{
			Map<ItemStack, ItemStack> smeltingMap = FurnaceRecipes.smelting().getSmeltingList();

			for (Entry<ItemStack, ItemStack> entry : smeltingMap.entrySet())
			{
				boolean found = false;

				for (int oreId : OreDictionary.getOreIDs(entry.getValue()))
				{
					String oreName = OreDictionary.getOreName(oreId);

					for (ItemStack ore : OreDictionary.getOres(oreName))
					{
						if (ore.getItem() != null && Item.itemRegistry.getNameForObject(ore.getItem()).startsWith("IC2:"))
						{
							entry.setValue(StackUtil.copyWithSize(ore, entry.getValue().stackSize));
							found = true;
							break;
						}
					}

					if (found)
					{
						break;
					}
				}
			}
		}

		TileEntityRecycler.initLate();
		GameRegistry.registerTileEntity(TileEntityBlock.class, "Empty Management TileEntity");
		UuIndex.instance.init();
		UuIndex.instance.refresh(true);
		platform.onPostInit();
		platform.registerRenderers();
		log.debug(LogCategory.General, "Finished post-init after %d ms.", Long.valueOf((System.nanoTime() - startTime) / 1000000L));
		log.info(LogCategory.General, "%s version %s loaded.", "IC2", "2.2.827-experimental");
	}

	private static boolean loadSubModule(String name)
	{
		log.debug(LogCategory.SubModule, "Loading %s submodule: %s.", "IC2", name);

		try
		{
			Class<?> subModuleClass = IC2.class.getClassLoader().loadClass("ic2." + name + ".SubModule");
			return ((Boolean) subModuleClass.getMethod("init").invoke(null, new Object[0])).booleanValue();
		}
		catch (Throwable var2)
		{
			log.debug(LogCategory.SubModule, "Submodule %s not loaded.", name);
			return false;
		}
	}

	@EventHandler
	public void serverStart(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandIc2());
		event.registerServerCommand(new CommandTps());
	}

	@EventHandler
	public void onMissingMappings(FMLMissingMappingsEvent event)
	{
		BlocksItems.onMissingMappings(event);
	}

	@Override
	public int getBurnTime(ItemStack stack)
	{
		if (stack != null)
		{
			if (Ic2Items.rubberSapling != null && stack.isItemEqual(Ic2Items.rubberSapling))
			{
				return 80;
			}

			if (stack.getItem() == Items.reeds)
			{
				return 50;
			}

			if (StackUtil.equals(Blocks.cactus, stack))
			{
				return 50;
			}

			if (stack.isItemEqual(Ic2Items.scrap))
			{
				return 350;
			}

			if (stack.isItemEqual(Ic2Items.scrapBox))
			{
				return 3150;
			}

			if (stack.isItemEqual(Ic2Items.lavaCell))
			{
				return TileEntityFurnace.getItemBurnTime(new ItemStack(Items.lava_bucket));
			}
		}

		return 0;
	}

	@Override
	public void generate(Random random1, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
	{
		if (ConfigUtil.getBool(MainConfig.get(), "worldgen/rubberTree"))
		{
			BiomeGenBase biomegenbase = world.getWorldChunkManager().getBiomeGenAt(chunkX * 16 + 16, chunkZ * 16 + 16);
			if (biomegenbase != null && biomegenbase.biomeName != null)
			{
				int rubbertrees = 0;
				if (BiomeDictionary.isBiomeOfType(biomegenbase, Type.SWAMP))
				{
					rubbertrees += random1.nextInt(10) + 5;
				}

				if (BiomeDictionary.isBiomeOfType(biomegenbase, Type.FOREST) || BiomeDictionary.isBiomeOfType(biomegenbase, Type.JUNGLE))
				{
					rubbertrees += random1.nextInt(5) + 1;
				}

				if (random1.nextInt(100) + 1 <= rubbertrees * 2)
				{
					new WorldGenRubTree().generate(world, random1, chunkX * 16 + random1.nextInt(16), rubbertrees, chunkZ * 16 + random1.nextInt(16));
				}
			}
		}

		int baseHeight = getSeaLevel(world) + 1;
		int baseScale = Math.round((float) baseHeight * ConfigUtil.getFloat(MainConfig.get(), "worldgen/oreDensityFactor"));
		if (ConfigUtil.getBool(MainConfig.get(), "worldgen/copperOre") && Ic2Items.copperOre != null)
		{
			int baseCount = 15 * baseScale / 64;
			int count = (int) Math.round(random1.nextGaussian() * Math.sqrt((double) baseCount) + (double) baseCount);

			for (int n = 0; n < count; ++n)
			{
				int x = chunkX * 16 + random1.nextInt(16);
				int y = random1.nextInt(40 * baseHeight / 64) + random1.nextInt(20 * baseHeight / 64) + 10 * baseHeight / 64;
				int z = chunkZ * 16 + random1.nextInt(16);
				new WorldGenMinable(StackUtil.getBlock(Ic2Items.copperOre), Ic2Items.copperOre.getItemDamage(), 10, Blocks.stone).generate(world, random1, x, y, z);
			}
		}

		if (ConfigUtil.getBool(MainConfig.get(), "worldgen/tinOre") && Ic2Items.tinOre != null)
		{
			int baseCount = 25 * baseScale / 64;
			int count = (int) Math.round(random1.nextGaussian() * Math.sqrt((double) baseCount) + (double) baseCount);

			for (int n = 0; n < count; ++n)
			{
				int x = chunkX * 16 + random1.nextInt(16);
				int y = random1.nextInt(40 * baseHeight / 64);
				int z = chunkZ * 16 + random1.nextInt(16);
				new WorldGenMinable(StackUtil.getBlock(Ic2Items.tinOre), Ic2Items.tinOre.getItemDamage(), 6, Blocks.stone).generate(world, random1, x, y, z);
			}
		}

		if (ConfigUtil.getBool(MainConfig.get(), "worldgen/uraniumOre") && Ic2Items.uraniumOre != null)
		{
			int baseCount = 20 * baseScale / 64;
			int count = (int) Math.round(random1.nextGaussian() * Math.sqrt((double) baseCount) + (double) baseCount);

			for (int n = 0; n < count; ++n)
			{
				int x = chunkX * 16 + random1.nextInt(16);
				int y = random1.nextInt(64 * baseHeight / 64);
				int z = chunkZ * 16 + random1.nextInt(16);
				new WorldGenMinable(StackUtil.getBlock(Ic2Items.uraniumOre), Ic2Items.uraniumOre.getItemDamage(), 3, Blocks.stone).generate(world, random1, x, y, z);
			}
		}

		if (ConfigUtil.getBool(MainConfig.get(), "worldgen/leadOre") && Ic2Items.leadOre != null)
		{
			int baseCount = 8 * baseScale / 64;
			int count = (int) Math.round(random1.nextGaussian() * Math.sqrt((double) baseCount) + (double) baseCount);

			for (int n = 0; n < count; ++n)
			{
				int x = chunkX * 16 + random1.nextInt(16);
				int y = random1.nextInt(64 * baseHeight / 64);
				int z = chunkZ * 16 + random1.nextInt(16);
				new WorldGenMinable(StackUtil.getBlock(Ic2Items.leadOre), Ic2Items.leadOre.getItemDamage(), 4, Blocks.stone).generate(world, random1, x, y, z);
			}
		}

	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerLoggedInEvent event)
	{
	}

	@SubscribeEvent
	public void onPlayerLogout(PlayerLoggedOutEvent event)
	{
		if (platform.isSimulating())
		{
			keyboard.removePlayerReferences(event.player);
		}

	}

	@SubscribeEvent
	public void onWorldUnload(Unload event)
	{
		WorldData.onWorldUnload(event.world);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onTextureStitchPost(Post event)
	{
		BlockTextureStitched.onPostStitch();
	}

	@SubscribeEvent
	public void onChunkWatchEvent(Watch event)
	{
		Chunk chunk = event.player.worldObj.getChunkFromChunkCoords(event.chunk.chunkXPos, event.chunk.chunkZPos);

		for (TileEntity tileEntity : (Iterable<? extends TileEntity>) chunk.chunkTileEntityMap.values())
		{
			network.get().sendInitialData(tileEntity, event.player);
		}

	}

	public static void explodeMachineAt(World world, int x, int y, int z, boolean noDrop)
	{
		ExplosionIC2 explosion = new ExplosionIC2(world, null, 0.5D + (double) x, 0.5D + (double) y, 0.5D + (double) z, 2.5F, 0.75F);
		explosion.destroy(x, y, z, noDrop);
		explosion.doExplosion();
	}

	public static int getSeaLevel(World world)
	{
		return world.provider.getAverageGroundLevel();
	}

	public static int getWorldHeight(World world)
	{
		return world.getHeight();
	}

	public static void addValuableOre(Block block, int value)
	{
		addValuableOre(new RecipeInputItemStack(new ItemStack(block)), value);
	}

	public static void addValuableOre(IRecipeInput input, int value)
	{
		if (input == null)
		{
			throw new NullPointerException("input is null");
		}
		else
		{
			valuableOres.put(input, Integer.valueOf(value));
		}
	}

	@SubscribeEvent
	public void registerOre(OreRegisterEvent event)
	{
		String oreClass = event.Name;
		ItemStack ore = event.Ore;
		if (ore.getItem() instanceof ItemBlock)
		{
			if (oreClass.startsWith("dense"))
			{
				oreClass = oreClass.substring("dense".length());
			}

			int value = 0;
			if (oreClass.equals("oreCoal"))
			{
				value = 1;
			}
			else if (!oreClass.equals("oreCopper") && !oreClass.equals("oreTin") && !oreClass.equals("oreLead") && !oreClass.equals("oreQuartz"))
			{
				if (!oreClass.equals("oreIron") && !oreClass.equals("oreGold") && !oreClass.equals("oreRedstone") && !oreClass.equals("oreLapis") && !oreClass.equals("oreSilver"))
				{
					if (!oreClass.equals("oreUranium") && !oreClass.equals("oreGemRuby") && !oreClass.equals("oreGemGreenSapphire") && !oreClass.equals("oreGemSapphire") && !oreClass.equals("oreRuby") && !oreClass.equals("oreGreenSapphire") && !oreClass.equals("oreSapphire"))
					{
						if (!oreClass.equals("oreDiamond") && !oreClass.equals("oreEmerald") && !oreClass.equals("oreTungsten"))
						{
							if (oreClass.startsWith("ore"))
							{
								value = 1;
							}
						}
						else
						{
							value = 5;
						}
					}
					else
					{
						value = 4;
					}
				}
				else
				{
					value = 3;
				}
			}
			else
			{
				value = 2;
			}

			if (value > 0)
			{
				if (event.Name.startsWith("dense"))
				{
					value *= 3;
				}

				addValuableOre(new RecipeInputItemStack(ore), value);
			}

		}
	}

	@SubscribeEvent
	public void onLivingSpecialSpawn(SpecialSpawn event)
	{
		if (seasonal && (event.entityLiving instanceof EntityZombie || event.entityLiving instanceof EntitySkeleton) && event.entityLiving.worldObj.rand.nextFloat() < 0.1F)
		{
			EntityLiving entity = (EntityLiving) event.entityLiving;

			for (int i = 0; i <= 4; ++i)
			{
				entity.setEquipmentDropChance(i, Float.NEGATIVE_INFINITY);
			}

			if (entity instanceof EntityZombie)
			{
				entity.setCurrentItemOrArmor(0, Ic2Items.nanoSaber.copy());
			}

			if (event.entityLiving.worldObj.rand.nextFloat() < 0.1F)
			{
				entity.setCurrentItemOrArmor(1, Ic2Items.quantumHelmet.copy());
				entity.setCurrentItemOrArmor(2, Ic2Items.quantumBodyarmor.copy());
				entity.setCurrentItemOrArmor(3, Ic2Items.quantumLeggings.copy());
				entity.setCurrentItemOrArmor(4, Ic2Items.quantumBoots.copy());
			}
			else
			{
				entity.setCurrentItemOrArmor(1, Ic2Items.nanoHelmet.copy());
				entity.setCurrentItemOrArmor(2, Ic2Items.nanoBodyarmor.copy());
				entity.setCurrentItemOrArmor(3, Ic2Items.nanoLeggings.copy());
				entity.setCurrentItemOrArmor(4, Ic2Items.nanoBoots.copy());
			}
		}

	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void textureHook(Post event)
	{
		if (event.map.getTextureType() == 0)
		{
			for (InternalName name : BlocksItems.getIc2FluidNames())
			{
				Block block = BlocksItems.getFluidBlock(name);
				Fluid fluid = BlocksItems.getFluid(name);
				fluid.setIcons(block.getBlockTextureFromSide(1), block.getBlockTextureFromSide(2));
			}
		}

	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onViewRenderFogDensity(FogDensity event)
	{
		if (event.block instanceof BlockIC2Fluid)
		{
			event.setCanceled(true);
			Fluid fluid = ((BlockIC2Fluid) event.block).getFluid();
			GL11.glFogi(2917, 2048);
			event.density = (float) Util.map((double) Math.abs(fluid.getDensity()), 20000.0D, 2.0D);
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onViewRenderFogColors(FogColors event)
	{
		if (event.block instanceof BlockIC2Fluid)
		{
			int color = ((BlockIC2Fluid) event.block).getColor();
			event.red = (float) (color >>> 16 & 255) / 255.0F;
			event.green = (float) (color >>> 8 & 255) / 255.0F;
			event.blue = (float) (color & 255) / 255.0F;
		}
	}

	static
	{
		try
		{
			new ChunkCoordinates(1, 2, 3).getDistanceSquared(2, 3, 4);
		}
		catch (Throwable var1)
		{
			throw new Error("IC2 is incompatible with this environment, use the normal IC2 version, not the dev one.", var1);
		}

		instance = null;
		network = new SideGateway("ic2.core.network.NetworkManager", "ic2.core.network.NetworkManagerClient");
		random = new Random();
		valuableOres = new HashMap();
		suddenlyHoes = false;
		seasonal = false;
		initialized = false;
		tabIC2 = new CreativeTabIC2();
		textureDomain = "IC2".toLowerCase(Locale.ENGLISH);
	}
}
