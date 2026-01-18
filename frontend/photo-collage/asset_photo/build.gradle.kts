plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("asset_photo")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}