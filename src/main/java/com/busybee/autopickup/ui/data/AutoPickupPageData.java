package com.busybee.autopickup.ui.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class AutoPickupPageData {
    public static final BuilderCodec<AutoPickupPageData> CODEC = BuilderCodec.<AutoPickupPageData>builder(AutoPickupPageData.class, AutoPickupPageData::new)
            .addField(new KeyedCodec<>("Button", Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .addField(new KeyedCodec<>("@Radius", Codec.FLOAT), (d, s) -> d.radius = s, d -> d.radius)
            .build();

    private String button;
    private Float radius;

    public String getButton() {
        return button;
    }

    public Float getRadius() {
        return radius;
    }
}