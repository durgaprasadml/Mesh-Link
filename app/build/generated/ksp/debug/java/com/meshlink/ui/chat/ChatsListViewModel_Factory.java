package com.meshlink.ui.chat;

import com.meshlink.data.local.ChatDao;
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
public final class ChatsListViewModel_Factory implements Factory<ChatsListViewModel> {
  private final Provider<ChatDao> chatDaoProvider;

  public ChatsListViewModel_Factory(Provider<ChatDao> chatDaoProvider) {
    this.chatDaoProvider = chatDaoProvider;
  }

  @Override
  public ChatsListViewModel get() {
    return newInstance(chatDaoProvider.get());
  }

  public static ChatsListViewModel_Factory create(Provider<ChatDao> chatDaoProvider) {
    return new ChatsListViewModel_Factory(chatDaoProvider);
  }

  public static ChatsListViewModel newInstance(ChatDao chatDao) {
    return new ChatsListViewModel(chatDao);
  }
}
