package com.fantasyclouds.numerical_sculptor.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ItemModifierData {
    @SerializedName("item")
    private String itemId;
    @SerializedName("modifiers")
    private List<ItemModifierEntry> modifiers;

    public String getItemId() { return itemId; }
    public List<ItemModifierEntry> getModifiers() { return modifiers; }

    public boolean isValid() {
        if (itemId == null || itemId.isEmpty()) return false;
        if (modifiers == null || modifiers.isEmpty()) return false;

        // 所有 modifier 必须有效
        if (!modifiers.stream().allMatch(ItemModifierEntry::isValid)) return false;

        // 检查物品 ID 是否合法且已注册
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return false;
        return ForgeRegistries.ITEMS.containsKey(id);
    }
}
