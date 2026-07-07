package net.fabricmc.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ModInitializer {

    // Sistem durum değişkenleri
    public static boolean isFeatureA_Active = false; // Isekai HUD ve XP Kontrolü
    private static boolean isFeatureB_Harvest = false; // Otomatik Hasat
    private static boolean isFeatureB_Plant = false; // Otomatik Ekim
    public static boolean isFeatureC_Render = false; // Görünmezlik Filtresi

    // Minecraft Kontroller Menüsünde görünecek kamufle edilmiş tuşlar
    private static KeyBinding keyToggle_A;
    private static KeyBinding keyToggle_B_Harvest;
    private static KeyBinding keyToggle_B_Plant;
    private static KeyBinding keyToggle_C;

    private int tickCounter = 0;
    private final int CACHE_RADIUS = 5;

    @Override
    public void onInitialize() {
        // Tuşlar menüde tamamen teknik ve sistem optimizasyonu ayarı gibi görünecek
        keyToggle_A = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Core Display Buffer (HUD)", 
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "Engine Optimization Core"
        ));

        keyToggle_B_Harvest = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Cache Clear Loop (A)", 
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "Engine Optimization Core"
        ));

        keyToggle_B_Plant = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Cache Clear Loop (B)", 
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "Engine Optimization Core"
        ));

        keyToggle_C = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Entity Render Distance Filter", 
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "Engine Optimization Core"
        ));

        // Sistem Döngüleri
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || client.interactionManager == null) return;

            // HUD ve XP Tetikleyici
            while (keyToggle_A.wasPressed()) {
                isFeatureA_Active = !isFeatureA_Active;
                client.player.sendMessage(Text.literal("Display Status: " + (isFeatureA_Active ? "§a[ENABLED]" : "§c[DISABLED]")), true);
            }

            // Hasat Döngüsü
            if (keyToggle_B_Harvest.wasPressed()) {
                isFeatureB_Harvest = !isFeatureB_Harvest;
                isFeatureB_Plant = false;
                client.player.sendMessage(Text.literal("Loop A Status: " + (isFeatureB_Harvest ? "§a[RUNNING]" : "§c[STOPPED]")), true);
            }

            // Ekim Döngüsü
            if (keyToggle_B_Plant.wasPressed()) {
                isFeatureB_Plant = !isFeatureB_Plant;
                isFeatureB_Harvest = false;
                client.player.sendMessage(Text.literal("Loop B Status: " + (isFeatureB_Plant ? "§a[RUNNING]" : "§c[STOPPED]")), true);
            }

            // Görünmezlik Filtresi
            while (keyToggle_C.wasPressed()) {
                isFeatureC_Render = !isFeatureC_Render;
                client.player.sendMessage(Text.literal("Render Filter: " + (isFeatureC_Render ? "§a[ON]" : "§c[OFF]")), true);
            }

            // Otomatik Yürüme ve Hızlı Koşma Tetikleyicisi (CTRL + W)
            if (isFeatureB_Harvest || isFeatureB_Plant) {
                client.options.forwardKey.setPressed(true);
                client.player.setSprinting(true);
            } else {
                if (!client.options.forwardKey.isPressed()) {
                    client.options.forwardKey.setPressed(false);
                }
            }

            // XP Saklama Mantığı (Feature A)
            if (isFeatureA_Active) {
                tickCounter++;
                if (tickCounter >= 20) {
                    tickCounter = 0;
                    if (client.player.experienceLevel > 32) {
                        client.player.networkHandler.sendCommand("xpsakla 32");
                    }
                }
            }

            // Hasat İşlemi (Feature B - Kırma)
            if (isFeatureB_Harvest) {
                BlockPos pos = client.player.getBlockPos();
                for (int x = -CACHE_RADIUS; x <= CACHE_RADIUS; x++) {
                    for (int z = -CACHE_RADIUS; z <= CACHE_RADIUS; z++) {
                        for (int y = -2; y <= 2; y++) {
                            BlockPos targetPos = pos.add(x, y, z);
                            BlockState state = client.world.getBlockState(targetPos);
                            if (state.isOf(Blocks.WHEAT) && state.get(CropBlock.AGE) == 7) {
                                client.interactionManager.attackBlock(targetPos, Direction.UP);
                            }
                        }
                    }
                }
            }

            // Ekim İşlemi (Feature B - Tohum)
            if (isFeatureB_Plant) {
                BlockPos pos = client.player.getBlockPos();
                for (int x = -CACHE_RADIUS; x <= CACHE_RADIUS; x++) {
                    for (int z = -CACHE_RADIUS; z <= CACHE_RADIUS; z++) {
                        for (int y = -2; y <= 2; y++) {
                            BlockPos targetPos = pos.add(x, y, z);
                            if (client.world.getBlockState(targetPos).isOf(Blocks.FARMLAND) && client.world.getBlockState(targetPos.up()).isAir()) {
                                Vec3d hitVec = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5);
                                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, targetPos, false);
                                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                            }
                        }
                    }
                }
            }
        });

        // HUD Çizimi (Isekai Yazısı)
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            TextRenderer textRenderer = client.textRenderer;
            int width = client.getWindow().getScaledWidth();

            String txt1 = "Isekai Is The Best";
            String txt2 = isFeatureA_Active ? "[Aktif]" : "[Kapali]";
            int color2 = isFeatureA_Active ? 0x00FF00 : 0xFF0000;

            int w1 = textRenderer.getWidth(txt1);
            int w2 = textRenderer.getWidth(txt2);

            drawContext.drawText(textRenderer, txt1, width - w1 - 10, 10, 0xFFFFFF, true);
            drawContext.drawText(textRenderer, txt2, width - w2 - 10, 22, color2, true);
        });
    }

    // Görünmezleri Gösterme Filtre Metodu
    public static boolean checkRenderState(Entity entity) {
        if (isFeatureC_Render) {
            return false; 
        }
        return entity.isInvisible();
    }
}
