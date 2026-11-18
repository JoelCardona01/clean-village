package dev.spotless.mixin;

import dev.spotless.command.DebugCommands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void spotless$register(Commands.CommandSelection selection, CommandBuildContext ctx, CallbackInfo ci) {
        DebugCommands.register(((Commands) (Object) this).getDispatcher());
    }
}
