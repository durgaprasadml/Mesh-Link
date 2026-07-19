package com.meshlink.database.data.source;

import com.meshlink.database.data.local.ChatDao;
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
public final class ChatLocalDataSourceImpl_Factory implements Factory<ChatLocalDataSourceImpl> {
  private final Provider<ChatDao> chatDaoProvider;

  public ChatLocalDataSourceImpl_Factory(Provider<ChatDao> chatDaoProvider) {
    this.chatDaoProvider = chatDaoProvider;
  }

  @Override
  public ChatLocalDataSourceImpl get() {
    return newInstance(chatDaoProvider.get());
  }

  public static ChatLocalDataSourceImpl_Factory create(Provider<ChatDao> chatDaoProvider) {
    return new ChatLocalDataSourceImpl_Factory(chatDaoProvider);
  }

  public static ChatLocalDataSourceImpl newInstance(ChatDao chatDao) {
    return new ChatLocalDataSourceImpl(chatDao);
  }
}
