package com.fantasyclouds.numerical_sculptor.core;

import com.fantasyclouds.numerical_sculptor.NumericalSculptor;
import com.fantasyclouds.numerical_sculptor.data.ItemModifierEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;
@Mod.EventBusSubscriber
public class ItemAttributeEventHandler {
    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BASE_ATTACK_SPEED_UUID   = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    @SubscribeEvent(priority = EventPriority.LOWEST) // 确保最后执行，完全覆盖
    public static void onItemAttribute(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        List<ItemModifierEntry> entries = ItemAttributeLoader.getModifiers(itemId);
        if (entries == null) return;

        // 完全清除该物品在当前槽位上的所有修饰符
        event.clearModifiers();

        String currentSlot = event.getSlotType().getName(); // 例如 "mainhand","feet" 等

        for (ItemModifierEntry entry : entries) {
            // 匹配槽位："any" 总是添加，或 slot 名称与当前槽位一致
            if ("any".equals(entry.getSlot()) || entry.getSlot().equals(currentSlot)) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(entry.getAttribute()));
                if (attribute == null) {
                    NumericalSculptor.LOGGER.warn("Unknown attribute: {} for item {}", entry.getAttribute(), itemId);
                    continue;
                }

                AttributeModifier.Operation op = switch (entry.getOperation()) {
                    case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
                    case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                    default -> AttributeModifier.Operation.ADDITION;
                };

                // 生成固定UUID，保证同属性同槽位的修饰符可被替换，同时避免冲突
                UUID uuid;
                if (attribute == Attributes.ATTACK_DAMAGE) {
                    uuid = BASE_ATTACK_DAMAGE_UUID;
                } else if (attribute == Attributes.ATTACK_SPEED) {
                    uuid = BASE_ATTACK_SPEED_UUID;
                } else {
                    uuid = UUID.nameUUIDFromBytes(
                            (itemId + entry.getSlot() + entry.getAttribute()).getBytes()
                    );
                }

                AttributeModifier modifier = new AttributeModifier(uuid, "Item modifier", entry.getAmount(), op);
                event.addModifier(attribute, modifier);
            }
        }
    }
}
