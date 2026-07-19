package com.meshlink.di;

import com.meshlink.database.data.local.MeshDatabase;
import com.meshlink.database.data.local.RelayDao;
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
public final class DatabaseModule_ProvideRelayDaoFactory implements Factory<RelayDao> {
  private final Provider<MeshDatabase> dbProvider;

  public DatabaseModule_ProvideRelayDaoFactory(Provider<MeshDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public RelayDao get() {
    return provideRelayDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideRelayDaoFactory create(Provider<MeshDatabase> dbProvider) {
    return new DatabaseModule_ProvideRelayDaoFactory(dbProvider);
  }

  public static RelayDao provideRelayDao(MeshDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideRelayDao(db));
  }
}
