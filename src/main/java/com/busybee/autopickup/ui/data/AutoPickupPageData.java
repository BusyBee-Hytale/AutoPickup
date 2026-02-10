package com.busybee.autopickup.ui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class AutoPickupPageData {
    public static final BuilderCodec<AutoPickupPageData> CODEC = BuilderCodec.<AutoPickupPageData>builder(AutoPickupPageData.class, AutoPickupPageData::new)
            .addField(new KeyedCodec<>("Button", Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .build();

    private String button;

    public String getButton() {
        return button;
    }
}