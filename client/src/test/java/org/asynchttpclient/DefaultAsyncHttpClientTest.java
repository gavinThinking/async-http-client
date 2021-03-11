package org.asynchttpclient;

import io.netty.util.Timer;
import org.asynchttpclient.cookie.CookieEvictionTask;
import org.asynchttpclient.cookie.CookieStore;
import org.asynchttpclient.cookie.ThreadSafeCookieStore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class DefaultAsyncHttpClientTest {

  @Test
  public void testWithSharedNettyTimerShouldScheduleCookieEvictionOnlyOnce() throws IOException {
    Timer nettyTimerMock = mock(Timer.class);
    CookieStore cookieStore = new ThreadSafeCookieStore();
    AsyncHttpClientConfig config = Dsl.config().setNettyTimer(nettyTimerMock).setCookieStore(cookieStore).build();

    try (AsyncHttpClient client1 = Dsl.asyncHttpClient(config)) {
      try (AsyncHttpClient client2 = Dsl.asyncHttpClient(config)) {
        assertEquals(2, cookieStore.count());
        verify(nettyTimerMock, times(1)).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
      }
    }
  }

  @Test
  public void testWitDefaultConfigShouldScheduleCookieEvictionForEachAHC() throws IOException {
    AsyncHttpClientConfig config1 = Dsl.config().build();
    try (AsyncHttpClient client1 = Dsl.asyncHttpClient(config1)) {
      AsyncHttpClientConfig config2 = Dsl.config().build();
      try (AsyncHttpClient client2 = Dsl.asyncHttpClient(config2)) {
        assertEquals(config1.getCookieStore().count(), 1);
        assertEquals(config2.getCookieStore().count(), 1);
      }
    }
  }

  @Test
  public void testWithSharedCookieStoreButNonSharedTimerShouldScheduleCookieEvictionForFirstAHC() throws IOException {
    CookieStore cookieStore = new ThreadSafeCookieStore();
    Timer nettyTimerMock1 = mock(Timer.class);
    AsyncHttpClientConfig config1 = Dsl.config()
      .setCookieStore(cookieStore).setNettyTimer(nettyTimerMock1).build();

    try (AsyncHttpClient client1 = Dsl.asyncHttpClient(config1)) {
      Timer nettyTimerMock2 = mock(Timer.class);
      AsyncHttpClientConfig config2 = Dsl.config()
        .setCookieStore(cookieStore).setNettyTimer(nettyTimerMock2).build();
      try (AsyncHttpClient client2 = Dsl.asyncHttpClient(config2)) {
        assertEquals(config1.getCookieStore().count(), 2);
        verify(nettyTimerMock1, times(1)).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
        verify(nettyTimerMock2, never()).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
      }
    }

    Timer nettyTimerMock3 = mock(Timer.class);
    AsyncHttpClientConfig config3 = Dsl.config()
      .setCookieStore(cookieStore).setNettyTimer(nettyTimerMock3).build();

    try (AsyncHttpClient client2 = Dsl.asyncHttpClient(config3)) {
      assertEquals(config1.getCookieStore().count(), 1);
      verify(nettyTimerMock3, times(1)).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
    }
  }

  @Test
  public void testWithSharedCookieStoreButNonSharedTimerShouldReScheduleCookieEvictionWhenFirstInstanceGetClosed() throws IOException {
    CookieStore cookieStore = new ThreadSafeCookieStore();
    Timer nettyTimerMock1 = mock(Timer.class);
    AsyncHttpClientConfig config1 = Dsl.config()
      .setCookieStore(cookieStore).setNettyTimer(nettyTimerMock1).build();

    try (AsyncHttpClient client1 = Dsl.asyncHttpClient(config1)) {
      assertEquals(config1.getCookieStore().count(), 1);
      verify(nettyTimerMock1, times(1)).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
    }

    assertEquals(config1.getCookieStore().count(), 0);

    Timer nettyTimerMock2 = mock(Timer.class);
    AsyncHttpClientConfig config2 = Dsl.config()
      .setCookieStore(cookieStore).setNettyTimer(nettyTimerMock2).build();

    try (AsyncHttpClient client2 = Dsl.asyncHttpClient(config2)) {
      assertEquals(config1.getCookieStore().count(), 1);
      verify(nettyTimerMock2, times(1)).newTimeout(any(CookieEvictionTask.class), anyLong(), any(TimeUnit.class));
    }
  }

  @Test
  public void testDisablingCookieStore() throws IOException {
    AsyncHttpClientConfig config = Dsl.config()
      .setCookieStore(null).build();
    try (AsyncHttpClient client = Dsl.asyncHttpClient(config)) {
      //
    }
  }
}
