package com.meshlink.messaging.data;

import com.meshlink.database.data.source.ChatLocalDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MessagingRepositoryImpl_Factory implements Factory<MessagingRepositoryImpl> {
  private final Provider<ChatLocalDataSource> chatLocalDataSourceProvider;

  public MessagingRepositoryImpl_Factory(
      Provider<ChatLocalDataSource> chatLocalDataSourceProvider) {
    this.chatLocalDataSourceProvider = chatLocalDataSourceProvider;
  }

  @Override
  public MessagingRepositoryImpl get() {
    return newInstance(chatLocalDataSourceProvider.get());
  }

  public static MessagingRepositoryImpl_Factory create(
      Provider<ChatLocalDataSource> chatLocalDataSourceProvider) {
    return new MessagingRepositoryImpl_Factory(chatLocalDataSourceProvider);
  }

  public static MessagingRepositoryImpl newInstance(ChatLocalDataSource chatLocalDataSource) {
    return new MessagingRepositoryImpl(chatLocalDataSource);
  }
}
