package de.miraculixx.nbtviewer.client

import com.mojang.blaze3d.platform.InputConstants
import de.miraculixx.nbtviewer.client.utils.isKeyPressed
import de.miraculixx.nbtviewer.config.NBTViewerAutoConfig
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.nbt.*
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.silkmc.silk.core.text.literalText
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt


class NBTViewer : ClientModInitializer {
    companion object {
        lateinit var keyBindShowInfo: KeyMapping
        lateinit var keyBindShowSettings: KeyMapping
        lateinit var config: NBTViewerAutoConfig
    }

    private val colorLine = 0x161616

    override fun onInitializeClient() {
        AutoConfig.register(NBTViewerAutoConfig::class.java) { definition, configClass -> GsonConfigSerializer(definition, configClass) }
        config = AutoConfig.getConfigHolder(NBTViewerAutoConfig::class.java).config

        keyBindShowInfo = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.nbt-viewer.view",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.nbt-viewer.main"
            )
        )
        keyBindShowSettings = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.nbt-viewer.settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.nbt-viewer.main"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client: Minecraft ->
            while (keyBindShowSettings.consumeClick()) {
                client.setScreen(AutoConfig.getConfigScreen(NBTViewerAutoConfig::class.java, client.screen).get())
            }
        }

        ItemTooltipCallback.EVENT.register { stack, tooltipContext, tooltipType, lines ->
            if (isKeyPressed(keyBindShowInfo)) {
                println(config)
                val excluded = config.exclude.split(',')

                stack.componentsPatch.split()
                val list: MutableList<Component> = mutableListOf()
                stack.components.forEach {
                    val key = it.type.toString().removePrefix("minecraft:")
                    if (excluded.contains(key)) return@forEach
                    val firstLine = literalText("▪ $key") { color = Color.GRAY.rgb }
                        .append(literalText(" -> ") { color = Color.GRAY.rgb })
                    val dataList = it.value.stringify()
                    firstLine.append(dataList.first())
                    list.add(firstLine)
                    if (dataList.size > 1) {
                        list.addAll(dataList.subList(1, dataList.size))
                    }
                }
                val first = lines.first()
                lines.clear()
                lines.addAll(listOf(first, literalText()))
                lines.addAll(list)
            }
        }
    }

    private fun Any.stringify(): List<Component> {
        return when (this) {
            is Int -> listOf(literalText(this.toString()) { color = Color.CYAN.rgb })
            is CustomModelData -> listOf(literalText(value.toString()) { color = Color.CYAN.rgb })
            is Enum<*> -> listOf(literalText(this.toString()) { color = Color.YELLOW.rgb })
            is Component -> listOf(literalText("\"") { color = Color.WHITE.rgb }.append(this).append(literalText("\"") { color = Color.WHITE.rgb }))
            is ItemAttributeModifiers -> {
                buildList {
                    add(literalText(if (this@stringify.showInTooltip) "Visible" else "Invisible") { color = Color.LIGHT_GRAY.rgb })
                    this@stringify.modifiers.forEach { modifier ->
                        val change = modifier.modifier
                        val amount = (change.amount * 100).roundToInt() / 100.0
                        val operatorChar = when (change.operation.id()) {
                            0 -> if (amount < 0) "" else "+"
                            1 -> "*"
                            2 -> "**"
                            else -> "?"
                        }
                        add(literalText("   $operatorChar$amount ${change.id.path}") { color = 0xFC54FC }
                            .append(literalText(" (${modifier.slot.name})") { color = Color.LIGHT_GRAY.rgb }))
                    }
                }
            }

            is CustomData -> {
                val compoundTag = copyTag()
                compoundTag.stringifyCompoundTag(1)
            }

            else -> listOf(literalText(this.toString() + " (${this::class.java.simpleName})") { color = Color.LIGHT_GRAY.rgb })
        }
    }

    private fun CompoundTag.stringifyCompoundTag(depth: Int): List<Component> {
        return buildList {
            add(literalText(" ") { color = Color.GRAY.rgb })
            val space = literalText("|  ".repeat(depth)) { color = colorLine }
            allKeys.forEach { key ->
                val value = get(key) ?: return@forEach
                val firstLine = space.copy().append(literalText("• $key -> ") { color = Color.GRAY.rgb })
                val display = if (value is CompoundTag) {
                    value.stringifyCompoundTag(depth + 1)
                } else value.stringifyTag(depth + 1)
                add(firstLine.append(display.first()))
                addAll(display.subList(1, display.size))
            }
        }
    }

    private fun Tag.stringifyTag(depth: Int): List<Component> {
        return when (this) {
            is NumericTag -> listOf(literalText(this.toString()) { color = Color.CYAN.rgb })
            is StringTag -> listOf(literalText("\"${asString}\"") { color = 0x54FB54 })
            is CollectionTag<*> -> {
                buildList {
                    add(literalText(" "))
                    val space = literalText("|  ".repeat(depth - 1) + "   ") { color = colorLine }
                    forEachIndexed { index: Int, tag: Tag? ->
                        add(space.copy().append(literalText("[$index]: ") { color = Color.GRAY.rgb }.append(tag?.stringifyTag(depth + 1)?.first() ?: literalText("null"))))
                    }
                }
            }

            else -> listOf(literalText(this.toString() + " (${this::class.java.simpleName})") { color = Color.LIGHT_GRAY.rgb })
        }
    }
}