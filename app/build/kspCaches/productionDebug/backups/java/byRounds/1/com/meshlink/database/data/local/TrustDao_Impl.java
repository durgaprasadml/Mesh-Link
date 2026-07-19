package com.meshlink.database.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
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
public final class TrustDao_Impl implements TrustDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrustEntity> __insertionAdapterOfTrustEntity;

  private final EntityDeletionOrUpdateAdapter<TrustEntity> __updateAdapterOfTrustEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTrustScoreAndLevel;

  public TrustDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrustEntity = new EntityInsertionAdapter<TrustEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `trust_table` (`peerId`,`deviceUUID`,`fingerprint`,`firstSeen`,`lastSeen`,`lastIPAddress`,`lastBLEAddress`,`keyVersion`,`trustLevel`,`verificationStatus`,`trustScore`,`identityHistory`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrustEntity entity) {
        statement.bindString(1, entity.getPeerId());
        if (entity.getDeviceUUID() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDeviceUUID());
        }
        if (entity.getFingerprint() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getFingerprint());
        }
        statement.bindLong(4, entity.getFirstSeen());
        statement.bindLong(5, entity.getLastSeen());
        if (entity.getLastIPAddress() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getLastIPAddress());
        }
        if (entity.getLastBLEAddress() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLastBLEAddress());
        }
        statement.bindLong(8, entity.getKeyVersion());
        statement.bindString(9, entity.getTrustLevel());
        statement.bindString(10, entity.getVerificationStatus());
        statement.bindLong(11, entity.getTrustScore());
        statement.bindString(12, entity.getIdentityHistory());
      }
    };
    this.__updateAdapterOfTrustEntity = new EntityDeletionOrUpdateAdapter<TrustEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `trust_table` SET `peerId` = ?,`deviceUUID` = ?,`fingerprint` = ?,`firstSeen` = ?,`lastSeen` = ?,`lastIPAddress` = ?,`lastBLEAddress` = ?,`keyVersion` = ?,`trustLevel` = ?,`verificationStatus` = ?,`trustScore` = ?,`identityHistory` = ? WHERE `peerId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrustEntity entity) {
        statement.bindString(1, entity.getPeerId());
        if (entity.getDeviceUUID() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDeviceUUID());
        }
        if (entity.getFingerprint() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getFingerprint());
        }
        statement.bindLong(4, entity.getFirstSeen());
        statement.bindLong(5, entity.getLastSeen());
        if (entity.getLastIPAddress() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getLastIPAddress());
        }
        if (entity.getLastBLEAddress() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLastBLEAddress());
        }
        statement.bindLong(8, entity.getKeyVersion());
        statement.bindString(9, entity.getTrustLevel());
        statement.bindString(10, entity.getVerificationStatus());
        statement.bindLong(11, entity.getTrustScore());
        statement.bindString(12, entity.getIdentityHistory());
        statement.bindString(13, entity.getPeerId());
      }
    };
    this.__preparedStmtOfUpdateTrustScoreAndLevel = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE trust_table SET trustScore = ?, trustLevel = ? WHERE peerId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdatePeerTrust(final TrustEntity trustEntity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrustEntity.insert(trustEntity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePeerTrust(final TrustEntity trustEntity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTrustEntity.handle(trustEntity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTrustScoreAndLevel(final String peerId, final int score, final String level,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTrustScoreAndLevel.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, score);
        _argIndex = 2;
        _stmt.bindString(_argIndex, level);
        _argIndex = 3;
        _stmt.bindString(_argIndex, peerId);
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
          __preparedStmtOfUpdateTrustScoreAndLevel.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllPeers(final Continuation<? super List<TrustEntity>> $completion) {
    final String _sql = "SELECT * FROM trust_table";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrustEntity>>() {
      @Override
      @NonNull
      public List<TrustEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPeerId = CursorUtil.getColumnIndexOrThrow(_cursor, "peerId");
          final int _cursorIndexOfDeviceUUID = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceUUID");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfLastIPAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastIPAddress");
          final int _cursorIndexOfLastBLEAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastBLEAddress");
          final int _cursorIndexOfKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "keyVersion");
          final int _cursorIndexOfTrustLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "trustLevel");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "trustScore");
          final int _cursorIndexOfIdentityHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "identityHistory");
          final List<TrustEntity> _result = new ArrayList<TrustEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrustEntity _item;
            final String _tmpPeerId;
            _tmpPeerId = _cursor.getString(_cursorIndexOfPeerId);
            final String _tmpDeviceUUID;
            if (_cursor.isNull(_cursorIndexOfDeviceUUID)) {
              _tmpDeviceUUID = null;
            } else {
              _tmpDeviceUUID = _cursor.getString(_cursorIndexOfDeviceUUID);
            }
            final String _tmpFingerprint;
            if (_cursor.isNull(_cursorIndexOfFingerprint)) {
              _tmpFingerprint = null;
            } else {
              _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            }
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpLastIPAddress;
            if (_cursor.isNull(_cursorIndexOfLastIPAddress)) {
              _tmpLastIPAddress = null;
            } else {
              _tmpLastIPAddress = _cursor.getString(_cursorIndexOfLastIPAddress);
            }
            final String _tmpLastBLEAddress;
            if (_cursor.isNull(_cursorIndexOfLastBLEAddress)) {
              _tmpLastBLEAddress = null;
            } else {
              _tmpLastBLEAddress = _cursor.getString(_cursorIndexOfLastBLEAddress);
            }
            final int _tmpKeyVersion;
            _tmpKeyVersion = _cursor.getInt(_cursorIndexOfKeyVersion);
            final String _tmpTrustLevel;
            _tmpTrustLevel = _cursor.getString(_cursorIndexOfTrustLevel);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final int _tmpTrustScore;
            _tmpTrustScore = _cursor.getInt(_cursorIndexOfTrustScore);
            final String _tmpIdentityHistory;
            _tmpIdentityHistory = _cursor.getString(_cursorIndexOfIdentityHistory);
            _item = new TrustEntity(_tmpPeerId,_tmpDeviceUUID,_tmpFingerprint,_tmpFirstSeen,_tmpLastSeen,_tmpLastIPAddress,_tmpLastBLEAddress,_tmpKeyVersion,_tmpTrustLevel,_tmpVerificationStatus,_tmpTrustScore,_tmpIdentityHistory);
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
  public Object getPeerTrust(final String peerId,
      final Continuation<? super TrustEntity> $completion) {
    final String _sql = "SELECT * FROM trust_table WHERE peerId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, peerId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TrustEntity>() {
      @Override
      @Nullable
      public TrustEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPeerId = CursorUtil.getColumnIndexOrThrow(_cursor, "peerId");
          final int _cursorIndexOfDeviceUUID = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceUUID");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfLastIPAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastIPAddress");
          final int _cursorIndexOfLastBLEAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastBLEAddress");
          final int _cursorIndexOfKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "keyVersion");
          final int _cursorIndexOfTrustLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "trustLevel");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "trustScore");
          final int _cursorIndexOfIdentityHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "identityHistory");
          final TrustEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPeerId;
            _tmpPeerId = _cursor.getString(_cursorIndexOfPeerId);
            final String _tmpDeviceUUID;
            if (_cursor.isNull(_cursorIndexOfDeviceUUID)) {
              _tmpDeviceUUID = null;
            } else {
              _tmpDeviceUUID = _cursor.getString(_cursorIndexOfDeviceUUID);
            }
            final String _tmpFingerprint;
            if (_cursor.isNull(_cursorIndexOfFingerprint)) {
              _tmpFingerprint = null;
            } else {
              _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            }
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpLastIPAddress;
            if (_cursor.isNull(_cursorIndexOfLastIPAddress)) {
              _tmpLastIPAddress = null;
            } else {
              _tmpLastIPAddress = _cursor.getString(_cursorIndexOfLastIPAddress);
            }
            final String _tmpLastBLEAddress;
            if (_cursor.isNull(_cursorIndexOfLastBLEAddress)) {
              _tmpLastBLEAddress = null;
            } else {
              _tmpLastBLEAddress = _cursor.getString(_cursorIndexOfLastBLEAddress);
            }
            final int _tmpKeyVersion;
            _tmpKeyVersion = _cursor.getInt(_cursorIndexOfKeyVersion);
            final String _tmpTrustLevel;
            _tmpTrustLevel = _cursor.getString(_cursorIndexOfTrustLevel);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final int _tmpTrustScore;
            _tmpTrustScore = _cursor.getInt(_cursorIndexOfTrustScore);
            final String _tmpIdentityHistory;
            _tmpIdentityHistory = _cursor.getString(_cursorIndexOfIdentityHistory);
            _result = new TrustEntity(_tmpPeerId,_tmpDeviceUUID,_tmpFingerprint,_tmpFirstSeen,_tmpLastSeen,_tmpLastIPAddress,_tmpLastBLEAddress,_tmpKeyVersion,_tmpTrustLevel,_tmpVerificationStatus,_tmpTrustScore,_tmpIdentityHistory);
          } else {
            _result = null;
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
  public Object getPeerByFingerprint(final String fingerprint,
      final Continuation<? super TrustEntity> $completion) {
    final String _sql = "SELECT * FROM trust_table WHERE fingerprint = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fingerprint);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TrustEntity>() {
      @Override
      @Nullable
      public TrustEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPeerId = CursorUtil.getColumnIndexOrThrow(_cursor, "peerId");
          final int _cursorIndexOfDeviceUUID = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceUUID");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfLastIPAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastIPAddress");
          final int _cursorIndexOfLastBLEAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "lastBLEAddress");
          final int _cursorIndexOfKeyVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "keyVersion");
          final int _cursorIndexOfTrustLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "trustLevel");
          final int _cursorIndexOfVerificationStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "verificationStatus");
          final int _cursorIndexOfTrustScore = CursorUtil.getColumnIndexOrThrow(_cursor, "trustScore");
          final int _cursorIndexOfIdentityHistory = CursorUtil.getColumnIndexOrThrow(_cursor, "identityHistory");
          final TrustEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPeerId;
            _tmpPeerId = _cursor.getString(_cursorIndexOfPeerId);
            final String _tmpDeviceUUID;
            if (_cursor.isNull(_cursorIndexOfDeviceUUID)) {
              _tmpDeviceUUID = null;
            } else {
              _tmpDeviceUUID = _cursor.getString(_cursorIndexOfDeviceUUID);
            }
            final String _tmpFingerprint;
            if (_cursor.isNull(_cursorIndexOfFingerprint)) {
              _tmpFingerprint = null;
            } else {
              _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            }
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final String _tmpLastIPAddress;
            if (_cursor.isNull(_cursorIndexOfLastIPAddress)) {
              _tmpLastIPAddress = null;
            } else {
              _tmpLastIPAddress = _cursor.getString(_cursorIndexOfLastIPAddress);
            }
            final String _tmpLastBLEAddress;
            if (_cursor.isNull(_cursorIndexOfLastBLEAddress)) {
              _tmpLastBLEAddress = null;
            } else {
              _tmpLastBLEAddress = _cursor.getString(_cursorIndexOfLastBLEAddress);
            }
            final int _tmpKeyVersion;
            _tmpKeyVersion = _cursor.getInt(_cursorIndexOfKeyVersion);
            final String _tmpTrustLevel;
            _tmpTrustLevel = _cursor.getString(_cursorIndexOfTrustLevel);
            final String _tmpVerificationStatus;
            _tmpVerificationStatus = _cursor.getString(_cursorIndexOfVerificationStatus);
            final int _tmpTrustScore;
            _tmpTrustScore = _cursor.getInt(_cursorIndexOfTrustScore);
            final String _tmpIdentityHistory;
            _tmpIdentityHistory = _cursor.getString(_cursorIndexOfIdentityHistory);
            _result = new TrustEntity(_tmpPeerId,_tmpDeviceUUID,_tmpFingerprint,_tmpFirstSeen,_tmpLastSeen,_tmpLastIPAddress,_tmpLastBLEAddress,_tmpKeyVersion,_tmpTrustLevel,_tmpVerificationStatus,_tmpTrustScore,_tmpIdentityHistory);
          } else {
            _result = null;
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
