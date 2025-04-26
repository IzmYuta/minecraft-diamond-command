package izm.diamond.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class ExampleMixin {
	@Inject(method = "onBreak", at = @At("HEAD"))
	private void onBlockBreak(World world, BlockPos pos, BlockState state, PlayerEntity player,
			CallbackInfoReturnable<Boolean> cir) {
		if (!world.isClient && player instanceof ServerPlayerEntity) {
			// ダイアモンド鉱石またはディープスレート・ダイアモンド鉱石が破壊された場合
			if (state.getBlock() == Blocks.DIAMOND_ORE || state.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE) {
				ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
				Scoreboard scoreboard = serverPlayer.getServer().getScoreboard();
				ScoreboardObjective objective = scoreboard.getNullableObjective("diamond_count");

				if (objective != null) {
					// スコアを直接更新（統計に依存せず即時反映）
					int currentScore = scoreboard.getOrCreateScore(serverPlayer, objective)
							.getScore();
					scoreboard.getOrCreateScore(serverPlayer, objective).setScore(currentScore + 1);
				}
			}
		}
	}
}