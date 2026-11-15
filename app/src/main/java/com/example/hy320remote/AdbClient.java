package com.example.hy320remote;

import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.math.BigInteger;

/** Minimal classic ADB-over-TCP client. First connection requires confirmation on target. */
public final class AdbClient {
 private static final int CNXN=0x4e584e43, AUTH=0x48545541, OPEN=0x4e45504f, OKAY=0x59414b4f, WRTE=0x45545257, CLSE=0x45534c43;
 private final Context context;
 public AdbClient(Context c){context=c.getApplicationContext();}
 private static class Msg {int cmd,a,b;byte[] data; Msg(int c,int x,int y,byte[] d){cmd=c;a=x;b=y;data=d;}}
 private static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
 private static byte[] nul(String s){return bytes(s+"\0");}
 private static void write(OutputStream out,int cmd,int a,int b,byte[] payload)throws IOException {
  if(payload==null)payload=new byte[0]; int sum=0;for(byte x:payload)sum+=(x&255);
  ByteBuffer h=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);h.putInt(cmd).putInt(a).putInt(b).putInt(payload.length).putInt(sum).putInt(cmd^(-1));out.write(h.array());out.write(payload);out.flush();
 }
 private static int readInt(InputStream in)throws IOException {byte[] b=readN(in,4);return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();}
 private static byte[] readN(InputStream in,int n)throws IOException {if(n<0||n>1024*1024)throw new IOException("Invalid ADB payload size");byte[] b=new byte[n];int i=0;while(i<n){int r=in.read(b,i,n-i);if(r<0)throw new EOFException();i+=r;}return b;}
 private static Msg read(InputStream in)throws IOException {int cmd=readInt(in),a=readInt(in),b=readInt(in),len=readInt(in),sum=readInt(in),magic=readInt(in);if((cmd^(-1))!=magic)throw new IOException("Bad ADB header");byte[] d=readN(in,len);int s=0;for(byte x:d)s+=(x&255);if(s!=sum)throw new IOException("Bad ADB checksum");return new Msg(cmd,a,b,d);}
 private KeyPair key()throws Exception {
  android.content.SharedPreferences p=context.getSharedPreferences("adb",0);String priv=p.getString("private",null),pub=p.getString("public",null);
  KeyFactory f=KeyFactory.getInstance("RSA");if(priv!=null&&pub!=null)return new KeyPair(f.generatePublic(new X509EncodedKeySpec(Base64.decode(pub,0))),f.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(priv,0))));
  KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);KeyPair k=g.generateKeyPair();p.edit().putString("private",Base64.encodeToString(k.getPrivate().getEncoded(),Base64.NO_WRAP)).putString("public",Base64.encodeToString(k.getPublic().getEncoded(),Base64.NO_WRAP)).apply();return k;
 }
 private static byte[] adbPublic(RSAPublicKey rsa){
  BigInteger n=rsa.getModulus(),e=rsa.getPublicExponent(),two=BigInteger.ONE.shiftLeft(32);
  long n0=n.and(two.subtract(BigInteger.ONE)).longValue();long inv=BigInteger.valueOf(n0).modInverse(two).longValue();long n0inv=(-inv)&0xffffffffL;
  BigInteger rr=BigInteger.ONE.shiftLeft(4096).mod(n);
  ByteBuffer b=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);b.putInt(64).putInt((int)n0inv);
  putLE(b,n,256);putLE(b,rr,256);b.putInt(e.intValue());return b.array();
 }
 private static void putLE(ByteBuffer b,BigInteger n,int len){byte[] v=n.toByteArray();for(int i=0;i<len;i++){int j=v.length-1-i;b.put(j>=0?v[j]:(byte)0);}}
 private static byte[] publicPayload(KeyPair k){String v=Base64.encodeToString(adbPublic((RSAPublicKey)k.getPublic()),Base64.NO_WRAP)+" hy320-remote@android\0";return bytes(v);}
 private static byte[] signToken(PrivateKey k,byte[] token)throws Exception {
  // ADB AUTH expects PKCS#1 v1.5 signature over the SHA-1 digest token.
  byte[] digestInfo=new byte[35];byte[] prefix=new byte[]{0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};System.arraycopy(prefix,0,digestInfo,0,prefix.length);System.arraycopy(token,0,digestInfo,prefix.length,Math.min(20,token.length));
  javax.crypto.Cipher c=javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");c.init(javax.crypto.Cipher.ENCRYPT_MODE,k);return c.doFinal(digestInfo);
 }
 public String shell(String host,int port,String command)throws Exception {return shell(host,port,command,12000);}
 public String shell(String host,int port,String command,int timeout)throws Exception {
  if(!host.matches("[a-zA-Z0-9.:-]+")||port<1||port>65535)throw new IllegalArgumentException("Неправильна IP-адреса або порт");
  try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(host,port),5000);socket.setSoTimeout(timeout);InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();KeyPair k=key();
   write(out,CNXN,0x01000000,4096,nul("host::hy320-remote"));boolean signed=false,publicSent=false;
   for(int i=0;i<12;i++){Msg m=read(in);if(m.cmd==CNXN)break;
    if(m.cmd!=AUTH)throw new IOException("Неочікувана відповідь ADB");
    if(m.a!=1)throw new IOException("Невідомий тип авторизації");
    if(!signed){write(out,AUTH,2,0,signToken(k.getPrivate(),m.data));signed=true;}
    else if(!publicSent){write(out,AUTH,3,0,publicPayload(k));publicSent=true;socket.setSoTimeout(timeout==12000?30000:timeout);}
    else throw new IOException("Підтверди RSA-доступ на проекторі та повтори підключення");
    if(i==11)throw new IOException("Не вдалося авторизувати ADB");
   }
   int local=1,remote=0;write(out,OPEN,local,0,nul("shell:"+command));StringBuilder result=new StringBuilder();
   while(true){Msg m=read(in);if(m.cmd==OKAY){remote=m.a;continue;}if(m.cmd==WRTE){result.append(new String(m.data,StandardCharsets.UTF_8));write(out,OKAY,local,m.a,new byte[0]);continue;}if(m.cmd==CLSE){write(out,CLSE,local,m.a,new byte[0]);return result.toString();}throw new IOException("ADB shell error: "+m.cmd);}
  }
 }
}
