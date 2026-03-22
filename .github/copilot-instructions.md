# Copilot Instructions - XP Abilities MOD

## プロジェクト概要
NeoForge 1.21.8向けのMinecraft MOD。経験値ポイントを消費してアビリティを習得するシステム。

## 技術スタック
- Java 21, NeoForge 21.8.52, ModDevGradle 2.0
- Gradle 8.12

## NeoForge 1.21.8 API注意点（重要）
- `INBTSerializable` は存在しない → Codec方式を使用
- `EventBusSubscriber` に `bus` パラメータは不要
- `PacketDistributor.sendToServer()` は存在しない → `Minecraft.getInstance().getConnection().send()` を使用
- `AttachmentType.serialize()` は `MapCodec` のみ受付 → `codec.fieldOf("key")` で変換
- `FoodData.setExhaustion()` / `getExhaustionLevel()` は存在しない → `MobEffects` を使用

## コーディング規約
- コメントは日本語で記述
- パッケージ構造: `com.fumih.xpabilities`
- 既存のパターンに従う（Ability enum, PlayerAbilityData, Event handler分離）

## ビルド方法
```bash
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
gradlew.bat build
```
