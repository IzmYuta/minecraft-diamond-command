# ダイアモンド収集ゲームMOD

## 概要

このMODは、Minecraft内でダイアモンド収集ゲームを実施するためのものです。プレイヤーは制限時間内にできるだけ多くのダイアモンドを集めることを競います。

## 機能

- `/game-diamond start <分数>` コマンドでゲームを開始
- `/game-diamond stop` コマンドでゲームを停止
- スコアボードによるリアルタイムのダイアモンド数表示
- 残り時間のアクションバー表示
- 残り時間アナウンス（1分前、30秒前、10秒前、カウントダウン）
- ゲーム終了時の結果発表

## 使い方

1. サーバーにMODをインストール
2. `/game-diamond start <分数>` コマンドを実行してゲームを開始
3. 制限時間内にできるだけ多くのダイアモンドを集める
4. 時間切れになるか、`/game-diamond stop` コマンドでゲームを終了
5. 結果が表示され、最も多くのダイアモンドを集めたプレイヤーが勝者

## 技術的詳細

- Fabric API を使用
- スコアボードシステムでプレイヤーのダイアモンド所持数を追跡
- タイマー機能で制限時間を管理
- タイトル表示とチャットメッセージでゲーム状況を通知

## 開発環境

- Minecraft 1.20.x
- Fabric Loader
- Java 21以上推奨

## 注意事項

- サーバー管理者権限を持つプレイヤーのみがゲームを開始・停止できます
- ゲーム中はプレイヤーのインベントリ内のダイアモンドのみがカウントされます

## ローカルで検証する場合

```bash
./gradlew runClient
```