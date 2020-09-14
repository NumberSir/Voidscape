package tamaized.voidscape;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTier;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tamaized.voidscape.network.NetworkMessages;
import tamaized.voidscape.turmoil.Insanity;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.Turmoil;
import tamaized.voidscape.world.VoidChunkGenerator;
import tamaized.voidscape.world.VoidTeleporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(Voidscape.MODID)
public class Voidscape {

	public static final String MODID = "voidscape";

	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder.
			named(new ResourceLocation(MODID, MODID)).
			clientAcceptedVersions(s -> true).
			serverAcceptedVersions(s -> true).
			networkProtocolVersion(() -> "1").
			simpleChannel();
	public static final RegistryKey<World> WORLD_KEY = RegistryKey.func_240903_a_(Registry.WORLD_KEY, new ResourceLocation(MODID, "void"));
	public static final SubCapability.ISubCap.SubCapKey<Turmoil> subCapTurmoilData = SubCapability.AttachedSubCap.register(Turmoil.class, Turmoil::new);
	public static final SubCapability.ISubCap.SubCapKey<Insanity> subCapInsanity = SubCapability.AttachedSubCap.register(Insanity.class, Insanity::new);
	private static final ResourceLocation DIMENSION_TYPE = new ResourceLocation(MODID, "void");

	public static final ItemGroup CREATIVE_TAB = new ItemGroup(MODID.concat(".item_group")) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(VOIDIC_CRYSTAL_ORE_ITEM.get());
		}
	};

	private static final List<DeferredRegister> REGISTERS = new ArrayList<>();
	public static final RegistryObject<SoundEvent> AMBIENCE = create(ForgeRegistries.SOUND_EVENTS).register("ambience", () -> new SoundEvent(new ResourceLocation(MODID, "ambience")));
	public static final RegistryObject<Block> VOIDIC_CRYSTAL_ORE = create(ForgeRegistries.BLOCKS).register("voidic_crystal_ore", () -> new Block(Block.Properties.create(Material.ROCK, MaterialColor.BLACK).
			harvestTool(ToolType.PICKAXE).
			hardnessAndResistance(3F, 3F).
			setRequiresTool().
			harvestLevel(ItemTier.DIAMOND.getHarvestLevel())) {
		@Override
		public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
			boolean flag = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
			world.setBlockState(pos, Blocks.BEDROCK.getDefaultState(), world.isRemote ? 11 : 3);
			return flag;
		}
	});
	public static final RegistryObject<Item> VOIDIC_CRYSTAL_ORE_ITEM = create(ForgeRegistries.ITEMS).register(VOIDIC_CRYSTAL_ORE.getId().getPath(), () -> new BlockItem(VOIDIC_CRYSTAL_ORE.get(), new Item.Properties().group(CREATIVE_TAB)));

	public Voidscape() {
		IEventBus busMod = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus busForge = MinecraftForge.EVENT_BUS;
		for (DeferredRegister register : REGISTERS)
			register.register(busMod);
		busMod.addListener((Consumer<FMLCommonSetupEvent>) event -> {
			NetworkMessages.register(NETWORK);
			Registry.register(Registry.field_239690_aB_, new ResourceLocation(MODID, "void"), VoidChunkGenerator.codec);
			CapabilityManager.INSTANCE.register(SubCapability.ISubCap.class, new SubCapability.ISubCap.Storage() {
			}, SubCapability.AttachedSubCap::new);
		});
		busMod.addListener((Consumer<FMLClientSetupEvent>) event -> {
			RenderTypeLookup.setRenderLayer(VOIDIC_CRYSTAL_ORE.get(), RenderType.getCutoutMipped());
			DimensionRenderInfo.field_239208_a_.put(getDimensionType(), new DimensionRenderInfo(Float.NaN, false, DimensionRenderInfo.FogType.NONE, false, false) {
				@Override
				public Vector3d func_230494_a_(Vector3d p_230494_1_, float p_230494_2_) {
					return Vector3d.ZERO;
				}

				@Override
				public boolean func_230493_a_(int p_230493_1_, int p_230493_2_) {
					return true;
				}

				@Override
				@Nullable
				public float[] func_230492_a_(float p_230492_1_, float p_230492_2_) {
					return null;
				}
			});
		});
		busForge.addListener((Consumer<FMLServerStartingEvent>) event ->

				event.getServer().getCommandManager().getDispatcher().register(LiteralArgumentBuilder.<CommandSource>literal("voidscape").
						then(VoidCommands.Debug.register()))

		);
		busForge.addListener((Consumer<EntityViewRenderEvent.FogColors>) event -> {
			if (Minecraft.getInstance().world != null && checkForVoidDimension(Minecraft.getInstance().world)) {
				event.setRed(0.04F);
				event.setGreen(0.03F);
				event.setBlue(0.05F);
			}
		});
		busForge.addListener((Consumer<LivingDeathEvent>) event -> {
			if (event.getEntity() instanceof PlayerEntity && checkForVoidDimension(event.getEntity().world)) {
				event.setCanceled(true);
				((PlayerEntity) event.getEntity()).setHealth(((PlayerEntity) event.getEntity()).getMaxHealth());
				event.getEntity().changeDimension(getWorld(event.getEntity().world, World.field_234918_g_), VoidTeleporter.INSTANCE);
			}
		});
		busForge.addListener((Consumer<TickEvent.PlayerTickEvent>) event -> {
			if (event.player.world != null && checkForVoidDimension(event.player.world) && event.player.ticksExisted % 30 == 0 && event.player.getRNG().nextFloat() <= 0.05F) {
				final int dist = 64;
				final int rad = dist / 2;
				final Supplier<Integer> exec = () -> event.player.getRNG().nextInt(dist) - rad;
				BlockPos dest = event.player.func_233580_cy_().add(exec.get(), exec.get(), exec.get());
				if (event.player.world.getBlockState(dest).equals(Blocks.BEDROCK.getDefaultState()))
					event.player.world.setBlockState(dest, VOIDIC_CRYSTAL_ORE.get().getDefaultState());
			}
		});
	}

	private static <R extends IForgeRegistryEntry<R>> DeferredRegister<R> create(IForgeRegistry<R> type) {
		DeferredRegister<R> def = DeferredRegister.create(type, MODID);
		REGISTERS.add(def);
		return def;
	}

	public static ResourceLocation getDimensionType() {
		return DIMENSION_TYPE;
	}

	public static boolean checkForVoidDimension(World world) {
		return checkForVoidDimension(world.func_230315_m_());
	}

	public static boolean checkForVoidDimension(DimensionType type) {
		return type.func_242725_p().equals(getDimensionType());
	}

	public static ServerWorld getWorld(World world, RegistryKey<World> dest) {
		return Objects.requireNonNull(Objects.requireNonNull(world.getServer()).getWorld(dest));
	}

	@Nonnull
	@SuppressWarnings("ConstantConditions")
	public static <T> T getNull() {
		return null;
	}

}
