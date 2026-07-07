package com.meshlink.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RelayDao_Impl implements RelayDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RelayPacketEntity> __insertionAdapterOfRelayPacketEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeletePacket;

  private final SharedSQLiteStatement __preparedStmtOfDeleteExpiredPackets;

  public RelayDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRelayPacketEntity = new EntityInsertionAdapter<RelayPacketEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `relay_packets` (`packetId`,`senderId`,`targetId`,`payload`,`type`,`timestamp`,`expiryTimestamp`,`ttl`,`hopCount`,`encrypted`,`transferId`,`chunkIndex`,`totalChunks`,`mimeType`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RelayPacketEntity entity) {
        statement.bindString(1, entity.getPacketId());
        statement.bindString(2, entity.getSenderId());
        statement.bindString(3, entity.getTargetId());
        statement.bindString(4, entity.getPayload());
        statement.bindString(5, entity.getType());
        statement.bindLong(6, entity.getTimestamp());
        statement.bindLong(7, entity.getExpiryTimestamp());
        statement.bindLong(8, entity.getTtl());
        statement.bindLong(9, entity.getHopCount());
        final int _tmp = entity.getEncrypted() ? 1 : 0;
        statement.bindLong(10, _tmp);
        if (entity.getTransferId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getTransferId());
        }
        statement.bindLong(12, entity.getChunkIndex());
        statement.bindLong(13, entity.getTotalChunks());
        if (entity.getMimeType() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getMimeType());
        }
      }
    };
    this.__preparedStmtOfDeletePacket = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM relay_packets WHERE packetId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteExpiredPackets = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM relay_packets WHERE expiryTimestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPacket(final RelayPacketEntity packet,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRelayPacketEntity.insert(packet);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePacket(final String packetId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePacket.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, packetId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePacket.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteExpiredPackets(final long now, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteExpiredPackets.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, now);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteExpiredPackets.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPacketsForTarget(final String targetId,
      final Continuation<? super List<RelayPacketEntity>> $completion) {
    final String _sql = "SELECT * FROM relay_packets WHERE targetId = ? OR targetId = 'BROADCAST'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, targetId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RelayPacketEntity>>() {
      @Override
      @NonNull
      public List<RelayPacketEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPacketId = CursorUtil.getColumnIndexOrThrow(_cursor, "packetId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfExpiryTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryTimestamp");
          final int _cursorIndexOfTtl = CursorUtil.getColumnIndexOrThrow(_cursor, "ttl");
          final int _cursorIndexOfHopCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hopCount");
          final int _cursorIndexOfEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "encrypted");
          final int _cursorIndexOfTransferId = CursorUtil.getColumnIndexOrThrow(_cursor, "transferId");
          final int _cursorIndexOfChunkIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "chunkIndex");
          final int _cursorIndexOfTotalChunks = CursorUtil.getColumnIndexOrThrow(_cursor, "totalChunks");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final List<RelayPacketEntity> _result = new ArrayList<RelayPacketEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RelayPacketEntity _item;
            final String _tmpPacketId;
            _tmpPacketId = _cursor.getString(_cursorIndexOfPacketId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpTargetId;
            _tmpTargetId = _cursor.getString(_cursorIndexOfTargetId);
            final String _tmpPayload;
            _tmpPayload = _cursor.getString(_cursorIndexOfPayload);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpExpiryTimestamp;
            _tmpExpiryTimestamp = _cursor.getLong(_cursorIndexOfExpiryTimestamp);
            final int _tmpTtl;
            _tmpTtl = _cursor.getInt(_cursorIndexOfTtl);
            final int _tmpHopCount;
            _tmpHopCount = _cursor.getInt(_cursorIndexOfHopCount);
            final boolean _tmpEncrypted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEncrypted);
            _tmpEncrypted = _tmp != 0;
            final String _tmpTransferId;
            if (_cursor.isNull(_cursorIndexOfTransferId)) {
              _tmpTransferId = null;
            } else {
              _tmpTransferId = _cursor.getString(_cursorIndexOfTransferId);
            }
            final int _tmpChunkIndex;
            _tmpChunkIndex = _cursor.getInt(_cursorIndexOfChunkIndex);
            final int _tmpTotalChunks;
            _tmpTotalChunks = _cursor.getInt(_cursorIndexOfTotalChunks);
            final String _tmpMimeType;
            if (_cursor.isNull(_cursorIndexOfMimeType)) {
              _tmpMimeType = null;
            } else {
              _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            }
            _item = new RelayPacketEntity(_tmpPacketId,_tmpSenderId,_tmpTargetId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpExpiryTimestamp,_tmpTtl,_tmpHopCount,_tmpEncrypted,_tmpTransferId,_tmpChunkIndex,_tmpTotalChunks,_tmpMimeType);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllRelayPackets(
      final Continuation<? super List<RelayPacketEntity>> $completion) {
    final String _sql = "SELECT * FROM relay_packets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RelayPacketEntity>>() {
      @Override
      @NonNull
      public List<RelayPacketEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPacketId = CursorUtil.getColumnIndexOrThrow(_cursor, "packetId");
          final int _cursorIndexOfSenderId = CursorUtil.getColumnIndexOrThrow(_cursor, "senderId");
          final int _cursorIndexOfTargetId = CursorUtil.getColumnIndexOrThrow(_cursor, "targetId");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfExpiryTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "expiryTimestamp");
          final int _cursorIndexOfTtl = CursorUtil.getColumnIndexOrThrow(_cursor, "ttl");
          final int _cursorIndexOfHopCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hopCount");
          final int _cursorIndexOfEncrypted = CursorUtil.getColumnIndexOrThrow(_cursor, "encrypted");
          final int _cursorIndexOfTransferId = CursorUtil.getColumnIndexOrThrow(_cursor, "transferId");
          final int _cursorIndexOfChunkIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "chunkIndex");
          final int _cursorIndexOfTotalChunks = CursorUtil.getColumnIndexOrThrow(_cursor, "totalChunks");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final List<RelayPacketEntity> _result = new ArrayList<RelayPacketEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RelayPacketEntity _item;
            final String _tmpPacketId;
            _tmpPacketId = _cursor.getString(_cursorIndexOfPacketId);
            final String _tmpSenderId;
            _tmpSenderId = _cursor.getString(_cursorIndexOfSenderId);
            final String _tmpTargetId;
            _tmpTargetId = _cursor.getString(_cursorIndexOfTargetId);
            final String _tmpPayload;
            _tmpPayload = _cursor.getString(_cursorIndexOfPayload);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpExpiryTimestamp;
            _tmpExpiryTimestamp = _cursor.getLong(_cursorIndexOfExpiryTimestamp);
            final int _tmpTtl;
            _tmpTtl = _cursor.getInt(_cursorIndexOfTtl);
            final int _tmpHopCount;
            _tmpHopCount = _cursor.getInt(_cursorIndexOfHopCount);
            final boolean _tmpEncrypted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEncrypted);
            _tmpEncrypted = _tmp != 0;
            final String _tmpTransferId;
            if (_cursor.isNull(_cursorIndexOfTransferId)) {
              _tmpTransferId = null;
            } else {
              _tmpTransferId = _cursor.getString(_cursorIndexOfTransferId);
            }
            final int _tmpChunkIndex;
            _tmpChunkIndex = _cursor.getInt(_cursorIndexOfChunkIndex);
            final int _tmpTotalChunks;
            _tmpTotalChunks = _cursor.getInt(_cursorIndexOfTotalChunks);
            final String _tmpMimeType;
            if (_cursor.isNull(_cursorIndexOfMimeType)) {
              _tmpMimeType = null;
            } else {
              _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            }
            _item = new RelayPacketEntity(_tmpPacketId,_tmpSenderId,_tmpTargetId,_tmpPayload,_tmpType,_tmpTimestamp,_tmpExpiryTimestamp,_tmpTtl,_tmpHopCount,_tmpEncrypted,_tmpTransferId,_tmpChunkIndex,_tmpTotalChunks,_tmpMimeType);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
