package de.ambertation.wunderreich.network;

import de.ambertation.wunderreich.Wunderreich;
import de.ambertation.wunderreich.config.Configs;
import de.ambertation.wunderreich.config.MainConfig;
import de.ambertation.wunderreich.interfaces.IMerchantMenu;
import de.ambertation.wunderreich.items.VillagerWhisperer;
import de.ambertation.wunderreich.registries.WunderreichBlocks;
import de.ambertation.wunderreich.registries.WunderreichItems;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import ru.bclib.api.dataexchange.DataExchangeAPI;
import ru.bclib.util.Triple;

public class CycleTradesMessage {
    public final static ResourceLocation CHANNEL = new ResourceLocation(Wunderreich.MOD_ID, "cycle_trades");

    public static void register() {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayNetworking.registerReceiver(handler, CHANNEL, (_server, _player, _handler, _buf, _responseSender) -> {
                cycleTrades(_player);
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayNetworking.unregisterReceiver(handler, CHANNEL);
        });
    }

    public static void send() {
        ClientPlayNetworking.send(CHANNEL, PacketByteBufs.create());
    }

    public static Triple<ItemStack, Player, EquipmentSlot> getClosestWhisperer(Villager villager, boolean doLog) {
        if (villager.level instanceof ServerLevel server) {
            Player p = server.getNearestPlayer(villager, 6);
            if (p == null) return null;

            final ItemStack mainHand = p.getMainHandItem();
            final ItemStack offHand = p.getOffhandItem();
            final EquipmentSlot slot;
            final ItemStack whisperer;
            if (mainHand.is(WunderreichItems.WHISPERER)) {
                whisperer = mainHand;
                slot = EquipmentSlot.MAINHAND;
            } else if (offHand.is(WunderreichItems.WHISPERER)) {
                whisperer = offHand;
                slot = EquipmentSlot.OFFHAND;
            } else {
                return null;
            }

            if (doLog) {
                Wunderreich.LOGGER.info("Player " + p.getName() + " uses Whisperer on Librarian");
            }
            return new Triple<>(whisperer, p, slot);
        }

        return null;
    }
    public static boolean canSelectTrades(Villager villager) {
        return canSelectTrades(villager, true);
    }

    public static boolean canSelectTrades(Villager villager, boolean doLog) {
        if (!Configs.MAIN.get(MainConfig.ALLOW_LIBRARIAN_SELECTION)) return false;
        if (villager == null || villager.getVillagerXp() > 0) return false;

        VillagerData villagerData = villager.getVillagerData();
        VillagerProfession profession = villagerData.getProfession();
        if (profession == null || !PoiType.LIBRARIAN.equals(profession.getJobPoiType())) return false;

        Triple<ItemStack, Player, EquipmentSlot> whispererStack = getClosestWhisperer(villager, doLog);
        if (whispererStack == null) return false;

        return true;
    }

    public static boolean hasSelectedTrades(Villager villager, MerchantOffers offers) {
        if (offers == null) return true;
        if (!canSelectTrades(villager, false)) return true;
        Triple<ItemStack, Player, EquipmentSlot> whispererStack = getClosestWhisperer(villager, false);
        if (whispererStack==null) return true;
        VillagerWhisperer whisperer = (VillagerWhisperer) whispererStack.first.getItem();

        for (MerchantOffer offer : offers) {
            if (offer.getResult().is(Items.ENCHANTED_BOOK)) {
                offer.getResult().getTagElement("StoredEnchantments");
                CompoundTag tag = (CompoundTag) offer.getResult().getTag().getList("StoredEnchantments", Tag.TAG_COMPOUND).get(0);
                //int level = tag.getShort("lvl");
                String type = tag.getString("id");

                if (type.equals(whisperer.getEnchantmentID())) {
                    whispererStack.first.hurtAndBreak(1, whispererStack.second, player->player.broadcastBreakEvent(whispererStack.third));
                    return true;
                }
            }
        }

        return false;
    }


    //Code adopted from "Easy Villagers"
    public static void cycleTrades(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu)) {
            return;
        }
        if (!Configs.MAIN.get(MainConfig.ALLOW_TRADES_CYCLING)) return;
        MerchantMenu menu = (MerchantMenu) player.containerMenu;

        Villager villager = ((IMerchantMenu) menu).getVillager();
        if (villager == null || villager.getVillagerXp() > 0) {
            return;
        }

        villager.setOffers(null);
//		for (MerchantOffer merchantoffer : villager.getOffers()) {
//			merchantoffer.resetSpecialPriceDiff();
//		}
//
//		int i = villager.getPlayerReputation(Minecraft.getInstance().player);
//		if (i != 0) {
//			for (MerchantOffer merchantoffer : villager.getOffers()) {
//				merchantoffer.addToSpecialPriceDiff((int)(-Math.floor((float) i * merchantoffer.getPriceMultiplier())));
//			}
//		}

        player.sendMerchantOffers(menu.containerId, villager.getOffers(), villager.getVillagerData().getLevel(), villager.getVillagerXp(), villager.showProgressBar(), villager.canRestock());
    }
}
