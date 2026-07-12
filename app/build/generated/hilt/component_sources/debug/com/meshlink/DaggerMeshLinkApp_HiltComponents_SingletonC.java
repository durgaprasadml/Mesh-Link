package com.meshlink;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.meshlink.data.analytics.MeshAnalytics;
import com.meshlink.data.ble.BleAdvertiserManager;
import com.meshlink.data.ble.BleGattManager;
import com.meshlink.data.ble.BleScannerManager;
import com.meshlink.data.ble.MeshRouter;
import com.meshlink.data.crypto.MeshCryptoManager;
import com.meshlink.data.local.ChatDao;
import com.meshlink.data.local.MeshDatabase;
import com.meshlink.data.local.RelayDao;
import com.meshlink.data.local.UserDao;
import com.meshlink.data.location.LocationProvider;
import com.meshlink.data.media.MediaTransferManager;
import com.meshlink.data.media.VoicePlayer;
import com.meshlink.data.media.VoiceRecorder;
import com.meshlink.data.repository.BleRepository;
import com.meshlink.data.wifi.WifiDirectManager;
import com.meshlink.di.AppModule_ProvideBleAdvertiserManagerFactory;
import com.meshlink.di.AppModule_ProvideBleGattManagerFactory;
import com.meshlink.di.AppModule_ProvideBleScannerManagerFactory;
import com.meshlink.di.AppModule_ProvideChatDaoFactory;
import com.meshlink.di.AppModule_ProvideDataStoreFactory;
import com.meshlink.di.AppModule_ProvideLocationProviderFactory;
import com.meshlink.di.AppModule_ProvideMediaTransferManagerFactory;
import com.meshlink.di.AppModule_ProvideMeshAnalyticsFactory;
import com.meshlink.di.AppModule_ProvideMeshCryptoManagerFactory;
import com.meshlink.di.AppModule_ProvideMeshDatabaseFactory;
import com.meshlink.di.AppModule_ProvideRelayDaoFactory;
import com.meshlink.di.AppModule_ProvideUserDaoFactory;
import com.meshlink.di.AppModule_ProvideUserRepositoryFactory;
import com.meshlink.di.AppModule_ProvideVoicePlayerFactory;
import com.meshlink.di.AppModule_ProvideVoiceRecorderFactory;
import com.meshlink.di.AppModule_ProvideWifiDirectManagerFactory;
import com.meshlink.domain.repository.UserRepository;
import com.meshlink.service.MeshRelayService;
import com.meshlink.service.MeshRelayService_MembersInjector;
import com.meshlink.ui.analytics.AnalyticsViewModel;
import com.meshlink.ui.analytics.AnalyticsViewModel_HiltModules;
import com.meshlink.ui.auth.AuthViewModel;
import com.meshlink.ui.auth.AuthViewModel_HiltModules;
import com.meshlink.ui.broadcast.BroadcastViewModel;
import com.meshlink.ui.broadcast.BroadcastViewModel_HiltModules;
import com.meshlink.ui.chat.ChatDetailViewModel;
import com.meshlink.ui.chat.ChatDetailViewModel_HiltModules;
import com.meshlink.ui.chat.ChatsListViewModel;
import com.meshlink.ui.chat.ChatsListViewModel_HiltModules;
import com.meshlink.ui.home.HomeViewModel;
import com.meshlink.ui.home.HomeViewModel_HiltModules;
import com.meshlink.ui.mesh.MeshDebugViewModel;
import com.meshlink.ui.mesh.MeshDebugViewModel_HiltModules;
import com.meshlink.ui.nearby.NearbyViewModel;
import com.meshlink.ui.nearby.NearbyViewModel_HiltModules;
import com.meshlink.ui.sos.SosViewModel;
import com.meshlink.ui.sos.SosViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerMeshLinkApp_HiltComponents_SingletonC {
  private DaggerMeshLinkApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public MeshLinkApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements MeshLinkApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements MeshLinkApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements MeshLinkApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements MeshLinkApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements MeshLinkApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements MeshLinkApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements MeshLinkApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public MeshLinkApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends MeshLinkApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends MeshLinkApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends MeshLinkApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends MeshLinkApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(9).put(LazyClassKeyProvider.com_meshlink_ui_analytics_AnalyticsViewModel, AnalyticsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_auth_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_broadcast_BroadcastViewModel, BroadcastViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_chat_ChatDetailViewModel, ChatDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_chat_ChatsListViewModel, ChatsListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_mesh_MeshDebugViewModel, MeshDebugViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_nearby_NearbyViewModel, NearbyViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_meshlink_ui_sos_SosViewModel, SosViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectBleRepository(instance, singletonCImpl.bleRepositoryProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_meshlink_ui_nearby_NearbyViewModel = "com.meshlink.ui.nearby.NearbyViewModel";

      static String com_meshlink_ui_mesh_MeshDebugViewModel = "com.meshlink.ui.mesh.MeshDebugViewModel";

      static String com_meshlink_ui_sos_SosViewModel = "com.meshlink.ui.sos.SosViewModel";

      static String com_meshlink_ui_home_HomeViewModel = "com.meshlink.ui.home.HomeViewModel";

      static String com_meshlink_ui_auth_AuthViewModel = "com.meshlink.ui.auth.AuthViewModel";

      static String com_meshlink_ui_chat_ChatsListViewModel = "com.meshlink.ui.chat.ChatsListViewModel";

      static String com_meshlink_ui_analytics_AnalyticsViewModel = "com.meshlink.ui.analytics.AnalyticsViewModel";

      static String com_meshlink_ui_broadcast_BroadcastViewModel = "com.meshlink.ui.broadcast.BroadcastViewModel";

      static String com_meshlink_ui_chat_ChatDetailViewModel = "com.meshlink.ui.chat.ChatDetailViewModel";

      @KeepFieldType
      NearbyViewModel com_meshlink_ui_nearby_NearbyViewModel2;

      @KeepFieldType
      MeshDebugViewModel com_meshlink_ui_mesh_MeshDebugViewModel2;

      @KeepFieldType
      SosViewModel com_meshlink_ui_sos_SosViewModel2;

      @KeepFieldType
      HomeViewModel com_meshlink_ui_home_HomeViewModel2;

      @KeepFieldType
      AuthViewModel com_meshlink_ui_auth_AuthViewModel2;

      @KeepFieldType
      ChatsListViewModel com_meshlink_ui_chat_ChatsListViewModel2;

      @KeepFieldType
      AnalyticsViewModel com_meshlink_ui_analytics_AnalyticsViewModel2;

      @KeepFieldType
      BroadcastViewModel com_meshlink_ui_broadcast_BroadcastViewModel2;

      @KeepFieldType
      ChatDetailViewModel com_meshlink_ui_chat_ChatDetailViewModel2;
    }
  }

  private static final class ViewModelCImpl extends MeshLinkApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AnalyticsViewModel> analyticsViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<BroadcastViewModel> broadcastViewModelProvider;

    private Provider<ChatDetailViewModel> chatDetailViewModelProvider;

    private Provider<ChatsListViewModel> chatsListViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<MeshDebugViewModel> meshDebugViewModelProvider;

    private Provider<NearbyViewModel> nearbyViewModelProvider;

    private Provider<SosViewModel> sosViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.analyticsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.broadcastViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.chatDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.chatsListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.meshDebugViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.nearbyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.sosViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(9).put(LazyClassKeyProvider.com_meshlink_ui_analytics_AnalyticsViewModel, ((Provider) analyticsViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_auth_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_broadcast_BroadcastViewModel, ((Provider) broadcastViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_chat_ChatDetailViewModel, ((Provider) chatDetailViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_chat_ChatsListViewModel, ((Provider) chatsListViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_mesh_MeshDebugViewModel, ((Provider) meshDebugViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_nearby_NearbyViewModel, ((Provider) nearbyViewModelProvider)).put(LazyClassKeyProvider.com_meshlink_ui_sos_SosViewModel, ((Provider) sosViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_meshlink_ui_home_HomeViewModel = "com.meshlink.ui.home.HomeViewModel";

      static String com_meshlink_ui_sos_SosViewModel = "com.meshlink.ui.sos.SosViewModel";

      static String com_meshlink_ui_broadcast_BroadcastViewModel = "com.meshlink.ui.broadcast.BroadcastViewModel";

      static String com_meshlink_ui_chat_ChatsListViewModel = "com.meshlink.ui.chat.ChatsListViewModel";

      static String com_meshlink_ui_mesh_MeshDebugViewModel = "com.meshlink.ui.mesh.MeshDebugViewModel";

      static String com_meshlink_ui_analytics_AnalyticsViewModel = "com.meshlink.ui.analytics.AnalyticsViewModel";

      static String com_meshlink_ui_nearby_NearbyViewModel = "com.meshlink.ui.nearby.NearbyViewModel";

      static String com_meshlink_ui_auth_AuthViewModel = "com.meshlink.ui.auth.AuthViewModel";

      static String com_meshlink_ui_chat_ChatDetailViewModel = "com.meshlink.ui.chat.ChatDetailViewModel";

      @KeepFieldType
      HomeViewModel com_meshlink_ui_home_HomeViewModel2;

      @KeepFieldType
      SosViewModel com_meshlink_ui_sos_SosViewModel2;

      @KeepFieldType
      BroadcastViewModel com_meshlink_ui_broadcast_BroadcastViewModel2;

      @KeepFieldType
      ChatsListViewModel com_meshlink_ui_chat_ChatsListViewModel2;

      @KeepFieldType
      MeshDebugViewModel com_meshlink_ui_mesh_MeshDebugViewModel2;

      @KeepFieldType
      AnalyticsViewModel com_meshlink_ui_analytics_AnalyticsViewModel2;

      @KeepFieldType
      NearbyViewModel com_meshlink_ui_nearby_NearbyViewModel2;

      @KeepFieldType
      AuthViewModel com_meshlink_ui_auth_AuthViewModel2;

      @KeepFieldType
      ChatDetailViewModel com_meshlink_ui_chat_ChatDetailViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.meshlink.ui.analytics.AnalyticsViewModel 
          return (T) new AnalyticsViewModel(singletonCImpl.provideMeshAnalyticsProvider.get(), singletonCImpl.meshRouterProvider.get());

          case 1: // com.meshlink.ui.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideUserRepositoryProvider.get());

          case 2: // com.meshlink.ui.broadcast.BroadcastViewModel 
          return (T) new BroadcastViewModel(singletonCImpl.bleRepositoryProvider.get(), singletonCImpl.provideChatDaoProvider.get());

          case 3: // com.meshlink.ui.chat.ChatDetailViewModel 
          return (T) new ChatDetailViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.bleRepositoryProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideVoiceRecorderProvider.get(), singletonCImpl.provideVoicePlayerProvider.get());

          case 4: // com.meshlink.ui.chat.ChatsListViewModel 
          return (T) new ChatsListViewModel(singletonCImpl.provideChatDaoProvider.get());

          case 5: // com.meshlink.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.bleRepositoryProvider.get(), singletonCImpl.provideChatDaoProvider.get());

          case 6: // com.meshlink.ui.mesh.MeshDebugViewModel 
          return (T) new MeshDebugViewModel(singletonCImpl.bleRepositoryProvider.get());

          case 7: // com.meshlink.ui.nearby.NearbyViewModel 
          return (T) new NearbyViewModel(singletonCImpl.bleRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideWifiDirectManagerProvider.get());

          case 8: // com.meshlink.ui.sos.SosViewModel 
          return (T) new SosViewModel(singletonCImpl.bleRepositoryProvider.get(), singletonCImpl.provideLocationProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends MeshLinkApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends MeshLinkApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectMeshRelayService(MeshRelayService meshRelayService) {
      injectMeshRelayService2(meshRelayService);
    }

    @CanIgnoreReturnValue
    private MeshRelayService injectMeshRelayService2(MeshRelayService instance) {
      MeshRelayService_MembersInjector.injectBleRepository(instance, singletonCImpl.bleRepositoryProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends MeshLinkApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<BleScannerManager> provideBleScannerManagerProvider;

    private Provider<BleAdvertiserManager> provideBleAdvertiserManagerProvider;

    private Provider<BleGattManager> provideBleGattManagerProvider;

    private Provider<WifiDirectManager> provideWifiDirectManagerProvider;

    private Provider<MeshAnalytics> provideMeshAnalyticsProvider;

    private Provider<MeshDatabase> provideMeshDatabaseProvider;

    private Provider<RelayDao> provideRelayDaoProvider;

    private Provider<MeshRouter> meshRouterProvider;

    private Provider<ChatDao> provideChatDaoProvider;

    private Provider<UserDao> provideUserDaoProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<UserRepository> provideUserRepositoryProvider;

    private Provider<MediaTransferManager> provideMediaTransferManagerProvider;

    private Provider<LocationProvider> provideLocationProvider;

    private Provider<MeshCryptoManager> provideMeshCryptoManagerProvider;

    private Provider<BleRepository> bleRepositoryProvider;

    private Provider<VoiceRecorder> provideVoiceRecorderProvider;

    private Provider<VoicePlayer> provideVoicePlayerProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideBleScannerManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleScannerManager>(singletonCImpl, 1));
      this.provideBleAdvertiserManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleAdvertiserManager>(singletonCImpl, 2));
      this.provideBleGattManagerProvider = DoubleCheck.provider(new SwitchingProvider<BleGattManager>(singletonCImpl, 3));
      this.provideWifiDirectManagerProvider = DoubleCheck.provider(new SwitchingProvider<WifiDirectManager>(singletonCImpl, 5));
      this.provideMeshAnalyticsProvider = DoubleCheck.provider(new SwitchingProvider<MeshAnalytics>(singletonCImpl, 6));
      this.provideMeshDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<MeshDatabase>(singletonCImpl, 8));
      this.provideRelayDaoProvider = DoubleCheck.provider(new SwitchingProvider<RelayDao>(singletonCImpl, 7));
      this.meshRouterProvider = DoubleCheck.provider(new SwitchingProvider<MeshRouter>(singletonCImpl, 4));
      this.provideChatDaoProvider = DoubleCheck.provider(new SwitchingProvider<ChatDao>(singletonCImpl, 9));
      this.provideUserDaoProvider = DoubleCheck.provider(new SwitchingProvider<UserDao>(singletonCImpl, 11));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 12));
      this.provideUserRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 10));
      this.provideMediaTransferManagerProvider = DoubleCheck.provider(new SwitchingProvider<MediaTransferManager>(singletonCImpl, 13));
      this.provideLocationProvider = DoubleCheck.provider(new SwitchingProvider<LocationProvider>(singletonCImpl, 14));
      this.provideMeshCryptoManagerProvider = DoubleCheck.provider(new SwitchingProvider<MeshCryptoManager>(singletonCImpl, 15));
      this.bleRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BleRepository>(singletonCImpl, 0));
      this.provideVoiceRecorderProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRecorder>(singletonCImpl, 16));
      this.provideVoicePlayerProvider = DoubleCheck.provider(new SwitchingProvider<VoicePlayer>(singletonCImpl, 17));
    }

    @Override
    public void injectMeshLinkApp(MeshLinkApp meshLinkApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.meshlink.data.repository.BleRepository 
          return (T) new BleRepository(singletonCImpl.provideBleScannerManagerProvider.get(), singletonCImpl.provideBleAdvertiserManagerProvider.get(), singletonCImpl.provideBleGattManagerProvider.get(), singletonCImpl.meshRouterProvider.get(), singletonCImpl.provideChatDaoProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideMediaTransferManagerProvider.get(), singletonCImpl.provideLocationProvider.get(), singletonCImpl.provideMeshCryptoManagerProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.meshlink.data.ble.BleScannerManager 
          return (T) AppModule_ProvideBleScannerManagerFactory.provideBleScannerManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.meshlink.data.ble.BleAdvertiserManager 
          return (T) AppModule_ProvideBleAdvertiserManagerFactory.provideBleAdvertiserManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.meshlink.data.ble.BleGattManager 
          return (T) AppModule_ProvideBleGattManagerFactory.provideBleGattManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.meshlink.data.ble.MeshRouter 
          return (T) new MeshRouter(singletonCImpl.provideBleGattManagerProvider.get(), singletonCImpl.provideWifiDirectManagerProvider.get(), singletonCImpl.provideMeshAnalyticsProvider.get(), singletonCImpl.provideRelayDaoProvider.get());

          case 5: // com.meshlink.data.wifi.WifiDirectManager 
          return (T) AppModule_ProvideWifiDirectManagerFactory.provideWifiDirectManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.meshlink.data.analytics.MeshAnalytics 
          return (T) AppModule_ProvideMeshAnalyticsFactory.provideMeshAnalytics();

          case 7: // com.meshlink.data.local.RelayDao 
          return (T) AppModule_ProvideRelayDaoFactory.provideRelayDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 8: // com.meshlink.data.local.MeshDatabase 
          return (T) AppModule_ProvideMeshDatabaseFactory.provideMeshDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.meshlink.data.local.ChatDao 
          return (T) AppModule_ProvideChatDaoFactory.provideChatDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 10: // com.meshlink.domain.repository.UserRepository 
          return (T) AppModule_ProvideUserRepositoryFactory.provideUserRepository(singletonCImpl.provideUserDaoProvider.get(), singletonCImpl.provideDataStoreProvider.get());

          case 11: // com.meshlink.data.local.UserDao 
          return (T) AppModule_ProvideUserDaoFactory.provideUserDao(singletonCImpl.provideMeshDatabaseProvider.get());

          case 12: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 13: // com.meshlink.data.media.MediaTransferManager 
          return (T) AppModule_ProvideMediaTransferManagerFactory.provideMediaTransferManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // com.meshlink.data.location.LocationProvider 
          return (T) AppModule_ProvideLocationProviderFactory.provideLocationProvider(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // com.meshlink.data.crypto.MeshCryptoManager 
          return (T) AppModule_ProvideMeshCryptoManagerFactory.provideMeshCryptoManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.meshlink.data.media.VoiceRecorder 
          return (T) AppModule_ProvideVoiceRecorderFactory.provideVoiceRecorder(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 17: // com.meshlink.data.media.VoicePlayer 
          return (T) AppModule_ProvideVoicePlayerFactory.provideVoicePlayer();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
