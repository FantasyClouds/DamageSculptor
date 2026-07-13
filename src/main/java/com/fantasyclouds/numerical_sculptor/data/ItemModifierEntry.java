package com.fantasyclouds.numerical_sculptor.data;

import com.google.gson.annotations.SerializedName;

public class ItemModifierEntry {
    @SerializedName("slot")
    private String slot;           // "mainhand","offhand","feet","legs","chest","head","any"
    @SerializedName("attribute")
    private String attribute;      // "minecraft:generic.armor" 等
    @SerializedName("amount")
    private double amount;
    @SerializedName("operation")
    private String operation;      // "addition","multiply_base","multiply_total"

    // getters
    public String getSlot() { return slot; }
    public String getAttribute() { return attribute; }
    public double getAmount() { return amount; }
    public String getOperation() { return operation; }

    // 校验方法
    public boolean isValid() {
        return slot != null && !slot.isEmpty()
                && attribute != null && !attribute.isEmpty()
                && operation != null && operation.matches("addition|multiply_base|multiply_total");
    }
}
