package net.minestom.server.network;

import com.google.gson.JsonObject;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.crypto.ChatSession;
import net.minestom.server.crypto.PlayerPublicKey;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.handshake.HandshakePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.handshake.ResponsePacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.packet.server.login.LoginSuccessPacket;
import net.minestom.server.network.packet.server.login.SetCompressionPacket;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket.Ingredient;
import net.minestom.server.network.packet.server.status.PongPacket;
import net.minestom.server.recipe.RecipeCategory;
import net.minestom.server.utils.crypto.KeyUtils;
import org.apache.commons.net.util.Base64;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ensures that packet can be written and read correctly.
 */
public class PacketWriteReadTest {
    private static final List<ServerPacket> SERVER_PACKETS = new ArrayList<>();
    private static final List<ClientPacket> CLIENT_PACKETS = new ArrayList<>();

    private static final Component COMPONENT = Component.text("Hey");
    private static final Vec VEC = new Vec(5, 5, 5);

    @BeforeAll
    public static void setupServer() {
        // Handshake
        SERVER_PACKETS.add(new ResponsePacket(new JsonObject().toString()));
        // Status
        SERVER_PACKETS.add(new PongPacket(5));
        // Login
        //SERVER_PACKETS.add(new EncryptionRequestPacket("server", generateByteArray(16), generateByteArray(16)));
        SERVER_PACKETS.add(new LoginDisconnectPacket(COMPONENT));
        //SERVER_PACKETS.add(new LoginPluginRequestPacket(5, "id", generateByteArray(16)));
        SERVER_PACKETS.add(new LoginSuccessPacket(UUID.randomUUID(), "TheMode911", 0));
        SERVER_PACKETS.add(new SetCompressionPacket(256));
        // Play
        SERVER_PACKETS.add(new AcknowledgeBlockChangePacket(0));
        SERVER_PACKETS.add(new ActionBarPacket(COMPONENT));
        SERVER_PACKETS.add(new AttachEntityPacket(5, 10));
        SERVER_PACKETS.add(new BlockActionPacket(VEC, (byte) 5, (byte) 5, 5));
        SERVER_PACKETS.add(new BlockBreakAnimationPacket(5, VEC, (byte) 5));
        SERVER_PACKETS.add(new BlockChangePacket(VEC, 0));
        SERVER_PACKETS.add(new BlockEntityDataPacket(VEC, 5, NBT.Compound(Map.of("key", NBT.String("value")))));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.AddAction(COMPONENT, 5f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS, (byte) 2)));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.RemoveAction()));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.UpdateHealthAction(5f)));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.UpdateTitleAction(COMPONENT)));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.UpdateStyleAction(BossBar.Color.BLUE, BossBar.Overlay.PROGRESS)));
        SERVER_PACKETS.add(new BossBarPacket(UUID.randomUUID(), new BossBarPacket.UpdateFlagsAction((byte) 5)));
        SERVER_PACKETS.add(new CameraPacket(5));
        SERVER_PACKETS.add(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, 2));
        SERVER_PACKETS.add(new SystemChatPacket(COMPONENT, false));
        SERVER_PACKETS.add(new ClearTitlesPacket(false));
        SERVER_PACKETS.add(new CloseWindowPacket((byte) 2));
        SERVER_PACKETS.add(new CollectItemPacket(5, 5, 5));
        SERVER_PACKETS.add(new CraftRecipeResponse((byte) 2, "recipe"));
        SERVER_PACKETS.add(new DeathCombatEventPacket(5, COMPONENT));
        SERVER_PACKETS.add(new DeclareRecipesPacket(
                List.of(new DeclareRecipesPacket.DeclaredShapelessCraftingRecipe(
                                "minecraft:sticks",
                                "sticks",
                                RecipeCategory.Crafting.MISC,
                                List.of(new Ingredient(List.of(ItemStack.of(Material.OAK_PLANKS)))),
                                ItemStack.of(Material.STICK)
                        ),
                        new DeclareRecipesPacket.DeclaredShapedCraftingRecipe(
                                "minecraft:torch",
                                1,
                                2,
                                "",
                                RecipeCategory.Crafting.MISC,
                                List.of(new Ingredient(List.of(ItemStack.of(Material.COAL))),
                                        new Ingredient(List.of(ItemStack.of(Material.STICK)))),
                                ItemStack.of(Material.TORCH),
                                true
                        ),
                        new DeclareRecipesPacket.DeclaredBlastingRecipe(
                                "minecraft:coal",
                                "forging",
                                RecipeCategory.Cooking.MISC,
                                new Ingredient(List.of(ItemStack.of(Material.COAL))),
                                ItemStack.of(Material.IRON_INGOT),
                                5,
                                5
                        ),
                        new DeclareRecipesPacket.DeclaredSmithingTransformRecipe(
                                "minecraft:iron_to_diamond",
                                new Ingredient(List.of(ItemStack.of(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE))),
                                new Ingredient(List.of(ItemStack.of(Material.DIAMOND))),
                                new Ingredient(List.of(ItemStack.of(Material.IRON_INGOT))),
                                ItemStack.of(Material.DIAMOND)
                        ),
                        new DeclareRecipesPacket.DeclaredSmithingTrimRecipe(
                                "minecraft:iron_to_coast",
                                new Ingredient(List.of(ItemStack.of(Material.IRON_INGOT))),
                                new Ingredient(List.of(ItemStack.of(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE))),
                                new Ingredient(List.of(ItemStack.of(Material.COAL)))
                        )
                )));

        SERVER_PACKETS.add(new DestroyEntitiesPacket(List.of(5, 5, 5)));
        SERVER_PACKETS.add(new DisconnectPacket(COMPONENT));
        SERVER_PACKETS.add(new DisplayScoreboardPacket((byte) 5, "scoreboard"));
        SERVER_PACKETS.add(new EffectPacket(5, VEC, 5, false));
        SERVER_PACKETS.add(new EndCombatEventPacket(5));
        SERVER_PACKETS.add(new EnterCombatEventPacket());
        SERVER_PACKETS.add(new EntityAnimationPacket(5, EntityAnimationPacket.Animation.TAKE_DAMAGE));
        SERVER_PACKETS.add(new EntityEquipmentPacket(6, Map.of(EquipmentSlot.MAIN_HAND, ItemStack.of(Material.DIAMOND_SWORD))));
        SERVER_PACKETS.add(new EntityHeadLookPacket(5, 90f));
        SERVER_PACKETS.add(new EntityMetaDataPacket(5, Map.of()));
        SERVER_PACKETS.add(new EntityMetaDataPacket(5, Map.of(1, Metadata.VarInt(5))));
        SERVER_PACKETS.add(new EntityPositionAndRotationPacket(5, (short) 0, (short) 0, (short) 0, 45f, 45f, false));
        SERVER_PACKETS.add(new EntityPositionPacket(5, (short) 0, (short) 0, (short) 0, true));
        SERVER_PACKETS.add(new EntityPropertiesPacket(5, List.of()));
        SERVER_PACKETS.add(new EntityRotationPacket(5, 45f, 45f, false));

        final PlayerSkin skin = new PlayerSkin("hh", "hh");
        List<PlayerInfoUpdatePacket.Property> prop = List.of(new PlayerInfoUpdatePacket.Property("textures", skin.textures(), skin.signature()));

        SERVER_PACKETS.add(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER,
                new PlayerInfoUpdatePacket.Entry(UUID.randomUUID(), "TheMode911", prop, false, 0, GameMode.SURVIVAL, null, null)));
