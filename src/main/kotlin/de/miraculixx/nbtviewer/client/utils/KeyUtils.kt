package de.miraculixx.nbtviewer.client.utils

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft

fun isKeyPressed(keyMapping: KeyMapping) =
    InputConstants.isKeyDown(Minecraft.getInstance().window.window, KeyBindingHelper.getBoundKeyOf(keyMapping).value)
