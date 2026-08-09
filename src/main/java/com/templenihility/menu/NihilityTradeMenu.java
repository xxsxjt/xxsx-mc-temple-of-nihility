package com.templenihility.menu;

import com.templenihility.entity.NihilityCreature;
import com.templenihility.init.ModMenus;
import com.templenihility.trade.TradeManager;
import com.templenihility.trade.TradeOffer;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NihilityTradeMenu extends AbstractContainerMenu {
    private static final double MAX_TRADING_DISTANCE_SQR = 8.0 * 8.0;
    public static final int PAYMENT_SLOT = 0;
    public static final int REQUIRED_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int PLAYER_INV_START = 3;
    public static final int OFFERS_PER_PAGE = 7;
    public static final int BUTTON_TRADE = 0;
    public static final int BUTTON_SELECT_BASE = 100;
    public static final int PAYMENT_SLOT_X = 136;
    public static final int REQUIRED_SLOT_X = 162;
    public static final int RESULT_SLOT_X = 220;
    public static final int TRADE_SLOT_Y = 37;

    private final SimpleContainer payment = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            updatePreview();
        }
    };
    private final SimpleContainer required = new SimpleContainer(1);
    private final SimpleContainer result = new SimpleContainer(1);
    private final List<TradeOffer> offers;
    private final int[] data = new int[3];
    private final int traderEntityId;

    public NihilityTradeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt(), buffer.readVarInt());
    }

    public NihilityTradeMenu(int id, Inventory inventory, NihilityCreature trader) {
        this(id, inventory, trader.getTier(), trader.getId());
    }

    private NihilityTradeMenu(int id, Inventory inventory, int tier, int traderEntityId) {
        super(ModMenus.NIHILITY_TRADE.get(), id);
        this.traderEntityId = traderEntityId;
        data[0] = Math.max(1, Math.min(5, tier));
        this.offers = TradeManager.getOffersForTier(data[0]);
        data[1] = 0;

        addSlot(new PaymentSlot(payment, 0, PAYMENT_SLOT_X, TRADE_SLOT_Y, this));
        addSlot(new PreviewSlot(required, 0, REQUIRED_SLOT_X, TRADE_SLOT_Y));
        addSlot(new PreviewSlot(result, 0, RESULT_SLOT_X, TRADE_SLOT_Y));
        addStandardInventorySlots(inventory, 108, 84);

        for (int i = 0; i < data.length; i++) {
            addDataSlot(DataSlot.shared(data, i));
        }
        updatePreview();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_TRADE) {
            return trade(player);
        }
        if (buttonId >= BUTTON_SELECT_BASE) {
            int index = buttonId - BUTTON_SELECT_BASE;
            if (index >= 0 && index < offers.size()) {
                data[1] = index;
                updatePreview();
                broadcastFullState();
                return true;
            }
        }
        return super.clickMenuButton(player, buttonId);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updatePreview();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == PAYMENT_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INV_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INV_START && acceptsPayment(source)) {
            if (!moveItemStackTo(source, PAYMENT_SLOT, PAYMENT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        updatePreview();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        NihilityCreature trader = getTrader(player);
        return trader != null && trader.isAlive() && !player.isSpectator()
            && trader.canTradeWith(player)
            && trader.distanceToSqr(player) <= MAX_TRADING_DISTANCE_SQR;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, payment);
        NihilityCreature trader = getTrader(player);
        if (trader != null) {
            trader.stopTrading(player);
        }
    }

    public boolean isTrader(NihilityCreature creature) {
        return creature != null && traderEntityId == creature.getId();
    }

    public int getTier() {
        return data[0];
    }

    public int getSelectedIndex() {
        return data[1];
    }

    public boolean canTradeSelected() {
        return data[2] == 1;
    }

    public int getOfferCount() {
        return offers.size();
    }

    public TradeOffer getOffer(int index) {
        return offers.get(index);
    }

    public TradeOffer getSelectedOffer() {
        if (offers.isEmpty()) {
            return null;
        }
        data[1] = Math.max(0, Math.min(data[1], offers.size() - 1));
        return offers.get(data[1]);
    }

    private boolean trade(Player player) {
        NihilityCreature trader = getTrader(player);
        if (trader == null || !stillValid(player) || !trader.checkTradeCooldown(player)) {
            return false;
        }

        TradeOffer offer = getSelectedOffer();
        if (offer == null) {
            return false;
        }

        ItemStack cost = offer.getCost();
        ItemStack paymentStack = payment.getItem(0);
        if (!offer.canAfford(paymentStack)) {
            player.sendSystemMessage(Component.translatable(
                "message.templenihility.trade_need_more",
                cost.getCount(), cost.getHoverName()));
            return true;
        }

        ItemStack resultStack = offer.getResult();
        paymentStack.shrink(cost.getCount());
        if (paymentStack.isEmpty()) {
            payment.setItem(0, ItemStack.EMPTY);
        } else {
            payment.setChanged();
        }

        if (!player.getInventory().add(resultStack.copy())) {
            player.drop(resultStack.copy(), false);
        }
        trader.markTradeCompleted(player);
        player.sendSystemMessage(Component.translatable(
            "message.templenihility.trade_success",
            cost.getCount(), cost.getHoverName(), resultStack.getCount(), resultStack.getHoverName()));
        updatePreview();
        broadcastFullState();
        return true;
    }

    private NihilityCreature getTrader(Player player) {
        return player.level().getEntity(traderEntityId) instanceof NihilityCreature trader ? trader : null;
    }

    private boolean acceptsPayment(ItemStack stack) {
        TradeOffer offer = getSelectedOffer();
        return offer != null && ItemStack.isSameItemSameComponents(offer.getCost(), stack);
    }

    private void updatePreview() {
        TradeOffer offer = getSelectedOffer();
        required.setItem(0, offer == null ? ItemStack.EMPTY : offer.getCost());
        result.setItem(0, offer == null ? ItemStack.EMPTY : offer.getResult());
        data[2] = offer != null && offer.canAfford(payment.getItem(0)) ? 1 : 0;
    }

    private static final class PaymentSlot extends Slot {
        private final NihilityTradeMenu menu;

        private PaymentSlot(Container container, int slot, int x, int y, NihilityTradeMenu menu) {
            super(container, slot, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return menu.acceptsPayment(stack);
        }
    }

    private static final class PreviewSlot extends Slot {
        private PreviewSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
