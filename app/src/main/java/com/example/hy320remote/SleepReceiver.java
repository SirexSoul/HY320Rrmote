package com.example.hy320remote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;

public final class SleepReceiver extends BroadcastReceiver {
 private static PendingIntent pending(Context c) {
  return PendingIntent.getBroadcast(c, 1, new Intent(c, SleepReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
 }
 public static void schedule(Context c, String host, int port, int minutes) {
  c.getSharedPreferences("sleep",0).edit().putString("host",host).putInt("port",port).apply();
  AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
  a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+minutes*60000L,pending(c));
 }
 public static void cancel(Context c) {
  ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c));
 }
 @Override public void onReceive(Context context, Intent intent) {
  Context c=context.getApplicationContext();
  PendingResult result=goAsync();
  PowerManager.WakeLock lock=((PowerManager)c.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"HY320Remote:Sleep");
  lock.acquire(9000);
  new Thread(()->{
   try {
    android.content.SharedPreferences p=c.getSharedPreferences("sleep",0);
    new AdbClient(c).shell(p.getString("host",""),p.getInt("port",5555),"input keyevent 223",2000);
    p.edit().putString("last_result","Команду сну надіслано").apply();
   } catch(Exception e) {
    c.getSharedPreferences("sleep",0).edit().putString("last_result","Таймер: "+e.getMessage()).apply();
   } finally {if(lock.isHeld())lock.release();result.finish();}
  }).start();
 }
}
