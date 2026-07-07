package com.templenihility.item;

import com.templenihility.blockentity.NihilityVaultBlockEntity;
import com.templenihility.storage.NihilityVaultNetwork;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NihilityTerminalItem extends Item {
    private static final String TAG_DIMENSION = "VaultDimension";
    private static final String TAG_POS = "VaultPos";

    public NihilityTerminalItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (player == null || !player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof NihilityVaultBlockEntity)) {
            return super.useOn(context);
        }

        if (!level.isClientSide()) {
            bind(stack, level, pos);
            player.sendSystemMessage(Component.translatable("message.templenihility.terminal_bound",
                pos.getX(), pos.getY(), pos.getZ()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            openBoundVault(player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isBound(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(NihilityAspect.VOID.line());
        tooltip.accept(Component.translatable("tooltip.templenihility.nihility_terminal").withStyle(ChatFormatting.DARK_AQUA));
        readBoundPos(stack).ifPresentOrElse(
            pos -> tooltip.accept(Component.translatable("tooltip.templenihility.nihility_terminal_bound",
                readDimension(stack), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA)),
            () -> tooltip.accept(Component.translatable("tooltip.templenihility.nihility_terminal_unbound").withStyle(ChatFormatting.GRAY))
        );
    }

    public static boolean openBoundVault(Player player, ItemStack stack) {
        Optional<BlockPos> boundPos = readBoundPos(stack);
        if (boundPos.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.templenihility.terminal_unbound"));
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        String stackDimension = readDimension(stack);
        String currentDimension = player.level().dimension().identifier().toString();
        if (!currentDimension.equals(stackDimension)) {
            player.sendSystemMessage(Component.translatable("message.templenihility.terminal_wrong_dimension"));
            return false;
        }

        BlockPos pos = boundPos.get();
        if (!serverLevel.isLoaded(pos) || !(serverLevel.getBlockEntity(pos) instanceof NihilityVaultBlockEntity)) {
            player.sendSystemMessage(Component.translatable("message.templenihility.vault_not_found"));
            return false;
        }
        return NihilityVaultNetwork.open(serverLevel, pos, player);
    }

    public static boolean isBound(ItemStack stack) {
        return readBoundPos(stack).isPresent();
    }

    private static void bind(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TAG_DIMENSION, level.dimension().identifier().toString());
        tag.putLong(TAG_POS, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static Optional<BlockPos> readBoundPos(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!data.contains(TAG_POS)) {
            return Optional.empty();
        }
        return Optional.of(BlockPos.of(data.copyTag().getLongOr(TAG_POS, 0L)));
    }

    private static String readDimension(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            .copyTag()
            .getStringOr(TAG_DIMENSION, "");
    }
}
