package com.meshlink.security.data;

import com.meshlink.database.data.local.AuditLogDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.meshlink.di.IoDispatcher")
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
public final class MeshSecurityMonitor_Factory implements Factory<MeshSecurityMonitor> {
  private final Provider<AuditLogDao> auditLogDaoProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public MeshSecurityMonitor_Factory(Provider<AuditLogDao> auditLogDaoProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.auditLogDaoProvider = auditLogDaoProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public MeshSecurityMonitor get() {
    return newInstance(auditLogDaoProvider.get(), ioDispatcherProvider.get());
  }

  public static MeshSecurityMonitor_Factory create(Provider<AuditLogDao> auditLogDaoProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new MeshSecurityMonitor_Factory(auditLogDaoProvider, ioDispatcherProvider);
  }

  public static MeshSecurityMonitor newInstance(AuditLogDao auditLogDao,
      CoroutineDispatcher ioDispatcher) {
    return new MeshSecurityMonitor(auditLogDao, ioDispatcher);
  }
}
