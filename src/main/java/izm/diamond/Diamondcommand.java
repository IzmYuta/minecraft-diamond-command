package izm.diamond;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Timer;
import java.util.TimerTask;

public class Diamondcommand implements ModInitializer {
	private static boolean gameRunning = false;
	private static Timer gameTimer;
	private static int remainingSeconds;
	private static ScoreboardObjective diamondObjective;
	private static ScoreboardObjective timerObjective;

	@Override
	public void onInitialize() {
		System.out.println("Diamond Game MOD: Initializing...");

		// コマンド登録
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			System.out.println("Diamond Game MOD: Registering commands...");
			dispatcher.register(CommandManager.literal("game-diamond")
					.then(CommandManager.literal("start")
							.then(CommandManager.argument("minutes", IntegerArgumentType.integer(1))
									.executes(context -> startGame(
											context.getSource(),
											IntegerArgumentType.getInteger(context, "minutes")))))
					.then(CommandManager.literal("stop")
							.executes(context -> stopGame(context.getSource()))));
		});
	}

	private int startGame(ServerCommandSource source, int minutes) {
		if (gameRunning) {
			source.sendFeedback(() -> Text.literal("ゲームは既に実行中です！").formatted(Formatting.RED), false);
			return 0;
		}

		// ゲーム開始
		gameRunning = true;
		remainingSeconds = minutes * 60;

		// スコアボード設定
		setupScoreboards(source);

		// タイマー開始
		startTimer(source);

		// 開始メッセージ
		broadcastGameStart(source, minutes);

		return 1;
	}

	private void setupScoreboards(ServerCommandSource source) {
		Scoreboard scoreboard = source.getServer().getScoreboard();

		// ダイアモンド数のスコアボード
		if (scoreboard.getNullableObjective("diamond_count") != null) {
			scoreboard.removeObjective(scoreboard.getNullableObjective("diamond_count"));
		}
		diamondObjective = scoreboard.addObjective(
				"diamond_count",
				ScoreboardCriterion.DUMMY,
				Text.literal("ダイアモンド数").formatted(Formatting.AQUA),
				ScoreboardCriterion.RenderType.INTEGER,
				true,
				null);
		scoreboard.setObjectiveSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR, diamondObjective);

		// タイマーのスコアボード
		if (scoreboard.getNullableObjective("game_timer") != null) {
			scoreboard.removeObjective(scoreboard.getNullableObjective("game_timer"));
		}
		timerObjective = scoreboard.addObjective(
				"game_timer",
				ScoreboardCriterion.DUMMY,
				Text.literal("残り時間").formatted(Formatting.GOLD),
				ScoreboardCriterion.RenderType.INTEGER,
				true,
				null);

		// 全プレイヤーのスコアを初期化
		for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
			scoreboard.getOrCreateScore(player, diamondObjective).setScore(0);
		}
	}

	private void startTimer(ServerCommandSource source) {
		if (gameTimer != null) {
			gameTimer.cancel();
		}

		gameTimer = new Timer();
		gameTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!gameRunning) {
					this.cancel();
					return;
				}

				remainingSeconds--;
				updateTimerDisplay(source);
				updateScoreboard(source); // スコアボードを定期的に更新

				// 特定の時間でアナウンス
				if (remainingSeconds == 60) { // 1分前
					announceTimeRemaining(source, "残り1分！", Formatting.RED);
				} else if (remainingSeconds == 30) { // 30秒前
					announceTimeRemaining(source, "残り30秒！", Formatting.RED);
				} else if (remainingSeconds == 10) { // 10秒前
					announceTimeRemaining(source, "残り10秒！", Formatting.RED);
				} else if (remainingSeconds <= 5 && remainingSeconds > 0) { // カウントダウン
					announceTimeRemaining(source, String.valueOf(remainingSeconds), Formatting.RED);
				} else if (remainingSeconds <= 0) { // 終了
					source.getServer().execute(() -> stopGame(source));
				}
			}
		}, 0, 1000); // 1秒ごとに実行
	}

	private void updateTimerDisplay(ServerCommandSource source) {
		source.getServer().execute(() -> {
			int minutes = remainingSeconds / 60;
			int seconds = remainingSeconds % 60;

			// アクションバーに表示
			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				player.sendMessage(
						Text.literal("残り時間: ")
								.append(Text.literal(minutes + "分 " + seconds + "秒"))
								.formatted(Formatting.GOLD),
						true);
			}
		});
	}

	private void updateScoreboard(ServerCommandSource source) {
		source.getServer().execute(() -> {
			Scoreboard scoreboard = source.getServer().getScoreboard();

			// ダイアモンドスコアボードを表示
			scoreboard.setObjectiveSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR, diamondObjective);

			// 各プレイヤーの所持ダイアモンド数を更新
			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				int diamondCount = countDiamondsInInventory(player);
				scoreboard.getOrCreateScore(player, diamondObjective).setScore(diamondCount);
			}

			// 残り時間をタイトルに表示
			int minutes = remainingSeconds / 60;
			int seconds = remainingSeconds % 60;
			String timeString = String.format("%d:%02d", minutes, seconds);

			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				player.sendMessage(
						Text.literal("残り時間: " + timeString).formatted(Formatting.GOLD),
						true);
			}
		});
	}

	// プレイヤーのインベントリ内のダイアモンドを数えるメソッド
	private int countDiamondsInInventory(ServerPlayerEntity player) {
		int count = 0;

		// メインインベントリをチェック
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.DIAMOND) {
				count += player.getInventory().getStack(i).getCount();
			}
		}

		return count;
	}

	private void announceTimeRemaining(ServerCommandSource source, String message, Formatting color) {
		source.getServer().execute(() -> {
			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				// タイトル表示
				player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
						Text.literal(message).formatted(color, Formatting.BOLD)));
				player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
						Text.empty()));
				player.networkHandler
						.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 40, 10));
			}
		});
	}

	private void broadcastGameStart(ServerCommandSource source, int minutes) {
		for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
			// タイトル表示
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
					Text.literal("ダイアモンド収集ゲーム開始！").formatted(Formatting.AQUA, Formatting.BOLD)));
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
					Text.literal("制限時間: " + minutes + "分").formatted(Formatting.YELLOW)));
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 70, 20));

			// チャットメッセージ
			player.sendMessage(Text.literal("=========================").formatted(Formatting.GREEN), false);
			player.sendMessage(Text.literal("ダイアモンド収集ゲーム開始！").formatted(Formatting.AQUA, Formatting.BOLD), false);
			player.sendMessage(
					Text.literal("制限時間: ").formatted(Formatting.YELLOW)
							.append(Text.literal(String.valueOf(minutes)).formatted(Formatting.GOLD))
							.append(Text.literal("分").formatted(Formatting.YELLOW)),
					false);
			player.sendMessage(Text.literal("=========================").formatted(Formatting.GREEN), false);
		}
	}

	private int stopGame(ServerCommandSource source) {
		if (!gameRunning) {
			source.sendFeedback(() -> Text.literal("ゲームは実行されていません！").formatted(Formatting.RED), false);
			return 0;
		}

		gameRunning = false;
		if (gameTimer != null) {
			gameTimer.cancel();
			gameTimer = null;
		}

		// 結果発表
		announceResults(source);

		return 1;
	}

	private void announceResults(ServerCommandSource source) {
		Scoreboard scoreboard = source.getServer().getScoreboard();

		for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
			// タイトル表示
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
					Text.literal("ゲーム終了！").formatted(Formatting.GOLD, Formatting.BOLD)));
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
					Text.empty()));
			player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 70, 20));

			// チャットメッセージ
			player.sendMessage(Text.literal("=========================").formatted(Formatting.GREEN), false);
			player.sendMessage(Text.literal("ダイアモンド収集ゲーム終了！").formatted(Formatting.GOLD, Formatting.BOLD), false);
			player.sendMessage(Text.literal("=========================").formatted(Formatting.GREEN), false);

		}

		// 結果表示
		source.getServer().execute(() -> {
			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				player.sendMessage(Text.literal("【最終結果】").formatted(Formatting.YELLOW, Formatting.BOLD), false);
			}

			for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
				int score = scoreboard.getOrCreateScore(
						player,
						diamondObjective).getScore();

				for (ServerPlayerEntity recipient : source.getServer().getPlayerManager().getPlayerList()) {
					recipient.sendMessage(
							Text.literal(player.getName().getString()).formatted(Formatting.AQUA)
									.append(Text.literal(": ").formatted(Formatting.WHITE))
									.append(Text.literal(String.valueOf(score)).formatted(Formatting.GOLD))
									.append(Text.literal("個").formatted(Formatting.WHITE)),
							false);
				}
			}
		});
	}
}