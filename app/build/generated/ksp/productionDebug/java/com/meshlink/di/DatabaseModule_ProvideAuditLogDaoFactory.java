package com.meshlink.di;

import com.meshlink.database.data.local.AuditLogDao;
import com.meshlink.database.data.local.MeshDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DatabaseModule_ProvideAuditLogDaoFactory implements Factory<AuditLogDao> {
  private final Provider<MeshDatabase> dbProvider;

  public DatabaseModule_ProvideAuditLogDaoFactory(Provider<MeshDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AuditLogDao get() {
    return provideAuditLogDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAuditLogDaoFactory create(Provider<MeshDatabase> dbProvider) {
    return new DatabaseModule_ProvideAuditLogDaoFactory(dbProvider);
  }

  public static AuditLogDao provideAuditLogDao(MeshDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAuditLogDao(db));
  }
}
