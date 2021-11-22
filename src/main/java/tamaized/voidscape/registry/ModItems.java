package tamaized.voidscape.registry;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.block.BlockEtherealPlant;
import tamaized.voidscape.network.server.ServerPacketTurmoilResetSkills;
import tamaized.voidscape.turmoil.SubCapability;

import java.util.UUID;

public class ModItems {

	static final DeferredRegister<Item> REGISTRY = RegUtil.create(ForgeRegistries.ITEMS);
	public static final RegistryObject<Item> VOIDIC_CRYSTAL = REGISTRY.register("voidic_crystal", () -> new Item(RegUtil.ItemProps.VOIDIC_CRYSTAL.get()) {
		@Override
		public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
			if (playerIn.getGameProfile().getId().equals(UUID.fromString("16fea09e-314e-4955-88c2-6b552ecf314a"))) {
				if (worldIn.isClientSide())
					Voidscape.NETWORK.sendToServer(new ServerPacketTurmoilResetSkills());
				return InteractionResultHolder.success(playerIn.getItemInHand(handIn));
			}
			return super.use(worldIn, playerIn, handIn);
		}
	});
	public static final RegistryObject<Item> ETHEREAL_ESSENCE = REGISTRY.register("ethereal_essence", () -> new Item(RegUtil.ItemProps.VOIDIC_CRYSTAL.get()) {
		@Override
		public InteractionResult useOn(UseOnContext context) {
			if (Voidscape.checkForVoidDimension(context.getLevel()) && context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.BEDROCK)) {
				context.getLevel().setBlockAndUpdate(context.getClickedPos(), ModBlocks.VOIDIC_CRYSTAL_ORE.get().defaultBlockState());
				context.getItemInHand().shrink(1);
				context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1F, 0.5F + context.getLevel().getRandom().nextFloat() * 0.5F);
				if (context.getLevel() instanceof ServerLevel)
					for (int i = 0; i < 50; i++)
						((ServerLevel) context.getLevel()).sendParticles(ParticleTypes.WITCH, context.
								getClickedPos().getX() + context.getLevel().getRandom().nextFloat(), context.
								getClickedPos().getY() + context.getLevel().getRandom().nextFloat(), context.
								getClickedPos().getZ() + context.getLevel().getRandom().nextFloat(), 0, 0, 0, 0, 1F);
				return InteractionResult.SUCCESS;
			}
			return super.useOn(context);
		}
	});
	public static final RegistryObject<Item> TENDRIL = REGISTRY.register("tendril", () -> new Item(RegUtil.ItemProps.VOIDIC_CRYSTAL.get()));
	public static final RegistryObject<Item> FRUIT = REGISTRY.register("fruit", () -> new Item(RegUtil.ItemProps.VOIDIC_CRYSTAL.get().
			food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).alwaysEat().build())) {
		@Override
		public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
			ItemStack itemstack = super.finishUsingItem(stack, level, entity);
			if (!level.isClientSide) {
				CompoundTag tag = stack.getTag();
				BlockEtherealPlant.State state = BlockEtherealPlant.State.VOID;
				if (tag != null)
					state = switch (tag.getString("augment")) {
						default -> BlockEtherealPlant.State.VOID;
						case "null" -> BlockEtherealPlant.State.NULL;
						case "overworld" -> BlockEtherealPlant.State.OVERWORLD;
						case "nether" -> BlockEtherealPlant.State.NETHER;
						case "end" -> BlockEtherealPlant.State.END;
					};
				switch (state) {
					case VOID -> entity.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapInsanity).ifPresent(data -> data.setInfusion(data.getInfusion() + 150)));
					case NULL -> {
						entity.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapInsanity).ifPresent(data -> data.setInfusion(data.getInfusion() - 150)));
						entity.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapInsanity).ifPresent(data -> data.setParanoia((int) data.getParanoia() - 150)));
					}
					case OVERWORLD -> entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 3));
					case NETHER -> entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 3));
					case END -> entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 3));
				}
			}
			return itemstack;
		}
	});

	static void classload() {

	}

}
