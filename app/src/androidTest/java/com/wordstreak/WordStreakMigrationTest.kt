package com.wordstreak

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wordstreak.data.local.WordStreakDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 真实 v1→v2 迁移测试（Android Instrumented Test / MigrationTestHelper）。
 * 在标准设备或 CI（connectedAndroidTest）环境运行：
 * - createDatabase(name, 1) 按 schemas/1.json 构造 v1 库并写入测试数据；
 * - runMigrationsAndValidate(name, 2, true, MIGRATION_1_2) 执行迁移并**按 schemas/2.json 严格校验结构**；
 * - 断言旧词库/单词/会话/学习记录/复习/打卡/每日统计均保留，快照回填，外键 SET NULL 生效。
 */
@RunWith(AndroidJUnit4::class)
class WordStreakMigrationTest {

    private val testDb = "migration-test.db"

    @Test
    fun migrate1To2_preservesDataAndSchema() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WordStreakDatabase::class.java
        )

        // 1) 构造 v1 结构并写入测试数据
        helper.createDatabase(testDb, 1).use { db ->
            db.execSQL("INSERT INTO `word_books` VALUES (1,'旧词库','desc',100,100)")
            db.execSQL("INSERT INTO `words` VALUES (1,1,'apple',NULL,'苹果',NULL,NULL,NULL,0,100,100)")
            db.execSQL("INSERT INTO `words` VALUES (2,1,'banana',NULL,'香蕉',NULL,NULL,NULL,0,100,100)")
            db.execSQL("INSERT INTO `study_sessions` VALUES (1,'2026-05-01',1000,2000,2,0,1)")
            db.execSQL("INSERT INTO `study_records` VALUES (1,1,1,'GOOD',1,1100)")
            db.execSQL("INSERT INTO `study_records` VALUES (2,1,2,'AGAIN',0,1200)")
            db.execSQL("INSERT INTO `review_states` VALUES (1,'2026-05-02',NULL,NULL,3,0,1)")
            db.execSQL("INSERT INTO `daily_stats` VALUES ('2026-05-01',2,0,1,1,120)")
            db.execSQL("INSERT INTO `check_ins` VALUES ('2026-05-01',1,10,10,123)")
            db.execSQL("INSERT INTO `user_settings` VALUES ('daily_goal','10')")
        }

        // 2) 迁移并按 2.json 严格校验结构
        val db = helper.runMigrationsAndValidate(
            testDb, 2, true, WordStreakDatabase.MIGRATION_1_2
        )
        try {
            // 原始迁移测试连接默认关闭外键；显式开启以验证 ON DELETE SET NULL（Room 运行时会自动开启）
            db.execSQL("PRAGMA foreign_keys=ON")
            // 数据保留
            db.query("SELECT COUNT(*) FROM word_books").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM words").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM study_records").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM review_states").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM daily_stats").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM check_ins").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT COUNT(*) FROM user_settings").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }

            // 快照回填
            db.query("SELECT wordSpelling, wordMeaning FROM study_records WHERE id=2").use { c ->
                c.moveToFirst()
                assertEquals("banana", c.getString(0))
                assertEquals("香蕉", c.getString(1))
            }

            // 表结构：wordId 可空 + 快照列存在
            var wordIdNullable = false; var hasSpell = false; var hasMeaning = false
            db.query("PRAGMA table_info(`study_records`)").use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndex("name"))
                    val notnull = c.getInt(c.getColumnIndex("notnull"))
                    if (name == "wordId" && notnull == 0) wordIdNullable = true
                    if (name == "wordSpelling") hasSpell = true
                    if (name == "wordMeaning") hasMeaning = true
                }
            }
            assertTrue(wordIdNullable); assertTrue(hasSpell); assertTrue(hasMeaning)

            // 外键：wordId ON DELETE SET NULL
            var fkSetNull = false
            db.query("PRAGMA foreign_key_list(`study_records`)").use { c ->
                while (c.moveToNext()) {
                    val from = c.getString(c.getColumnIndex("from"))
                    val onDel = c.getString(c.getColumnIndex("on_delete"))
                    if (from == "wordId" && onDel.equals("SET NULL", ignoreCase = true)) fkSetNull = true
                }
            }
            assertTrue(fkSetNull)

            // 删除单词后历史记录保留 + 外键置空
            db.execSQL("DELETE FROM words WHERE id=2")
            db.query("SELECT COUNT(*) FROM study_records WHERE id=2").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.query("SELECT wordId, wordSpelling FROM study_records WHERE id=2").use { c ->
                c.moveToFirst()
                assertTrue(c.isNull(0))
                assertEquals("banana", c.getString(1))
            }
        } finally {
            db.close()
        }
    }
}
