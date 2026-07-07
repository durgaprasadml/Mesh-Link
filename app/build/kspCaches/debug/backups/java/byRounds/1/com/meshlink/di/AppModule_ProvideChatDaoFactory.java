package com.meshlink.di;

import com.meshlink.data.local.ChatDao;
import com.meshlink.data.local.MeshDatabase;
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
public final class AppModule_ProvideChatDaoFactory implements Factory<ChatDao> {
  private final Provider<MeshDatabase> dbProvider;

  public AppModule_ProvideChatDaoFactory(Provider<MeshDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ChatDao get() {
    return provideChatDao(dbProvider.get());
  }

  public static AppModule_ProvideChatDaoFactory create(Provider<MeshDatabase> dbProvider) {
    return new AppModule_ProvideChatDaoFactory(dbProvider);
  }

  public static ChatDao provideChatDao(MeshDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatDao(db));
  }
}