//        SERVER_PACKETS.add(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
//                new PlayerInfoUpdatePacket.Entry(UUID.randomUUID(), "", List.of(), false, 0, GameMode.SURVIVAL, Component.text("NotTheMode911"), null)));
        SERVER_PACKETS.add(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                new PlayerInfoUpdatePacket.Entry(UUID.randomUUID(), "", List.of(), false, 0, GameMode.CREATIVE, null, null)));
        SERVER_PACKETS.add(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                new PlayerInfoUpdatePacket.Entry(UUID.randomUUID(), "", List.of(), false, 20, GameMode.SURVIVAL, null, null)));
        SERVER_PACKETS.add(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                new PlayerInfoUpdatePacket.Entry(UUID.randomUUID(), "", List.of(), true, 0, GameMode.SURVIVAL, null, null)));
        SERVER_PACKETS.add(new PlayerInfoRemovePacket(UUID.randomUUID()));

        //SERVER_PACKETS.add(new MultiBlockChangePacket(5,5,5,true, new long[]{0,5,543534,1321}));
    }

    @BeforeAll
    public static void setupClient() {
        CLIENT_PACKETS.add(new HandshakePacket(755, "localhost", 25565, 2));
    }

    @Test
    public void serverTest() {
        SERVER_PACKETS.forEach(PacketWriteReadTest::testPacket);
    }

    @Test
    public void clientTest() {
        CLIENT_PACKETS.forEach(PacketWriteReadTest::testPacket);
    }

    private static void testPacket(NetworkBuffer.Writer writeable) {
        try {
            byte[] bytes = NetworkBuffer.makeArray(buffer -> buffer.write(writeable));
            var readerConstructor = writeable.getClass().getConstructor(NetworkBuffer.class);

            NetworkBuffer reader = new NetworkBuffer();
            reader.write(NetworkBuffer.RAW_BYTES, bytes);
            var createdPacket = readerConstructor.newInstance(reader);
            assertEquals(writeable, createdPacket);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                 | IllegalAccessException e) {
            fail(writeable.toString(), e);
        }
    }

    private static byte[] generateByteArray(int size) {
        byte[] array = new byte[size];
        ThreadLocalRandom.current().nextBytes(array);
        return array;
    }
}
