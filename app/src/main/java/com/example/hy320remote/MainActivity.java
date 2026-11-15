package com.example.hy320remote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class MainActivity extends Activity {
 EditText ip,port,command; TextView status; final ExecutorService worker=Executors.newSingleThreadExecutor(); final Handler handler=new Handler(Looper.getMainLooper());
 int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 LinearLayout layout; Button add(String title,Runnable action){Button b=new Button(this);b.setText(title);layout.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->action.run());return b;}
 void label(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(17);t.setPadding(0,dp(12),0,dp(8));layout.addView(t);}
 @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView sc=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(dp(20),dp(16),dp(20),dp(30));sc.addView(layout);setContentView(sc);
 sc.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
 if(Intent.ACTION_SEND.equals(getIntent().getAction())){String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT);getIntent().putExtra("shared_link",shared);}
 label("HY320 Remote · Wi-Fi ADB");ip=new EditText(this);ip.setSingleLine(true);ip.setHint("IP проектора, наприклад 192.168.1.166");ip.setText(getPreferences(0).getString("ip","192.168.1.166"));layout.addView(ip);
 port=new EditText(this);port.setInputType(2);port.setText(getPreferences(0).getString("port","5555"));layout.addView(port);
 status=new TextView(this);status.setText("Телефон і проектор мають бути в одній Wi-Fi мережі. Увімкни ADB debugging на проекторі.");status.setTextIsSelectable(true);status.setPadding(0,dp(10),0,dp(10));layout.addView(status);
 add("Перевірити ADB",()->run("echo HY320_connected"));add("YouTube",()->run("am start -a android.intent.action.VIEW -d 'https://www.youtube.com'"));
 add("Гучніше",()->run("input keyevent 24"));add("Тихіше",()->run("input keyevent 25"));add("Пауза / відтворення",()->run("input keyevent 85"));add("Додому",()->run("input keyevent 3"));add("Назад",()->run("input keyevent 4"));add("Сон (екран)",()->run("input keyevent 223"));
 add("↑",()->run("input keyevent 19"));add("←",()->run("input keyevent 21"));add("OK",()->run("input keyevent 23"));add("→",()->run("input keyevent 22"));add("↓",()->run("input keyevent 20"));
 add("Скасувати таймер",()->{SleepReceiver.cancel(this);status.setText("Таймер скасовано");});
 label("Команда: посилання на YouTube або «вимкни через 60 хв»");command=new EditText(this);command.setHint("Встав посилання або напиши команду");command.setMinLines(2);layout.addView(command);add("Виконати",this::parse);String shared=getIntent().getStringExtra("shared_link");if(shared!=null)command.setText(shared);
 label("Увага: сон Android не гарантує вимкнення лампи проектора. Таймер працює через системний будильник. Android може затримати його у режимі економії енергії. Примусова зупинка застосунку або перезавантаження телефона скасовують таймер.");
 }
 void parse(){String s=command.getText().toString().trim();Matcher m=Pattern.compile("https?://(?:www\\.)?(?:youtube\\.com|youtu\\.be)/[^\\s]+",Pattern.CASE_INSENSITIVE).matcher(s);
 if(m.find()){String url=m.group().replace("'","");run("am start -a android.intent.action.VIEW -d '"+url+"'");Matcher t=Pattern.compile("(?:через|на)\\s*(\\d+)\\s*(хв|год)",Pattern.CASE_INSENSITIVE).matcher(s);if(t.find())schedule(timerMinutes(t.group(1),t.group(2)));return;}
 Matcher t=Pattern.compile("(\\d+)\\s*(хв|год)",Pattern.CASE_INSENSITIVE).matcher(s);if(t.find()&&(s.toLowerCase().contains("вимк")||s.toLowerCase().contains("сон"))){schedule(timerMinutes(t.group(1),t.group(2)));return;}
 String l=s.toLowerCase();if(l.contains("гучніш"))run("input keyevent 24");else if(l.contains("тихіш"))run("input keyevent 25");else if(l.contains("пауз"))run("input keyevent 85");else if(l.contains("додому"))run("input keyevent 3");else status.setText("Не розпізнано. Встав YouTube-посилання або напиши «вимкни через 30 хв». ");}
 int timerMinutes(String number,String unit){try{long value=Long.parseLong(number)*(unit.equalsIgnoreCase("год")?60L:1L);return value>=1&&value<=1440?(int)value:0;}catch(NumberFormatException e){return 0;}}
 void schedule(int mins){
  if(mins<1||mins>1440){status.setText("Таймер: 1–1440 хвилин");return;}
  try{String host=ip.getText().toString().trim();int n=Integer.parseInt(port.getText().toString().trim());
   if(!host.matches("[a-zA-Z0-9.:-]+")||n<1||n>65535)throw new IllegalArgumentException();
   SleepReceiver.schedule(this,host,n,mins);status.setText("Таймер сну: приблизно "+mins+" хв. Телефон має залишатися в мережі проектора.");
  }catch(Exception e){status.setText("Перевір IP-адресу та порт");}
 }

 void run(String cmd){String host=ip.getText().toString().trim(),p=port.getText().toString().trim();int n;try{n=Integer.parseInt(p);}catch(Exception e){status.setText("Неправильний порт");return;}
 getPreferences(0).edit().putString("ip",host).putString("port",p).apply();status.setText("Виконую…");worker.execute(()->{String result;try{result=new AdbClient(this).shell(host,n,cmd);if(result.isEmpty())result="Команду надіслано";}catch(Exception e){result="Помилка: "+e.getMessage()+". Перевір ADB, порт і підтвердження RSA на проекторі.";}String output=result;runOnUiThread(()->status.setText(output));});}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
