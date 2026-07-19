package com.meshlink.di;

import com.meshlink.database.data.local.MeshDatabase;
import com.meshlink.database.data.local.TrustDao;
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
public final class DatabaseModule_ProvideTrustDaoFactory implements Factory<TrustDao> {
  private final Provider<MeshDatabase> dbProvider;

  public DatabaseModule_ProvideTrustDaoFactory(Provider<MeshDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TrustDao get() {
    return provideTrustDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideTrustDaoFactory create(Provider<MeshDatabase> dbProvider) {
    return new DatabaseModule_ProvideTrustDaoFactory(dbProvider);
  }

  public static TrustDao provideTrustDao(MeshDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideTrustDao(db));
  }
}
