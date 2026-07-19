package com.meshlink.database.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MeshDatabase_Impl extends MeshDatabase {
  private volatile UserDao _userDao;

  private volatile ChatDao _chatDao;

  private volatile RelayDao _relayDao;

  private volatile TrustDao _trustDao;

  private volatile AuditLogDao _auditLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(8) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`meshId` TEXT NOT NULL, `name` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `pinHash` TEXT NOT NULL, PRIMARY KEY(`meshId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `lastMessage` TEXT, `lastMessageAt` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chats_lastMessageAt` ON `chats` (`lastMessageAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `messageId` TEXT NOT NULL, `chatId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isFromMe` INTEGER NOT NULL, `status` TEXT NOT NULL, `messageType` TEXT NOT NULL, `mediaPath` TEXT, `mediaDurationMs` INTEGER, `latitude` REAL, `longitude` REAL, `batteryPercent` INTEGER)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_messageId` ON `messages` (`messageId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestamp` ON `messages` (`chatId`, `timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_status` ON `messages` (`status`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `relay_packets` (`packetId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `targetId` TEXT NOT NULL, `payload` TEXT NOT NULL, `type` TEXT NOT NULL, `priority` TEXT NOT NULL, `broadcastType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `expiryTimestamp` INTEGER NOT NULL, `ttl` INTEGER NOT NULL, `hopCount` INTEGER NOT NULL, `encrypted` INTEGER NOT NULL, `transferId` TEXT, `chunkIndex` INTEGER NOT NULL, `totalChunks` INTEGER NOT NULL, `mimeType` TEXT, PRIMARY KEY(`packetId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relay_packets_targetId` ON `relay_packets` (`targetId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_relay_packets_expiryTimestamp` ON `relay_packets` (`expiryTimestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `trust_table` (`peerId` TEXT NOT NULL, `deviceUUID` TEXT, `fingerprint` TEXT, `firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `lastIPAddress` TEXT, `lastBLEAddress` TEXT, `keyVersion` INTEGER NOT NULL, `trustLevel` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `trustScore` INTEGER NOT NULL, `identityHistory` TEXT NOT NULL, PRIMARY KEY(`peerId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_trust_table_fingerprint` ON `trust_table` (`fingerprint`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_log_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `peerId` TEXT NOT NULL, `eventName` TEXT NOT NULL, `severity` INTEGER NOT NULL, `details` TEXT NOT NULL, `actionTaken` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3033b1c4293cd57ea8f82356200c799a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `chats`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `relay_packets`");
        db.execSQL("DROP TABLE IF EXISTS `trust_table`");
        db.execSQL("DROP TABLE IF EXISTS `audit_log_table`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(4);
        _columnsUsers.put("meshId", new TableInfo.Column("meshId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("pinHash", new TableInfo.Column("pinHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.meshlink.database.data.local.UserEntity).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsChats = new HashMap<String, TableInfo.Column>(5);
        _columnsChats.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("lastMessage", new TableInfo.Column("lastMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("lastMessageAt", new TableInfo.Column("lastMessageAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("unreadCount", new TableInfo.Column("unreadCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChats = new HashSet<TableInfo.Index>(1);
        _indicesChats.add(new TableInfo.Index("index_chats_lastMessageAt", false, Arrays.asList("lastMessageAt"), Arrays.asList("ASC")));
        final TableInfo _infoChats = new TableInfo("chats", _columnsChats, _foreignKeysChats, _indicesChats);
        final TableInfo _existingChats = TableInfo.read(db, "chats");
        if (!_infoChats.equals(_existingChats)) {
          return new RoomOpenHelper.ValidationResult(false, "chats(com.meshlink.database.data.local.ChatEntity).\n"
                  + " Expected:\n" + _infoChats + "\n"
                  + " Found:\n" + _existingChats);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(14);
        _columnsMessages.put("localId", new TableInfo.Column("localId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("messageId", new TableInfo.Column("messageId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("chatId", new TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("isFromMe", new TableInfo.Column("isFromMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("messageType", new TableInfo.Column("messageType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mediaPath", new TableInfo.Column("mediaPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("mediaDurationMs", new TableInfo.Column("mediaDurationMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("batteryPercent", new TableInfo.Column("batteryPercent", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(3);
        _indicesMessages.add(new TableInfo.Index("index_messages_messageId", true, Arrays.asList("messageId"), Arrays.asList("ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_chatId_timestamp", false, Arrays.asList("chatId", "timestamp"), Arrays.asList("ASC", "ASC")));
        _indicesMessages.add(new TableInfo.Index("index_messages_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.meshlink.database.data.local.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsRelayPackets = new HashMap<String, TableInfo.Column>(16);
        _columnsRelayPackets.put("packetId", new TableInfo.Column("packetId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("senderId", new TableInfo.Column("senderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("targetId", new TableInfo.Column("targetId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("payload", new TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("priority", new TableInfo.Column("priority", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("broadcastType", new TableInfo.Column("broadcastType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("expiryTimestamp", new TableInfo.Column("expiryTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("ttl", new TableInfo.Column("ttl", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("hopCount", new TableInfo.Column("hopCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("encrypted", new TableInfo.Column("encrypted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("transferId", new TableInfo.Column("transferId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("chunkIndex", new TableInfo.Column("chunkIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("totalChunks", new TableInfo.Column("totalChunks", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRelayPackets.put("mimeType", new TableInfo.Column("mimeType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRelayPackets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRelayPackets = new HashSet<TableInfo.Index>(2);
        _indicesRelayPackets.add(new TableInfo.Index("index_relay_packets_targetId", false, Arrays.asList("targetId"), Arrays.asList("ASC")));
        _indicesRelayPackets.add(new TableInfo.Index("index_relay_packets_expiryTimestamp", false, Arrays.asList("expiryTimestamp"), Arrays.asList("ASC")));
        final TableInfo _infoRelayPackets = new TableInfo("relay_packets", _columnsRelayPackets, _foreignKeysRelayPackets, _indicesRelayPackets);
        final TableInfo _existingRelayPackets = TableInfo.read(db, "relay_packets");
        if (!_infoRelayPackets.equals(_existingRelayPackets)) {
          return new RoomOpenHelper.ValidationResult(false, "relay_packets(com.meshlink.database.data.local.RelayPacketEntity).\n"
                  + " Expected:\n" + _infoRelayPackets + "\n"
                  + " Found:\n" + _existingRelayPackets);
        }
        final HashMap<String, TableInfo.Column> _columnsTrustTable = new HashMap<String, TableInfo.Column>(12);
        _columnsTrustTable.put("peerId", new TableInfo.Column("peerId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("deviceUUID", new TableInfo.Column("deviceUUID", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("fingerprint", new TableInfo.Column("fingerprint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("firstSeen", new TableInfo.Column("firstSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("lastSeen", new TableInfo.Column("lastSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("lastIPAddress", new TableInfo.Column("lastIPAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("lastBLEAddress", new TableInfo.Column("lastBLEAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("keyVersion", new TableInfo.Column("keyVersion", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("trustLevel", new TableInfo.Column("trustLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("verificationStatus", new TableInfo.Column("verificationStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("trustScore", new TableInfo.Column("trustScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrustTable.put("identityHistory", new TableInfo.Column("identityHistory", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrustTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrustTable = new HashSet<TableInfo.Index>(1);
        _indicesTrustTable.add(new TableInfo.Index("index_trust_table_fingerprint", false, Arrays.asList("fingerprint"), Arrays.asList("ASC")));
        final TableInfo _infoTrustTable = new TableInfo("trust_table", _columnsTrustTable, _foreignKeysTrustTable, _indicesTrustTable);
        final TableInfo _existingTrustTable = TableInfo.read(db, "trust_table");
        if (!_infoTrustTable.equals(_existingTrustTable)) {
          return new RoomOpenHelper.ValidationResult(false, "trust_table(com.meshlink.database.data.local.TrustEntity).\n"
                  + " Expected:\n" + _infoTrustTable + "\n"
                  + " Found:\n" + _existingTrustTable);
        }
        final HashMap<String, TableInfo.Column> _columnsAuditLogTable = new HashMap<String, TableInfo.Column>(7);
        _columnsAuditLogTable.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("peerId", new TableInfo.Column("peerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("eventName", new TableInfo.Column("eventName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("severity", new TableInfo.Column("severity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("details", new TableInfo.Column("details", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAuditLogTable.put("actionTaken", new TableInfo.Column("actionTaken", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAuditLogTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAuditLogTable = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAuditLogTable = new TableInfo("audit_log_table", _columnsAuditLogTable, _foreignKeysAuditLogTable, _indicesAuditLogTable);
        final TableInfo _existingAuditLogTable = TableInfo.read(db, "audit_log_table");
        if (!_infoAuditLogTable.equals(_existingAuditLogTable)) {
          return new RoomOpenHelper.ValidationResult(false, "audit_log_table(com.meshlink.database.data.local.AuditLogEntity).\n"
                  + " Expected:\n" + _infoAuditLogTable + "\n"
                  + " Found:\n" + _existingAuditLogTable);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3033b1c4293cd57ea8f82356200c799a", "dca51c68c4459226bdeb56bb00ad6562");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","chats","messages","relay_packets","trust_table","audit_log_table");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `chats`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `relay_packets`");
      _db.execSQL("DELETE FROM `trust_table`");
      _db.execSQL("DELETE FROM `audit_log_table`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChatDao.class, ChatDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RelayDao.class, RelayDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrustDao.class, TrustDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AuditLogDao.class, AuditLogDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao getUserDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public ChatDao getChatDao() {
    if (_chatDao != null) {
      return _chatDao;
    } else {
      synchronized(this) {
        if(_chatDao == null) {
          _chatDao = new ChatDao_Impl(this);
        }
        return _chatDao;
      }
    }
  }

  @Override
  public RelayDao getRelayDao() {
    if (_relayDao != null) {
      return _relayDao;
    } else {
      synchronized(this) {
        if(_relayDao == null) {
          _relayDao = new RelayDao_Impl(this);
        }
        return _relayDao;
      }
    }
  }

  @Override
  public TrustDao getTrustDao() {
    if (_trustDao != null) {
      return _trustDao;
    } else {
      synchronized(this) {
        if(_trustDao == null) {
          _trustDao = new TrustDao_Impl(this);
        }
        return _trustDao;
      }
    }
  }

  @Override
  public AuditLogDao getAuditLogDao() {
    if (_auditLogDao != null) {
      return _auditLogDao;
    } else {
      synchronized(this) {
        if(_auditLogDao == null) {
          _auditLogDao = new AuditLogDao_Impl(this);
        }
        return _auditLogDao;
      }
    }
  }
}
