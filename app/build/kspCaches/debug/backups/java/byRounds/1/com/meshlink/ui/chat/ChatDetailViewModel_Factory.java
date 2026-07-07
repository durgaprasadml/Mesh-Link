package com.meshlink.ui.chat;

import androidx.lifecycle.SavedStateHandle;
import com.meshlink.data.local.ChatDao;
import com.meshlink.data.media.VoicePlayer;
import com.meshlink.data.media.VoiceRecorder;
import com.meshlink.data.repository.BleRepository;
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
public final class ChatDetailViewModel_Factory implements Factory<ChatDetailViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<BleRepository> bleRepositoryProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<VoiceRecorder> voiceRecorderProvider;

  private final Provider<VoicePlayer> voicePlayerProvider;

  public ChatDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<BleRepository> bleRepositoryProvider, Provider<ChatDao> chatDaoProvider,
      Provider<VoiceRecorder> voiceRecorderProvider, Provider<VoicePlayer> voicePlayerProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.bleRepositoryProvider = bleRepositoryProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.voiceRecorderProvider = voiceRecorderProvider;
    this.voicePlayerProvider = voicePlayerProvider;
  }

  @Override
  public ChatDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), bleRepositoryProvider.get(), chatDaoProvider.get(), voiceRecorderProvider.get(), voicePlayerProvider.get());
  }

  public static ChatDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<BleRepository> bleRepositoryProvider, Provider<ChatDao> chatDaoProvider,
      Provider<VoiceRecorder> voiceRecorderProvider, Provider<VoicePlayer> voicePlayerProvider) {
    return new ChatDetailViewModel_Factory(savedStateHandleProvider, bleRepositoryProvider, chatDaoProvider, voiceRecorderProvider, voicePlayerProvider);
  }

  public static ChatDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      BleRepository bleRepository, ChatDao chatDao, VoiceRecorder voiceRecorder,
      VoicePlayer voicePlayer) {
    return new ChatDetailViewModel(savedStateHandle, bleRepository, chatDao, voiceRecorder, voicePlayer);
  }
}
