package com.meshlink.service.work;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.meshlink.database.data.local.AuditLogDao;
import com.meshlink.database.data.local.RelayDao;
import com.meshlink.storage.data.local.CacheManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class CleanupWorker_Factory {
  private final Provider<CacheManager> cacheManagerProvider;

  private final Provider<RelayDao> relayDaoProvider;

  private final Provider<AuditLogDao> auditLogDaoProvider;

  public CleanupWorker_Factory(Provider<CacheManager> cacheManagerProvider,
      Provider<RelayDao> relayDaoProvider, Provider<AuditLogDao> auditLogDaoProvider) {
    this.cacheManagerProvider = cacheManagerProvider;
    this.relayDaoProvider = relayDaoProvider;
    this.auditLogDaoProvider = auditLogDaoProvider;
  }

  public CleanupWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, cacheManagerProvider.get(), relayDaoProvider.get(), auditLogDaoProvider.get());
  }

  public static CleanupWorker_Factory create(Provider<CacheManager> cacheManagerProvider,
      Provider<RelayDao> relayDaoProvider, Provider<AuditLogDao> auditLogDaoProvider) {
    return new CleanupWorker_Factory(cacheManagerProvider, relayDaoProvider, auditLogDaoProvider);
  }

  public static CleanupWorker newInstance(Context context, WorkerParameters workerParams,
      CacheManager cacheManager, RelayDao relayDao, AuditLogDao auditLogDao) {
    return new CleanupWorker(context, workerParams, cacheManager, relayDao, auditLogDao);
  }
}
